package com.example.macmet.data.api.netatmo

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class NetatmoAuthInterceptor(
    private val clientId: String,
    private val clientSecret: String,
    private var accessToken: String,
    private var refreshToken: String,
    private val tokenRefreshAction: suspend (String, String, String) -> Pair<String, String>?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 1. Kérés indítása a jelenlegi access token-el
        val requestWithToken = originalRequest.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

        var response = chain.proceed(requestWithToken)

        // 2. Ha 401 / 403 a válasz (lejárt token), akkor refresh
        if (response.code == 401 || response.code == 403) {
            response.close() // Régi válasz lezárása

            synchronized(this) {
                // Token frissítése coroutine hívásként
                val newTokens = runBlocking {
                    tokenRefreshAction(refreshToken, clientId, clientSecret)
                }

                if (newTokens != null) {
                    this.accessToken = newTokens.first
                    this.refreshToken = newTokens.second
                    // Újrapróbálkozás az új tokennel
                    val newRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $accessToken")
                        .build()
                    response = chain.proceed(newRequest)
                }
            }
        }

        return response
    }
}
