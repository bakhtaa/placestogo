package com.example.placesproject.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PlaceSavedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val placeName = intent.getStringExtra("place_name") ?: "Inconnu"
        Log.d("RECEIVER", "Lieu reçu : $placeName")
    }
}