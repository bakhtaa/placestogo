package com.example.placesproject

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.placesproject.data.database.PlaceDatabase
import com.example.placesproject.data.model.Place
import com.example.placesproject.data.repository.PlaceRepository
import com.example.placesproject.databinding.ActivityMainBinding
import com.example.placesproject.databinding.DialogAddPlaceBinding
import com.example.placesproject.viewmodel.PlaceViewModel
import com.example.placesproject.viewmodel.PlaceViewModelFactory
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var viewModel: PlaceViewModel

    private val LOCATION_PERMISSION_CODE = 100
    private var currentLat = 0.0
    private var currentLon = 0.0

    // Image sélectionnée
    private var selectedImageUri: String = ""
    private lateinit var dialogBinding: DialogAddPlaceBinding

    // Launcher galerie
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it.toString()
            dialogBinding.ivPreview.visibility = android.view.View.VISIBLE
            Glide.with(this).load(it).centerCrop().into(dialogBinding.ivPreview)
        }
    }

    // Launcher caméra
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            val uri = saveBitmapToFile(it)
            selectedImageUri = uri.toString()
            dialogBinding.ivPreview.visibility = android.view.View.VISIBLE
            Glide.with(this).load(uri).centerCrop().into(dialogBinding.ivPreview)
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) getLocation()
        else Toast.makeText(this, "Permission refusée", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val username = intent.getStringExtra("username") ?: "Visiteur"
        binding.tvWelcome.text = "Bienvenue, $username ! 👋"

        val db = PlaceDatabase.getDatabase(this)
        val factory = PlaceViewModelFactory(PlaceRepository(db.placeDao()))
        viewModel = ViewModelProvider(this, factory)[PlaceViewModel::class.java]

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        sendWelcomeNotification(username)

        binding.btnMyLocation.setOnClickListener {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        binding.btnAddPlace.setOnClickListener { showAddPlaceDialog() }

        binding.btnViewList.setOnClickListener {
            startActivity(Intent(this, PlaceListActivity::class.java))
        }

        binding.btnExplore.setOnClickListener {
            startActivity(Intent(this, ExploreActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    // ── GPS ──────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        val cancellationToken = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationToken.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                currentLat = location.latitude
                currentLon = location.longitude
                lifecycleScope.launch {
                    val address = getAddress(location.latitude, location.longitude)
                    binding.tvLocation.text = "📍 $address"
                }
                val intent = Intent(this, LocationActivity::class.java)
                intent.putExtra("lat", location.latitude)
                intent.putExtra("lon", location.longitude)
                startActivity(intent)
            } else {
                currentLat = 36.8065
                currentLon = 10.1815
                binding.tvLocation.text = "📍 Tunis (par défaut)"
            }
        }
    }

    private suspend fun getAddress(lat: Double, lon: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
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

    // ── DIALOG AJOUTER LIEU ───────────────────────────────

    private fun showAddPlaceDialog() {
        selectedImageUri = ""
        dialogBinding = DialogAddPlaceBinding.inflate(LayoutInflater.from(this))

        val dialog = AlertDialog.Builder(this)
            .setTitle("➕ Ajouter un lieu")
            .setView(dialogBinding.root)
            .setPositiveButton("Ajouter") { _, _ ->
                val name = dialogBinding.etPlaceName.text.toString().trim()
                if (name.isNotEmpty()) {
                    val place = Place(
                        name = name,
                        latitude = if (currentLat != 0.0) currentLat else 36.8065,
                        longitude = if (currentLon != 0.0) currentLon else 10.1815,
                        imageUrl = selectedImageUri
                    )
                    viewModel.insert(place)
                    sendNotification(name)
                    Toast.makeText(this, "✅ $name ajouté !", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Entrez un nom", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .create()

        dialogBinding.btnCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                cameraLauncher.launch(null)
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.CAMERA), 102
                )
            }
        }

        dialogBinding.btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        dialog.show()
    }

    // ── UTILITAIRES ───────────────────────────────────────

    private fun saveBitmapToFile(bitmap: Bitmap): Uri {
        val file = File(cacheDir, "photo_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return Uri.fromFile(file)
    }

    // ── NOTIFICATIONS ─────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "places_channel", "Lieux favoris", NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun sendWelcomeNotification(username: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101
                )
                return
            }
        }
        val notification = NotificationCompat.Builder(this, "places_channel")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Bienvenue $username ! 🗺️")
            .setContentText("Retrouvez vos lieux favoris")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(1, notification)
    }

    private fun sendNotification(placeName: String) {
        val intent = Intent("com.example.placesproject.PLACE_SAVED")
        intent.putExtra("place_name", placeName)
        intent.setPackage(packageName)
        sendBroadcast(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }
        val notification = NotificationCompat.Builder(this, "places_channel")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Lieu ajouté !")
            .setContentText("$placeName a été sauvegardé 📍")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java)
            .notify(System.currentTimeMillis().toInt(), notification)
    }
}