package com.ivanovsky.passnotes.data.entity

class OperationResult<T> {

    private var deferred = false
    private var succeeded = false
    private var value: T? = null
    private var operationError: OperationError? = null

    var obj: T
        @Suppress("UNCHECKED_CAST")
        get() = value as T
        set(value) {
            this.value = value
            succeeded = true
        }

    var error: OperationError
        get() = checkNotNull(operationError)
        set(value) {
            operationError = value
            succeeded = false
        }

    fun from(src: OperationResult<T>) {
        value = src.value
        succeeded = src.succeeded
        deferred = src.deferred
        operationError = src.operationError
    }

    fun <E> takeError(): OperationResult<E> = OperationResult<E>().also {
        it.operationError = operationError
    }

    fun <E> takeStatusWith(newObj: E): OperationResult<E> = OperationResult<E>().also {
        if (isSucceededOrDeferred) {
            it.value = newObj
            it.succeeded = succeeded
            it.deferred = deferred
        } else {
            it.operationError = operationError
        }
    }

    val isSucceeded: Boolean
        get() = succeeded

    val isDeferred: Boolean
        get() = deferred

    val isFailed: Boolean
        get() = !succeeded

    val isFailedDueToNetwork: Boolean
        get() = isFailed && error.type == OperationError.Type.NETWORK_IO_ERROR

    val isSucceededOrDeferred: Boolean
        get() = isSucceeded || isDeferred

    override fun toString(): String =
        "OperationResult(" +
            "succeeded=$succeeded, deferred=$deferred, " +
            "obj=$value, error=$operationError)"

    companion object {
        @JvmStatic
        fun <T> success(obj: T?): OperationResult<T> =
            OperationResult<T>().apply {
                value = obj
                succeeded = true
            }

        @JvmStatic
        fun <T> error(error: OperationError): OperationResult<T> =
            OperationResult<T>().apply { this.error = error }

        @JvmStatic
        fun <T> deferred(obj: T?, error: OperationError?): OperationResult<T> =
            OperationResult<T>().apply {
                succeeded = obj != null
                deferred = obj != null
                value = obj
                operationError = error
            }
    }
}