package com.ivanovsky.passnotes.data.entity;

public class OperationResult<T> {

	private T result;
	private OperationError error;

	public static <T> OperationResult<T> success(T obj) {
		OperationResult<T> result = new OperationResult<>();
		result.setResult(obj);
		return result;
	}

	public static <T> OperationResult<T> error(OperationError error) {
		OperationResult<T> result = new OperationResult<>();
		result.setError(error);
		return result;
	}

	public OperationResult() {
	}

	public void setResult(T result) {
		this.result = result;
	}

	public void setError(OperationError error) {
		this.error = error;
	}

	public T getResult() {
		return result;
	}

	public OperationError getError() {
		return error;
	}

	public boolean isSuccessful() {
		return result != null;
	}
}
