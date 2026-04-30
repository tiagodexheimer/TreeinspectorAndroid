package com.dexheimer.treeinspectorandroid.core.di

import android.content.Context
import com.dexheimer.treeinspectorandroid.BuildConfig
import com.dexheimer.treeinspectorandroid.core.util.SessionManager // Importante
import com.dexheimer.treeinspectorandroid.data.remote.ApiService
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // REMOVIDO: fun provideCookieManager()... não é mais necessário

    @Provides
    @Singleton
    fun provideOkHttpClient(
            @ApplicationContext context: Context,
            sessionManager: SessionManager // <--- Injeta o SessionManager
    ): OkHttpClient {

        val authInterceptor = AuthInterceptor(context, sessionManager)

        return OkHttpClient.Builder()
                // .cookieJar(...) <--- REMOVIDO: Fazemos manual agora
                .addInterceptor(authInterceptor) // O nosso interceptor cuida de tudo
                .addInterceptor(
                        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
                )
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
    }

    @Provides @Singleton fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
