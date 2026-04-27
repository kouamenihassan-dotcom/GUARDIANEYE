package com.example.guardianeye.ui.main

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.guardianeye.admin.DeviceAdminReceiver
import com.example.guardianeye.databinding.ActivityLockedBinding
import com.example.guardianeye.repository.FirebaseRepository
import com.example.guardianeye.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LockedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockedBinding
    private val repository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Afficher par dessus l'écran de verrouillage
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        // Écouter si le statut revient à NORMAL
        listenForUnlock()

        // Empêcher de quitter
        binding.root.setOnClickListener { }
    }

    private fun listenForUnlock() {
        val prefs = getSharedPreferences(
            Constants.PREF_NAME,
            Context.MODE_PRIVATE
        )
        val deviceId = prefs.getString(
            Constants.PREF_DEVICE_ID, ""
        ) ?: ""

        if (deviceId.isEmpty()) return

        val db = com.google.firebase.database
            .FirebaseDatabase
            .getInstance(Constants.DATABASE_URL)

        db.reference
            .child("devices")
            .child(deviceId)
            .child("status")
            .addValueEventListener(
                object : com.google.firebase.database
                .ValueEventListener {
                    override fun onDataChange(
                        snapshot: com.google.firebase
                        .database.DataSnapshot
                    ) {
                        val status = snapshot
                            .getValue(String::class.java)
                        if (status == Constants.STATUS_NORMAL) {
                            // Déverrouiller
                            finish()
                        }
                    }
                    override fun onCancelled(
                        error: com.google.firebase
                        .database.DatabaseError
                    ) {}
                }
            )
    }

    override fun onBackPressed() {
        // Bloquer le bouton retour
    }
}