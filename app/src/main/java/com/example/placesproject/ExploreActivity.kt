package com.example.placesproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.placesproject.data.api.RetrofitClient
import com.example.placesproject.data.database.PlaceDatabase
import com.example.placesproject.data.model.Place
import com.example.placesproject.data.model.UnsplashPhoto
import com.example.placesproject.databinding.ActivityExploreBinding
import com.example.placesproject.databinding.ItemExploreBinding
import kotlinx.coroutines.launch

class ExploreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExploreBinding
    private val ACCESS_KEY = "STFlDLWl9ywhUXsFy1plroEfRf8XGPEVGu3Ki6QaVcA"
    private val photos = mutableListOf<UnsplashPhoto>()
    private lateinit var adapter: ExploreAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExploreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        adapter = ExploreAdapter(photos) { photo, comment ->
            saveToFavorites(photo, comment)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Recherche par défaut
        searchPlaces("beautiful places")

        binding.btnSearch.setOnClickListener {
            val query = binding.etSearch.text.toString().trim()
            if (query.isNotEmpty()) searchPlaces(query)
            else searchPlaces("beautiful places")
        }
    }

    private fun searchPlaces(query: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.searchPhotos(
                    auth = "Client-ID $ACCESS_KEY",
                    query = query
                )
                photos.clear()
                photos.addAll(response.results)
                adapter.notifyDataSetChanged()

                if (photos.isEmpty()) {
                    Toast.makeText(
                        this@ExploreActivity,
                        "Aucun résultat",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ExploreActivity,
                    "Erreur: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun saveToFavorites(photo: UnsplashPhoto, comment: String) {
        val db = PlaceDatabase.getDatabase(this)
        lifecycleScope.launch {
            val place = Place(
                name = photo.alt_description
                    ?: photo.description
                    ?: "Lieu favori",
                latitude = 0.0,
                longitude = 0.0
            )
            db.placeDao().insert(place)
            Toast.makeText(
                this@ExploreActivity,
                "❤️ Ajouté aux favoris !",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

class ExploreAdapter(
    private val photos: List<UnsplashPhoto>,
    private val onLike: (UnsplashPhoto, String) -> Unit
) : RecyclerView.Adapter<ExploreAdapter.ExploreViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExploreViewHolder {
        val binding = ItemExploreBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ExploreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExploreViewHolder, position: Int) {
        holder.bind(photos[position])
    }

    override fun getItemCount() = photos.size

    inner class ExploreViewHolder(private val binding: ItemExploreBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: UnsplashPhoto) {
            binding.tvPlaceName.text = photo.alt_description
                ?: photo.description
                        ?: "Lieu magnifique"
            binding.tvAuthor.text = "📷 ${photo.user.name}"

            Glide.with(binding.root.context)
                .load(photo.urls.regular)
                .centerCrop()
                .into(binding.ivPhoto)

            binding.btnLike.setOnClickListener {
                val comment = binding.etComment.text.toString()
                onLike(photo, comment)
            }
        }
    }
}