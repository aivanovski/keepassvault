package com.ivanovsky.passnotes.data.repository.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.ivanovsky.passnotes.data.entity.UsedFile;

import java.util.List;

@Dao
public interface UsedFileDao {

	@Query("SELECT * FROM used_file")
	List<UsedFile> getAll();

	@Insert
	long insert(UsedFile file);
}
