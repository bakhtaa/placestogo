package com.example.placesproject.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "places")
data class Place(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    val latitude: Double,

    val longitude: Double
)