package com.ivanovsky.passnotes.data.repository.file.webdav

import android.annotation.SuppressLint
import com.ivanovsky.passnotes.BuildConfig
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
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

            builder.sslSocketFactory(sslContext.socketFactory, unsecuredTrustManager)
            builder.hostnameVerifier { _, _ -> true }
        }

        return builder.build()
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
}