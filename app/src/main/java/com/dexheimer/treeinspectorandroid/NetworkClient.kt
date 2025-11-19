// app/src/main/java/com/dexheimer/treeinspectorandroid/NetworkClient.kt
package com.dexheimer.treeinspectorandroid

import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URL

object NetworkClient {

	// A URL base é lida do BuildConfig (definida no build.gradle)
	private val BASE_URL = BuildConfig.API_BASE_URL

	// Extrai o host (ex: "www.treeinspector.com.br") para usar nos headers de segurança
	private val API_HOST = URL(BASE_URL).host

	// [NOVO] Variável estática para armazenar o token de sessão temporariamente.
	// O Worker (sincronização) vai preencher isso antes de chamar a API.
	var authToken: String? = null

	// 1. Gerenciador de Cookies (Mantém a sessão enquanto o app está aberto)
	private val cookieManager = CookieManager().apply {
		setCookiePolicy(CookiePolicy.ACCEPT_ALL)
	}
	private val cookieJar = JavaNetCookieJar(cookieManager)

	// 2. Cliente OkHttp com interceptadores
	private val okHttpClient = OkHttpClient.Builder()
		.cookieJar(cookieJar)       // Gerencia cookies automaticamente na sessão ativa
		.followRedirects(false)     // Importante para capturar o 302 do Login manualmente
		.addInterceptor(HttpLoggingInterceptor().apply {
			level = HttpLoggingInterceptor.Level.BODY // Logs detalhados para debug
		})
		.addInterceptor { chain ->
			val original = chain.request()
			val requestBuilder = original.newBuilder()

			// [NOVO] Injeção de Token Manual (Essencial para o Worker de Sincronização)
			// Se o Worker definiu um authToken, nós o forçamos no header Cookie.
			// Isso permite que o Worker autentique mesmo sem o CookieJar estar populado.
			if (!authToken.isNullOrEmpty()) {
				requestBuilder.addHeader("Cookie", authToken!!)
			}

			// --- Cabeçalhos de Segurança (Necessários para o NextAuth/CSRF) ---

			// Adiciona o cabeçalho 'Host' se não existir
			if (original.header("Host") == null) {
				requestBuilder.header("Host", API_HOST)
			}

			// Adiciona o cabeçalho 'Origin' se não existir
			if (original.header("Origin") == null) {
				val origin = if (BASE_URL.startsWith("https")) "https://" else "http://"
				requestBuilder.header("Origin", origin + API_HOST)
			}

			val request = requestBuilder.build()
			chain.proceed(request)
		}
		.build()

	// 3. Instância do Retrofit (Singleton)
	val api: ApiService by lazy {
		Retrofit.Builder()
			.baseUrl(BASE_URL)
			.client(okHttpClient)
			.addConverterFactory(GsonConverterFactory.create())
			.build()
			.create(ApiService::class.java)
	}
}