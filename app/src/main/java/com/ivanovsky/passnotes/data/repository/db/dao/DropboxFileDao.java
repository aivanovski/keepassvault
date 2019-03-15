package com.ivanovsky.passnotes.data.repository.db.dao;

import com.ivanovsky.passnotes.data.entity.DropboxFile;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface DropboxFileDao {

	@Query("SELECT * FROM dropbox_file")
	List<DropboxFile> getAll();

	@Insert
	long insert(DropboxFile file);

	@Update
	void update(DropboxFile file);

	@Query("DELETE FROM dropbox_file WHERE id = :id")
	void delete(long id);

	@Query("SELECT * FROM dropbox_file WHERE uid = :uid")
	DropboxFile findByUid(String uid);
}
