package com.ivanovsky.passnotes.domain.entity

data class ParsedUrl(
    val scheme: String?,
    val domain: String?,
    val path: String?
) {

    fun isValid(): Boolean {
        return !scheme.isNullOrEmpty() && !domain.isNullOrEmpty()
    }

    fun formatToString(): String {
        return StringBuilder()
            .apply {
                if (scheme != null) {
                    append(scheme).append("://")
                }

                if (domain != null) {
                    append(domain)
                }

                if (path != null) {
                    append(path)
                }
            }
            .toString()
    }
}