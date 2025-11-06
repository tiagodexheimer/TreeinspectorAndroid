package com.dexheimer.treeinspectorandroid

import com.google.gson.annotations.SerializedName

// Corrigido para espelhar a resposta completa da API (AGORA COM GEOMETRY)
data class RotaDetalhe(

	// Mapeia o objeto "rota"
	@SerializedName("rota")
	val rota: RotaInfo,

	// Mapeia a lista "demandas"
	@SerializedName("demandas")
	val demandas: List<Demanda>,

	// --- NOVO CAMPO ---
	// Mapeia o polyline encodado do OSRM
	// (String? significa que pode ser nulo, caso o OSRM falhe)
	@SerializedName("geometry")
	val geometry: String?
)