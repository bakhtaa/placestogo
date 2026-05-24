package com.example.placesproject

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.placesproject.data.api.RetrofitClient
import com.example.placesproject.data.database.PlaceDatabase
import com.example.placesproject.data.model.Comment
import com.example.placesproject.data.model.Place
import com.example.placesproject.data.model.UnsplashPhoto
import com.example.placesproject.databinding.ActivityExploreBinding
import com.example.placesproject.databinding.ItemExploreBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.location.Geocoder
import java.util.Locale

class ExploreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExploreBinding
    private val ACCESS_KEY = "LembTH7zONCbWYuqxbqArFIDL8yNNjTpZq_96nPbFlc"
    private val photos = mutableListOf<UnsplashPhoto>()
    private lateinit var adapter: ExploreAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExploreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        adapter = ExploreAdapter(
            photos,
            onLike = { photo, comment -> saveToFavorites(photo, comment) },
            onComment = { comment ->
                Toast.makeText(this, "💬 Commentaire sauvegardé", Toast.LENGTH_SHORT).show()
            },
            onCardClick = { photo -> openMap(photo) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        searchPlaces("beautiful places")

        binding.btnSearch.setOnClickListener {
            val query = binding.etSearch.text.toString().trim()
            if (query.isNotEmpty()) searchPlaces(query)
            else searchPlaces("beautiful places")
        }
    }

    private fun openMap(photo: UnsplashPhoto) {
        val placeName = photo.alt_description ?: photo.description ?: "beautiful place"
        lifecycleScope.launch {
            val coords = getCoordinates(placeName)
            val intent = Intent(this@ExploreActivity, LocationActivity::class.java)
            intent.putExtra("lat", coords.first)
            intent.putExtra("lon", coords.second)
            intent.putExtra("place_name", placeName)
            startActivity(intent)
        }
    }

    private suspend fun getCoordinates(placeName: String): Pair<Double, Double> {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(this@ExploreActivity, Locale.getDefault())
                val addresses = geocoder.getFromLocationName(placeName, 1)
                if (!addresses.isNullOrEmpty()) {
                    Pair(addresses[0].latitude, addresses[0].longitude)
                } else {
                    Pair(36.8065, 10.1815) // Tunis par défaut
                }
            } catch (e: Exception) {
                Pair(36.8065, 10.1815)
            }
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
                    Toast.makeText(this@ExploreActivity, "Aucun résultat", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ExploreActivity, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveToFavorites(photo: UnsplashPhoto, comment: String) {
        val db = PlaceDatabase.getDatabase(this)
        lifecycleScope.launch {
            val placeName = photo.alt_description ?: photo.description ?: "Lieu favori"
            val coords = getCoordinates(placeName)
            val place = Place(
                name = placeName,
                latitude = coords.first,
                longitude = coords.second,
                comment = comment,
                imageUrl = photo.urls.regular
            )
            db.placeDao().insert(place)
            Toast.makeText(this@ExploreActivity, "❤️ Ajouté aux favoris !", Toast.LENGTH_SHORT).show()
        }
    }
}

class ExploreAdapter(
    private val photos: List<UnsplashPhoto>,
    private val onLike: (UnsplashPhoto, String) -> Unit,
    private val onComment: (String) -> Unit,
    private val onCardClick: (UnsplashPhoto) -> Unit
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
                ?: photo.description ?: "Lieu magnifique"
            binding.tvAuthor.text = "📷 ${photo.user.name}"

            Glide.with(binding.root.context)
                .load(photo.urls.regular)
                .centerCrop()
                .into(binding.ivPhoto)

            // Clic sur la photo → ouvre la carte
            binding.ivPhoto.setOnClickListener {
                onCardClick(photo)
            }

            // Charger commentaire existant
            val db = PlaceDatabase.getDatabase(binding.root.context)
            GlobalScope.launch {
                val comments = db.commentDao().getCommentsForPhoto(photo.id)
                if (comments.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        binding.tvComment.text = "💬 ${comments.last().text}"
                        binding.tvComment.visibility = View.VISIBLE
                    }
                }
            }

            binding.btnLike.setOnClickListener {
                val comment = binding.etComment.text.toString()
                onLike(photo, comment)
            }

            binding.btnComment.setOnClickListener {
                val comment = binding.etComment.text.toString().trim()
                if (comment.isNotEmpty()) {
                    binding.tvComment.text = "💬 $comment"
                    binding.tvComment.visibility = View.VISIBLE
                    binding.etComment.setText("")
                    GlobalScope.launch {
                        db.commentDao().insert(
                            Comment(
                                photoId = photo.id,
                                photoName = photo.alt_description ?: "Lieu",
                                text = comment
                            )
                        )
                    }
                    onComment(comment)
                } else {
                    Toast.makeText(
                        binding.root.context,
                        "Écris un commentaire d'abord",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}