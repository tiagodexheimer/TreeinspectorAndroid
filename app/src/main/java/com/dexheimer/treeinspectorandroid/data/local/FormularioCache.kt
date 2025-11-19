package com.dexheimer.treeinspectorandroid.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "formularios_cache")
data class FormularioCache(
	@PrimaryKey
	val tipoDemanda: String, // Ex: "Poda", "Supressão"
	val jsonEstrutura: String // O JSON completo da definição dos campos
)