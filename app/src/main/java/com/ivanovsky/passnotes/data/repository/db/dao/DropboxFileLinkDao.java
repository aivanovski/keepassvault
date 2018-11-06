package com.ivanovsky.passnotes.data.repository.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.ivanovsky.passnotes.data.entity.DropboxFileLink;

import java.util.List;

@Dao
public interface DropboxFileLinkDao {

	@Query("SELECT * FROM dropbox_file_link")
	List<DropboxFileLink> getAll();

	@Insert
	long insert(DropboxFileLink link);

	@Update
	void update(DropboxFileLink link);

	@Query("DELETE FROM dropbox_file_link WHERE id = :id")
	void delete(int id);

	@Query("SELECT * FROM dropbox_file_link WHERE uid = :uid")
	DropboxFileLink findByUid(String uid);
}
