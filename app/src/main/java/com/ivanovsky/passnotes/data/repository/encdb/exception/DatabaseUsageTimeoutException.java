package com.ivanovsky.passnotes.data.repository.encdb.exception;

public class DatabaseUsageTimeoutException extends EncryptedDatabaseException {

	public DatabaseUsageTimeoutException(Exception reason) {
		super(reason);
	}
}
