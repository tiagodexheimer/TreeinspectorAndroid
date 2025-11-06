package com.dexheimer.treeinspectorandroid

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Rota::class, Demanda::class], version = 1, exportSchema = false)
@TypeConverters(GeomTypeConverter::class) // <-- Importante para salvar o 'Geom'
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
					// (Em um app real, adicione '.addMigrations(...)' aqui)
					.build()

				INSTANCE = instance
				// Retorna a nova instância
				instance
			}
		}
	}
}