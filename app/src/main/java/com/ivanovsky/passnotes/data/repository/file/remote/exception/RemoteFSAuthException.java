package com.ivanovsky.passnotes.data.repository.file.remote.exception;

public class RemoteFSAuthException extends RemoteFSApiException {

    public RemoteFSAuthException() {
        super("Auth failed");
    }
}
