package com.ivanovsky.passnotes.util

import android.net.Uri
import com.ivanovsky.passnotes.domain.entity.ParsedUrl
import com.ivanovsky.passnotes.util.StringUtils.DOT
import com.ivanovsky.passnotes.util.StringUtils.STAR
import java.lang.Exception
import java.net.MalformedURLException
import java.net.URL
import kotlin.text.StringBuilder
import timber.log.Timber

object UrlUtils {

    const val SECRET_HOST_MASK = "***"
    const val SCHEME_SSH = "ssh://"
    private const val SCHEME_HTTP = "http://"
    private const val SCHEME_HTTPS = "https://"
    private const val WWW_PREFIX = "www."
    private val SSH_URL_PATTERN = Regex(
        """^(ssh://)?([a-zA-Z0-9_.+-]+)@([a-zA-Z0-9.-]+)(:[0-9]+)?(/?.+)?$"""
    )

    fun extractCleanWebDomain(url: String): String? {
        val domain = parseUrl(url)?.domain ?: return null

        return if (domain.startsWith(WWW_PREFIX, ignoreCase = true)) {
            if (domain.length > WWW_PREFIX.length) {
                domain.substring(WWW_PREFIX.length)
            } else {
                null
            }
        } else {
            domain
        }
    }

    fun formatSecretUrl(url: String): String {
        val parsedUrl = parseUrl(url) ?: return url.substituteAt(0, url.length, STAR)

        return parsedUrl.copy(
            domain = SECRET_HOST_MASK
        ).formatToString()
    }

    fun parseUrl(url: String): ParsedUrl? {
        return when {
            isSshUrl(url) -> parseSshUrl(url)
            isHttpUrl(url) -> parseHttpUrl(url)
            else -> null
        }
    }

    private fun isSshUrl(url: String): Boolean {
        return url.startsWith(SCHEME_SSH) || SSH_URL_PATTERN.matches(url)
    }

    private fun isHttpUrl(url: String): Boolean {
        return url.contains(DOT) && url.isNotEmpty()
    }

    private fun parseHttpUrl(url: String): ParsedUrl? {
        val fixedUrl = if (!url.startsWith(SCHEME_HTTP) && !url.startsWith(SCHEME_HTTPS)) {
            SCHEME_HTTPS + url.trim()
        } else {
            url.trim()
        }

        return try {
            URL(fixedUrl).toParsedUrl()
        } catch (exception: MalformedURLException) {
            Timber.d(exception)
            null
        }
    }

    private fun parseSshUrl(url: String): ParsedUrl? {
        val fixedUrl = if (!url.contains(SCHEME_SSH)) {
            SCHEME_SSH + url.trim()
        } else {
            url.trim()
        }

        val uri = try {
            Uri.parse(fixedUrl)
        } catch (exception: Exception) {
            Timber.d(exception)
            return null
        }

        val scheme = uri.scheme
        val authority = uri.authority

        val path = StringBuilder()
            .apply {
                if (!uri.path.isNullOrEmpty()) {
                    append(uri.path)
                }
            }

        return ParsedUrl(
            scheme = scheme,
            domain = authority,
            path = if (path.isNotEmpty()) {
                path.toString()
            } else {
                null
            }
        )
    }

    private fun URL.toParsedUrl(): ParsedUrl? {
        if (host.equals(WWW_PREFIX, ignoreCase = true)) {
            return null
        }

        val domain = StringBuilder(
            if (authority != null) {
                authority
            } else {
                host
            }
        )

        val path = StringBuilder(path)
        if (!query.isNullOrBlank()) {
            path.append("?").append(query)
        }
        if (!ref.isNullOrBlank()) {
            path.append("#").append(ref)
        }

        return ParsedUrl(
            scheme = protocol,
            domain = domain.toString(),
            path = path.toString()
        )
    }
}