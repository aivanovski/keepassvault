package com.ivanovsky.passnotes.data.entity;

public class OperationError {

	public static final String MESSAGE_FAILED_TO_FIND_GROUP = "Failed to find group";
	public static final String MESSAGE_FAILED_TO_FIND_NOTE = "Failed to find note";
	public static final String MESSAGE_FAILED_TO_COMMIT = "Failed to commit";
	public static final String MESSAGE_UNKNOWN_ERROR = "Unknown error";

	private Type type;
	private String message;
	private Throwable throwable;

	public static OperationError newDbError(String message) {
		OperationError error = new OperationError(Type.DB_ERROR);
		error.setMessage(message);
		return error;
	}

	public enum Type {
		GENERIC_ERROR,
		DB_AUTH_ERROR,
		DB_ERROR
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
