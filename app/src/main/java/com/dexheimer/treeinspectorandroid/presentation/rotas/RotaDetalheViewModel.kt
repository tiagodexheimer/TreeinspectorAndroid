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
import kotlin.math.pow
import kotlin.math.sqrt

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

	/**
	 * Algoritmo do Vizinho Mais Próximo (Nearest Neighbor).
	 * Reordena as demandas pendentes baseando-se na proximidade geográfica,
	 * partindo da última demanda concluída (ou da primeira da lista).
	 */
	fun otimizarRota() {
		val listaAtual = _uiState.value.demandas
		if (listaAtual.isEmpty()) return

		// 1. Separa concluídas de pendentes
		val concluidas = listaAtual.filter { it.statusVistoria != "pendente" }
		val pendentes = listaAtual.filter { it.statusVistoria == "pendente" }.toMutableList()

		if (pendentes.isEmpty()) return

		// 2. Ordenação
		val pendentesOrdenadas = mutableListOf<Demanda>()

		// Ponto de partida: última concluída ou a primeira pendente (se nenhuma foi feita ainda)
		var pontoAtual = if (concluidas.isNotEmpty()) {
			concluidas.last()
		} else {
			// Remove a primeira para ser o ponto de partida
			pendentes.removeAt(0).also { pendentesOrdenadas.add(it) }
		}

		while (pendentes.isNotEmpty()) {
			// Encontra qual das pendentes restantes está mais perto do pontoAtual
			val vizinhoMaisProximo = pendentes.minByOrNull { candidato ->
				calcularDistancia(pontoAtual, candidato)
			}

			vizinhoMaisProximo?.let {
				pendentesOrdenadas.add(it)
				pendentes.remove(it)
				pontoAtual = it // O vizinho vira o novo ponto de referência
			}
		}

		// 3. Reconstrói a lista completa: Concluídas (fixas) + Pendentes (reordenadas)
		val novaListaCompleta = concluidas + pendentesOrdenadas

		// 4. Atualiza a UI com a nova ordem
		atualizarListas(_uiState.value.rota, novaListaCompleta)
	}

	// Cálculo simples de distância Euclidiana (suficiente para ordenação visual local)
	private fun calcularDistancia(d1: Demanda, d2: Demanda): Double {
		val lat1 = d1.lat ?: 0.0
		val lng1 = d1.lng ?: 0.0
		val lat2 = d2.lat ?: 0.0
		val lng2 = d2.lng ?: 0.0

		return sqrt((lat1 - lat2).pow(2) + (lng1 - lng2).pow(2))
	}

	private fun atualizarListas(rota: Rota?, demandas: List<Demanda>) {
		// Filtra as pendentes mantendo a ordem da lista 'demandas' passada
		// (Removemos o .sortedBy antigo para que a otimização funcione)
		val pendentes = demandas.filter {
			it.statusVistoria.equals("pendente", ignoreCase = true)
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