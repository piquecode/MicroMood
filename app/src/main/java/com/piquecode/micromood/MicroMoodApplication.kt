package com.piquecode.micromood

import android.app.Application
import com.piquecode.micromood.handlers.error.ErrorHandler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MicroMoodApplication : Application() {
    @Inject
    lateinit var errorHandler: ErrorHandler

    override fun onCreate() {
        super.onCreate()

        errorHandler.init()
    }
}
