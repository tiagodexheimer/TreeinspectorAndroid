package com.dexheimer.treeinspectorandroid.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dexheimer.treeinspectorandroid.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
	val isLoading: Boolean = false,
	val isLoggedIn: Boolean = false,
	val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
	private val loginUseCase: LoginUseCase
) : ViewModel() {

	private val _uiState = MutableStateFlow(LoginUiState())
	val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

	fun login(email: String, password: String) {
		_uiState.value = _uiState.value.copy(isLoading = true, error = null)

		viewModelScope.launch {
			val result = loginUseCase(email, password)

			result.fold(
				onSuccess = {
					_uiState.value = _uiState.value.copy(
						isLoading = false,
						isLoggedIn = true,
						error = null
					)
				},
				onFailure = { e ->
					_uiState.value = _uiState.value.copy(
						isLoading = false,
						error = e.message ?: "Erro de conexão"
					)
				}
			)
		}
	}
}