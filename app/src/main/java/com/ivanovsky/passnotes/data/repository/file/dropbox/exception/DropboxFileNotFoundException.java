package com.ivanovsky.passnotes.data.repository.file.dropbox.exception;

public class DropboxFileNotFoundException extends DropboxException {

    public DropboxFileNotFoundException(String path) {
        super(String.format("File not found %s", path));
    }
}
