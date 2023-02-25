package com.ivanovsky.passnotes.data.repository.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.ivanovsky.passnotes.data.entity.RemoteFile;
import java.util.List;

@Dao
public interface RemoteFileDao {

    @Query("SELECT * FROM remote_file")
    List<RemoteFile> getAll();

    @Insert
    long insert(RemoteFile file);

    @Update
    void update(RemoteFile file);

    @Query("DELETE FROM remote_file WHERE id = :id")
    void delete(long id);
}
