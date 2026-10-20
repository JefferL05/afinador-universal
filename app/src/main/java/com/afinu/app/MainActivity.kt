package com.afinu.app

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.afinu.app.core.permission.MicPermission
import com.afinu.app.data.prefs.SettingsRepository
import com.afinu.app.ui.navigation.AppRoot
import com.afinu.app.ui.theme.AfinadorTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var micPermission: MicPermission
    @Inject lateinit var settingsRepository: SettingsRepository

    /**
     * A permissão é pedida depois da explicação, nunca antes. Um diálogo de
     * sistema sem contexto é a forma mais rápida de o usuário negar acesso
     * para sempre, e não há como perguntar de novo.
     *
     * A resposta **tem** que chegar ao afinador. Antes o callback era um bloco
     * vazio e o comentário dizia que "o estado da tela reflete a ausência" —
     * mas nada observava esse estado, então conceder a permissão não recuperava
     * a tela e só um reinício do app resolvia.
     *
     * O acesso a [micPermission] dentro do lambda é seguro apesar de `lateinit`:
     * a injeção acontece no `super.onCreate()`, e o diálogo só fecha depois,
     * quando o campo já está preenchido.
     */
    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        micPermission.registrarResposta(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // "Manter tela acesa" é preferência do usuário, não constante. Antes o
        // `addFlags` rodava incondicionalmente no `onResume`: o ajuste gravava
        // no DataStore, aparecia com toggle nos Ajustes e nunca era lido — um
        // botão decorativo.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.settings.collect { ajustes ->
                    if (ajustes.keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }
        }

        setContent {
            AfinadorTheme {
                AppRoot(onRequestPermission = {
                    requestPermission.launch(Manifest.permission.RECORD_AUDIO)
                })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // A permissão pode mudar sem passar por este diálogo: Ajustes do
        // sistema, painel de permissões ou o próprio usuário. Reavaliar aqui
        // evita o afinador preso ouvindo silêncio depois de uma revogação.
        micPermission.registrarResposta(micPermission.concedida())
    }
}
