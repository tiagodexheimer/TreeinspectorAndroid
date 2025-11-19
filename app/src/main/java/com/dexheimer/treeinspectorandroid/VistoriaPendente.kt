package com.dexheimer.treeinspectorandroid

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vistorias_pendentes")
data class VistoriaPendente(
	@PrimaryKey(autoGenerate = true)
	val id: Int = 0,
	val demandaId: Int,
	val jsonRespostas: String, // O JSON com as respostas preenchidas
	val dataCriacao: Long = System.currentTimeMillis()
)