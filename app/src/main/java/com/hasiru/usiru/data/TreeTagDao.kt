package com.hasiru.usiru.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TreeTagDao {
    @Query("SELECT * FROM tree_tags ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TreeTag>>

    @Query("SELECT * FROM tree_tags WHERE synced = 0")
    suspend fun unsynced(): List<TreeTag>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: TreeTag): Long

    @Query("UPDATE tree_tags SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
