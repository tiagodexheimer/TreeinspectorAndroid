package com.dexheimer.treeinspectorandroid

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DemandaDao {

	/**
	 * Busca todas as demandas associadas a um ID de rota específico,
	 * ordenadas pela 'ordem' (que assumimos ser o 'id' por enquanto,
	 * ou você pode adicionar um campo 'ordem' se a API o fornecer).
	 */
	@Query("SELECT * FROM demandas WHERE rota_id = :rotaId ORDER BY id ASC")
	suspend fun getDemandasDaRota(rotaId: Int): List<Demanda>

	/**
	 * Insere uma lista de demandas. Se uma demanda já existir, ela é substituída.
	 */
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertAll(demandas: List<Demanda>)

	/**
	 * Deleta uma demanda específica (usado ao "Finalizar Vistoria").
	 */
	@Delete
	suspend fun deleteDemanda(demanda: Demanda)

	/**
	 * Limpa todas as demandas de uma rota (usado antes de inserir dados novos).
	 */
	@Query("DELETE FROM demandas WHERE rota_id = :rotaId")
	suspend fun clearDemandasDaRota(rotaId: Int)

	@Query("UPDATE demandas SET status_vistoria = :status WHERE id = :demandaId")
	suspend fun updateStatus(demandaId: Int, status: String) // <-- ADICIONE ESTA FUNÇÃO

}