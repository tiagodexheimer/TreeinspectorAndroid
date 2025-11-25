package com.dexheimer.treeinspectorandroid.presentation.rotas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dexheimer.treeinspectorandroid.data.local.VistoriaDao
import com.dexheimer.treeinspectorandroid.domain.model.Rota
import com.dexheimer.treeinspectorandroid.domain.repository.RotaRepository
import com.dexheimer.treeinspectorandroid.domain.usecase.SyncVistoriasUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RotasUiState(
	val isLoading: Boolean = false,
	val rotas: List<Rota> = emptyList(),
	val error: String? = null,
	val pendingUploads: Int = 0 // Quantas vistorias faltam subir
)

@HiltViewModel
class RotasViewModel @Inject constructor(
	private val repository: RotaRepository,
	private val vistoriaDao: VistoriaDao,
	private val syncVistoriasUseCase: SyncVistoriasUseCase
) : ViewModel() {

	private val _uiState = MutableStateFlow(RotasUiState())
	val uiState: StateFlow<RotasUiState> = _uiState.asStateFlow()

	init {
		carregarRotas()
		verificarPendencias()
	}

	// Carrega do banco (rápido)
	fun carregarRotas() {
		viewModelScope.launch {
			_uiState.value = _uiState.value.copy(isLoading = true)
			repository.getRotas().fold(
				onSuccess = { rotas ->
					_uiState.value = _uiState.value.copy(isLoading = false, rotas = rotas)
				},
				onFailure = { e ->
					_uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
				}
			)
			verificarPendencias()
		}
	}

	// Ação do SwipeRefresh (Lento, mas completo)
	fun forcarSincronizacaoCompleta() {
		viewModelScope.launch {
			_uiState.value = _uiState.value.copy(isLoading = true)

			// 1. Tenta enviar o que está pendente primeiro
			syncVistoriasUseCase()

			// 2. Baixa tudo novo (Rotas + Demandas + Formulários)
			val syncResult = repository.sincronizarRotas()

			syncResult.fold(
				onSuccess = {
					// Recarrega a lista atualizada do banco
					carregarRotas()
				},
				onFailure = { e ->
					_uiState.value = _uiState.value.copy(
						isLoading = false,
						error = "Erro na sincronização: ${e.message}"
					)
				}
			)
			verificarPendencias()
		}
	}

	private fun verificarPendencias() {
		viewModelScope.launch {
			val qtd = vistoriaDao.contarPendentes() // Crie este método no DAO: "SELECT COUNT(*) FROM vistorias_realizadas"
			_uiState.value = _uiState.value.copy(pendingUploads = qtd)
		}
	}
}