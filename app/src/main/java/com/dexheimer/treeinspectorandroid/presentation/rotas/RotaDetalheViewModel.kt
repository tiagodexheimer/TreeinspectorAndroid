package com.dexheimer.treeinspectorandroid.presentation.rotas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dexheimer.treeinspectorandroid.domain.model.Demanda
import com.dexheimer.treeinspectorandroid.domain.model.Rota
import com.dexheimer.treeinspectorandroid.domain.repository.DemandaRepository
import com.dexheimer.treeinspectorandroid.domain.repository.RotaRepository
import com.dexheimer.treeinspectorandroid.domain.usecase.SyncVistoriasUseCase
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
	private val syncVistoriasUseCase: SyncVistoriasUseCase,
	savedStateHandle: SavedStateHandle
) : ViewModel() {

	private val _uiState = MutableStateFlow(RotaDetalheUiState())
	val uiState: StateFlow<RotaDetalheUiState> = _uiState.asStateFlow()

	private val rotaId: Int = savedStateHandle.get<Int>("ROTA_ID") ?: -1

	init {
		if (rotaId != -1) {
			carregarDados()
			sincronizar() // Auto-sync ao abrir a rota
		} else {
			_uiState.value = _uiState.value.copy(error = "ID da Rota inválido")
		}
	}

	fun sincronizar() {
		viewModelScope.launch {
			_uiState.value = _uiState.value.copy(isSyncing = true)
			
			// 1. Envia vistorias pendentes
			syncVistoriasUseCase()
			
			// 2. Atualiza dados da rota
			val result = rotaRepository.getRotaDetalhes(rotaId)
			result.onSuccess { pair ->
				atualizarListas(pair.first, pair.second)
			}
			
			_uiState.value = _uiState.value.copy(isSyncing = false)
		}
	}

	fun carregarDados() {
		viewModelScope.launch {
			_uiState.value = _uiState.value.copy(isLoading = true)
			val result = rotaRepository.getRotaDetalhes(rotaId)
			result.fold(
				onSuccess = { pair ->
					val rota = pair.first
					val demandas = pair.second
					atualizarListas(rota, demandas)
				},
				onFailure = { e ->
					_uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Erro ao carregar rota")
				}
			)
		}
	}

	fun atualizarStatusDemanda(demandaId: Int, novoStatus: String) {
		viewModelScope.launch {
			demandaRepository.atualizarStatus(demandaId, novoStatus)
			val demandasNaOrdemAtual = _uiState.value.demandas.map {
				if (it.id == demandaId) it.copy(statusVistoria = novoStatus) else it
			}
			atualizarListas(_uiState.value.rota, demandasNaOrdemAtual)
		}
	}

	// --- [NOVO] Função adicionada aqui dentro do ViewModel ---
	fun iniciarAtendimento(demandaId: Int) {
		viewModelScope.launch {
			// 1. Atualiza Banco
			demandaRepository.atualizarStatus(demandaId, "Em Rota")

			// 2. Atualiza Memória
			val listaAtual = _uiState.value.demandas
			val novaLista = listaAtual.map { demanda ->
				if (demanda.id == demandaId) {
					// Cria uma cópia com o novo status
					demanda.copy(
						statusVistoria = "Em Rota",
						statusNome = "Em Rota" // Atualiza o nome visual também se houver
					)
				} else {
					demanda
				}
			}

			// 3. Atualiza StateFlow
			// Nota: Precisamos ajustar a lógica de "pendentes" para que o card continue aparecendo
			// Se "Em Rota" sumir da lista de pendentes, o card some.
			val pendentesAtualizados = novaLista.filter {
				val s = it.statusVistoria?.lowercase() ?: ""
				// Mantém na lista se for "pendente" OU "em rota"
				s == "pendente" || s == "em rota"
			}

			_uiState.value = _uiState.value.copy(
				demandas = novaLista,
				demandasPendentes = pendentesAtualizados
			)
		}
	}
	// --------------------------------------------------------

	fun otimizarRota(userLat: Double?, userLng: Double?) {
		val listaAtual = _uiState.value.demandas
		if (listaAtual.isEmpty()) return

		val concluidas = listaAtual.filter { !it.statusVistoria.equals("pendente", ignoreCase = true) }
		val pendentes = listaAtual.filter { it.statusVistoria.equals("pendente", ignoreCase = true) }.toMutableList()

		if (pendentes.isEmpty()) return

		val pendentesOrdenadas = mutableListOf<Demanda>()
		var pontoDeReferencia: Demanda? = null
		var primeiraDemandaOtimizada: Demanda? = null

		if (userLat != null && userLng != null) {
			val pontoUsuario = Demanda(
				id = -1, lat = userLat, lng = userLng, statusVistoria = "temp", rotaId = rotaId,
				statusNome = null, statusCor = null, protocolo = null, nomeSolicitante = null,
				telefoneSolicitante = null, emailSolicitante = null, prazo = null, dataCriacao = null,
				dataAtualizacao = null, cep = null, logradouro = null, numero = null,
				complemento = null, bairro = null, cidade = null, uf = null, tipoDemanda = null,
				descricao = null, geom = null, anexos = null
			)
			primeiraDemandaOtimizada = pendentes.minByOrNull { calcularDistancia(pontoUsuario, it) }
		}

		if (primeiraDemandaOtimizada != null) {
			pendentes.remove(primeiraDemandaOtimizada)
			pendentesOrdenadas.add(primeiraDemandaOtimizada)
			pontoDeReferencia = primeiraDemandaOtimizada
		} else if (concluidas.isNotEmpty()) {
			pontoDeReferencia = concluidas.last()
		} else if (pendentes.isNotEmpty()) {
			pontoDeReferencia = pendentes.removeAt(0).also { pendentesOrdenadas.add(it) }
		}

		if (pontoDeReferencia != null) {
			var pontoAtual = pontoDeReferencia!!
			while (pendentes.isNotEmpty()) {
				val vizinhoMaisProximo = pendentes.minByOrNull { calcularDistancia(pontoAtual, it) }
				vizinhoMaisProximo?.let {
					pendentesOrdenadas.add(it)
					pendentes.remove(it)
					pontoAtual = it
				}
			}
		}
		val novaListaCompleta = concluidas + pendentesOrdenadas
		atualizarListas(_uiState.value.rota, novaListaCompleta)
	}

	private fun calcularDistancia(d1: Demanda, d2: Demanda): Double {
		val lat1 = d1.lat ?: 0.0
		val lng1 = d1.lng ?: 0.0
		val lat2 = d2.lat ?: 0.0
		val lng2 = d2.lng ?: 0.0
		return sqrt((lat1 - lat2).pow(2) + (lng1 - lng2).pow(2))
	}

	private fun atualizarListas(rota: Rota?, demandas: List<Demanda>) {
		// [CORREÇÃO] O filtro agora aceita "pendente" OU "em rota"
		// Isso garante que a demanda que você acabou de iniciar continue aparecendo no card "Próxima Parada"
		val pendentes = demandas.filter {
			val status = it.statusVistoria?.lowercase() ?: ""
			status == "pendente" || status == "em rota"
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