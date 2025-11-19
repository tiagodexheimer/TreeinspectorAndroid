package com.dexheimer.treeinspectorandroid.domain.usecase

import android.util.Log
import com.dexheimer.treeinspectorandroid.core.util.SessionManager
import com.dexheimer.treeinspectorandroid.data.remote.ApiService
import com.dexheimer.treeinspectorandroid.data.remote.LoginRequest
import javax.inject.Inject

// O UseCase é puro, só usa os Repositórios/Serviços
class LoginUseCase @Inject constructor(
	private val apiService: ApiService,
	private val sessionManager: SessionManager
) {
	suspend operator fun invoke(email: String, password: String): Result<Unit> {
		return try {
			// FASE 1: Obter Token CSRF
			val csrfResponse = apiService.getCsrfToken()
			if (!csrfResponse.isSuccessful || csrfResponse.body()?.csrfToken == null) {
				return Result.failure(Exception("Falha ao obter token de segurança."))
			}
			val csrfToken = csrfResponse.body()!!.csrfToken

			// FASE 2: Enviar Credenciais
			val request = LoginRequest(email = email, password = password, csrfToken = csrfToken)
			val loginResponse = apiService.login(request)

			// FASE 3: Processar Resposta (Lógica do NextAuth)
			if (loginResponse.isSuccessful) {
				sessionManager.setLoggedIn(true)
				Result.success(Unit)
			} else if (loginResponse.code() == 302) {
				val cookies = loginResponse.headers().values("Set-Cookie")
				val cookieString = cookies.find {
					it.contains("next-auth.session-token") || it.contains("authjs.session-token")
				}

				if (cookieString != null) {
					val rawToken = cookieString.split(";")[0]
					sessionManager.saveAuthToken(rawToken) // Salva o token para o Worker
					sessionManager.setLoggedIn(true)
					Result.success(Unit)
				} else {
					val location = loginResponse.headers()["Location"] ?: ""
					if (location.contains("error")) {
						Result.failure(Exception("Credenciais inválidas."))
					} else {
						sessionManager.setLoggedIn(true) // Assumir que o CookieJar pegou
						Result.success(Unit)
					}
				}
			} else if (loginResponse.code() == 401) {
				Result.failure(Exception("Email ou senha incorretos."))
			} else {
				Result.failure(Exception("Falha no login. Código: ${loginResponse.code()}"))
			}

		} catch (e: Exception) {
			Log.e("LoginUseCase", "Erro no login: ${e.message}", e)
			Result.failure(e)
		}
	}
}