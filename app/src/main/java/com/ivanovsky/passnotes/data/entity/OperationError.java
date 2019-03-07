package com.ivanovsky.passnotes.data.entity;

public class OperationError {

	public static final String MESSAGE_FAILED_TO_FIND_GROUP = "Failed to find group";
	public static final String MESSAGE_FAILED_TO_FIND_NOTE = "Failed to find note";
	public static final String MESSAGE_FAILED_TO_COMMIT = "Failed to commit";
	public static final String MESSAGE_UNKNOWN_ERROR = "Unknown error";
	public static final String MESSAGE_FILE_ACCESS_IS_FORBIDDEN = "File access is forbidden";
	public static final String MESSAGE_FILE_IS_NOT_A_DIRECTORY = "File is not a directory";
	public static final String MESSAGE_FILE_NOT_FOUND = "File not found";
	public static final String MESSAGE_FILE_DOES_NOT_EXIST = "File doesn't exist";
	public static final String MESSAGE_AUTH_FAILED = "Auth failed";
	public static final String MESSAGE_FAILED_TO_LOAD_FILE = "Failed to load file";
	public static final String MESSAGE_FAILED_TO_LOAD_FILE_LIST = "Failed to load file list";
	public static final String MESSAGE_IO_ERROR = "IO error";
	public static final String MESSAGE_RECORD_IS_ALREADY_EXISTS = "Record is already exists";
	public static final String MESSAGE_FILE_IS_ALREADY_EXISTS = "File is already exists";
	public static final String MESSAGE_FAILED_TO_OPEN_DB_FILE = "Failed to open DB file";

	private Type type;
	private String message;
	private Throwable throwable;

	public static OperationError newDbError(String message) {
		OperationError error = new OperationError(Type.DB_ERROR);
		error.message = message;
		return error;
	}

	public static OperationError newGenericError(String message) {
		OperationError error = new OperationError(Type.GENERIC_ERROR);
		error.message = message;
		return error;
	}

	public static OperationError newGenericError(String message, Throwable throwable) {
		OperationError error = new OperationError(Type.GENERIC_ERROR);
		error.message = message;
		error.throwable = throwable;
		return error;
	}

	public static OperationError newFileAccessError(String message, Throwable throwable) {
		OperationError error = new OperationError(Type.FILE_ACCESS_ERROR);
		error.message = message;
		error.throwable = throwable;
		return error;
	}

	public static OperationError newGenericIOError(String message) {
		OperationError error = new OperationError(Type.GENERIC_IO_ERROR);
		error.message = message;
		return error;
	}

	public static OperationError newGenericIOError(String message, Throwable throwable) {
		OperationError error = new OperationError(Type.GENERIC_IO_ERROR);
		error.message = message;
		error.throwable = throwable;
		return error;
	}

	public static OperationError newAuthError(String message) {
		OperationError error = new OperationError(Type.AUTH_ERROR);
		error.message = message;
		return error;
	}

	public static OperationError newNetworkIOError() {
		OperationError error = new OperationError(Type.NETWORK_IO_ERROR);
		error.message = MESSAGE_IO_ERROR;
		return error;
	}

	public static OperationError newFileIsAlreadyExistsError() {
		OperationError error = new OperationError(Type.FILE_IS_ALREADY_EXISTS);
		error.message = MESSAGE_FILE_IS_ALREADY_EXISTS;
		return error;
	}

	public enum Type {
		GENERIC_ERROR,
		DB_AUTH_ERROR,
		DB_ERROR,
		FILE_ACCESS_ERROR,
		GENERIC_IO_ERROR,
		AUTH_ERROR,
		NETWORK_IO_ERROR,
		FILE_IS_ALREADY_EXISTS
	}

	//TODO: remove unnecessary constructors and refactor

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
