package com.dexheimer.treeinspectorandroid.domain.usecase

import android.util.Log
import com.dexheimer.treeinspectorandroid.core.util.SessionManager
import com.dexheimer.treeinspectorandroid.data.remote.ApiService
import com.dexheimer.treeinspectorandroid.data.remote.LoginRequest
import javax.inject.Inject

class LoginUseCase @Inject constructor(
	private val apiService: ApiService,
	private val sessionManager: SessionManager
) {
	suspend operator fun invoke(email: String, password: String): Result<Unit> {
		return try {
			// FASE 1: (Opcional) Obter Token CSRF se necessário, ou enviar string vazia
			// A rota mobile-login ignora CSRF, então podemos simplificar
			val request = LoginRequest(email = email, password = password)

			val response = apiService.login(request)

			if (response.isSuccessful && response.body()?.success == true) {
				// SUCESSO!
				// O AuthInterceptor JÁ CAPTUROU o cookie e salvou no SessionManager.
				// Não precisamos fazer nada manual aqui além de confirmar o login.
				sessionManager.setLoggedIn(true)
				Result.success(Unit)
			} else {
				// Tenta ler a mensagem de erro do corpo da resposta
				val errorMsg = response.errorBody()?.string() ?: "Erro desconhecido"
				Result.failure(Exception("Login falhou: $errorMsg"))
			}

		} catch (e: Exception) {
			Log.e("LoginUseCase", "Erro no login: ${e.message}", e)
			Result.failure(e)
		}
	}
}