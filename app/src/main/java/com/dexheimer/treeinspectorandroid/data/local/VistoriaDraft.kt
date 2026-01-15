package com.dexheimer.treeinspectorandroid.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vistoria_drafts")
data class VistoriaDraft(
    @PrimaryKey val demandaId: Int,
    val respostasJson: String,
    val fotosEstaticasJson: String,
    val lastUpdated: Long
)
