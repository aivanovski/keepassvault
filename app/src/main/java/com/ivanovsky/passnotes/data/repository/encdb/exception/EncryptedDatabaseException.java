package com.ivanovsky.passnotes.data.repository.encdb.exception;

import com.ivanovsky.passnotes.data.entity.OperationError;

public class EncryptedDatabaseException extends Exception {

    private OperationError error;

    public EncryptedDatabaseException(String message) {
        super(message);
    }

    public EncryptedDatabaseException(Exception reason) {
        super(reason);
    }

    public EncryptedDatabaseException(OperationError error) {
        super(error.getMessage());
        this.error = error;
    }

    public OperationError getError() {
        return error;
    }
}
