package com.ivanovsky.passnotes.data.repository.db.migration

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ivanovsky.passnotes.data.crypto.DataCipherProvider
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

class MigrationFrom2To3(
    private val cipherProvider: DataCipherProvider
) : Migration(2, 3) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS git_root (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            fs_authority TEXT NOT NULL,
            path TEXT NOT NULL)
            """.trimIndent()
        )

        val cursor = database.query("SELECT * FROM $TABLE_USED_FILE")

        val columnId = cursor.getColumnIndex(COLUMN_ID)
        val columnFsAuthority = cursor.getColumnIndex(COLUMN_FS_AUTHORITY)

        while (cursor.moveToNext()) {
            val id: Int = cursor.getInt(columnId)
            val oldFsAuthority: String? = cursor.getString(columnFsAuthority)
            if (oldFsAuthority.isNullOrEmpty()) {
                continue
            }

            when (val action = migrateFsAuthorityData(oldFsAuthority)) {
                is Action.Update -> {
                    val values = ContentValues()
                        .apply {
                            put(COLUMN_FS_AUTHORITY, action.updatedRow)
                        }
                    database.update(
                        TABLE_USED_FILE,
                        SQLiteDatabase.CONFLICT_REPLACE,
                        values,
                        "$COLUMN_ID = $id",
                        emptyArray()
                    )
                }
                is Action.Remove -> {
                    database.delete(
                        TABLE_USED_FILE,
                        "$COLUMN_ID = $id",
                        emptyArray()
                    )
                }
                is Action.Skip -> continue
            }
        }

        cursor.close()
    }

    private fun migrateFsAuthorityData(oldFsAuthority: String): Action {
        val cipher = cipherProvider.getCipher()

        try {
            val oldFsAuthorityObj = JSONObject(oldFsAuthority)
            val fsType = oldFsAuthorityObj.optString(FS_TYPE)
                ?: return Action.Remove

            val encodedOldCreds = oldFsAuthorityObj.optString(CREDENTIALS)
            if (encodedOldCreds.isNullOrEmpty()) {
                return Action.Skip
            }

            val decodeOldCreds = cipher.decode(encodedOldCreds)
                ?: return Action.Remove

            val oldCredsObj = JSONObject(decodeOldCreds)

            val url = oldCredsObj.optString(SERVER_URL)
            val username = oldCredsObj.optString(USERNAME)
            val password = oldCredsObj.optString(PASSWORD)

            val newCredsObj = JSONObject()
                .apply {
                    put(TYPE, TYPE_BASIC_CREDENTIALS)
                    put(URL, url)
                    put(USERNAME, username)
                    put(PASSWORD, password)
                }

            val encodedNewCreds = cipher.encode(newCredsObj.toString())
                ?: return Action.Remove

            val newFsAuthorityObj = JSONObject()
                .apply {
                    put(FS_TYPE, fsType)
                    put(CREDENTIALS, encodedNewCreds)
                }

            return Action.Update(newFsAuthorityObj.toString())
        } catch (exception: JSONException) {
            Timber.d(exception)
            return Action.Remove
        }
    }

    sealed class Action {
        data class Update(val updatedRow: String) : Action()
        object Skip : Action()
        object Remove : Action()
    }

    companion object {
        private const val TABLE_USED_FILE = "used_file"

        private const val COLUMN_ID = "id"
        private const val COLUMN_FS_AUTHORITY = "fs_authority"

        private const val CREDENTIALS = "credentials"
        private const val FS_TYPE = "fsType"
        private const val TYPE = "type"
        private const val URL = "url"
        private const val SERVER_URL = "serverUrl"
        private const val USERNAME = "username"
        private const val PASSWORD = "password"
        private const val TYPE_BASIC_CREDENTIALS = "BasicCredentials"
    }
}