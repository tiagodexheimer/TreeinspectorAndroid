package com.dexheimer.treeinspectorandroid.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dexheimer.treeinspectorandroid.core.util.GeomTypeConverter
import com.dexheimer.treeinspectorandroid.data.local.VistoriaDraft
import com.dexheimer.treeinspectorandroid.data.local.VistoriaDraftDao

// Certifique-se que 'FormularioCache::class' está na lista de entities
@Database(
	entities = [RotaEntity::class, DemandaEntity::class, FormularioCache::class, VistoriaPendente::class, VistoriaDraft::class],
	version = 5, // Se der erro de migração, aumente para 5
	exportSchema = false
)
@TypeConverters(GeomTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

	abstract fun rotaDao(): RotaDao
	abstract fun demandaDao(): DemandaDao
	abstract fun formularioDao(): FormularioDao
	abstract fun vistoriaDao(): VistoriaDao
	abstract fun vistoriaDraftDao(): VistoriaDraftDao

	companion object {
		@Volatile
		private var INSTANCE: AppDatabase? = null

		fun getInstance(context: Context): AppDatabase {
			return INSTANCE ?: synchronized(this) {
				val instance = Room.databaseBuilder(
					context.applicationContext,
					AppDatabase::class.java,
					"tree_inspector_db"
				)
					.fallbackToDestructiveMigration()
					.build()
				INSTANCE = instance
				instance
			}
		}
	}
}