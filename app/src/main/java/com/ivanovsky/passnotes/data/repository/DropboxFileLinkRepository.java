package com.ivanovsky.passnotes.data.repository;

import com.ivanovsky.passnotes.data.entity.DropboxFileLink;
import com.ivanovsky.passnotes.data.repository.db.AppDatabase;
import com.ivanovsky.passnotes.data.repository.db.dao.DropboxFileLinkDao;

import java.util.List;

public class DropboxFileLinkRepository {

	private DropboxFileLinkDao dao;

	public DropboxFileLinkRepository(AppDatabase db) {
		this.dao = db.getDropboxFileLinkDao();
	}

	public List<DropboxFileLink> getAll() {
		return dao.getAll();
	}

	public DropboxFileLink findByUid(String uid) {
		return dao.findByUid(uid);
	}

	public void insert(DropboxFileLink link) {
		long id = dao.insert(link);
		link.setId((int) id);
	}

	public void update(DropboxFileLink link) {
		dao.update(link);
	}

	public void delete(int id) {
		dao.delete(id);
	}
}
