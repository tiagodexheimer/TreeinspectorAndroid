package com.dexheimer.treeinspectorandroid.data.remote

import com.google.gson.annotations.SerializedName

// Esta classe representa o objeto "rota"
data class RotaInfo(
	@SerializedName("id")
	val id: Int,

	@SerializedName("nome")
	val nome: String,

	@SerializedName("responsavel")
	val responsavel: String?, // Permite nulo (safe)

	@SerializedName("status")
	val status: String? // Permite nulo (safe)
)