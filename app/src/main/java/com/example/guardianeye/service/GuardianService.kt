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
import androidx.core.app.NotificationCompat
import com.example.guardianeye.admin.DeviceAdminReceiver
import com.example.guardianeye.camera.SilentCameraManager
import com.example.guardianeye.location.LocationTracker
import com.example.guardianeye.repository.FirebaseRepository
import com.example.guardianeye.utils.Constants
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

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        locationTracker = LocationTracker(this)
        cameraManager = SilentCameraManager(this)
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

        if (deviceId.isNotEmpty()) {
            listenForCommands()
        }
        return START_STICKY
    }

    private fun listenForCommands() {
        repository.listenToStatus(deviceId) { status ->
            when (status) {
                Constants.STATUS_STOLEN -> {
                    scope.launch {
                        locationTracker.startTracking(deviceId)
                        cameraManager.takeSilentPhoto(deviceId)
                    }
                }
                Constants.STATUS_LOCKED -> {
                    lockDevice()
                }
            }
        }
    }

    private fun lockDevice() {
        val dpm = getSystemService(
            Context.DEVICE_POLICY_SERVICE
        ) as DevicePolicyManager
        val adminComponent = ComponentName(
            this,
            DeviceAdminReceiver::class.java
        )
        if (dpm.isAdminActive(adminComponent)) {
            dpm.lockNow()
            android.util.Log.d("Guardian", "Appareil bloqué ✅")
        } else {
            android.util.Log.d("Guardian", "Admin pas actif ❌")
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            Constants.CHANNEL_ID,
            "Guardian Eye",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Protection active"
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
            .setContentText("Protection active")
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
    }
}