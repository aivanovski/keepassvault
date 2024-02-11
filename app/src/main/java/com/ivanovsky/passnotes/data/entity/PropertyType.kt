package com.ivanovsky.passnotes.data.entity

enum class PropertyType(val propertyName: String) {
    TITLE("Title"),
    PASSWORD("Password"),
    USER_NAME("UserName"),
    URL("URL"),
    NOTES("Notes"),
    OTP("otp");

    companion object {
        val DEFAULT_TYPES = setOf(TITLE, PASSWORD, USER_NAME, URL, NOTES)

        private val TYPES_MAP = mapOf(
            TITLE.propertyName.lowercase() to TITLE,
            PASSWORD.propertyName.lowercase() to PASSWORD,
            USER_NAME.propertyName.lowercase() to USER_NAME,
            URL.propertyName.lowercase() to URL,
            NOTES.propertyName.lowercase() to NOTES,
            OTP.propertyName.lowercase() to OTP
        )

        fun getByName(name: String?): PropertyType? {
            val loweredName = name?.lowercase() ?: return null

            return TYPES_MAP[loweredName]
        }
    }
}
