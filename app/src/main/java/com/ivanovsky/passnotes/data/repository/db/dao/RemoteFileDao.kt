package com.ivanovsky.passnotes.data.repository.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ivanovsky.passnotes.data.entity.RemoteFile

@Dao
interface RemoteFileDao {

    @Query("SELECT * FROM remote_file")
    fun getAll(): List<RemoteFile>

    @Insert
    fun insert(file: RemoteFile): Long

    @Update
    fun update(file: RemoteFile)

    @Query("DELETE FROM remote_file WHERE id = :id")
    fun delete(id: Long)
}
