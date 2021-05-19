package com.ivanovsky.passnotes.data.repository.db.converters

import androidx.room.TypeConverter
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.ServerCredentials
import org.json.JSONObject

object FSAuthorityTypeConverter {

    // TODO: performance optimization (probably FSAuthority value can be inserted in another db table)

    private const val CREDENTIALS = "credentials"
    private const val SERVER_URL = "serverUrl"
    private const val USERNAME = "username"
    private const val PASSWORD = "password"
    private const val FS_TYPE = "fsType"

    @TypeConverter
    @JvmStatic
    fun fromDatabaseValue(value: String): FSAuthority {
        val obj = JSONObject(value)

        val creds = obj.optJSONObject(CREDENTIALS)
        val credentials = if (creds != null) {
            ServerCredentials(
                serverUrl = creds.optString(SERVER_URL),
                username = creds.optString(USERNAME),
                password = creds.optString(PASSWORD)
            )
        } else {
            null
        }

        return FSAuthority(
            credentials = credentials,
            type = FSType.findByValue(obj.optString(FS_TYPE)) ?: FSType.REGULAR_FS
        )
    }

    @TypeConverter
    @JvmStatic
    fun toDatabaseValue(fsAuthority: FSAuthority): String {
        val obj = JSONObject()

        if (fsAuthority.credentials != null) {
            val creds = JSONObject()

            creds.put(SERVER_URL, fsAuthority.credentials.serverUrl)
            creds.put(USERNAME, fsAuthority.credentials.username)
            creds.put(PASSWORD, fsAuthority.credentials.password)

            obj.put(CREDENTIALS, creds)
        }

        obj.put(FS_TYPE, fsAuthority.type.value)

        return obj.toString()
    }
}