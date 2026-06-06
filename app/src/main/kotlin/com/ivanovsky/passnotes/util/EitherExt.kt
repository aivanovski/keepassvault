package com.ivanovsky.passnotes.util

import arrow.core.Either
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationResult

fun <T> Either<OperationError, T>.toOperationResult(): OperationResult<T> {
    return fold(
        ifLeft = { error -> OperationResult.error(error) },
        ifRight = { result -> OperationResult.success(result) }
    )
}