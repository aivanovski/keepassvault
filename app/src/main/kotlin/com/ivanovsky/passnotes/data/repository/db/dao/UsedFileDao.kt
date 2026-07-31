package com.ivanovsky.passnotes.data.repository.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ivanovsky.passnotes.data.entity.UsedFile

@Dao
interface UsedFileDao {
    @get:Query("SELECT * FROM used_file")
    val all: List<UsedFile>

    @Query("SELECT * FROM used_file WHERE id = :id")
    fun getById(id: Int): UsedFile?

    @Insert
    fun insert(file: UsedFile): Long

    @Update
    fun update(file: UsedFile)

    @Query("DELETE FROM used_file WHERE id = :id")
    fun remove(id: Int)
}