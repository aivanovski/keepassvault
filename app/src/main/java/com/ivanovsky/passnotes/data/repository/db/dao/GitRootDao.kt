package com.ivanovsky.passnotes.data.repository.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ivanovsky.passnotes.data.entity.GitRoot

@Dao
interface GitRootDao {

    @Query("SELECT * FROM git_root")
    fun getAll(): List<GitRoot>

    @Insert
    fun insert(repo: GitRoot): Long

    @Query("DELETE FROM git_root WHERE id = :id")
    fun remove(id: Int)
}