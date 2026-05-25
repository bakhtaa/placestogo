package com.example.placesproject.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.placesproject.data.api.RetrofitClient
import com.example.placesproject.data.model.UnsplashPhoto
import com.example.placesproject.utils.ApiState
import kotlinx.coroutines.launch

class ExploreViewModel : ViewModel() {

    private val _state = MutableLiveData<ApiState<List<UnsplashPhoto>>>()
    val state: LiveData<ApiState<List<UnsplashPhoto>>> = _state

    fun searchPhotos(accessKey: String, query: String) {
        _state.value = ApiState.Loading

        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.searchPhotos(
                    auth = "Client-ID $accessKey",
                    query = query
                )
                _state.value = ApiState.Success(response.results)
            } catch (e: Exception) {
                _state.value = ApiState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }
}