package com.dexheimer.treeinspectorandroid.core.di

import android.content.Context
import android.content.Intent
import com.dexheimer.treeinspectorandroid.core.util.SessionManager
import com.dexheimer.treeinspectorandroid.presentation.login.MainActivity
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
	private val context: Context,
	private val sessionManager: SessionManager
) : Interceptor {

	override fun intercept(chain: Interceptor.Chain): Response {
		val originalRequest = chain.request()
		val requestBuilder = originalRequest.newBuilder()

		// --- 1. INJEÇÃO DE COOKIE (Para todas as chamadas) ---
		// Se temos um cookie salvo, anexamos ele no cabeçalho
		sessionManager.getCookieString()?.let { cookie ->
			requestBuilder.addHeader("Cookie", cookie)
		}

		val response = chain.proceed(requestBuilder.build())

		val url = originalRequest.url.toString()
		val isMinhaApi = url.contains("treeinspector.com.br") ||
				url.contains("10.0.2.2") ||
				url.contains("localhost")

		// --- 2. CAPTURA DE COOKIE (Apenas no Login) ---
		if (url.contains("mobile-login") && response.headers("Set-Cookie").isNotEmpty()) {
			val cookies = response.headers("Set-Cookie")
			// O servidor manda algo como "nome=valor; Path=/; HttpOnly"
			// Nós precisamos pegar apenas a parte "nome=valor" e juntar tudo com "; "
			val cookieString = cookies.joinToString("; ") { it.substringBefore(";") }

			sessionManager.saveCookieString(cookieString)
			sessionManager.setLoggedIn(true)
		}

		// --- 3. TRATAMENTO DE ERRO (Logout se 401) ---
		if (isMinhaApi && (response.code == 401 || response.code == 403)) {
			// Ignora erro na própria tela de login para não dar loop
			if (!url.contains("mobile-login")) {
				sessionManager.clearSession()
				val intent = Intent(context, MainActivity::class.java).apply {
					flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
					putExtra("LOGIN_EXPIRED", true)
				}
				context.startActivity(intent)
			}
		}

		return response
	}
}