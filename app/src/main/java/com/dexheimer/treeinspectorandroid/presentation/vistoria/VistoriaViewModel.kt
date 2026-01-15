package com.dexheimer.treeinspectorandroid.presentation.vistoria

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dexheimer.treeinspectorandroid.data.local.FormularioDao
import com.dexheimer.treeinspectorandroid.data.remote.ApiService
import com.dexheimer.treeinspectorandroid.data.remote.FormField
import com.dexheimer.treeinspectorandroid.domain.model.Demanda
import com.dexheimer.treeinspectorandroid.domain.usecase.SalvarVistoriaUseCase
import com.dexheimer.treeinspectorandroid.data.local.VistoriaDraftDao
import android.util.Log
import com.dexheimer.treeinspectorandroid.data.local.VistoriaDraft
import com.dexheimer.treeinspectorandroid.data.local.VistoriaDao
import com.dexheimer.treeinspectorandroid.domain.usecase.SaveResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VistoriaUiState(
	val isLoading: Boolean = false,
	val formFields: List<FormField> = emptyList(),
	val error: String? = null,
	val saveResult: SaveResult? = null,
	val draft: Map<String, Any>? = null // Draft data (answers)
)

@HiltViewModel
class VistoriaViewModel @Inject constructor(
	private val formularioDao: FormularioDao,
	private val apiService: ApiService,
	private val salvarVistoriaUseCase: SalvarVistoriaUseCase,
	private val vistoriaDraftDao: VistoriaDraftDao,
	private val vistoriaDao: VistoriaDao
) : ViewModel() {

	private val _uiState = MutableStateFlow(VistoriaUiState())
	val uiState: StateFlow<VistoriaUiState> = _uiState.asStateFlow()

	fun buscarFormulario(tipoDemanda: String) {
		_uiState.value = _uiState.value.copy(isLoading = true)

		viewModelScope.launch {
			var campos: List<FormField>? = null

			// 1. Tenta API
			try {
				val response = apiService.getFormularioPorTipo(tipoDemanda)
				if (response.isSuccessful && response.body() != null) {
					campos = response.body()
					salvarFormularioNoCache(tipoDemanda, campos!!)
				}
			} catch (e: Exception) {
				// Falha de rede.
			}

			// 2. Tenta Cache Local
			if (campos == null) {
				val jsonCache = formularioDao.getFormularioJson(tipoDemanda)
				if (jsonCache != null) {
					try {
						val listType = object : TypeToken<List<FormField>>() {}.type
						campos = Gson().fromJson(jsonCache, listType)
					} catch (e: Exception) {
						// Erro ao ler cache.
					}
				}
			}

			// 3. Atualiza Estado
			if (!campos.isNullOrEmpty()) {
				_uiState.value = _uiState.value.copy(
					isLoading = false,
					formFields = campos,
					error = null
				)
			} else {
				_uiState.value = _uiState.value.copy(
					isLoading = false,
					formFields = emptyList(),
					error = "Formulário não disponível (Sem conexão e sem cache)."
				)
			}
		}
	}

	fun salvarVistoria(demanda: Demanda, respostas: Map<String, Any>) {
		_uiState.value = _uiState.value.copy(isLoading = true, saveResult = null)

		viewModelScope.launch {
			val result = salvarVistoriaUseCase(demanda, respostas)

			_uiState.value = _uiState.value.copy(
				isLoading = false,
				saveResult = result,
				error = if (result is SaveResult.Failure) result.message else null
			)
		}
	}

	private suspend fun salvarFormularioNoCache(tipo: String, campos: List<FormField>) {
		val json = Gson().toJson(campos)
		formularioDao.insertOrUpdate(tipo, json)
	}

	fun carregarRascunho(demandaId: Int) {
		viewModelScope.launch {
			// 1. Tenta carregar Draft (Prioridade: Trabalho em andamento)
			val draft = vistoriaDraftDao.getDraft(demandaId)
			if (draft != null) {
				try {
					val type = object : TypeToken<Map<String, Any>>() {}.type
					val respostas: Map<String, Any> = Gson().fromJson(draft.respostasJson, type)
					_uiState.value = _uiState.value.copy(draft = respostas)
					Log.d("VistoriaViewModel", "Draft loaded for demanda $demandaId: $respostas")
					return@launch
				} catch (e: Exception) {
					Log.e("VistoriaViewModel", "Error parsing draft", e)
				}
			} else {
				Log.d("VistoriaViewModel", "No draft found for demanda $demandaId")
			}

			// 2. Se não tem draft, tenta carregar Vistoria já realizada (Edição/Visualização)
			val vistoriaExistente = vistoriaDao.getVistoriaPorDemanda(demandaId)
			if (vistoriaExistente != null) {
				try {
					val type = object : TypeToken<Map<String, Any>>() {}.type
					val respostas: Map<String, Any> = Gson().fromJson(vistoriaExistente.jsonRespostas, type)
					_uiState.value = _uiState.value.copy(draft = respostas)
					Log.d("VistoriaViewModel", "Existing vistoria loaded for demanda $demandaId: $respostas")
				} catch (e: Exception) {
					Log.e("VistoriaViewModel", "Error parsing existing vistoria", e)
				}
			} else {
				Log.d("VistoriaViewModel", "No existing vistoria found for demanda $demandaId")
			}
		}
	}

	fun salvarRascunho(demandaId: Int, respostas: Map<String, Any>) {
		viewModelScope.launch(Dispatchers.IO) {
			try {
				val jsonRespostas = Gson().toJson(respostas)
				val draft = VistoriaDraft(
					demandaId = demandaId,
					respostasJson = jsonRespostas,
					fotosEstaticasJson = "",
					lastUpdated = System.currentTimeMillis()
				)
				vistoriaDraftDao.insertOrUpdate(draft)
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	fun limparRascunho(demandaId: Int) {
		viewModelScope.launch(Dispatchers.IO) {
			vistoriaDraftDao.deleteDraft(demandaId)
		}
	}
}