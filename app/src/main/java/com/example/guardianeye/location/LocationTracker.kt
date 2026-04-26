package com.example.guardianeye.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.example.guardianeye.repository.FirebaseRepository
import com.example.guardianeye.utils.Constants
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocationTracker(private val context: Context) {

    private val fusedClient = LocationServices
        .getFusedLocationProviderClient(context)
    private val repository = FirebaseRepository()
    private var deviceId = ""

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        Constants.LOCATION_INTERVAL
    ).apply {
        setMinUpdateIntervalMillis(Constants.LOCATION_INTERVAL)
        setWaitForAccurateLocation(false)
    }.build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        repository.updateLocation(
                            deviceId,
                            location.latitude,
                            location.longitude
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking(deviceId: String) {
        this.deviceId = deviceId
        fusedClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopTracking() {
        fusedClient.removeLocationUpdates(locationCallback)
    }
}