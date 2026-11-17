package com.conest.app.data.profile

import android.net.Uri
import com.conest.app.data.model.UserProfile
import com.conest.app.domain.lifestyle.LifestyleAnswers
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    /**
     * The profile at `users/{uid}`, live. Emits null only when the server confirms there's no
     * profile — never on a cold, offline cache miss (that would wrongly send a returning user
     * into onboarding).
     */
    fun observeProfile(uid: String): Flow<UserProfile?>

    /**
     * Uploads any new local photos, then writes the profile. [photo] / [roomPhotos] are local
     * content URIs picked on-device; already-uploaded images stay as URLs on [profile].
     */
    suspend fun saveProfile(profile: UserProfile, photo: Uri?, roomPhotos: List<Uri>, isNew: Boolean = true): Result<Unit>

    /** One-shot read of someone's public profile; null if it doesn't exist (e.g. deleted account). */
    suspend fun getProfile(uid: String): UserProfile?

    /** Pause (false) or resume (true) appearing in other people's Discover. */
    suspend fun setDiscoverable(uid: String, discoverable: Boolean): Result<Unit>

    /** Removes the person's profile, answers, photos, swipes and blocks. Auth deletion is separate. */
    suspend fun deleteAccountData(uid: String): Result<Unit>

    suspend fun getLifestyleAnswers(uid: String): LifestyleAnswers?

    /** Writes `lifestyle_answers/{uid}` and marks the quiz complete on the profile, atomically. */
    suspend fun saveLifestyleAnswers(uid: String, answers: LifestyleAnswers): Result<Unit>
}
