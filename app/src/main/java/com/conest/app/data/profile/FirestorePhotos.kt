package com.conest.app.data.profile

import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import okio.Buffer

/**
 * Photos are stored as Firestore documents (`photos/{id}`), not in Cloud Storage, so CoNest
 * runs on the free Spark plan. Profiles reference them as `conest-photo://{id}`.
 */
object PhotoRef {
    const val SCHEME = "conest-photo"
    fun of(id: String) = "$SCHEME://$id"
}

/**
 * Teaches Coil to load `conest-photo://` references straight from Firestore. Firestore's own
 * offline cache plus Coil's memory cache keep repeat views from costing extra reads.
 */
class FirestorePhotoFetcher(private val id: String, private val options: Options) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val snap = FirebaseFirestore.getInstance().collection("photos").document(id).get().await()
        val bytes = snap.getBlob("data")?.toBytes() ?: error("Photo $id not found")
        return SourceFetchResult(
            source = ImageSource(Buffer().write(bytes), options.fileSystem),
            mimeType = snap.getString("contentType") ?: "image/jpeg",
            dataSource = if (snap.metadata.isFromCache) DataSource.DISK else DataSource.NETWORK,
        )
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme != PhotoRef.SCHEME) return null
            val id = data.authority?.takeIf { it.isNotBlank() } ?: return null
            return FirestorePhotoFetcher(id, options)
        }
    }
}
