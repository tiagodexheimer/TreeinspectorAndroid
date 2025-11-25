package com.dexheimer.treeinspectorandroid.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RotaDao {
	// Corrigindo 'getAllRotas'
	@Query("SELECT * FROM rotas")
	suspend fun getAllRotas(): List<RotaEntity> // Adicionado 'suspend' para ser assíncrono

	@Query("SELECT * FROM rotas WHERE id = :id")
	suspend fun getRotaById(id: Int): RotaEntity?

	// Corrigindo 'insertAll' (Substitui se já existir)
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertAll(rotas: List<RotaEntity>)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertRota(rota: RotaEntity)

	// Corrigindo 'deleteAll'
	@Query("DELETE FROM rotas")
	suspend fun deleteAll()
}