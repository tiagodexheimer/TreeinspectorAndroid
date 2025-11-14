package com.dexheimer.treeinspectorandroid

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// 1. AUMENTADO A VERSÃO PARA 3 para forçar o Room a reconhecer as mudanças
@Database(entities = [Rota::class, Demanda::class], version = 3, exportSchema = false)
@TypeConverters(GeomTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

	// Define os DAOs que este banco de dados fornecerá
	abstract fun rotaDao(): RotaDao
	abstract fun demandaDao(): DemandaDao

	companion object {
		// Singleton para garantir que exista apenas uma instância do banco
		@Volatile
		private var INSTANCE: AppDatabase? = null

		fun getInstance(context: Context): AppDatabase {
			// Verifica se a instância já existe
			return INSTANCE ?: synchronized(this) {
				// Se não existir, cria uma nova instância
				val instance = Room.databaseBuilder(
					context.applicationContext,
					AppDatabase::class.java,
					"tree_inspector_db" // Nome do arquivo do banco de dados
				)
					// 2. CRÍTICO: DELETARÁ O BANCO DE DADOS ANTIGO E RECRIARÁ O NOVO
					.fallbackToDestructiveMigration()
					.build()

				INSTANCE = instance
				// Retorna a nova instância
				instance
			}
		}
	}
}