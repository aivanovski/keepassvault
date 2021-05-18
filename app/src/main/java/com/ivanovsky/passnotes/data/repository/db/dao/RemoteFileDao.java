package com.ivanovsky.passnotes.data.repository.db.dao;

import com.ivanovsky.passnotes.data.entity.RemoteFile;
import com.ivanovsky.passnotes.data.repository.file.FSType;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface RemoteFileDao {

	@Query("SELECT * FROM remote_file WHERE fs_type = :fsType")
	List<RemoteFile> getAll(String fsType);

	@Insert
	long insert(RemoteFile file);

	@Update
	void update(RemoteFile file);

	@Query("DELETE FROM remote_file WHERE id = :id")
	void delete(long id);

	@Query("SELECT * FROM remote_file WHERE uid = :uid AND fs_type = :fsType")
    RemoteFile findByUidAndFsType(String uid, String fsType);

	@Query("SELECT * FROM remote_file WHERE remote_path = :remotePath AND fs_type = :fsType")
    RemoteFile findByRemotePathAndFsType(String remotePath, String fsType);
}
