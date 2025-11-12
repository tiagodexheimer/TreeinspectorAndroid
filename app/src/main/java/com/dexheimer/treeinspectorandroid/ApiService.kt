// app/src/main/java/com/dexheimer/treeinspectorandroid/ApiService.kt
package com.dexheimer.treeinspectorandroid

// Importações do Retrofit

// Importações do GSON (para @SerializedName)

// Importação do seu modelo de dados 'Demanda'
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// --- Data Classes para Login ---

/**
 * Define o corpo (Body) da requisição que ENVIAMOS para o backend
 * durante a Etapa 2 do login.
 */
data class LoginRequest(
	val email: String,
	val password: String,
	val csrfToken: String?, // Token é necessário para o NextAuth
	val callbackUrl: String? = null,
	val json: Boolean = true
)

/**
 * Define a resposta que RECEBEMOS do backend
 * se o login for bem-sucedido.
 */
data class LoginResponse(
	val id: String,
	val name: String?,
	val email: String?,
	val role: String
)

/**
 * Define a resposta que RECEBEMOS do backend
 * ao solicitar o token de segurança (Etapa 1).
 */
data class CsrfResponse(
	@SerializedName("csrfToken")
	val csrfToken: String
)

// --- Interface da API ---

/**
 * Define todos os endpoints da API que nosso app irá consumir.
 * O Retrofit usa esta interface para gerar o código de rede.
 */
interface ApiService {

	/**
	 * Etapa 1 do Login: Buscar o token de segurança CSRF.
	 * Esta chamada é necessária antes de tentar o login.
	 */
	@GET("api/auth/csrf")
	suspend fun getCsrfToken(): Response<CsrfResponse>

	/**
	 * Etapa 2 do Login: Enviar credenciais (email, senha) + token CSRF.
	 */
	@POST("api/auth/callback/credentials")
	suspend fun login(
		@Body loginRequest: LoginRequest
	): Response<LoginResponse>

	/**
	 * Endpoint para buscar a lista de demandas (usado após o login).
	 * O cookie de sessão salvo pelo 'login' será enviado automaticamente.
	 */
	@GET("api/demandas")
	suspend fun getDemandas(): Response<List<Demanda>>

	// ... (No futuro, você pode adicionar POST /api/demandas, GET /api/rotas, etc.)
}