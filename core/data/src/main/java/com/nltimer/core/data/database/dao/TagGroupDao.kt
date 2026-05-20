package com.nltimer.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nltimer.core.data.database.entity.TagGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagGroupDao {
    @Query("SELECT * FROM tag_groups ORDER BY sortOrder ASC, id ASC")
    fun getAll(): Flow<List<TagGroupEntity>>

    @Query("SELECT * FROM tag_groups ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllSync(): List<TagGroupEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(group: TagGroupEntity): Long

    @Update
    suspend fun update(group: TagGroupEntity)

    @Delete
    suspend fun delete(group: TagGroupEntity)

    @Query("UPDATE tags SET groupId = NULL WHERE groupId = :groupId")
    suspend fun ungroupAllTags(groupId: Long)

    @Query("SELECT * FROM tag_groups WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): TagGroupEntity?

    @Query("UPDATE tag_groups SET name = :newName WHERE name = :oldName")
    suspend fun renameByName(oldName: String, newName: String)

    @Query("DELETE FROM tag_groups WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("DELETE FROM tag_groups")
    suspend fun deleteAll()

    @Query("SELECT MAX(sortOrder) FROM tag_groups")
    suspend fun getMaxSortOrder(): Int?

    @Query("SELECT * FROM tag_groups WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TagGroupEntity?

    @Query("UPDATE tag_groups SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)
}
