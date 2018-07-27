package com.ivanovsky.passnotes.data.entity;

public class OperationError {

	private Type type;
	private String message;
	private Throwable throwable;

	public enum Type {
		GENERIC_ERROR,
		DB_AUTH_ERROR
	}

	public OperationError(Type type) {
		this.type = type;
	}

	public OperationError(Type type, String message) {
		this.type = type;
		this.message = message;
	}

	public OperationError(Type type, Throwable throwable) {
		this.type = type;
		this.throwable = throwable;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Throwable getThrowable() {
		return throwable;
	}

	public void setThrowable(Throwable throwable) {
		this.throwable = throwable;
	}
}
