package com.example.manu_mc_proj1

import android.app.Application

class HealthApp: Application() {
    lateinit var dbHelper: FeedReaderDbHelper

    override fun onCreate() {
        super.onCreate()
        dbHelper = FeedReaderDbHelper(this)
    }
}