package com.conest.app.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.conest.app.data.model.Diet
import com.conest.app.data.model.Drinking
import com.conest.app.data.model.Furnishing
import com.conest.app.data.model.OccupationType
import com.conest.app.data.model.RoomDetails
import com.conest.app.data.model.RoomType
import com.conest.app.data.model.UserProfile
import com.conest.app.data.model.UserType
import com.conest.app.domain.lifestyle.LifestyleAnswers
import com.conest.app.domain.lifestyle.LifestyleCategory
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

class ProfileError(val userMessage: String, cause: Throwable? = null) : Exception(userMessage, cause)

@Singleton
class FirestoreProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : ProfileRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun observeProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        // INCLUDE metadata changes: when the server merely *confirms* a cached "no such doc", that's
        // a metadata-only change — without this the listener would never fire again and we'd wait forever.
        val reg = db.collection(USERS).document(uid).addSnapshotListener(MetadataChanges.INCLUDE) { snap, error ->
            when {
                error != null -> close(error)
                snap == null -> Unit
                snap.exists() -> trySend(snap.toProfile())
                !snap.metadata.isFromCache -> trySend(null) // confirmed: no profile yet
                // else: offline cache miss — wait for the server rather than guess
            }
        }
        awaitClose { reg.remove() }
    }.distinctUntilChanged()

    override suspend fun saveProfile(profile: UserProfile, photo: Uri?, roomPhotos: List<Uri>): Result<Unit> = runCatching {
        // Photos live in Firestore (free Spark plan — no Cloud Storage). Each is its own doc so
        // the profile stays small; photos and profile are committed in one atomic batch, so a
        // failed save never leaves orphaned photos or a profile pointing at missing ones.
        val stamp = System.currentTimeMillis()
        val profilePhoto = photo?.let { PendingPhoto("${profile.uid}_p_$stamp", encode(it)) }
        val roomUploads = roomPhotos.mapIndexed { i, uri -> PendingPhoto("${profile.uid}_r_${stamp}_$i", encode(uri)) }

        val finalProfile = profile.copy(
            photoUrl = profilePhoto?.let { PhotoRef.of(it.id) } ?: profile.photoUrl,
            room = profile.room?.let { r -> r.copy(photoUrls = r.photoUrls + roomUploads.map { PhotoRef.of(it.id) }) },
        )

        db.runBatch { batch ->
            (listOfNotNull(profilePhoto) + roomUploads).forEach { p ->
                batch.set(
                    db.collection(PHOTOS).document(p.id),
                    mapOf(
                        "ownerId" to profile.uid,
                        "data" to Blob.fromBytes(p.bytes),
                        "contentType" to "image/jpeg",
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }
            batch.set(db.collection(USERS).document(profile.uid), finalProfile.toMap(isNew = true), SetOptions.merge())
        }.await()
        Unit
    }.recoverAsProfileError()

    override suspend fun getLifestyleAnswers(uid: String): LifestyleAnswers? {
        val snap = runCatching { db.collection(ANSWERS).document(uid).get().await() }.getOrNull() ?: return null
        if (!snap.exists()) return null
        val values = LifestyleCategory.entries.mapNotNull { c -> snap.getLong(c.key)?.let { c to it.toInt() } }.toMap()
        return LifestyleAnswers(values)
    }

    override suspend fun saveLifestyleAnswers(uid: String, answers: LifestyleAnswers): Result<Unit> = runCatching {
        val data = buildMap<String, Any> {
            put("userId", uid)
            answers.values.forEach { (c, v) -> put(c.key, v) }
            put("updatedAt", FieldValue.serverTimestamp())
        }
        db.runBatch { batch ->
            batch.set(db.collection(ANSWERS).document(uid), data)
            batch.set(
                db.collection(USERS).document(uid),
                mapOf("quizComplete" to true, "updatedAt" to FieldValue.serverTimestamp()),
                SetOptions.merge(),
            )
        }.await()
        Unit
    }.recoverAsProfileError()

    // -----------------------------------------------------------------------------------------

    private class PendingPhoto(val id: String, val bytes: ByteArray)

    private suspend fun encode(uri: Uri): ByteArray = withContext(Dispatchers.IO) { compress(uri) }

    /**
     * Downscale to ≤1080px on the long edge, then lower JPEG quality until it fits the per-photo
     * budget — Firestore documents max out at 1 MiB, and smaller photos mean cheaper reads.
     */
    private fun compress(uri: Uri): ByteArray {
        val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder honours EXIF rotation, so portraits stay upright.
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                val longest = max(info.size.width, info.size.height)
                if (longest > MAX_EDGE) {
                    val scale = MAX_EDGE.toFloat() / longest
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: throw ProfileError("We couldn't read that photo. Try a different one.")
        }
        var quality = 82
        while (true) {
            val bytes = ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.toByteArray()
            }
            if (bytes.size <= MAX_PHOTO_BYTES || quality <= 40) {
                if (bytes.size > MAX_PHOTO_BYTES) throw ProfileError("That photo is too detailed to save. Try a different one.")
                return bytes
            }
            quality -= 8
        }
    }

    private fun <T> Result<T>.recoverAsProfileError(): Result<T> = recoverCatching { e ->
        throw when (e) {
            is ProfileError -> e
            is FirebaseNetworkException -> ProfileError("You seem to be offline. We'll need a connection to save this.", e)
            is FirebaseFirestoreException if e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                ProfileError("CoNest couldn't save your profile (permission denied). Check the Firestore rules.", e)
            else -> ProfileError("Something went wrong saving your profile. Please try again.", e)
        }
    }

    private companion object {
        const val USERS = "users"
        const val ANSWERS = "lifestyle_answers"
        const val PHOTOS = "photos"
        const val MAX_EDGE = 1080
        const val MAX_PHOTO_BYTES = 300 * 1024
    }
}

// --- Firestore mapping -----------------------------------------------------------------------

private fun UserProfile.toMap(isNew: Boolean): Map<String, Any?> = buildMap {
    put("uid", uid)
    put("name", name.trim())
    put("age", age)
    put("photoUrl", photoUrl)
    put("occupationType", occupationType?.key)
    put("occupation", occupation.trim())
    put("city", city)
    put("preferredLocalities", preferredLocalities)
    put("budgetMin", budgetMin)
    put("budgetMax", budgetMax)
    put("moveIn", moveIn)
    put("userType", userType?.key)
    put("bio", bio.trim())
    put("interests", interests)
    put("diet", diet?.key)
    put("drinking", drinking?.key)
    put(
        "room",
        room?.let {
            mapOf(
                "type" to it.type?.key,
                "furnishing" to it.furnishing?.key,
                "locality" to it.locality,
                "rent" to it.rent,
                "photoUrls" to it.photoUrls,
            )
        },
    )
    put("profileComplete", profileComplete)
    put("quizComplete", quizComplete)
    if (isNew) put("createdAt", FieldValue.serverTimestamp())
    put("updatedAt", FieldValue.serverTimestamp())
}

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.toProfile(): UserProfile {
    val roomMap = get("room") as? Map<String, Any?>
    return UserProfile(
        uid = id,
        name = getString("name").orEmpty(),
        age = getLong("age")?.toInt() ?: 0,
        photoUrl = getString("photoUrl"),
        occupationType = OccupationType.from(getString("occupationType")),
        occupation = getString("occupation").orEmpty(),
        city = getString("city").orEmpty(),
        preferredLocalities = (get("preferredLocalities") as? List<String>).orEmpty(),
        budgetMin = getLong("budgetMin")?.toInt() ?: 0,
        budgetMax = getLong("budgetMax")?.toInt() ?: 0,
        moveIn = getString("moveIn").orEmpty(),
        userType = UserType.from(getString("userType")),
        bio = getString("bio").orEmpty(),
        interests = (get("interests") as? List<String>).orEmpty(),
        diet = Diet.from(getString("diet")),
        drinking = Drinking.from(getString("drinking")),
        room = roomMap?.let {
            RoomDetails(
                type = RoomType.from(it["type"] as? String),
                furnishing = Furnishing.from(it["furnishing"] as? String),
                locality = it["locality"] as? String ?: "",
                rent = (it["rent"] as? Number)?.toInt() ?: 0,
                photoUrls = (it["photoUrls"] as? List<String>).orEmpty(),
            )
        },
        profileComplete = getBoolean("profileComplete") ?: false,
        quizComplete = getBoolean("quizComplete") ?: false,
    )
}
