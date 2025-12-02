package com.dexheimer.treeinspectorandroid.data.remote

import com.dexheimer.treeinspectorandroid.data.local.RotaEntity
import com.dexheimer.treeinspectorandroid.domain.model.Demanda
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

// --- Data Classes para Envio (Requests) ---

data class VistoriaRequest(
	val demandaId: Int,
	val respostas: Map<String, Any>
)

data class LoginRequest(
	val email: String,
	val password: String,
	// Campos opcionais mantidos para compatibilidade, mas não usados na nova rota
	val csrfToken: String? = null,
	val callbackUrl: String? = null,
	val json: Boolean = true
)

data class UploadResponse(
	val url: String
)

// --- Data Classes para Resposta (Responses) ---

// [NOVO] Wrapper para ler a resposta da rota api/mobile-login
// O servidor retorna: { "success": true, "user": { "id": "...", ... } }
data class MobileAuthResponse(
	val success: Boolean,
	val user: LoginResponse // Reutilizamos a sua classe LoginResponse para o objeto interno
)

// Mantido igual (agora representa o objeto "user" dentro da resposta)
data class LoginResponse(
	val id: String,
	val name: String?,
	val email: String?,
	val role: String
)

// Não é mais estritamente necessário, mas pode manter se tiver uso futuro
data class CsrfResponse(
	@SerializedName("csrfToken")
	val csrfToken: String
)

data class DemandasResponse(
	@SerializedName("demandas")
	val demandas: List<Demanda>,
	@SerializedName("totalCount")
	val totalCount: Int?,
	@SerializedName("limit")
	val limit: Int?
)

// --- Interface da API ---

interface ApiService {

	// Rota antiga (com CSRF) substituída pela nova:
	@POST("api/mobile-login")
	suspend fun login(@Body loginRequest: LoginRequest): Response<MobileAuthResponse>

	@GET("api/auth/csrf")
	suspend fun getCsrfToken(): Response<CsrfResponse>

	@GET("api/demandas")
	suspend fun getDemandas(): Response<DemandasResponse>

	@GET("api/rotas/{id}")
	suspend fun getRotaDetalhes(@Path("id") rotaId: Int): Response<RotaDetalheResponse>

	@GET("api/rotas")
	suspend fun getRotas(): Response<List<RotaEntity>>

	@GET("api/mobile/formulario-por-tipo")
	suspend fun getFormularioPorTipo(@Query("tipo") tipo: String): Response<List<FormField>>

	@POST("api/mobile/salvar-vistoria")
	suspend fun salvarVistoria(@Body request: VistoriaRequest): Response<Void>

	@Multipart
	@POST("api/mobile/upload")
	suspend fun uploadImage(
		@Part file: MultipartBody.Part,
		@Query("filename") filename: String? = null
	): Response<UploadResponse>
}