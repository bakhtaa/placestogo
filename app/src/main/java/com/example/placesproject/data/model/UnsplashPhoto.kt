package com.example.placesproject.data.model

data class UnsplashPhoto(
    val id: String,
    val description: String?,
    val alt_description: String?,
    val urls: PhotoUrls,
    val user: UnsplashUser
)

data class PhotoUrls(
    val regular: String,
    val small: String
)

data class UnsplashUser(
    val name: String
)

data class UnsplashResponse(
    val results: List<UnsplashPhoto>
)