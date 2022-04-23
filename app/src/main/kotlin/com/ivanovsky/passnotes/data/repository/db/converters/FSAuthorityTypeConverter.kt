package com.ivanovsky.passnotes.data.repository.db.converters

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.ivanovsky.passnotes.data.crypto.DataCipherProvider
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.ServerCredentials
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
                type = FSType.findByValue(obj.optString(FS_TYPE)) ?: FSType.REGULAR_FS
            )
        } catch (e: JSONException) {
            Timber.e("Failed to parse ${FSAuthority::class.simpleName} object: exception=%s", e)
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
            obj.put(CREDENTIALS, toEncodedString(fsAuthority.credentials))
        }

        obj.put(FS_TYPE, fsAuthority.type.value)

        return obj.toString()
    }

    private fun parseEncodedCredentials(encodedText: String): ServerCredentials? {
        val decodedText = cipherProvider.getCipher().decode(encodedText)

        try {
            val credsObj = JSONObject(decodedText ?: encodedText)

            return ServerCredentials(
                serverUrl = credsObj.optString(SERVER_URL),
                username = credsObj.optString(USERNAME),
                password = credsObj.optString(PASSWORD)
            )
        } catch (e: JSONException) {
            Timber.e("Failed to parse ${ServerCredentials::class.simpleName} object: exception=%s", e)
            Timber.d(e)
        }

        return null
    }

    private fun toEncodedString(credentials: ServerCredentials): String {
        val creds = JSONObject()

        creds.put(SERVER_URL, credentials.serverUrl)
        creds.put(USERNAME, credentials.username)
        creds.put(PASSWORD, credentials.password)

        val text = creds.toString()
        val encodedText = cipherProvider.getCipher().encode(text)

        return encodedText ?: text
    }

    companion object {
        private const val CREDENTIALS = "credentials"
        private const val SERVER_URL = "serverUrl"
        private const val USERNAME = "username"
        private const val PASSWORD = "password"
        private const val FS_TYPE = "fsType"
    }
}