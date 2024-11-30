package com.ivanovsky.passnotes.data.repository.db.converters

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.ivanovsky.passnotes.data.crypto.DataCipherProvider
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSCredentials
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.FileId
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
                type = FSType.findByValue(obj.optString(FS_TYPE)) ?: FSType.UNDEFINED,
                isBrowsable = obj.optBoolean(IS_BROWSABLE, true)
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
        obj.put(IS_BROWSABLE, fsAuthority.isBrowsable)

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
                        password = credsObj.optString(PASSWORD),
                        isIgnoreSslValidation = credsObj.optBoolean(IS_IGNORE_SSL_VALIDATION)
                    )
                }

                TYPE_GIT_CREDENTIALS -> {
                    FSCredentials.GitCredentials(
                        url = credsObj.optString(URL),
                        isSecretUrl = credsObj.optBoolean(IS_SECRET_URL),
                        salt = credsObj.optString(SALT)
                    )
                }

                TYPE_SSH_CREDENTIALS -> {
                    val keyFsAuthority = fromDatabaseValue(
                        credsObj.optString(KEY_FILE_FS_AUTHORITY)
                    ) ?: return null

                    FSCredentials.SshCredentials(
                        url = credsObj.optString(URL),
                        isSecretUrl = credsObj.optBoolean(IS_SECRET_URL),
                        salt = credsObj.optString(SALT),
                        password = credsObj.optString(PASSWORD),
                        keyFile = FileId(
                            fsAuthority = keyFsAuthority,
                            path = credsObj.optString(KEY_FILE_PATH),
                            uid = credsObj.optString(KEY_FILE_UID),
                            name = credsObj.optString(KEY_FILE_NAME)
                        )
                    )
                }

                else -> {
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
                creds.put(IS_IGNORE_SSL_VALIDATION, credentials.isIgnoreSslValidation)
            }

            is FSCredentials.GitCredentials -> {
                creds.put(TYPE, TYPE_GIT_CREDENTIALS)
                creds.put(URL, credentials.url)
                creds.put(IS_SECRET_URL, credentials.isSecretUrl)
                creds.put(SALT, credentials.salt)
            }

            is FSCredentials.SshCredentials -> {
                creds.put(TYPE, TYPE_SSH_CREDENTIALS)
                creds.put(URL, credentials.url)
                creds.put(IS_SECRET_URL, credentials.isSecretUrl)
                creds.put(SALT, credentials.salt)
                creds.put(PASSWORD, credentials.password)
                creds.put(KEY_FILE_FS_AUTHORITY, toDatabaseValue(credentials.keyFile.fsAuthority))
                creds.put(KEY_FILE_PATH, credentials.keyFile.path)
                creds.put(KEY_FILE_UID, credentials.keyFile.uid)
                creds.put(KEY_FILE_NAME, credentials.keyFile.name)
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
        private const val IS_BROWSABLE = "isBrowsable"

        private const val USERNAME = "username"
        private const val PASSWORD = "password"
        private const val IS_IGNORE_SSL_VALIDATION = "isIgnoreSslValidation"

        private const val IS_SECRET_URL = "isSecretUrl"
        private const val SALT = "salt"

        private const val KEY_FILE_FS_AUTHORITY = "keyFileFsAuthority"
        private const val KEY_FILE_PATH = "keyFilePath"
        private const val KEY_FILE_UID = "keyFileUid"
        private const val KEY_FILE_NAME = "keyFileName"

        private const val TYPE_BASIC_CREDENTIALS = "BasicCredentials"
        private const val TYPE_GIT_CREDENTIALS = "GitCredentials"
        private const val TYPE_SSH_CREDENTIALS = "SshCredentials"
    }
}