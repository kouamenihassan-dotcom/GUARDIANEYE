package com.example.guardianeye.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.guardianeye.repository.FirebaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SilentCameraManager(private val context: Context) {

    private val repository = FirebaseRepository()
    private var cameraExecutor: ExecutorService =
        Executors.newSingleThreadExecutor()

    suspend fun takeSilentPhoto(deviceId: String) {
        withContext(Dispatchers.Main) {
            try {
                val cameraProviderFuture =
                    ProcessCameraProvider.getInstance(context)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(
                            ImageCapture
                                .CAPTURE_MODE_MINIMIZE_LATENCY
                        )
                        .build()

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(
                            CameraSelector.LENS_FACING_FRONT
                        )
                        .build()

                    try {
                        cameraProvider.unbindAll()
                        if (context is LifecycleOwner) {
                            cameraProvider.bindToLifecycle(
                                context,
                                cameraSelector,
                                imageCapture
                            )
                        }
                        capturePhoto(
                            imageCapture,
                            deviceId,
                            cameraProvider
                        )
                    } catch (e: Exception) {
                        Log.e("Camera", "Erreur: ${e.message}")
                    }
                }, ContextCompat.getMainExecutor(context))

            } catch (e: Exception) {
                Log.e("Camera", "Init erreur: ${e.message}")
            }
        }
    }

    private fun capturePhoto(
        imageCapture: ImageCapture,
        deviceId: String,
        cameraProvider: ProcessCameraProvider
    ) {
        val photoFile = File(
            context.cacheDir,
            "guardian_${System.currentTimeMillis()}.jpg"
        )
        val outputOptions = ImageCapture
            .OutputFileOptions
            .Builder(photoFile)
            .build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(
                    output: ImageCapture.OutputFileResults
                ) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            repository.uploadPhoto(
                                deviceId,
                                photoFile
                            )
                            photoFile.delete()
                            cameraProvider.unbindAll()
                        } catch (e: Exception) {
                            Log.e("Camera", "Upload: ${e.message}")
                        }
                    }
                }

                override fun onError(
                    exception: ImageCaptureException
                ) {
                    Log.e("Camera", "Capture: ${exception.message}")
                    cameraProvider.unbindAll()
                }
            }
        )
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }
}