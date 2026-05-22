package com.example.placesproject.data.repository

import com.example.placesproject.data.dao.PlaceDao
import com.example.placesproject.data.model.Place

class PlaceRepository(private val dao: PlaceDao) {
    suspend fun insert(place: Place) = dao.insert(place)
    suspend fun update(place: Place) = dao.update(place)
    suspend fun delete(place: Place) = dao.delete(place)
    suspend fun getById(id: Int) = dao.getPlaceById(id)
    suspend fun getAll() = dao.getAllPlaces()
}