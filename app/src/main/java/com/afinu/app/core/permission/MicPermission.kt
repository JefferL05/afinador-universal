package com.afinu.app.core.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estado real da permissão de microfone, compartilhado entre a Activity e o
 * ViewModel do afinador.
 *
 * ## Por que este objeto existe
 * O diálogo de permissão é um componente do sistema e a resposta dele chega
 * só na `MainActivity`. Quem precisa reagir é o `TunerViewModel`, que é
 * escopado ao destino da navegação — a Activity não tem como alcançá-lo
 * diretamente, e descer a resposta pela árvore de Composables empurraria um
 * parâmetro por quatro níveis.
 *
 * Este singleton é a ponte: a Activity publica a resposta, o ViewModel coleta.
 *
 * ## Por que a permissão é sempre LIDA, nunca lembrada
 * [concedida] consulta o `PackageManager` a cada chamada. O estado pode mudar
 * fora do diálogo — Ajustes do sistema, painel de permissões, outro app
 * revogando. Guardar um `Boolean` em memória é o que transformou o
 * "Tentar de novo" num botão sem efeito.
 */
@Singleton
class MicPermission @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val _resposta = MutableStateFlow<Boolean?>(null)

    /**
     * Última resposta do diálogo de sistema, ou `null` se ainda não houve
     * nenhum pedido nesta execução. Só serve para **disparar** a reavaliação:
     * o valor verdadeiro vem sempre de [concedida].
     */
    val resposta: StateFlow<Boolean?> = _resposta.asStateFlow()

    /** O sistema considera o microfone acessível agora? */
    fun concedida(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    /** Chamado pela Activity quando o diálogo do sistema fecha. */
    fun registrarResposta(concedida: Boolean) {
        _resposta.value = concedida
    }
}
