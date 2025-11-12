// app/src/main/java/com/dexheimer/treeinspectorandroid/NetworkClient.kt
package com.dexheimer.treeinspectorandroid

import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy

object NetworkClient {

	// IMPORTANTE: Para o emulador, 'localhost' é 10.0.2.2
	// Se estiver testando em um celular físico na mesma rede, use o IP da sua máquina
	private const val BASE_URL = "http://10.0.2.2:3000/"

	// 1. Gerenciador de Cookies para salvar a sessão do NextAuth
	private val cookieManager = CookieManager().apply {
		setCookiePolicy(CookiePolicy.ACCEPT_ALL)
	}
	private val cookieJar = JavaNetCookieJar(cookieManager)

	// 2. Cliente OkHttp que usa o CookieJar e o Logging (que você já tinha)
	private val okHttpClient = OkHttpClient.Builder()
		.cookieJar(cookieJar)
		.addInterceptor(HttpLoggingInterceptor().apply {
			level = HttpLoggingInterceptor.Level.BODY // Ótimo para debug
		})
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