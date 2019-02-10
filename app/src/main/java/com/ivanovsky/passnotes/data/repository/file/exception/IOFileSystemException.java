package com.ivanovsky.passnotes.data.repository.file.exception;

public class IOFileSystemException extends FileSystemException {

	public IOFileSystemException(Exception reason) {
		super(reason);
	}

	public IOFileSystemException() {
		super("IO error");
	}
}
