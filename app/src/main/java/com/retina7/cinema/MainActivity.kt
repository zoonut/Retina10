package com.retina7.cinema

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import com.retina7.cinema.ui.screens.CameraScreen
import com.retina7.cinema.ui.theme.Retina7CinemaTheme
import com.retina7.cinema.ui.viewmodel.CameraViewModel

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM) // API 35 (Android 15) per Prompt.txt
class MainActivity : ComponentActivity() {

    private val viewModel: CameraViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (!cameraGranted || !audioGranted) {
            Toast.makeText(
                this,
                "Camera & Microphone permissions are required for Retina 7 Cinema Recording.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request runtime permissions
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )

        setContent {
            Retina7CinemaTheme {
                CameraScreen(viewModel = viewModel)
            }
        }
    }
}
