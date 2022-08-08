package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.data.entity.OperationResult

inline fun <T, R> OperationResult<T>.map(transform: (T) -> R): OperationResult<R> {
    return when {
        isSucceeded -> OperationResult.success(transform.invoke(obj))
        isDeferred -> OperationResult.deferred(transform.invoke(obj), error)
        else -> OperationResult.error(error)
    }
}

fun <T, R> OperationResult<T>.mapWithObject(newObject: R): OperationResult<R> {
    return when {
        isSucceeded -> OperationResult.success(newObject)
        isDeferred -> OperationResult.deferred(newObject, error)
        else -> OperationResult.error(error)
    }
}

fun <T, R> OperationResult<T>.mapError(): OperationResult<R> {
    if (isSucceededOrDeferred) {
        throw IllegalStateException()
    }

    return OperationResult.error(error)
}