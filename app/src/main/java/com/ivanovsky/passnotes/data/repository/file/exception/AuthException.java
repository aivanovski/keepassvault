package com.ivanovsky.passnotes.data.repository.file.exception;

public class AuthException extends FileSystemException {

	public AuthException() {
		super("Auth failed");
	}
}
