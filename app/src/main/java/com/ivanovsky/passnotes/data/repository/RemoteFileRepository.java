package com.ivanovsky.passnotes.data.repository;

import com.ivanovsky.passnotes.data.entity.RemoteFile;
import com.ivanovsky.passnotes.data.repository.db.dao.RemoteFileDao;
import com.ivanovsky.passnotes.data.repository.file.FSType;

import java.util.List;

public class RemoteFileRepository {

	private final RemoteFileDao dao;

	public RemoteFileRepository(RemoteFileDao dao) {
		this.dao = dao;
	}

	public List<RemoteFile> getAllByFsType(FSType fsType) {
		return dao.getAll(fsType.getValue());
	}

	public RemoteFile findByUidAndFsType(String uid, FSType fsType) {
		return dao.findByUidAndFsType(uid, fsType.getValue());
	}

	public RemoteFile findByRemotePathAndFsType(String remotePath, FSType fsType) {
		return dao.findByRemotePathAndFsType(remotePath, fsType.getValue());
	}

	public void insert(RemoteFile link) {
		long id = dao.insert(link);
		link.setId(id);
	}

	public void update(RemoteFile link) {
		dao.update(link);
	}

	public void delete(int id) {
		dao.delete(id);
	}
}
