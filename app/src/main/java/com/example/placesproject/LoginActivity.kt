package com.example.placesproject

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.placesproject.data.database.PlaceDatabase
import com.example.placesproject.data.model.User
import com.example.placesproject.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateMode()

        binding.btnTabLogin.setOnClickListener {
            isLoginMode = true
            updateMode()
        }

        binding.btnTabRegister.setOnClickListener {
            isLoginMode = false
            updateMode()
        }

        binding.btnAction.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Remplissez tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isLoginMode) doLogin(username, password)
            else doRegister(username, password)
        }
    }

    private fun updateMode() {
        if (isLoginMode) {
            binding.btnAction.text = "Se connecter"
            binding.btnTabLogin.backgroundTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#7ECECA"))
            binding.btnTabRegister.backgroundTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#C4B8D8"))
        } else {
            binding.btnAction.text = "S'inscrire"
            binding.btnTabRegister.backgroundTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#7ECECA"))
            binding.btnTabLogin.backgroundTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#C4B8D8"))
        }
    }

    private fun doLogin(username: String, password: String) {
        val db = PlaceDatabase.getDatabase(this)
        lifecycleScope.launch {
            val user = db.userDao().login(username, password)
            if (user != null) {
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                intent.putExtra("username", username)
                startActivity(intent)
                finish()
            } else {
                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        "Identifiants incorrects",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun doRegister(username: String, password: String) {
        val db = PlaceDatabase.getDatabase(this)
        lifecycleScope.launch {
            val existing = db.userDao().getUserByUsername(username)
            if (existing != null) {
                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        "Nom d'utilisateur déjà pris",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@launch
            }
            db.userDao().insert(User(username = username, password = password))
            runOnUiThread {
                Toast.makeText(
                    this@LoginActivity,
                    "Compte créé ! Connectez-vous",
                    Toast.LENGTH_SHORT
                ).show()
                isLoginMode = true
                updateMode()
            }
        }
    }
}