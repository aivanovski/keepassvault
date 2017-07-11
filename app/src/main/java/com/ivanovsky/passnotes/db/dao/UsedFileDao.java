package com.ivanovsky.passnotes.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.ivanovsky.passnotes.db.model.UsedFile;

import java.util.List;

@Dao
public interface UsedFileDao {

	@Query("SELECT * FROM used_file")
	List<UsedFile> getAll();

	@Insert
	void insert(UsedFile file);
}
