package com.example.guardianeye.ui.main

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.guardianeye.databinding.ActivityCamouflageBinding
import com.example.guardianeye.utils.Constants

class CamouflageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCamouflageBinding

    // Liste des camouflages disponibles
    private val camouflages = listOf(
        Pair("Calculatrice", ".Calculatrice"),
        Pair("Météo", ".Meteo"),
        Pair("Notes", ".Notes"),
        Pair("Horloge", ".Horloge")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCamouflageBinding
            .inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinner()
        setupClickListeners()
    }

    private fun setupSpinner() {
        val names = camouflages.map { it.first }
        val adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            names
        )
        binding.spinnerCamouflage.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnSaveCamouflage.setOnClickListener {
            val selectedIndex = binding
                .spinnerCamouflage.selectedItemPosition
            val selected = camouflages[selectedIndex]

            activateCamouflage(selected.first, selected.second)
        }

        binding.btnDisableCamouflage.setOnClickListener {
            disableCamouflage()
        }
    }

    private fun activateCamouflage(
        name: String,
        aliasName: String
    ) {
        val pm = packageManager

        // Désactiver l'icône normale
        pm.setComponentEnabledSetting(
            ComponentName(this,
                "com.example.guardianeye.ui.splash.SplashActivity"),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )

        // Activer l'icône camouflage
        pm.setComponentEnabledSetting(
            ComponentName(this,
                "com.example.guardianeye$aliasName"),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        // Sauvegarder le choix
        getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE)
            .edit()
            .putString("camouflage_name", name)
            .putString("camouflage_alias", aliasName)
            .apply()

        Toast.makeText(
            this,
            "Camouflage activé : $name",
            Toast.LENGTH_LONG
        ).show()

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun disableCamouflage() {
        val pm = packageManager

        // Réactiver l'icône normale
        pm.setComponentEnabledSetting(
            ComponentName(this,
                "com.example.guardianeye.ui.splash.SplashActivity"),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        // Désactiver tous les alias
        camouflages.forEach { (_, alias) ->
            pm.setComponentEnabledSetting(
                ComponentName(this,
                    "com.example.guardianeye$alias"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        Toast.makeText(
            this,
            "Camouflage désactivé",
            Toast.LENGTH_SHORT
        ).show()
    }
}