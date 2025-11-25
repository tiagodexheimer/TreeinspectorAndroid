package com.dexheimer.treeinspectorandroid.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface VistoriaDao {
	@Insert
	suspend fun adicionarFila(vistoria: VistoriaPendente)

	@Query("SELECT * FROM vistorias_pendentes")
	suspend fun getTodasPendentes(): List<VistoriaPendente>

	@Delete
	suspend fun removerDaFila(vistoria: VistoriaPendente)

	@Query("SELECT COUNT(*) FROM vistorias_pendentes") // Ou o nome correto da sua tabela
	suspend fun contarPendentes(): Int
}