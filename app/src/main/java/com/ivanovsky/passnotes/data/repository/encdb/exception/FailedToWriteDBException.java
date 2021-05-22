package com.ivanovsky.passnotes.data.repository.encdb.exception;

public class FailedToWriteDBException extends EncryptedDatabaseException {

    public FailedToWriteDBException() {
        super("Failed to write db to file");
    }
}
