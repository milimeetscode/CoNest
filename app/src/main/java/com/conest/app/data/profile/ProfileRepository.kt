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
    suspend fun saveProfile(profile: UserProfile, photo: Uri?, roomPhotos: List<Uri>): Result<Unit>

    suspend fun getLifestyleAnswers(uid: String): LifestyleAnswers?

    /** Writes `lifestyle_answers/{uid}` and marks the quiz complete on the profile, atomically. */
    suspend fun saveLifestyleAnswers(uid: String, answers: LifestyleAnswers): Result<Unit>
}
