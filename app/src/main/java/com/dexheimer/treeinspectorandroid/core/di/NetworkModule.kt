package com.dexheimer.treeinspectorandroid.core.di

import com.dexheimer.treeinspectorandroid.BuildConfig
import com.dexheimer.treeinspectorandroid.data.remote.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

	@Provides
	@Singleton
	fun provideCookieManager(): CookieManager {
		return CookieManager().apply {
			setCookiePolicy(CookiePolicy.ACCEPT_ALL) // Aceita todos os cookies
		}
	}

	@Provides
	@Singleton
	fun provideOkHttpClient(cookieManager: CookieManager): OkHttpClient {
		val baseUrl = URL(BuildConfig.API_BASE_URL)
		val apiHost = baseUrl.host
		val origin = if (baseUrl.protocol == "https") "https://" else "http://"

		return OkHttpClient.Builder()
			.cookieJar(JavaNetCookieJar(cookieManager)) // <--- CRÍTICO: Gerencia a sessão de forma persistente
			.followRedirects(false)
			.connectTimeout(30, TimeUnit.SECONDS)
			.readTimeout(30, TimeUnit.SECONDS)
			.addInterceptor(HttpLoggingInterceptor().apply {
				level = HttpLoggingInterceptor.Level.BODY
			})
			.addInterceptor { chain ->
				val original = chain.request()
				val requestBuilder = original.newBuilder()

				// Cabeçalhos de Segurança (Host/Origin)
				if (original.header("Host") == null) {
					requestBuilder.header("Host", apiHost)
				}
				if (original.header("Origin") == null) {
					requestBuilder.header("Origin", origin + apiHost)
				}

				chain.proceed(requestBuilder.build())
			}
			.build()
	}

	@Provides
	@Singleton
	fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
		return Retrofit.Builder()
			.baseUrl(BuildConfig.API_BASE_URL)
			.client(okHttpClient)
			.addConverterFactory(GsonConverterFactory.create())
			.build()
	}

	@Provides
	@Singleton
	fun provideApiService(retrofit: Retrofit): ApiService {
		return retrofit.create(ApiService::class.java)
	}
}