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
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

// --- Data Classes para Envio (Requests) ---

data class VistoriaRequest(val demandaId: Int, val respostas: Map<String, Any>)

data class LoginRequest(
        val email: String,
        val password: String,
        // Campos opcionais mantidos para compatibilidade, mas não usados na nova rota
        val csrfToken: String? = null,
        val callbackUrl: String? = null,
        val json: Boolean = true
)

data class CreateDemandaRequest(
        val nome_solicitante: String,
        val telefone_solicitante: String? = null,
        val email_solicitante: String? = null,
        val cep: String,
        val logradouro: String? = null,
        val numero: String,
        val complemento: String? = null,
        val bairro: String? = null,
        val cidade: String? = null,
        val uf: String? = null,
        val tipo_demanda: String,
        val descricao: String,
        val coordinates: List<Double>? = null, // [lat, lng]
        val anexos: List<String>? = null
)

data class UploadResponse(val url: String)

// --- Data Classes para Resposta (Responses) ---

// [NOVO] Wrapper para ler a resposta da rota api/mobile-login
// O servidor retorna: { "success": true, "user": { "id": "...", ... } }
data class MobileAuthResponse(
        val success: Boolean,
        val user: LoginResponse // Reutilizamos a sua classe LoginResponse para o objeto interno
)

// Mantido igual (agora representa o objeto "user" dentro da resposta)
data class LoginResponse(val id: String, val name: String?, val email: String?, val role: String)

// Não é mais estritamente necessário, mas pode manter se tiver uso futuro
data class CsrfResponse(@SerializedName("csrfToken") val csrfToken: String)

data class DemandasResponse(
        @SerializedName("demandas") val demandas: List<Demanda>,
        @SerializedName("totalCount") val totalCount: Int?,
        @SerializedName("limit") val limit: Int?
)

data class Species(
        val id: Int,
        @SerializedName("nome_comum") val nomeComum: String,
        @SerializedName("nome_cientifico") val nomeCientifico: String
)

data class SpeciesResponse(val results: List<Species>)

// --- Interface da API ---

interface ApiService {

    // Rota antiga (com CSRF) substituída pela nova:
    @POST("api/mobile-login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<MobileAuthResponse>

    @GET("api/auth/csrf") suspend fun getCsrfToken(): Response<CsrfResponse>

    @GET("api/demandas") suspend fun getDemandas(): Response<DemandasResponse>

    @GET("api/rotas/{id}")
    suspend fun getRotaDetalhes(@Path("id") rotaId: Int): Response<RotaDetalheResponse>

    @GET("api/rotas") suspend fun getRotas(): Response<List<RotaEntity>>

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

    // [NOVO] Endpoint para atualizar apenas o status
    @PUT("api/demandas/{id}/status")
    suspend fun updateStatus(
            @Path("id") id: Int,
            @Body body: Map<String, String> // ex: {"status": "Em Rota"}
    ): Response<Void>

    @POST("api/demandas")
    suspend fun criarDemanda(@Body request: CreateDemandaRequest): Response<Void>

    @GET("api/especies")
    suspend fun getSpecies(@Query("q") query: String): Response<SpeciesResponse>

    @GET("api/demandas-tipos")
    suspend fun getDemandTypes():
            Response<List<com.dexheimer.treeinspectorandroid.domain.model.TipoDemanda>>
}
