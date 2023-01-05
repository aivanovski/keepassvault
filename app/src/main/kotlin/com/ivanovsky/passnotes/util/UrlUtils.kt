package com.ivanovsky.passnotes.util

import com.ivanovsky.passnotes.domain.entity.ParsedUrl
import com.ivanovsky.passnotes.util.StringUtils.DOT
import com.ivanovsky.passnotes.util.StringUtils.STAR
import timber.log.Timber
import java.net.MalformedURLException
import java.net.URL
import kotlin.text.StringBuilder

object UrlUtils {

    const val SECRET_HOST_MASK = "***"
    private const val SCHEME_HTTP = "http://"
    private const val SCHEME_HTTPS = "https://"
    private const val WWW_PREFIX = "www."

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
        if (!url.contains(DOT)) {
            return null
        }

        val fixedUrl = if (!url.startsWith(SCHEME_HTTP) && !url.startsWith(SCHEME_HTTPS)) {
            SCHEME_HTTPS + url
        } else {
            url
        }

        return try {
            URL(fixedUrl).toParsedUrl()
        } catch (exception: MalformedURLException) {
            Timber.d(exception)
            null
        }
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