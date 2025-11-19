package com.dexheimer.treeinspectorandroid.domain.model

import java.io.Serializable

data class Rota(
	val id: Int,
	val nome: String,
	val responsavel: String?,
	val status: String?,
	val dataRota: String?,
	val dataCriacao: String?,
	val totalDemandas: Int = 0
) : Serializable