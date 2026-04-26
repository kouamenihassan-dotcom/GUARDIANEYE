package com.example.guardianeye.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.guardianeye.databinding.ActivityCamouflageBinding
import com.example.guardianeye.utils.Constants

class CamouflageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCamouflageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCamouflageBinding
            .inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnSaveCamouflage.setOnClickListener {
            val appName = binding.etAppName
                .text.toString().trim()

            if (appName.isEmpty()) {
                Toast.makeText(
                    this,
                    "Entrez un nom d'application",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val prefs = getSharedPreferences(
                Constants.PREF_NAME,
                MODE_PRIVATE
            )
            prefs.edit()
                .putString("camouflage_name", appName)
                .apply()

            Toast.makeText(
                this,
                "Mode camouflage active : $appName",
                Toast.LENGTH_LONG
            ).show()

            startActivity(
                Intent(this, MainActivity::class.java)
            )
            finish()
        }
    }
}