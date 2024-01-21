package com.example.camera

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.camera.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var imageCapture: ImageCapture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (cameraPermissionsGranted()) {
            configureCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                PERMISSION_REQUEST_CODE
            )
        }

        with(binding) {
            btnTakePicture.setOnClickListener {
                takePicture()
            }
        }
    }

    private fun takePicture() {
        imageCapture.let {
            val pictureFile = getPictureFile(getOutputDirectory())

            val outputOptions = ImageCapture.OutputFileOptions.Builder(pictureFile).build()
            it.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this@MainActivity),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Glide.with(this@MainActivity).load(pictureFile).into(binding.imageView)
                        Log.e("MainActivity", "Image saved: ${pictureFile.absolutePath}")
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("MainActivity", "Image capture failed: ${exception.message}")
                    }
                }
            )
        }
    }

//    private fun cameraPermissionsGranted() = ContextCompat.checkSelfPermission(
//        this,
//        android.Manifest.permission.CAMERA
//    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun cameraPermissionsGranted() = CAMERA_PERMISSIONS_REQUESTED.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun configureCamera() {
        ProcessCameraProvider.getInstance(this@MainActivity).also { cameraProviderFuture ->
            cameraProviderFuture.addListener(
                { configureCameraUseCase(cameraProviderFuture.get()) },
                ContextCompat.getMainExecutor(this@MainActivity)
            )
        }
    }

    private fun configureCameraUseCase(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(binding.previewView.surfaceProvider)

        imageCapture = ImageCapture.Builder()
            .setTargetRotation(this@MainActivity.windowManager.defaultDisplay.rotation)
            .build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        cameraProvider.unbindAll()

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (cameraPermissionsGranted()) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getPictureFile(mediaDir: File): File =
        File(
            mediaDir,
            SimpleDateFormat(
                "yyyy-MM-dd-HH-mm-ss-SSS", java.util.Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

    private fun getOutputDirectory(): File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).let {
            File(it, resources.getString(R.string.app_name)).apply { mkdir() }
        }

    companion object {
        // , android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        private val CAMERA_PERMISSIONS_REQUESTED = arrayOf(android.Manifest.permission.CAMERA)
        private const val PERMISSION_REQUEST_CODE = 100
    }
}