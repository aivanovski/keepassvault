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

	@Insert
	long insert(UsedFile file);

	@Update
	void update(UsedFile file);
}
