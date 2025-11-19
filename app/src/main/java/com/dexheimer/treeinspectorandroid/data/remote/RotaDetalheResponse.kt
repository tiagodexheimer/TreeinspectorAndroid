package com.dexheimer.treeinspectorandroid.data.remote

import com.dexheimer.treeinspectorandroid.data.local.RotaEntity
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class RotaDetalheResponse(
	@SerializedName("rota")
	val rota: RotaEntity, // Se você já renomeou Rota -> RotaEntity

	@SerializedName("demandas")
	val demandas: List<DemandaDTO>, // <--- AQUI! Mude para DemandaDTO

	@SerializedName("geometry")
	val geometry: String?
) : Serializable