package com.dexheimer.treeinspectorandroid.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface VistoriaDao {
	@Insert
	suspend fun adicionarFila(vistoria: VistoriaPendente)

	@Query("SELECT * FROM vistorias_pendentes WHERE sincronizado = 0")
	suspend fun getTodasPendentes(): List<VistoriaPendente>

	@Delete
	suspend fun removerDaFila(vistoria: VistoriaPendente)

	@Query("UPDATE vistorias_pendentes SET sincronizado = 1 WHERE id = :id")
	suspend fun marcarComoSincronizada(id: Int)

	@Query("UPDATE vistorias_pendentes SET jsonRespostas = :json, sincronizado = :sincronizado, dataCriacao = :data WHERE demandaId = :demandaId")
	suspend fun atualizarVistoria(demandaId: Int, json: String, sincronizado: Boolean, data: Long)

	@Query("SELECT COUNT(*) FROM vistorias_pendentes WHERE sincronizado = 0")
	suspend fun contarPendentes(): Int

	@Query("SELECT * FROM vistorias_pendentes WHERE demandaId = :demandaId ORDER BY id DESC LIMIT 1")
	suspend fun getVistoriaPorDemanda(demandaId: Int): VistoriaPendente?
}