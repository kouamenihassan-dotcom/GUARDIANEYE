package com.example.guardianeye.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.guardianeye.databinding.ActivityRegisterBinding
import com.example.guardianeye.service.GuardianService
import com.example.guardianeye.ui.main.MainActivity
import com.example.guardianeye.utils.Constants
import com.example.guardianeye.viewmodel.AuthViewModel
import com.google.firebase.messaging.FirebaseMessaging
import java.util.UUID

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupClickListeners()
        setupObservers()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text
                .toString().trim()
            val password = binding.etPassword.text
                .toString().trim()
            val secretCode = binding.etSecretCode.text
                .toString().trim()

            if (email.isEmpty()) {
                binding.tilEmail.error = "Email requis"
                return@setOnClickListener
            }
            if (password.length < 6) {
                binding.tilPassword.error =
                    "Minimum 6 caracteres"
                return@setOnClickListener
            }
            if (secretCode.isEmpty()) {
                binding.tilSecretCode.error =
                    "Code secret requis"
                return@setOnClickListener
            }

            binding.tilEmail.error = null
            binding.tilPassword.error = null
            binding.tilSecretCode.error = null

            val prefs = getSharedPreferences(
                Constants.PREF_NAME,
                MODE_PRIVATE
            )
            val deviceId = UUID.randomUUID().toString()
            prefs.edit()
                .putString(Constants.PREF_SECRET_CODE, secretCode)
                .putString(Constants.PREF_DEVICE_ID, deviceId)
                .putBoolean(Constants.PREF_IS_REGISTERED, true)
                .apply()

            viewModel.register(email, password)
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility =
                if (loading) View.VISIBLE else View.GONE
            binding.btnRegister.isEnabled = !loading
        }

        viewModel.registerSuccess.observe(this) { success ->
            if (success) {
                registerDeviceAndStart()
            }
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(
                    this@RegisterActivity,
                    it,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun registerDeviceAndStart() {
        val prefs = getSharedPreferences(
            Constants.PREF_NAME,
            MODE_PRIVATE
        )
        val deviceId = prefs.getString(
            Constants.PREF_DEVICE_ID, ""
        ) ?: ""

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                viewModel.registerDevice(deviceId, token)
                goToMain()
            }
            .addOnFailureListener {
                goToMain()
            }
    }

    private fun startGuardianService() {
        val serviceIntent = Intent(
            this@RegisterActivity,
            GuardianService::class.java
        )
        startForegroundService(serviceIntent)
    }

    private fun goToMain() {
        startGuardianService()
        val intent = Intent(
            this@RegisterActivity,
            MainActivity::class.java
        )
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}