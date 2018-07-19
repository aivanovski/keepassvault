package com.ivanovsky.passnotes.data.repository.encdb;

public class EncryptedDatabaseOperationException extends Exception {

	public EncryptedDatabaseOperationException(Exception reason) {
		super(reason);
	}
}
