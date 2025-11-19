package com.dexheimer.treeinspectorandroid.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RotaDao {

	/**
	 * Busca uma rota específica pelo seu ID.
	 */
	@Query("SELECT * FROM rotas WHERE id = :rotaId LIMIT 1")
	suspend fun getRotaById(rotaId: Int): Rota?

	/**
	 * Insere ou atualiza uma rota no banco de dados.
	 */
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertRota(rota: Rota)
}