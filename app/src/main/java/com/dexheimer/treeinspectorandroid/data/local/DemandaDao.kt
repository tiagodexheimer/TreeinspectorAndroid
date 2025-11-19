package com.dexheimer.treeinspectorandroid.data.local // <--- Ajuste o pacote

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DemandaDao {

	// Busca todas as demandas da rota (retorna a Entity do banco)
	@Query("SELECT * FROM demandas WHERE rota_id = :rotaId ORDER BY id ASC")
	suspend fun getDemandasDaRota(rotaId: Int): List<DemandaEntity>

	// Insere lista de Entities
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertAll(demandas: List<DemandaEntity>)

	// Deleta uma Entity
	@Delete
	suspend fun deleteDemanda(demanda: DemandaEntity)

	// Limpa demandas (sem alteração na query, apenas no nome do método se quiser manter padrão)
	@Query("DELETE FROM demandas WHERE rota_id = :rotaId")
	suspend fun clearDemandasDaRota(rotaId: Int)

	// Atualiza status (sem alteração na assinatura, pois usa tipos primitivos Int/String)
	@Query("UPDATE demandas SET status_vistoria = :status WHERE id = :demandaId")
	suspend fun updateStatus(demandaId: Int, status: String)
}