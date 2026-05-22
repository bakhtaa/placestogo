package com.example.placesproject.data.dao

import androidx.room.*
import com.example.placesproject.data.model.Place
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(place: Place)

    @Update
    suspend fun update(place: Place)

    @Delete
    suspend fun delete(place: Place)

    @Query("SELECT * FROM places ORDER BY id DESC")
    suspend fun getAllPlaces(): List<Place>

    @Query("SELECT * FROM places WHERE id = :id")
    suspend fun getPlaceById(id: Int): Place?
}