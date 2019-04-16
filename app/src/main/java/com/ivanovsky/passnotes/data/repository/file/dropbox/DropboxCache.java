package com.ivanovsky.passnotes.data.repository.file.dropbox;

import com.ivanovsky.passnotes.data.entity.DropboxFile;
import com.ivanovsky.passnotes.data.repository.DropboxFileRepository;

public class DropboxCache {

	private final DropboxFileRepository repository;

	public DropboxCache(DropboxFileRepository repository) {
		this.repository = repository;
	}

	DropboxFile getByRemotPath(String remotePath) {
		return repository.findByRemotePath(remotePath);
	}

	DropboxFile getByUid(String uid) {
		return repository.findByUid(uid);
	}

	void put(DropboxFile file) {
		repository.insert(file);

	}

	void update(DropboxFile file) {
		repository.update(file);
	}
}
