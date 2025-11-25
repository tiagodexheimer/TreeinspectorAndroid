package com.dexheimer.treeinspectorandroid.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "formularios_cache")
data class FormularioCache(
	@PrimaryKey
	val tipoDemanda: String, // Chave primária
	val jsonEstrutura: String // Conteúdo JSON
)