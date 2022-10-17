package com.ivanovsky.passnotes.util

import android.net.Uri
import com.ivanovsky.passnotes.util.StringUtils.DOT
import com.ivanovsky.passnotes.util.StringUtils.STAR
import java.lang.StringBuilder

object UrlUtils {

    private val UNNECESSARY_URL_PREFIXES = listOf("https", "http", "://", "www", ".")

    fun extractWebDomain(url: String): String? {
        var cleanedUrl = url.trim()
        for (prefix in UNNECESSARY_URL_PREFIXES) {
            if (cleanedUrl.startsWith(prefix, ignoreCase = true)) {
                val start = prefix.length
                val end = cleanedUrl.length
                if (start < end) {
                    cleanedUrl = cleanedUrl.substring(start, end)
                } else {
                    return null
                }
            }
        }

        val lastDotIdx = cleanedUrl.lastIndexOf(DOT)
        if (lastDotIdx == -1 && cleanedUrl.isNotEmpty()) {
            return cleanedUrl
        }

        return if (lastDotIdx > 0) {
            cleanedUrl.substring(0, lastDotIdx)
        } else {
            null
        }
    }

    fun formatSecretUrl(url: String): String {
        val parsedUrl = parseUrl(url) ?: return url.substituteAt(0, url.length, STAR)

        val result = StringBuilder()

        if (parsedUrl.scheme != null) {
            result.append(parsedUrl.scheme).append("://")
        }
        if (parsedUrl.domain != null) {
            result.append("****")
        }
        if (parsedUrl.path != null) {
            result.append(parsedUrl.path)
        }

        return result.toString()
    }

    private fun parseUrl(url: String): ParsedUrl? {
        val uri = Uri.parse(url)

        val scheme = uri.scheme
        val domain = uri.authority
        val path = uri.path

        return if (!scheme.isNullOrEmpty() && !domain.isNullOrEmpty() && !path.isNullOrEmpty()) {
            ParsedUrl(scheme, domain, path)
        } else {
            null
        }
    }

    data class ParsedUrl(
        val scheme: String?,
        val domain: String?,
        val path: String?
    )
}