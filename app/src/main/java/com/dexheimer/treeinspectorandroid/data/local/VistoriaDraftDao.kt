package com.dexheimer.treeinspectorandroid.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VistoriaDraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(draft: VistoriaDraft)

    @Query("SELECT * FROM vistoria_drafts WHERE demandaId = :demandaId")
    suspend fun getDraft(demandaId: Int): VistoriaDraft?

    @Query("DELETE FROM vistoria_drafts WHERE demandaId = :demandaId")
    suspend fun deleteDraft(demandaId: Int)
}
