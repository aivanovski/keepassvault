package com.ivanovsky.passnotes.data.repository.db.migration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.ivanovsky.passnotes.TestData.DB_NAME
import com.ivanovsky.passnotes.TestData.dateInMillis
import com.ivanovsky.passnotes.TestDatabase.initMigrationHelper
import com.ivanovsky.passnotes.TestDatabase.insertRow
import com.ivanovsky.passnotes.extensions.readRow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationFrom4To5Test {

    @get:Rule
    val helper = initMigrationHelper()

    @Test
    fun shouldMigrateDataInUsedFileTable() {
        // arrange
        val row = mapOf<String, Any?>(
            COLUMN_ID to 1L,
            COLUMN_FS_AUTHORITY to """{"fsType":"SAF"}""",
            COLUMN_FILE_PATH to "file_path",
            COLUMN_FILE_UID to "file_uid",
            COLUMN_FILE_NAME to "file_name",
            COLUMN_ADDED_TIME to dateInMillis(2020, 2, 2),
            COLUMN_LAST_ACCESS_TIME to dateInMillis(2020, 2, 2),
            COLUMN_KEY_TYPE to "PASSWORD",
            COLUMN_KEY_FILE_FS_AUTHORITY to null,
            COLUMN_KEY_FILE_PATH to null,
            COLUMN_KEY_FILE_UID to null,
            COLUMN_KEY_FILE_NAME to null,
            COLUMN_BIOMETRIC_DATA to null
        )
        val expectedRow = row.toMutableMap()
            .apply {
                this[COLUMN_IS_ROOT] = 0L
            }
        helper.createDatabase(DB_NAME, 4)
            .apply {
                insertRow(TABLE_USED_FILE, row)
                close()
            }

        // act
        val db = helper.runMigrationsAndValidate(
            DB_NAME,
            5,
            true,
            MigrationFrom4To5()
        )

        // assert
        db.query("SELECT * FROM $TABLE_USED_FILE")
            .use { cursor ->
                assertThat(cursor.count).isEqualTo(1)
                assertThat(cursor.readRow()).isEqualTo(expectedRow)
            }
    }

    companion object {
        private const val TABLE_USED_FILE = "used_file"

        private const val COLUMN_ID = "id"
        private const val COLUMN_FS_AUTHORITY = "fs_authority"
        private const val COLUMN_FILE_PATH = "file_path"
        private const val COLUMN_FILE_UID = "file_uid"
        private const val COLUMN_FILE_NAME = "file_name"
        private const val COLUMN_ADDED_TIME = "added_time"
        private const val COLUMN_IS_ROOT = "is_root"
        private const val COLUMN_LAST_ACCESS_TIME = "last_access_time"
        private const val COLUMN_KEY_TYPE = "key_type"
        private const val COLUMN_KEY_FILE_FS_AUTHORITY = "key_file_fs_authority"
        private const val COLUMN_KEY_FILE_PATH = "key_file_path"
        private const val COLUMN_KEY_FILE_UID = "key_file_uid"
        private const val COLUMN_KEY_FILE_NAME = "key_file_name"
        private const val COLUMN_BIOMETRIC_DATA = "biometric_data"
    }
}