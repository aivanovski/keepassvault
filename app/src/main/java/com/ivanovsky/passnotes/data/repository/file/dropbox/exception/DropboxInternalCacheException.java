package com.ivanovsky.passnotes.data.repository.file.dropbox.exception;

public class DropboxInternalCacheException extends DropboxException {

	public DropboxInternalCacheException() {
		super("Failed to save file to internal cache");
	}
}
