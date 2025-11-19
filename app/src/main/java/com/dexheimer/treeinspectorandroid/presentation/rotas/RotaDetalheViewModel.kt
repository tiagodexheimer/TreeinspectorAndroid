package com.dexheimer.treeinspectorandroid.presentation.rotas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dexheimer.treeinspectorandroid.domain.model.Demanda
import com.dexheimer.treeinspectorandroid.domain.model.Rota
import com.dexheimer.treeinspectorandroid.domain.repository.DemandaRepository
import com.dexheimer.treeinspectorandroid.domain.repository.RotaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RotaDetalheUiState(
	val isLoading: Boolean = false,
	val rota: Rota? = null,
	val demandas: List<Demanda> = emptyList(),
	val demandasPendentes: List<Demanda> = emptyList(),
	val error: String? = null,
	val isSyncing: Boolean = false
)

@HiltViewModel
class RotaDetalheViewModel @Inject constructor(
	private val rotaRepository: RotaRepository,
	private val demandaRepository: DemandaRepository,
	savedStateHandle: SavedStateHandle // Para pegar o ID da rota passado via Intent automaticamente
) : ViewModel() {

	private val _uiState = MutableStateFlow(RotaDetalheUiState())
	val uiState: StateFlow<RotaDetalheUiState> = _uiState.asStateFlow()

	// O Hilt pega automaticamente o "ROTA_ID" que veio no Intent da Activity
	private val rotaId: Int = savedStateHandle.get<Int>("ROTA_ID") ?: -1

	init {
		if (rotaId != -1) {
			carregarDados()
		} else {
			_uiState.value = _uiState.value.copy(error = "ID da Rota inválido")
		}
	}

	fun carregarDados() {
		viewModelScope.launch {
			_uiState.value = _uiState.value.copy(isLoading = true)

			// Chama o repositório (que já decide se pega do banco ou API)
			val result = rotaRepository.getRotaDetalhes(rotaId)

			result.fold(
				onSuccess = { pair ->
					val rota = pair.first
					val demandas = pair.second
					atualizarListas(rota, demandas)
				},
				onFailure = { e ->
					_uiState.value = _uiState.value.copy(
						isLoading = false,
						error = e.message ?: "Erro ao carregar rota"
					)
				}
			)
		}
	}

	// Chamado quando voltamos da tela de vistoria para atualizar o status
	fun atualizarStatusDemanda(demandaId: Int, novoStatus: String) {
		viewModelScope.launch {
			// 1. Atualiza no banco
			demandaRepository.atualizarStatus(demandaId, novoStatus)

			// 2. Atualiza o estado em memória (para a UI reagir instantaneamente)
			val novasDemandas = _uiState.value.demandas.map {
				if (it.id == demandaId) it.copy(statusVistoria = novoStatus) else it
			}

			// Recalcula pendentes e atualiza UI
			atualizarListas(_uiState.value.rota, novasDemandas)
		}
	}

	private fun atualizarListas(rota: Rota?, demandas: List<Demanda>) {
		val pendentes = demandas.filter {
			it.statusVistoria.equals("pendente", ignoreCase = true)
		}.sortedBy {
			demandas.indexOf(it) // Mantém ordem original
		}

		_uiState.value = _uiState.value.copy(
			isLoading = false,
			rota = rota,
			demandas = demandas,
			demandasPendentes = pendentes,
			error = null
		)
	}
}