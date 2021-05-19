package com.ivanovsky.passnotes.data.repository.file.remote.exception;

public class InternalCacheException extends RemoteFSException {

	public InternalCacheException() {
		super("Failed to save file to internal cache");
	}
}
