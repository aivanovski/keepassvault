package com.ivanovsky.passnotes.data.repository.db.converters

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.ivanovsky.passnotes.data.crypto.DataCipherProvider
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSCredentials
import com.ivanovsky.passnotes.data.entity.FSType
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

@ProvidedTypeConverter
class FSAuthorityTypeConverter(
    private val cipherProvider: DataCipherProvider
) {

    @TypeConverter
    fun fromDatabaseValue(value: String?): FSAuthority? {
        if (value.isNullOrEmpty()) {
            return null
        }

        try {
            val obj = JSONObject(value)

            val creds = obj.optString(CREDENTIALS)
            val credentials = if (creds.isNotEmpty()) {
                parseEncodedCredentials(creds)
            } else {
                null
            }

            return FSAuthority(
                credentials = credentials,
                type = FSType.findByValue(obj.optString(FS_TYPE)) ?: FSType.UNDEFINED
            )
        } catch (e: JSONException) {
            Timber.e("Failed to parse %s object: exception=%s", FSAuthority::class.simpleName, e)
            Timber.d(e)
            return null
        }
    }

    @TypeConverter
    fun toDatabaseValue(fsAuthority: FSAuthority?): String? {
        if (fsAuthority == null) {
            return null
        }

        val obj = JSONObject()

        if (fsAuthority.credentials != null) {
            obj.put(CREDENTIALS, encodeCredentialsToString(fsAuthority.credentials))
        }

        obj.put(FS_TYPE, fsAuthority.type.value)

        return obj.toString()
    }

    private fun parseEncodedCredentials(encodedText: String): FSCredentials? {
        val decodedText = cipherProvider.getCipher().decode(encodedText)

        try {
            val credsObj = JSONObject(decodedText ?: encodedText)

            return when (credsObj.optString(TYPE)) {
                TYPE_BASIC_CREDENTIALS -> {
                    FSCredentials.BasicCredentials(
                        url = credsObj.optString(URL),
                        username = credsObj.optString(USERNAME),
                        password = credsObj.optString(PASSWORD)
                    )
                }
                TYPE_GIT_CREDENTIALS -> {
                    FSCredentials.GitCredentials(
                        url = credsObj.optString(URL),
                        isSecretUrl = credsObj.optBoolean(IS_SECRET_URL),
                        salt = credsObj.optString(SALT)
                    )
                }
                else -> {
                    // TODO: parse to FSCredentials.LoginCredentials
                    null
                }
            }
        } catch (e: JSONException) {
            Timber.e("Failed to parse %s object: exception=%s", FSCredentials::class.simpleName, e)
            Timber.d(e)
        }

        return null
    }

    private fun encodeCredentialsToString(credentials: FSCredentials): String {
        val creds = JSONObject()

        when (credentials) {
            is FSCredentials.BasicCredentials -> {
                creds.put(TYPE, TYPE_BASIC_CREDENTIALS)
                creds.put(URL, credentials.url)
                creds.put(USERNAME, credentials.username)
                creds.put(PASSWORD, credentials.password)
            }
            is FSCredentials.GitCredentials -> {
                creds.put(TYPE, TYPE_GIT_CREDENTIALS)
                creds.put(URL, credentials.url)
                creds.put(IS_SECRET_URL, credentials.isSecretUrl)
                creds.put(SALT, credentials.salt)
            }
        }

        val text = creds.toString()
        val encodedText = cipherProvider.getCipher().encode(text)

        return encodedText ?: text
    }

    companion object {
        private const val CREDENTIALS = "credentials"
        private const val FS_TYPE = "fsType"
        private const val TYPE = "type"
        private const val URL = "url"

        private const val USERNAME = "username"
        private const val PASSWORD = "password"

        private const val IS_SECRET_URL = "isSecretUrl"
        private const val SALT = "salt"

        private const val TYPE_BASIC_CREDENTIALS = "BasicCredentials"
        private const val TYPE_GIT_CREDENTIALS = "GitCredentials"
    }
}