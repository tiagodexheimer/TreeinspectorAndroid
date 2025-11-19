// Em ApiService.kt (ou um novo arquivo de modelo)

import com.dexheimer.treeinspectorandroid.data.local.Demanda
import com.google.gson.annotations.SerializedName

/**
 * Define a estrutura da resposta paginada do GET /api/demandas
 */
data class DemandasResponse(
	@SerializedName("demandas")
	val demandas: List<Demanda>,

	// Incluir metadados (opcional, mas recomendado)
	@SerializedName("totalCount")
	val totalCount: Int?,
	@SerializedName("limit")
	val limit: Int?
)