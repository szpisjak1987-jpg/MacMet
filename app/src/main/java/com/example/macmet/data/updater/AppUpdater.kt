package com.example.macmet.data.updater

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.macmet.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.io.File

class AppUpdater(
    private val context: Context,
    private val githubOwner: String = "szpisjak1987-jpg",
    private val githubRepo: String = "MacMet",
    private val apiService: GithubApiService = RetrofitClient.githubApiService
) {

    suspend fun checkForUpdate(currentVersion: String): Pair<Boolean, GithubReleaseDto?> = withContext(Dispatchers.IO) {
        try {
            Log.d("AppUpdater", "Checking GitHub release for $githubOwner/$githubRepo, current app version: $currentVersion")
            val response = apiService.getLatestRelease(githubOwner, githubRepo)
            if (response.isSuccessful) {
                val release = response.body()
                if (release != null) {
                    val latestVersion = release.tagName.replace("v", "").trim()
                    val currentVer = currentVersion.replace("v", "").trim()
                    
                    Log.d("AppUpdater", "Latest release tag: ${release.tagName} (parsed: $latestVersion), current: $currentVer")

                    if (isNewerVersion(latestVersion, currentVer)) {
                        Log.d("AppUpdater", "New update available!")
                        return@withContext Pair(true, release)
                    } else {
                        Log.d("AppUpdater", "App is up-to-date")
                    }
                }
            } else {
                Log.e("AppUpdater", "Failed to fetch release: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("AppUpdater", "Error checking for update", e)
        }
        return@withContext Pair(false, null)
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        
        val maxLength = maxOf(latestParts.size, currentParts.size)
        
        for (i in 0 until maxLength) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    fun downloadUpdate(apkUrl: String, version: String): Long {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        
        val uri = Uri.parse(apkUrl)
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadDir, "MacMet-$version.apk")
        if (file.exists()) {
            file.delete()
        }

        val request = DownloadManager.Request(uri).apply {
            setTitle("MacMet Frissítés")
            setDescription("Verzió: $version letöltése...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationUri(Uri.fromFile(file))
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        return downloadManager.enqueue(request)
    }
}
