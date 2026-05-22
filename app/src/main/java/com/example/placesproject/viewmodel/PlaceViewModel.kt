package com.example.placesproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.placesproject.data.model.Place
import com.example.placesproject.data.repository.PlaceRepository
import kotlinx.coroutines.launch

class PlaceViewModel(private val repository: PlaceRepository) : ViewModel() {

    fun insert(place: Place) {
        viewModelScope.launch {
            repository.insert(place)
        }
    }

    fun update(place: Place) {
        viewModelScope.launch {
            repository.update(place)
        }
    }

    fun delete(place: Place) {
        viewModelScope.launch {
            repository.delete(place)
        }
    }

    suspend fun getAllPlaces() = repository.getAll()
    suspend fun getPlaceById(id: Int) = repository.getById(id)
}