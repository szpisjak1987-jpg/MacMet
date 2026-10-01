package com.example.macmet

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.macmet.di.AppContainer
import com.example.macmet.di.DefaultAppContainer
import com.example.macmet.worker.WeatherAlertWorker
import java.util.concurrent.TimeUnit

class MacMetApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        
        setupAlertWorker()
    }

    private fun setupAlertWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val alertRequest = PeriodicWorkRequestBuilder<WeatherAlertWorker>(
            repeatInterval = 1, // Minden 1 órában
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WeatherAlertWork",
            ExistingPeriodicWorkPolicy.KEEP,
            alertRequest
        )
    }
}
