package com.ivanovsky.passnotes.data.repository.file.webdav

import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.data.entity.FSCredentials
import com.ivanovsky.passnotes.data.entity.OperationError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import com.thegrizzlylabs.sardineandroid.impl.SardineException
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import timber.log.Timber

class WebDavNetworkLayer {

    private val clients: MutableMap<HttpClientType, OkHttpSardine> = ConcurrentHashMap()

    @Volatile
    private var webDavClient: OkHttpSardine? = null

    fun setCredentials(credentials: FSCredentials.BasicCredentials) {
        setupClient(isIgnoreSslValidation = credentials.isIgnoreSslValidation)
        webDavClient?.setCredentials(credentials.username, credentials.password)
    }

    fun <T> execute(call: (webDavClient: OkHttpSardine) -> T): OperationResult<T> {
        val client = webDavClient
        requireNotNull(client)

        try {
            return OperationResult.success(call.invoke(client))
        } catch (exception: SardineException) {
            Timber.d(exception)
            return when (exception.statusCode) {
                HTTP_UNAUTHORIZED -> OperationResult.error(OperationError.newAuthError())
                HTTP_NOT_FOUND -> OperationResult.error(OperationError.newFileNotFoundError())
                else -> OperationResult.error(OperationError.newRemoteApiError(exception.message))
            }
        } catch (exception: IOException) {
            Timber.d(exception)
            return OperationResult.error(OperationError.newNetworkIOError(exception))
        } catch (exception: Exception) {
            Timber.d(exception)
            return OperationResult.error(OperationError.newGenericError(exception))
        }
    }

    private fun setupClient(
        isIgnoreSslValidation: Boolean
    ) {
        val clientType = if (isIgnoreSslValidation && BuildConfig.DEBUG) {
            HttpClientType.UNSECURE
        } else {
            HttpClientType.SECURE
        }

        webDavClient = clients[clientType]
            ?: OkHttpSardine(HttpClientFactory.createHttpClient(clientType))
                .apply {
                    clients[clientType] = this
                }
    }

    companion object {
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_NOT_FOUND = 404
    }
}