package com.example.guardianeye.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.guardianeye.admin.DeviceAdminReceiver
import com.example.guardianeye.camera.SilentCameraManager
import com.example.guardianeye.location.LocationTracker
import com.example.guardianeye.repository.FirebaseRepository
import com.example.guardianeye.ui.main.LockedActivity
import com.example.guardianeye.utils.Constants
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class GuardianService : Service() {

    private val scope = CoroutineScope(
        Dispatchers.IO + SupervisorJob()
    )
    private val repository = FirebaseRepository()
    private lateinit var locationTracker: LocationTracker
    private lateinit var cameraManager: SilentCameraManager
    private var deviceId = ""
    private var isTracking = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        locationTracker = LocationTracker(this)
        cameraManager = SilentCameraManager(this)
        Log.d("GuardianService", "Service créé ✅")
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        startForeground(
            Constants.NOTIFICATION_ID,
            buildNotification()
        )

        val prefs = getSharedPreferences(
            Constants.PREF_NAME,
            Context.MODE_PRIVATE
        )
        deviceId = prefs.getString(
            Constants.PREF_DEVICE_ID, ""
        ) ?: ""

        Log.d("GuardianService", "DeviceId: $deviceId")

        if (deviceId.isNotEmpty()) {
            listenForCommands()
        }
        return START_STICKY
    }

    private fun listenForCommands() {
        Log.d("GuardianService", "Écoute des commandes...")

        FirebaseDatabase.getInstance(Constants.DATABASE_URL)
            .reference
            .child("devices")
            .child(deviceId)
            .child("status")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {
                    val status = snapshot
                        .getValue(String::class.java)
                        ?: "NORMAL"

                    Log.d("GuardianService",
                        "Status reçu: $status")

                    when (status) {
                        Constants.STATUS_STOLEN -> {
                            if (!isTracking) {
                                isTracking = true
                                Log.d("GuardianService",
                                    "Mode VOLÉ activé !")
                                startStolenMode()
                            }
                        }
                        Constants.STATUS_LOCKED -> {
                            Log.d("GuardianService",
                                "Blocage demandé !")
                            lockDevice()
                        }
                        Constants.STATUS_NORMAL -> {
                            isTracking = false
                            locationTracker.stopTracking()
                            Log.d("GuardianService",
                                "Mode normal")
                        }
                    }
                }

                override fun onCancelled(
                    error: DatabaseError
                ) {
                    Log.e("GuardianService",
                        "Erreur: ${error.message}")
                }
            })
    }

    private fun startStolenMode() {
        scope.launch {
            try {
                Log.d("GuardianService",
                    "Démarrage GPS...")
                locationTracker.startTracking(deviceId)

                Log.d("GuardianService",
                    "Capture photo...")
                cameraManager.takeSilentPhoto(deviceId)

            } catch (e: Exception) {
                Log.e("GuardianService",
                    "Erreur mode volé: ${e.message}")
            }
        }
    }

    private fun lockDevice() {
        try {
            val dpm = getSystemService(
                Context.DEVICE_POLICY_SERVICE
            ) as DevicePolicyManager
            val adminComponent = ComponentName(
                this,
                DeviceAdminReceiver::class.java
            )
            if (dpm.isAdminActive(adminComponent)) {
                dpm.lockNow()

                val intent = Intent(
                    this,
                    LockedActivity::class.java
                ).apply {
                    flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                Intent.FLAG_ACTIVITY_NO_HISTORY
                }
                startActivity(intent)
                Log.d("GuardianService",
                    "Appareil bloqué ✅")
            } else {
                Log.e("GuardianService",
                    "Admin pas actif ❌")
            }
        } catch (e: Exception) {
            Log.e("GuardianService",
                "Erreur blocage: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            Constants.CHANNEL_ID,
            "Guardian Eye Protection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Protection active en arrière-plan"
        }
        val manager = getSystemService(
            NotificationManager::class.java
        )
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat
            .Builder(this, Constants.CHANNEL_ID)
            .setContentTitle("Guardian Eye")
            .setContentText("Protection active ✅")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        locationTracker.stopTracking()
        Log.d("GuardianService", "Service détruit ⚠️")
    }
}