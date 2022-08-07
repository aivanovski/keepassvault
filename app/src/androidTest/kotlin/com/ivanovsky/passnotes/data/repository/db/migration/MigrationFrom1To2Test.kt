package com.ivanovsky.passnotes.data.repository.db.migration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.ivanovsky.passnotes.TestData.DB_NAME
import com.ivanovsky.passnotes.dateInMillis
import com.ivanovsky.passnotes.extensions.readRow
import com.ivanovsky.passnotes.initMigrationHelper
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationFrom1To2Test {

    @get:Rule
    val helper = initMigrationHelper()

    @Test
    fun shouldMigrateDataInUsedFileTable() {
        // arrange
        val expectedRow = mapOf<String, Any?>(
            COLUMN_ID to FILE_ID,
            COLUMN_FS_AUTHORITY to FILE_FS_AUTHORITY,
            COLUMN_FILE_PATH to FILE_PATH,
            COLUMN_FILE_UID to FILE_UID,
            COLUMN_FILE_NAME to FILE_NAME,
            COLUMN_ADDED_TIME to FILE_ADDED_TIME,
            COLUMN_LAST_ACCESS_TIME to null,
            COLUMN_KEY_TYPE to "PASSWORD",
            COLUMN_KEY_FILE_FS_AUTHORITY to null,
            COLUMN_KEY_FILE_PATH to null,
            COLUMN_KEY_FILE_UID to null,
            COLUMN_KEY_FILE_NAME to null
        )

        helper.createDatabase(DB_NAME, 1).apply {
            execSQL(
                """
                INSERT INTO $TABLE_USED_FILE (
                    $COLUMN_ID,
                    $COLUMN_FS_AUTHORITY,
                    $COLUMN_FILE_PATH,
                    $COLUMN_FILE_UID,
                    $COLUMN_FILE_NAME,
                    $COLUMN_ADDED_TIME,
                    $COLUMN_LAST_ACCESS_TIME
                ) VALUES (
                    $FILE_ID,
                    '$FILE_FS_AUTHORITY',
                    '$FILE_PATH',
                    '$FILE_UID',
                    '$FILE_NAME',
                    '$FILE_ADDED_TIME',
                    null
                )
                """.trimIndent()
            )
            close()
        }

        // act
        val db = helper.runMigrationsAndValidate(DB_NAME, 2, true, MigrationFrom1To2())
        val cursor = db.query("SELECT * FROM $TABLE_USED_FILE")

        // assert
        assertThat(cursor.count).isEqualTo(1)
        assertThat(cursor.readRow()).isEqualTo(expectedRow)

        cursor.close()
    }

    companion object {
        private const val TABLE_USED_FILE = "used_file"

        private const val COLUMN_ID = "id"
        private const val COLUMN_FS_AUTHORITY = "fs_authority"
        private const val COLUMN_FILE_PATH = "file_path"
        private const val COLUMN_FILE_UID = "file_uid"
        private const val COLUMN_FILE_NAME = "file_name"
        private const val COLUMN_ADDED_TIME = "added_time"
        private const val COLUMN_LAST_ACCESS_TIME = "last_access_time"
        private const val COLUMN_KEY_TYPE = "key_type"
        private const val COLUMN_KEY_FILE_FS_AUTHORITY = "key_file_fs_authority"
        private const val COLUMN_KEY_FILE_PATH = "key_file_path"
        private const val COLUMN_KEY_FILE_UID = "key_file_uid"
        private const val COLUMN_KEY_FILE_NAME = "key_file_name"

        private const val FILE_ID = 1L
        private const val FILE_FS_AUTHORITY = "{\"fsType\":\"SAF\"}"
        private const val FILE_PATH = "/dev/null/file.kdbx"
        private const val FILE_UID = "/dev/null/file.kdbx"
        private const val FILE_NAME = "file.kdbx"
        private val FILE_ADDED_TIME = dateInMillis(2020, 2, 2)
    }
}