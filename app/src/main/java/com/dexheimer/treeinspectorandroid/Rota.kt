package com.dexheimer.treeinspectorandroid

import com.google.gson.annotations.SerializedName
import java.io.Serializable // <-- 1. IMPORTE SERIALIZABLE

// Implementamos Serializable para passar o objeto entre Activities
// 2. ADICIONE ": Serializable"
data class Rota(
	// ... (seus campos id, nome, data_criacao, total_demandas)
	@SerializedName("id")
	val id: Int,

	@SerializedName("nome")
	val nome: String,

	@SerializedName("created_at")
	val data_criacao: String,

	@SerializedName("total_demandas")
	val total_demandas: Int

) : Serializable // <-- IMPORTANTE