package com.ivanovsky.passnotes.data.repository.file.dropbox.exception;

public class DropboxAuthException extends DropboxApiException {

	public DropboxAuthException() {
		super("Auth failed");
	}
}
