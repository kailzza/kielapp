package com.example.scholartrack

import android.app.Application
import org.osmdroid.config.Configuration
import android.preference.PreferenceManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Set the osmdroid configuration
        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
    }
}
