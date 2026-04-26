package com.example.guardianeye.ui.main

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.guardianeye.R
import com.example.guardianeye.admin.DeviceAdminReceiver
import com.example.guardianeye.databinding.ActivityMainBinding
import com.example.guardianeye.service.GuardianService
import com.example.guardianeye.ui.auth.LoginActivity
import com.example.guardianeye.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase
        .getInstance(Constants.DATABASE_URL)

    companion object {
        private const val REQUEST_ADMIN = 1001
        private const val REQUEST_PERMISSIONS = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dpm = getSystemService(
            DEVICE_POLICY_SERVICE
        ) as DevicePolicyManager

        adminComponent = ComponentName(
            this,
            DeviceAdminReceiver::class.java
        )

        requestAllPermissions()
        setupUI()
        listenToStatus()
    }

    // ─── PERMISSIONS ────────────────────────────────────
    private fun requestAllPermissions() {
        val permissions = mutableListOf<String>()

        // Camera
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.CAMERA)
        }

        // Location
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                REQUEST_PERMISSIONS
            )
        } else {
            // Toutes permissions ok
            startGuardianService()
            requestAdminRights()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        )
        if (requestCode == REQUEST_PERMISSIONS) {
            // Demander permission background location séparément
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission
                            .ACCESS_BACKGROUND_LOCATION
                    ),
                    REQUEST_PERMISSIONS + 1
                )
            }
            startGuardianService()
            requestAdminRights()
        }
    }

    // ─── ADMIN ──────────────────────────────────────────
    private fun requestAdminRights() {
        if (!dpm.isAdminActive(adminComponent)) {
            val intent = Intent(
                DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN
            )
            intent.putExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                adminComponent
            )
            intent.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Guardian Eye a besoin des droits admin " +
                        "pour proteger votre appareil contre le vol."
            )
            startActivityForResult(intent, REQUEST_ADMIN)
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADMIN) {
            if (dpm.isAdminActive(adminComponent)) {
                Toast.makeText(
                    this,
                    "Protection admin activee",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ─── UI ─────────────────────────────────────────────
    private fun setupUI() {
        val prefs = getSharedPreferences(
            Constants.PREF_NAME, MODE_PRIVATE
        )
        val deviceId = prefs.getString(
            Constants.PREF_DEVICE_ID, "inconnu"
        ) ?: "inconnu"

        binding.tvDeviceId.text =
            getString(R.string.device_id_format, deviceId.take(8))

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(
                this@MainActivity,
                LoginActivity::class.java
            )
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun startGuardianService() {
        val serviceIntent = Intent(
            this@MainActivity,
            GuardianService::class.java
        )
        startForegroundService(serviceIntent)
    }

    // ─── STATUS ─────────────────────────────────────────
    private fun listenToStatus() {
        val prefs = getSharedPreferences(
            Constants.PREF_NAME, MODE_PRIVATE
        )
        val deviceId = prefs.getString(
            Constants.PREF_DEVICE_ID, ""
        ) ?: ""

        if (deviceId.isEmpty()) return

        database.reference
            .child("devices")
            .child(deviceId)
            .child("status")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot
                        .getValue(String::class.java)
                        ?: Constants.STATUS_NORMAL
                    updateStatusUI(status)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateStatusUI(status: String) {
        binding.tvCurrentStatus.text = status
        val color = when (status) {
            Constants.STATUS_STOLEN ->
                getColor(android.R.color.holo_red_light)
            Constants.STATUS_LOCKED ->
                getColor(android.R.color.holo_orange_light)
            else ->
                getColor(R.color.green_primary)
        }
        binding.tvCurrentStatus.setTextColor(color)
    }
}