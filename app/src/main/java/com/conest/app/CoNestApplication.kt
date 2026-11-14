package com.conest.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import com.conest.app.data.profile.FirestorePhotoFetcher
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CoNestApplication : Application(), SingletonImageLoader.Factory {

    // App-wide image loader: understands conest-photo:// (Firestore-stored photos).
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(FirestorePhotoFetcher.Factory()) }
            .crossfade(true)
            .build()
}
