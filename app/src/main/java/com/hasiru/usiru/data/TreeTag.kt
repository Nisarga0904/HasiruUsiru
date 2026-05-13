package com.hasiru.usiru.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tree_tags")
data class TreeTag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TreeType,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val species: Species?,
    val girthCm: Int,
    val health: TreeHealth,
    val photoUri: String?,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
) {
    val oxygenScore: Double
        get() {
            if (type == TreeType.EMPTY_PIT || species == null) return 0.0
            return girthCm * species.factor * health.multiplier
        }
}
