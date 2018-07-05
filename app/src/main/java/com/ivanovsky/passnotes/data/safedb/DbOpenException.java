package com.ivanovsky.passnotes.data.safedb;

public class DbOpenException extends Exception {

	public DbOpenException(Exception reason) {
		super(reason);
	}
}
