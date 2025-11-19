package com.dexheimer.treeinspectorandroid.presentation.demandas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dexheimer.treeinspectorandroid.domain.model.Demanda
import com.dexheimer.treeinspectorandroid.domain.repository.DemandaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DemandasListUiState(
	val isLoading: Boolean = false,
	val demandas: List<Demanda> = emptyList(),
	val error: String? = null
)

@HiltViewModel
class DemandasListViewModel @Inject constructor(
	private val repository: DemandaRepository,
	savedStateHandle: SavedStateHandle
) : ViewModel() {

	private val _uiState = MutableStateFlow(DemandasListUiState())
	val uiState: StateFlow<DemandasListUiState> = _uiState.asStateFlow()

	private val rotaId: Int = savedStateHandle.get<Int>("ROTA_ID") ?: -1

	init {
		carregarDemandas()
	}

	fun carregarDemandas() {
		if (rotaId == -1) {
			_uiState.value = _uiState.value.copy(error = "ID da Rota inválido")
			return
		}

		viewModelScope.launch {
			_uiState.value = _uiState.value.copy(isLoading = true)

			try {
				// Chama o repositório (que carrega do cache local)
				val listaCompleta = repository.getDemandasDaRota(rotaId)

				// Ordena para mostrar pendentes primeiro (mesma lógica do código antigo)
				val listaOrdenada = listaCompleta.sortedBy { it.statusVistoria != "pendente" }

				_uiState.value = _uiState.value.copy(
					isLoading = false,
					demandas = listaOrdenada,
					error = null
				)
			} catch (e: Exception) {
				_uiState.value = _uiState.value.copy(
					isLoading = false,
					error = e.message ?: "Erro ao carregar demandas"
				)
			}
		}
	}

	// Chamado após a vistoria ser concluída
	fun atualizarStatusDemanda(demandaId: Int, novoStatus: String) {
		viewModelScope.launch {
			// 1. Atualiza no banco através do repositório
			repository.atualizarStatus(demandaId, novoStatus)

			// 2. Atualiza a lista em memória (DOMÍNIO)
			val listaAtualizada = _uiState.value.demandas.map { demanda ->
				if (demanda.id == demandaId) {
					demanda.copy(statusVistoria = novoStatus)
				} else {
					demanda
				}
			}

			// 3. Reordena e atualiza o estado da UI
			val listaOrdenada = listaAtualizada.sortedBy { it.statusVistoria != "pendente" }

			_uiState.value = _uiState.value.copy(
				demandas = listaOrdenada
			)
		}
	}
}