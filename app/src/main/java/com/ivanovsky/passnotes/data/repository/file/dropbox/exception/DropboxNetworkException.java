package com.ivanovsky.passnotes.data.repository.file.dropbox.exception;

public class DropboxNetworkException extends DropboxException {

	public DropboxNetworkException() {
		super("Network error is occurred");
	}
}
