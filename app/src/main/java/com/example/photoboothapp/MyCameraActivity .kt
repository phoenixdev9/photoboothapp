package com.example.photoboothapp

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class MyCameraActivity : Activity() {
    private val imageUris: MutableList<Uri?> = ArrayList()
    private var textureView: TextureView? = null
    private var cameraDevice: CameraDevice? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private val capturedImages: MutableList<Bitmap?> = ArrayList()
    private val buttons: MutableList<ImageButton> = ArrayList()
    private var currentButtonIndex = 0
    private var cameraId: String? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("CameraConnect", "Camera is Created")
        setContentView(R.layout.mycameraactivity)

        textureView = findViewById(R.id.textureView)

        buttons.add(findViewById(R.id.button1))
        buttons.add(findViewById(R.id.button2))
        buttons.add(findViewById(R.id.button3))
        buttons.add(findViewById(R.id.button4))

        // Set a touch listener on the TextureView to start the auto capture sequence on screen tap
        textureView?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                startAutoCaptureSequence()
                // Disable further touches until the capture sequence is complete
                textureView?.isEnabled = false
            }
            true
        }

        // Request permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                CAMERA_REQUEST
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (textureView!!.isAvailable) {
            openCamera()
        } else {
            textureView!!.surfaceTextureListener = textureListener
        }
    }



    private val textureListener: TextureView.SurfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            closeCamera() // Close camera when surface is destroyed
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }

    private fun closeCamera() {
        try {
            cameraCaptureSession?.close()
            cameraCaptureSession = null
            cameraDevice?.close()
            cameraDevice = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var cameraResolutionWidth: Int = 0
    private var cameraResolutionHeight: Int = 0

    private fun openCamera() {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            val cameraIdList = manager.cameraIdList
            if (cameraIdList.isEmpty()) {
                Toast.makeText(this, "No camera found on this device.", Toast.LENGTH_SHORT).show()
                return
            }

            // Select the camera (front or back based on index)
            val cameraId = cameraIdList[1]
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            // Get the maximum supported resolution for the camera
            val largestSize = map?.getOutputSizes(SurfaceTexture::class.java)?.maxByOrNull { it.width * it.height }

            // Store the camera's maximum resolution
            if (largestSize != null) {
                cameraResolutionWidth = largestSize.width
                cameraResolutionHeight = largestSize.height
            }

            // Ensure the app has permission to open the camera
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST)
                return
            }

            // Open the camera
            manager.openCamera(cameraId, stateCallback, null)

        } catch (e: CameraAccessException) {
            e.printStackTrace()
            Toast.makeText(this, "Error accessing the camera.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Unexpected error occurred.", Toast.LENGTH_SHORT).show()
        }
    }




    private val stateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice!!.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice!!.close()
            cameraDevice = null
        }
    }

    private fun createCameraPreview() {
        try {
            val texture = textureView?.surfaceTexture ?: throw IllegalStateException("SurfaceTexture is null")
            texture.setDefaultBufferSize(textureView!!.width, textureView!!.height)
            val surface = Surface(texture)

            captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder!!.addTarget(surface)

            // Use different methods depending on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // For API 28 and above, use the new SessionConfiguration method
                val outputConfig = OutputConfiguration(surface)
                val sessionConfig = SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    listOf(outputConfig),
                    Executors.newSingleThreadExecutor(),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            if (cameraDevice == null) return
                            cameraCaptureSession = session
                            updatePreview()
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Toast.makeText(this@MyCameraActivity, "Camera configuration failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                cameraDevice!!.createCaptureSession(sessionConfig)
            } else {
                // For older Android versions, use the deprecated method
                cameraDevice!!.createCaptureSession(
                    listOf(surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            if (cameraDevice == null) return
                            cameraCaptureSession = session
                            updatePreview()
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Toast.makeText(this@MyCameraActivity, "Camera configuration failed.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    null
                )
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            Toast.makeText(this, "Camera access error: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Unexpected error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePreview() {
        if (cameraDevice == null) return
        captureRequestBuilder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        try {
            cameraCaptureSession?.setRepeatingRequest(captureRequestBuilder!!.build(), null, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to start camera preview.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun captureImage(buttonIndex: Int) {
        if (cameraDevice == null) return
        try {
            val captureBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(Surface(textureView!!.surfaceTexture))
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

            cameraCaptureSession!!.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult,
                ) {
                    // Get the bitmap from the TextureView
                    val capturedImage = textureView!!.bitmap

                    // Resize the bitmap to the camera's resolution
                    val resizedImage = Bitmap.createScaledBitmap(
                        capturedImage!!,
                        cameraResolutionWidth,
                        cameraResolutionHeight,
                        false
                    )

                    // Set the captured image to the button (thumbnail preview)
                    val button = buttons[buttonIndex]
                    button.setImageBitmap(
                        Bitmap.createScaledBitmap(
                            capturedImage,
                            button.width,
                            button.height,
                            false
                        )
                    )
                    button.isEnabled = false

                    // Save the resized image to the gallery
                    val imageUri = saveImageToGallery(resizedImage, buttonIndex)
                    imageUris.add(imageUri)

                    if (imageUris.size == 4) {
                        // Pass all URIs to PhotoListActivity
                        val intent = Intent(this@MyCameraActivity, PhotoListActivity::class.java)
                        for (i in imageUris.indices) {
                            intent.putExtra("image_uri_$i", imageUris[i].toString())
                        }
                        startActivity(intent)
                    }
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }


    private fun startAutoCaptureSequence() {
        // Start the countdown and capture the first photo
        capturePhotoWithCountdown(0)
    }

    private fun capturePhotoWithCountdown(photoIndex: Int) {
        if (photoIndex >= 4) {
            // All 4 photos have been captured
            return
        }

        // Set countdown time: 5 seconds for the first photo, 3 seconds for the rest
        val countdownTime = if (photoIndex == 0) 5 else 3

        // Show the countdown popup using the correct images for the photo index
        showCountdownPopup(countdownTime, photoIndex) {
            // Capture the photo after the countdown finishes
            captureImage(photoIndex)

            // Schedule the next capture after the delay
            textureView?.postDelayed({
                capturePhotoWithCountdown(photoIndex + 1)
            }, countdownTime * 1000L)
        }
    }


    private fun showCountdownPopup(seconds: Int, photoIndex: Int, onCountdownFinished: () -> Unit) {
        val alertTextView = findViewById<TextView>(R.id.alertTextView)
        alertTextView.visibility = View.VISIBLE

        var remainingSeconds = seconds
        updateCircleBackground(remainingSeconds, photoIndex, alertTextView) // Update the background based on initial time
        alertTextView.text = remainingSeconds.toString()

        // Animate the first countdown
        animateCircle(alertTextView)

        // Update the countdown text and circle background every second
        val countdownRunnable = object : Runnable {
            override fun run() {
                remainingSeconds--
                if (remainingSeconds > 0) {
                    alertTextView.text = remainingSeconds.toString()
                    updateCircleBackground(remainingSeconds, photoIndex, alertTextView)  // Update the background dynamically
                    animateCircle(alertTextView) // Apply the animation for each countdown tick
                    alertTextView.postDelayed(this, 1000)
                } else {
                    alertTextView.visibility = View.GONE
                    onCountdownFinished()
                }
            }
        }
        alertTextView.postDelayed(countdownRunnable, 1000)
    }

    private fun animateCircle(view: View) {
        // Animate the scale of the view (circle "pop" effect)
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.5f, 1.2f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.5f, 1.2f, 1.0f)

        // Optional: Add a fade-in/fade-out effect
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0.3f, 1.0f)

        // Create an animator set to play all animations together
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, alpha)
        animatorSet.duration = 800 // 800ms for the animation

        // Start the animation
        animatorSet.start()
    }

    private fun updateCircleBackground(remainingSeconds: Int, photoIndex: Int, alertTextView: TextView) {
        // Use circle5 series for the first photo
        if (photoIndex == 0) {
            when (remainingSeconds) {
                5 -> alertTextView.setBackgroundResource(R.drawable.circle5)
                4 -> alertTextView.setBackgroundResource(R.drawable.circle4)
                3 -> alertTextView.setBackgroundResource(R.drawable.circle3)
                2 -> alertTextView.setBackgroundResource(R.drawable.circle2)
                1 -> alertTextView.setBackgroundResource(R.drawable.circle1)
            }
        } else {
            // Use newcircle series for the remaining photos
            when (remainingSeconds) {
                3 -> alertTextView.setBackgroundResource(R.drawable.newcircle3)
                2 -> alertTextView.setBackgroundResource(R.drawable.newcircle2)
                1 -> alertTextView.setBackgroundResource(R.drawable.newcircle1)
            }
        }
    }



    private fun saveImageToGallery(bitmap: Bitmap?, index: Int): Uri? {
        val contentValues = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "CapturedImage_" + System.currentTimeMillis() + "_" + (index + 1) + ".jpg"
            )
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoBooth")
            }
        }

        val uri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            try {
                contentResolver.openOutputStream(uri).use { outputStream ->
                    if (outputStream != null) {
                        bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return uri
    }


    companion object {
        private const val CAMERA_REQUEST = 100
    }

    private fun alertFunc(msg: String, delayMillis: Long, onAlertDismissed: () -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(msg)
        val alertDialog: AlertDialog = builder.create()

        // Show the alert after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            alertDialog.show()
        }, delayMillis)

        // Dismiss the alert after a delay and execute the callback
        Handler(Looper.getMainLooper()).postDelayed({
            alertDialog.dismiss()
            onAlertDismissed()  // Call the callback after alert is dismissed
        }, delayMillis + 2000) // Assuming 2 seconds for the alert to be visible
    }

    private fun restartActivity() {
        val intent = Intent(this, MyCameraActivity::class.java)
        finish()  // Finish the current activity
        startActivity(intent)  // Start the activity again to restart it
    }

    private fun alertFunc1(msg: String, delayMillis: Long) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(msg)
        val alertDialog: AlertDialog = builder.create()

        // Delay showing the alert by delayMillis
        Handler(Looper.getMainLooper()).postDelayed({
            alertDialog.show()
        }, delayMillis)
    }
}
