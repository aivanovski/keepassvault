package com.ivanovsky.passnotes.domain.interactor

import android.content.Context
import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.OperationError
import java.lang.StringBuilder

class ErrorInteractor(private val context: Context) {

    fun processAndGetMessage(error: OperationError): String {
        return if (BuildConfig.DEBUG) {
            formatDebugMessage(error)
        } else {
            formatReleaseMessage(error)
        }
    }

    private fun formatReleaseMessage(error: OperationError): String {
        return when (error.type) {
            OperationError.Type.NETWORK_IO_ERROR -> {
                context.getString(R.string.network_error_message)
            }
            OperationError.Type.FILE_PERMISSION_ERROR -> {
                context.getString(R.string.file_permission_error_message)
            }
            else -> {
                if (!error.message.isNullOrEmpty()) {
                    error.message
                } else {
                    context.getString(R.string.error_has_been_occurred)
                }
            }
        }
    }

    private fun formatDebugMessage(error: OperationError): String {
        val sb = StringBuilder()
        sb.append(error.type)

        if (error.message != null) {
            sb.append(": ").append(error.message)
        }

        if (error.throwable != null) {
            sb.append(": ").append(error.throwable.toString())
        }

        return sb.toString()
    }
}