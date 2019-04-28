package com.ivanovsky.passnotes.data.repository.file.dropbox;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.ivanovsky.passnotes.data.entity.DropboxFile;
import com.ivanovsky.passnotes.data.repository.DropboxFileRepository;

import java.util.List;

public class DropboxCache {

	private final DropboxFileRepository repository;

	public DropboxCache(DropboxFileRepository repository) {
		this.repository = repository;
	}

	DropboxFile getByRemotePath(String remotePath) {
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

	List<DropboxFile> getLocallyModifiedFiles() {
		return Stream.of(repository.getAll())
				.filter(DropboxFile::isLocallyModified)
				.collect(Collectors.toList());
	}
}
