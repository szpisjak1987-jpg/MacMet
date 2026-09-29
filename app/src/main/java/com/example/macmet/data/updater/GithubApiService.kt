package com.example.macmet.data.updater

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface GithubApiService {

    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GithubReleaseDto>

    companion object {
        const val BASE_URL = "https://api.github.com/"
    }
}
