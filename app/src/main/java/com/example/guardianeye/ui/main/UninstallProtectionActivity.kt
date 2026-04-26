package com.example.guardianeye.ui.main

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.guardianeye.admin.DeviceAdminReceiver
import com.example.guardianeye.utils.Constants

class UninstallProtectionActivity : AppCompatActivity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dpm = getSystemService(
            Context.DEVICE_POLICY_SERVICE
        ) as DevicePolicyManager

        adminComponent = ComponentName(
            this,
            DeviceAdminReceiver::class.java
        )

        showSecretCodeDialog()
    }

    private fun showSecretCodeDialog() {
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Entrez le code secret"
        }

        AlertDialog.Builder(this)
            .setTitle("Protection Guardian Eye")
            .setMessage(
                "Entrez le code secret pour desactiver " +
                        "la protection administrateur"
            )
            .setView(input)
            .setPositiveButton("Confirmer") { _, _ ->
                val enteredCode = input.text.toString()
                verifySecretCode(enteredCode)
            }
            .setNegativeButton("Annuler") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun verifySecretCode(enteredCode: String) {
        val prefs = getSharedPreferences(
            Constants.PREF_NAME,
            Context.MODE_PRIVATE
        )
        val savedCode = prefs.getString(
            Constants.PREF_SECRET_CODE, ""
        ) ?: ""

        if (enteredCode == savedCode) {
            // Code correct - désactiver admin
            dpm.removeActiveAdmin(adminComponent)
            Toast.makeText(
                this,
                "Protection desactivee",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        } else {
            Toast.makeText(
                this,
                "Code incorrect - desinstallation bloquee",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }
}