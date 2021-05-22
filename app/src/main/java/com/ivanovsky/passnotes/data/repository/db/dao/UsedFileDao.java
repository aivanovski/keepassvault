package com.ivanovsky.passnotes.data.repository.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.ivanovsky.passnotes.data.entity.UsedFile;

import java.util.List;

@Dao
public interface UsedFileDao {

    @Query("SELECT * FROM used_file")
    List<UsedFile> getAll();

    @Query("SELECT * FROM used_file WHERE id = :id")
    UsedFile getById(int id);

    @Insert
    long insert(UsedFile file);

    @Update
    void update(UsedFile file);
}
