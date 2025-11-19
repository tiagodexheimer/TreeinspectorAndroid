// app/src/main/java/com/dexheimer/treeinspectorandroid/ApiService.kt
package com.dexheimer.treeinspectorandroid.data.remote

import com.dexheimer.treeinspectorandroid.data.local.Demanda
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// Data Class para enviar os dados
data class VistoriaRequest(
	val demandaId: Int,
	val respostas: Map<String, Any> // O Gson converte Map automaticamente para JSON Object
)

// --- Data Classes para Login ---

data class LoginRequest(
	val email: String,
	val password: String,
	val csrfToken: String?,
	val callbackUrl: String? = null,
	val json: Boolean = true
)

data class LoginResponse(
	val id: String,
	val name: String?,
	val email: String?,
	val role: String
)

data class CsrfResponse(
	@SerializedName("csrfToken")
	val csrfToken: String
)

// --- Interface da API ---

interface ApiService {

	@GET("api/auth/csrf")
	suspend fun getCsrfToken(): Response<CsrfResponse>

	@POST("api/auth/callback/credentials")
	suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

	@GET("api/demandas")
	suspend fun getDemandas(): Response<DemandasResponse>

	// NOVO: Endpoint para detalhes da rota
	@GET("api/rotas/{id}")
	suspend fun getRotaDetalhes(@Path("id") rotaId: Int): Response<RotaDetalheResponse>

	// NOVO: Busca o formulário pelo nome do tipo (ex: "Poda")
	@GET("api/mobile/formulario-por-tipo")
	suspend fun getFormularioPorTipo(@Query("tipo") tipo: String): Response<List<FormField>>

	// NOVO: Envia a vistoria realizada
	@POST("api/mobile/salvar-vistoria")
	suspend fun salvarVistoria(@Body request: VistoriaRequest): Response<Void> // Void pois não precisamos de corpo na resposta, só 200 OK
}

// Classes de Resposta Auxiliares

data class DemandasResponse(
	@SerializedName("demandas")
	val demandas: List<Demanda>,
	@SerializedName("totalCount")
	val totalCount: Int?,
	@SerializedName("limit")
	val limit: Int?
)