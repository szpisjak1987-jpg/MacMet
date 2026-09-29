package com.example.macmet

import android.app.Application
import com.example.macmet.di.AppContainer
import com.example.macmet.di.DefaultAppContainer

class MacMetApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
