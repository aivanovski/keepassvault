package com.ivanovsky.passnotes.data.repository.file.remote.exception;

public class RemoteFSFileNotFoundException extends RemoteFSApiException {

    public RemoteFSFileNotFoundException(String path) {
        super(String.format("File not found %s", path));
    }
}
