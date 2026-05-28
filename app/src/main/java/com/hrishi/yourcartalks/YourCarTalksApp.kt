package com.hrishi.yourcartalks

import android.app.Application
import com.hrishi.yourcartalks.data.PreferencesManager

class YourCarTalksApp : Application() {

    lateinit var preferencesManager: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
    }
}
