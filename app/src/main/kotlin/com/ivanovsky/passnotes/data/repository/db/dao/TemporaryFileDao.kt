package com.ivanovsky.passnotes.data.repository.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ivanovsky.passnotes.data.entity.TemporaryFile

@Dao
interface TemporaryFileDao {

    // TODO: create repository + do cleanup

    @Query("SELECT * FROM temporary_file")
    fun geAll(): List<TemporaryFile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(file: TemporaryFile): Long

    @Query("SELECT * FROM temporary_file WHERE path = :path")
    fun getByPath(path: String): TemporaryFile?
}