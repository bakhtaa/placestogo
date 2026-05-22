package com.example.placesproject.data.api

import com.example.placesproject.data.model.UnsplashResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface UnsplashApi {

    @GET("search/photos")
    suspend fun searchPhotos(
        @Header("Authorization") auth: String,
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 20,
        @Query("orientation") orientation: String = "landscape"
    ): UnsplashResponse
}