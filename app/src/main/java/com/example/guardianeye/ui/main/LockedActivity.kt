package com.example.guardianeye.ui.main

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.guardianeye.admin.DeviceAdminReceiver
import com.example.guardianeye.databinding.ActivityLockedBinding
import com.example.guardianeye.utils.Constants
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LockedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockedBinding
    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dpm = getSystemService(
            Context.DEVICE_POLICY_SERVICE
        ) as DevicePolicyManager

        adminComponent = ComponentName(
            this,
            DeviceAdminReceiver::class.java
        )

        // Afficher par dessus tout
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        // Bloquer en fullscreen
        window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                        android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        listenForUnlock()
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
                    if (status == Constants.STATUS_NORMAL) {
                        finish()
                    }
                }
                override fun onCancelled(
                    error: DatabaseError
                ) {}
            })
    }

    // Bloquer tous les boutons
    override fun onBackPressed() {}

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_BACK -> true
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onUserLeaveHint() {
        // Empêcher de quitter via bouton home
        val intent = android.content.Intent(
            this,
            LockedActivity::class.java
        ).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    override fun onPause() {
        super.onPause()
        // Revenir sur l'écran de blocage
        val intent = android.content.Intent(
            this,
            LockedActivity::class.java
        ).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
}