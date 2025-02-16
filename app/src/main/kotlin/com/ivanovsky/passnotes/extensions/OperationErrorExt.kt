package com.ivanovsky.passnotes.extensions

import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.domain.ResourceProvider

fun OperationError.formatReadableMessage(
    resourceProvider: ResourceProvider
): String {
    val typeMessage = when (type) {
        OperationError.Type.NETWORK_IO_ERROR -> {
            resourceProvider.getString(R.string.network_error_message)
        }

        OperationError.Type.FILE_PERMISSION_ERROR -> {
            resourceProvider.getString(R.string.file_permission_error_message)
        }

        OperationError.Type.FILE_NOT_FOUND_ERROR -> {
            resourceProvider.getString(R.string.file_not_found)
        }

        else -> null
    }

    val message = message
    val exceptionMessage = throwable?.message

    return when {
        message != null -> message
        typeMessage != null -> typeMessage
        exceptionMessage != null -> exceptionMessage
        else -> resourceProvider.getString(R.string.error_has_been_occurred)
    }
}