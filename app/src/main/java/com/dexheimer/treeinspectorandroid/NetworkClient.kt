// app/src/main/java/com/dexheimer/treeinspectorandroid/NetworkClient.kt
package com.dexheimer.treeinspectorandroid

import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URL // Adicionado import

object NetworkClient {

	// A URL base é lida do BuildConfig (correta para produção: https://www.treeinspector.com.br/)
	private val BASE_URL = BuildConfig.API_BASE_URL

	// EXTRAI O HOST EXATO (e.g., "www.treeinspector.com.br")
	private val API_HOST = URL(BASE_URL).host


	// 1. Gerenciador de Cookies para salvar a sessão do NextAuth
	private val cookieManager = CookieManager().apply {
		setCookiePolicy(CookiePolicy.ACCEPT_ALL)
	}
	private val cookieJar = JavaNetCookieJar(cookieManager)

	// 2. Cliente OkHttp com interceptor de segurança
	private val okHttpClient = OkHttpClient.Builder()
		.cookieJar(cookieJar)
		.followRedirects(false)
		.addInterceptor(HttpLoggingInterceptor().apply {
			level = HttpLoggingInterceptor.Level.BODY // Ótimo para debug
		})
		// --- INÍCIO DA CORREÇÃO CRÍTICA: INJETAR HEADERS ---
		.addInterceptor { chain ->
			val original = chain.request()
			val requestBuilder = original.newBuilder()

			// Adiciona o cabeçalho 'Host'
			if (original.header("Host") == null) {
				requestBuilder.header("Host", API_HOST)
			}

			// Adiciona o cabeçalho 'Origin' (essencial para CSRF)
			if (original.header("Origin") == null) {
				val origin = if (BASE_URL.startsWith("https")) "https://" else "http://"
				requestBuilder.header("Origin", origin + API_HOST)
			}

			val request = requestBuilder.build()
			chain.proceed(request)
		}
		// --- FIM DA CORREÇÃO CRÍTICA ---
		.build()

	// 3. Instância do Retrofit
	val api: ApiService by lazy {
		Retrofit.Builder()
			.baseUrl(BASE_URL)
			.client(okHttpClient)
			.addConverterFactory(GsonConverterFactory.create())
			.build()
			.create(ApiService::class.java)
	}
}