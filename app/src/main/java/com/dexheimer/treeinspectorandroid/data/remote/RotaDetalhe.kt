package com.dexheimer.treeinspectorandroid.data.remote

import com.dexheimer.treeinspectorandroid.data.local.Demanda
import com.google.gson.annotations.SerializedName

// MODELO CORRETO - Garante que as novas Demandas são desserializadas.
data class RotaDetalhe(

	@SerializedName("rota")
	val rota: RotaInfo,

	// O Demanda aqui deve ser a nova classe com todos os novos campos.
	@SerializedName("demandas")
	val demandas: List<Demanda>,

	@SerializedName("geometry")
	val geometry: String?
)