package com.ivanovsky.passnotes.data.safedb;

public class EncryptedDatabaseOperationException extends Exception {

	public EncryptedDatabaseOperationException(Exception reason) {
		super(reason);
	}
}
