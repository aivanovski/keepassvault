package com.ivanovsky.passnotes.data.repository.file.webdav

import com.ivanovsky.passnotes.data.entity.FSCredentials
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import com.thegrizzlylabs.sardineandroid.impl.SardineException
import java.io.IOException
import okhttp3.OkHttpClient
import timber.log.Timber

class WebDavNetworkLayer(
    httpClient: OkHttpClient
) {

    private val webDavClient = OkHttpSardine(httpClient)

    fun setCredentials(credentials: FSCredentials.BasicCredentials) {
        webDavClient.setCredentials(credentials.username, credentials.password)
    }

    fun <T> execute(call: (webDavClient: OkHttpSardine) -> T): OperationResult<T> {
        try {
            return OperationResult.success(call.invoke(webDavClient))
        } catch (exception: SardineException) {
            Timber.d(exception)
            return when (exception.statusCode) {
                HTTP_UNAUTHORIZED -> OperationResult.error(OperationError.newAuthError())
                HTTP_NOT_FOUND -> OperationResult.error(OperationError.newFileNotFoundError())
                else -> OperationResult.error(OperationError.newRemoteApiError(exception.message))
            }
        } catch (exception: IOException) {
            Timber.d(exception)
            return OperationResult.error(OperationError.newNetworkIOError())
        } catch (exception: Exception) {
            Timber.d(exception)
            return OperationResult.error(
                OperationError.newGenericError(OperationError.MESSAGE_UNKNOWN_ERROR)
            )
        }
    }

    companion object {
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_NOT_FOUND = 404
    }
}