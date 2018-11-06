package com.ivanovsky.passnotes.data.repository.file.exception;

public class FileSystemException extends Exception {

	public FileSystemException(Exception reason) {
		super(reason);
	}

	public FileSystemException(String message) {
		super(message);
	}
}
