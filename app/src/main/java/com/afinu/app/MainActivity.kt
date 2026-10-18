package com.afinu.app

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.afinu.app.feature.tuner.TunerScreen
import com.afinu.app.ui.theme.AfinadorTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * A permissão é pedida ao abrir, não antes de explicar. Um diálogo de
     * sistema sem contexto é a forma mais rápida de o usuário negar acesso
     * para sempre.
     */
    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* O estado da tela reflete a ausencia via MicState.hasPermission */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AfinadorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(Modifier.fillMaxSize()) {
                        TunerScreen(
                            onRequestPermission = {
                                requestPermission.launch(Manifest.permission.RECORD_AUDIO)
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Manter a tela acesa é parte do produto: um afinador que apaga no meio
        // da afinação obriga o usuário a tocar na tela a cada corda.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}