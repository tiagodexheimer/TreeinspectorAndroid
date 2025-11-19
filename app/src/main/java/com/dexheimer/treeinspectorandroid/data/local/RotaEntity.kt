package com.dexheimer.treeinspectorandroid.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "rotas")
data class RotaEntity(
	@PrimaryKey
	@SerializedName("id")
	val id: Int,

	@SerializedName("nome")
	val nome: String,

	@SerializedName("responsavel")
	val responsavel: String?,

	@SerializedName("status")
	val status: String?,

	@SerializedName("data_rota")
	val dataRota: String?,

	@SerializedName("created_at") // <--- Isso corrige o "null"
	val createdAt: String?,

	@SerializedName("total_demandas") // <--- Isso corrige o "0"
	val totalDemandas: Int? = 0
)