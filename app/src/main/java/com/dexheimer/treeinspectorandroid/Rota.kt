package com.dexheimer.treeinspectorandroid

import com.google.gson.annotations.SerializedName

// Esta classe agora bate 100% com o seu JSON
data class Rota(
	// O @SerializedName("id") é opcional se o nome for igual
	@SerializedName("id")
	val id: Int,

	@SerializedName("nome")
	val nome: String,

	// Mapeia o "created_at" do JSON para o "data_criacao" do Kotlin
	@SerializedName("created_at")
	val data_criacao: String,

	// Mapeia o "total_demandas" do JSON (que é um Int)
	// para o "total_demandas" do Kotlin (também um Int)
	@SerializedName("total_demandas")
	val total_demandas: Int

	// Nota: O GSON irá ignorar os campos extras do JSON (responsavel, status, etc.)
	// se eles não estiverem definidos aqui, o que é perfeito.
)