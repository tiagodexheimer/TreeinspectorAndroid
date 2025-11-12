// app/src/main/java/com/dexheimer/treeinspectorandroid/NetworkClient.kt
package com.dexheimer.treeinspectorandroid

// 1. IMPORTE A CLASSE BUILDCONFIG GERADA
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy

object NetworkClient {

	// 2. USE A VARIÁVEL GLOBAL DO BUILDCONFIG
	// O Retrofit precisa que a URL termine com "/"
	// O seu buildConfigField já tem "/api", então adicionamos a "/" no final.
	private val BASE_URL = BuildConfig.API_BASE_URL


	// 1. Gerenciador de Cookies para salvar a sessão do NextAuth
	private val cookieManager = CookieManager().apply {
		setCookiePolicy(CookiePolicy.ACCEPT_ALL)
	}
	private val cookieJar = JavaNetCookieJar(cookieManager)

	// 2. Cliente OkHttp que usa o CookieJar e o Logging
	private val okHttpClient = OkHttpClient.Builder()
		.cookieJar(cookieJar)
		.addInterceptor(HttpLoggingInterceptor().apply {
			level = HttpLoggingInterceptor.Level.BODY // Ótimo para debug
		})
		.build()

	// 3. Instância do Retrofit
	val api: ApiService by lazy {
		Retrofit.Builder()
			.baseUrl(BASE_URL) // <-- AGORA ESTÁ USANDO A URL CORRETA (de debug ou release)
			.client(okHttpClient)
			.addConverterFactory(GsonConverterFactory.create())
			.build()
			.create(ApiService::class.java)
	}
}