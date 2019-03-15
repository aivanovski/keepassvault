package com.ivanovsky.passnotes.data.entity;

import androidx.annotation.NonNull;

import static com.ivanovsky.passnotes.data.entity.OperationStatus.DEFERRED;
import static com.ivanovsky.passnotes.data.entity.OperationStatus.FAILED;
import static com.ivanovsky.passnotes.data.entity.OperationStatus.SUCCEEDED;

public class OperationResult<T> {

	private boolean deferred;
	private T obj;
	private OperationError error;

	public static <T> OperationResult<T> success(T obj) {
		OperationResult<T> result = new OperationResult<>();
		result.setObj(obj);
		return result;
	}

	public static <T> OperationResult<T> error(OperationError error) {
		OperationResult<T> result = new OperationResult<>();
		result.setError(error);
		return result;
	}

	public static <T> OperationResult<T> defer(T obj, OperationError error) {
		OperationResult<T> result = new OperationResult<>();
		result.deferred = true;
		result.setObj(obj);
		result.setError(error);
		return result;
	}

	public OperationResult() {
	}

	public void setObj(T obj) {
		this.obj = obj;
	}

	public void setDeferredObj(T obj) {
		this.obj = obj;
		deferred = true;
	}

	public void setError(OperationError error) {
		this.error = error;
	}

	public T getObj() {
		return obj;
	}

	public void copyObjFrom(OperationResult<T> src) {
		this.obj = src.obj;
		this.deferred = src.deferred;
	}

	public OperationError getError() {
		return error;
	}

	public boolean isSucceeded() {
		return getStatus() == SUCCEEDED;
	}

	public boolean isDeferred() {
		return getStatus() == DEFERRED;
	}

	public boolean isFailed() {
		return getStatus() == FAILED;
	}

	public boolean isSucceededOrDeferred() {
		OperationStatus status = getStatus();
		return status == SUCCEEDED || status == DEFERRED;
	}

	@NonNull
	public OperationStatus getStatus() {
		OperationStatus status;

		if (deferred) {
			status = DEFERRED;
		} else if (obj != null) {
			status = SUCCEEDED;
		} else {
			status = FAILED;
		}

		return status;
	}
}
