package com.dexheimer.treeinspectorandroid.data.local // <--- Ajuste o pacote

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RotaDao {

	@Query("SELECT * FROM rotas WHERE id = :rotaId LIMIT 1")
	suspend fun getRotaById(rotaId: Int): RotaEntity? // Retorna a Entity

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertRota(rota: RotaEntity) // Recebe a Entity
}