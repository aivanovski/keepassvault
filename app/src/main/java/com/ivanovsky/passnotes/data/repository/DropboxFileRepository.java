package com.ivanovsky.passnotes.data.repository;

import com.ivanovsky.passnotes.data.entity.DropboxFile;
import com.ivanovsky.passnotes.data.repository.db.dao.DropboxFileDao;

import java.util.List;

public class DropboxFileRepository {

	private final DropboxFileDao dao;

	public DropboxFileRepository(DropboxFileDao dao) {
		this.dao = dao;
	}

	public List<DropboxFile> getAll() {
		return dao.getAll();
	}

	public DropboxFile findByUid(String uid) {
		return dao.findByUid(uid);
	}

	public void insert(DropboxFile link) {
		long id = dao.insert(link);
		link.setId(id);
	}

	public void update(DropboxFile link) {
		dao.update(link);
	}

	public void delete(int id) {
		dao.delete(id);
	}
}
