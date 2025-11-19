package com.dexheimer.treeinspectorandroid.presentation.rotas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dexheimer.treeinspectorandroid.domain.model.Rota
import com.dexheimer.treeinspectorandroid.domain.repository.RotaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Define os estados possíveis da tela
sealed class RotasUiState {
	object Loading : RotasUiState()
	data class Success(val rotas: List<Rota>) : RotasUiState()
	data class Error(val message: String) : RotasUiState()
	object Empty : RotasUiState()
}

@HiltViewModel
class RotasViewModel @Inject constructor(
	private val repository: RotaRepository
) : ViewModel() {

	private val _uiState = MutableStateFlow<RotasUiState>(RotasUiState.Loading)
	val uiState: StateFlow<RotasUiState> = _uiState.asStateFlow()

	init {
		carregarRotas()
	}

	fun carregarRotas() {
		viewModelScope.launch {
			_uiState.value = RotasUiState.Loading

			val result = repository.getRotas()

			result.fold(
				onSuccess = { lista ->
					if (lista.isEmpty()) {
						_uiState.value = RotasUiState.Empty
					} else {
						_uiState.value = RotasUiState.Success(lista)
					}
				},
				onFailure = { exception ->
					_uiState.value = RotasUiState.Error(exception.message ?: "Erro desconhecido")
				}
			)
		}
	}
}