package com.ivanovsky.passnotes.data.entity;

import androidx.annotation.NonNull;

public class OperationResult<T> {

    private boolean deferred;
    private boolean succeeded;
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

    public static <T> OperationResult<T> deferred(T obj, OperationError error) {
        OperationResult<T> result = new OperationResult<>();
        result.succeeded = (obj != null);
        result.deferred = (obj != null);
        result.obj = obj;
        result.error = error;
        return result;
    }

    public OperationResult() {
    }

    public void setObj(T obj) {
        this.obj = obj;
        succeeded = true;
    }

    public void setDeferredObj(T obj) {
        this.obj = obj;
        succeeded = true;
        deferred = true;
    }

    public void setError(OperationError error) {
        this.error = error;
        succeeded = false;
    }

    public T getObj() {
        return obj;
    }

    public void from(OperationResult<T> src) {
        this.obj = src.obj;
        this.succeeded = src.succeeded;
        this.deferred = src.deferred;
        this.error = src.error;
    }

    public <E> OperationResult<E> takeError() {
        OperationResult<E> newResult = new OperationResult<>();
        newResult.error = error;
        return newResult;
    }

    public <E> OperationResult<E> takeStatusWith(E newObj) {
        OperationResult<E> newResult = new OperationResult<>();

        if (isSucceededOrDeferred()) {
            newResult.obj = newObj;
            newResult.succeeded = succeeded;
            newResult.deferred = deferred;
        } else {
            newResult.error = error;
        }

        return newResult;
    }

    public OperationError getError() {
        return error;
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    public boolean isDeferred() {
        return deferred;
    }

    public boolean isFailed() {
        return !succeeded;
    }

    public boolean isFailedDueToNetwork() {
        return isFailed() && error.getType() == OperationError.Type.NETWORK_IO_ERROR;
    }

    public boolean isSucceededOrDeferred() {
        return isSucceeded() || isDeferred();
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(OperationResult.class.getSimpleName());
        sb.append("(");

        sb.append("succeeded=").append(succeeded).append(", ");
        sb.append("deferred=").append(deferred).append(", ");
        sb.append("obj=").append(obj).append(", ");
        sb.append("error=").append(error);

        sb.append(")");
        return sb.toString();
    }
}
