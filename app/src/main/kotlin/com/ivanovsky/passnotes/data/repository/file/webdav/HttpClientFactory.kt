package com.ivanovsky.passnotes.data.repository.file.webdav

import android.annotation.SuppressLint
import com.ivanovsky.passnotes.BuildConfig
import java.net.InetAddress
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager
import okhttp3.OkHttp
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber

object HttpClientFactory {

    fun createHttpClient(type: HttpClientType): OkHttpClient {
        val builder = OkHttpClient.Builder()

        val interceptor = HttpLoggingInterceptor {
            Timber.tag(OkHttp::class.java.simpleName).d(it)
        }.apply {
            setLevel(HttpLoggingInterceptor.Level.BASIC)
        }

        builder.addInterceptor(interceptor)

        if (BuildConfig.DEBUG && type == HttpClientType.UNSECURE) {
            Timber.w("--------------------------------------------")
            Timber.w("--                                        --")
            Timber.w("--                                        --")
            Timber.w("-- SSL Certificate validation is disabled --")
            Timber.w("--                                        --")
            Timber.w("--                                        --")
            Timber.w("--------------------------------------------")

            val unsecuredTrustManager = createUnsecuredTrustManager()
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(unsecuredTrustManager), SecureRandom())

            val protocolFilteringSocketFactory = ProtocolFilteringSslSocketFactory(sslContext.socketFactory)
            builder.sslSocketFactory(protocolFilteringSocketFactory, unsecuredTrustManager)
            builder.hostnameVerifier { _, _ -> true }
        }

        return builder.build()
    }

    internal fun filterEnabledProtocols(enabledProtocols: Array<String>): Array<String> {
        val filteredProtocols = enabledProtocols.filterNot { protocol ->
            protocol.startsWith("SSL", ignoreCase = true) ||
                protocol.equals("TLSv1", ignoreCase = true) ||
                protocol.equals("TLSv1.0", ignoreCase = true) ||
                protocol.equals("TLSv1.1", ignoreCase = true)
        }

        check(filteredProtocols.isNotEmpty()) {
            "No acceptable TLS protocols are enabled by the SSL socket provider"
        }

        return filteredProtocols.toTypedArray()
    }

    @SuppressLint("CustomX509TrustManager")
    private fun createUnsecuredTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            @SuppressLint("TrustAllX509TrustManager")
            override fun checkClientTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?
            ) {
            }

            @SuppressLint("TrustAllX509TrustManager")
            override fun checkServerTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?
            ) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        }
    }

    private class ProtocolFilteringSslSocketFactory(
        private val delegate: SSLSocketFactory
    ) : SSLSocketFactory() {

        override fun getDefaultCipherSuites(): Array<String> {
            return delegate.defaultCipherSuites
        }

        override fun getSupportedCipherSuites(): Array<String> {
            return delegate.supportedCipherSuites
        }

        override fun createSocket(): Socket {
            return configure(delegate.createSocket())
        }

        override fun createSocket(host: String, port: Int): Socket {
            return configure(delegate.createSocket(host, port))
        }

        override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket {
            return configure(delegate.createSocket(host, port, localHost, localPort))
        }

        override fun createSocket(host: InetAddress, port: Int): Socket {
            return configure(delegate.createSocket(host, port))
        }

        override fun createSocket(
            address: InetAddress,
            port: Int,
            localAddress: InetAddress,
            localPort: Int
        ): Socket {
            return configure(delegate.createSocket(address, port, localAddress, localPort))
        }

        override fun createSocket(socket: Socket, host: String, port: Int, autoClose: Boolean): Socket {
            return configure(delegate.createSocket(socket, host, port, autoClose))
        }

        private fun configure(socket: Socket): Socket {
            try {
                val sslSocket = socket as? SSLSocket
                    ?: throw IllegalStateException("SSL socket factory returned a non-SSL socket")
                sslSocket.enabledProtocols = filterEnabledProtocols(sslSocket.enabledProtocols)
                return sslSocket
            } catch (exception: Exception) {
                try {
                    socket.close()
                } catch (_: Exception) {
                }
                throw exception
            }
        }
    }
}