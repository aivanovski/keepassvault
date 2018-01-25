package com.ivanovsky.passnotes.data.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.ivanovsky.passnotes.data.db.model.UsedFile;

import java.util.List;

@Dao
public interface UsedFileDao {

	@Query("SELECT * FROM used_file")
	List<UsedFile> getAll();

	@Insert
	long insert(UsedFile file);
}
