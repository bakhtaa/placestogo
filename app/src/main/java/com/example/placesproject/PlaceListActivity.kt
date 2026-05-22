package com.example.placesproject

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.placesproject.data.database.PlaceDatabase
import com.example.placesproject.data.repository.PlaceRepository
import com.example.placesproject.databinding.ActivityPlaceListBinding
import com.example.placesproject.viewmodel.PlaceViewModel
import com.example.placesproject.viewmodel.PlaceViewModelFactory
import kotlinx.coroutines.launch

class PlaceListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaceListBinding
    private lateinit var adapter: PlaceAdapter
    private lateinit var viewModel: PlaceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaceListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val db = PlaceDatabase.getDatabase(this)
        val factory = PlaceViewModelFactory(PlaceRepository(db.placeDao()))
        viewModel = ViewModelProvider(this, factory)[PlaceViewModel::class.java]

        adapter = PlaceAdapter { place ->
            viewModel.delete(place)
            loadPlaces()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        loadPlaces()
    }

    private fun loadPlaces() {
        lifecycleScope.launch {
            val places = viewModel.getAllPlaces()
            adapter.submitList(places)
        }
    }
}