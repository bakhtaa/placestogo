package com.example.placesproject

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.placesproject.databinding.ActivityLocationBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityLocationBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap
    private val LOCATION_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialiser la carte
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnGetLocation.setOnClickListener { checkPermissionAndGetLocation() }
        binding.btnStartUpdates.setOnClickListener { checkPermissionAndStartUpdates() }
        binding.btnStopUpdates.setOnClickListener { stopLocationUpdates() }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Position reçue depuis MainActivity
        val lat = intent.getDoubleExtra("lat", 0.0)
        val lon = intent.getDoubleExtra("lon", 0.0)
        if (lat != 0.0) {
            val position = LatLng(lat, lon)
            googleMap.addMarker(MarkerOptions().position(position).title("Ma position"))
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
            lifecycleScope.launch {
                val address = getAddress(lat, lon)
                binding.locationTextView.text = "📍 $address"
            }
        } else {
            // Position par défaut — Tunis
            val tunis = LatLng(36.8065, 10.1815)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tunis, 12f))
        }
    }

    private fun checkPermissionAndGetLocation() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getLastLocation()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        }
    }

    private fun checkPermissionAndStartUpdates() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            getLastLocation()
        } else {
            Toast.makeText(this, "Permission refusée", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        val cancellationToken = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationToken.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                val position = LatLng(location.latitude, location.longitude)

                // Afficher sur la carte
                googleMap.clear()
                googleMap.addMarker(
                    MarkerOptions().position(position).title("Ma position")
                )
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(position, 15f)
                )

                // Afficher l'adresse
                lifecycleScope.launch {
                    val address = getAddress(location.latitude, location.longitude)
                    binding.locationTextView.text = "📍 $address"
                }
            } else {
                binding.locationTextView.text = "Position introuvable"
                Toast.makeText(this, "Active le GPS dans Extended Controls", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000
        ).build()

        val intent = Intent(this, com.example.placesproject.receiver.PlaceSavedReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fusedLocationClient.requestLocationUpdates(locationRequest, pendingIntent)
        binding.btnStopUpdates.visibility = View.VISIBLE
        Toast.makeText(this, "Suivi démarré ▶", Toast.LENGTH_SHORT).show()
    }

    private fun stopLocationUpdates() {
        val intent = Intent(this, com.example.placesproject.receiver.PlaceSavedReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        fusedLocationClient.removeLocationUpdates(pendingIntent)
        binding.btnStopUpdates.visibility = View.GONE
        Toast.makeText(this, "Suivi arrêté ⏹", Toast.LENGTH_SHORT).show()
    }

    private suspend fun getAddress(lat: Double, lon: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(this@LocationActivity, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val street = address.thoroughfare ?: "Rue inconnue"
                    val city = address.locality ?: ""
                    "$street, $city"
                } else "Adresse introuvable"
            } catch (e: Exception) {
                "Lat: $lat, Lon: $lon"
            }
        }
    }
}