package com.example.placesproject

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.placesproject.data.model.Place
import com.example.placesproject.databinding.ItemPlaceBinding

class PlaceAdapter(private val onDelete: (Place) -> Unit) :
    RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>() {

    private var places = listOf<Place>()

    fun submitList(list: List<Place>) {
        places = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val binding = ItemPlaceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PlaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(places[position])
    }

    override fun getItemCount() = places.size

    inner class PlaceViewHolder(private val binding: ItemPlaceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(place: Place) {
            binding.tvPlaceName.text = place.name
            binding.tvCoords.text = "📍 %.4f, %.4f".format(place.latitude, place.longitude)

            // Afficher commentaire
            if (place.comment.isNotEmpty()) {
                binding.tvComment.text = "💬 ${place.comment}"
                binding.tvComment.visibility = View.VISIBLE
            }

            // Afficher image si disponible
            if (place.imageUrl.isNotEmpty()) {
                binding.ivPlaceImage.visibility = View.VISIBLE
                Glide.with(binding.root.context)
                    .load(place.imageUrl)
                    .centerCrop()
                    .into(binding.ivPlaceImage)
            }

            // Supprimer
            binding.btnDelete.setOnClickListener { onDelete(place) }

            // Ouvrir carte
            binding.btnShowMap.setOnClickListener {
                val intent = Intent(binding.root.context, LocationActivity::class.java)
                intent.putExtra("lat", place.latitude)
                intent.putExtra("lon", place.longitude)
                intent.putExtra("place_name", place.name)
                binding.root.context.startActivity(intent)
            }
        }
    }
}