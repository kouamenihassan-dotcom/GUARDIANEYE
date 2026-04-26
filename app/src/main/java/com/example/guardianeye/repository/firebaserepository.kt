package com.example.guardianeye.repository

import android.net.Uri
import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await
import java.io.File

class FirebaseRepository {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance(
        "https://guardianeye-9d027-default-rtdb.firebaseio.com"
    )

    // ─── AUTH ───────────────────────────────────────────
    suspend fun login(email: String, password: String) =
        auth.signInWithEmailAndPassword(email, password).await()

    suspend fun register(email: String, password: String) =
        auth.createUserWithEmailAndPassword(email, password).await()

    fun getCurrentUser() = auth.currentUser

    fun logout() = auth.signOut()

    // ─── DEVICE ─────────────────────────────────────────
    suspend fun registerDevice(
        deviceId: String,
        fcmToken: String
    ) {
        val deviceData = mapOf(
            "deviceId"  to deviceId,
            "status"    to "NORMAL",
            "fcmToken"  to fcmToken,
            "lastSeen"  to System.currentTimeMillis(),
            "owner"     to (auth.currentUser?.uid ?: "")
        )
        database.getReference("devices/$deviceId")
            .setValue(deviceData).await()
    }

    suspend fun updateLocation(
        deviceId: String,
        lat: Double,
        lng: Double
    ) {
        val data = mapOf(
            "latitude"  to lat,
            "longitude" to lng,
            "timestamp" to System.currentTimeMillis()
        )
        database.getReference("devices/$deviceId/location")
            .setValue(data).await()
    }

    suspend fun updateStatus(
        deviceId: String,
        status: String
    ) {
        database.getReference("devices/$deviceId/status")
            .setValue(status).await()
    }

    // ─── PHOTO (Base64 gratuit) ──────────────────────────
    suspend fun uploadPhoto(deviceId: String, imageFile: File) {
        try {
            val timestamp = System.currentTimeMillis()

            // Convertir photo en Base64
            val bytes = imageFile.readBytes()
            val base64Image = Base64.encodeToString(
                bytes,
                Base64.DEFAULT
            )

            // Stocker dans Realtime Database
            database.getReference(
                "photos/$deviceId/$timestamp"
            ).setValue(mapOf(
                "image"     to base64Image,
                "timestamp" to timestamp,
                "deviceId"  to deviceId
            )).await()

            // Supprimer le fichier local
            imageFile.delete()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ─── LISTEN ─────────────────────────────────────────
    fun listenToStatus(
        deviceId: String,
        onStatus: (String) -> Unit
    ) {
        database.getReference("devices/$deviceId/status")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {
                    val status = snapshot
                        .getValue(String::class.java)
                        ?: "NORMAL"
                    onStatus(status)
                }
                override fun onCancelled(
                    error: DatabaseError
                ) {}
            })
    }
}