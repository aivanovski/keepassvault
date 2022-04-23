package com.ivanovsky.passnotes.data.repository.db.migration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.ivanovsky.passnotes.dateInMillis
import com.ivanovsky.passnotes.initMigrationHelper
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationFrom1To2Test {

    @get:Rule
    val helper = initMigrationHelper()

    @Test
    fun testMigrationForm1To2() {
        helper.createDatabase(DB_NAME, 1).apply {
            execSQL(INSERT_STATEMENT)
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 2, true, MigrationFrom1To2())
        val cursor = db.query(SELECT_STATEMENT)

        assertThat(cursor.count).isEqualTo(1)
        assertThat(cursor.columnCount).isEqualTo(12)

        cursor.moveToFirst()

        assertThat(cursor.getInt(cursor.getColumnIndex(COLUMN_ID)))
            .isEqualTo(FILE_ID)
        assertThat(cursor.getString(cursor.getColumnIndex(COLUMN_FS_AUTHORITY)))
            .isEqualTo(FILE_FS_AUTHORITY)
        assertThat(cursor.getString(cursor.getColumnIndex(COLUMN_FILE_PATH)))
            .isEqualTo(FILE_PATH)
        assertThat(cursor.getString(cursor.getColumnIndex(COLUMN_FILE_UID)))
            .isEqualTo(FILE_UID)
        assertThat(cursor.getString(cursor.getColumnIndex(COLUMN_FILE_NAME)))
            .isEqualTo(FILE_NAME)
        assertThat(cursor.getLong(cursor.getColumnIndex(COLUMN_ADDED_TIME)))
            .isEqualTo(FILE_ADDED_TIME)
        assertThat(cursor.isNull(cursor.getColumnIndex(COLUMN_LAST_ACCESS_TIME)))
            .isTrue()
        assertThat(cursor.getString(cursor.getColumnIndex(COLUMN_KEY_TYPE)))
            .isEqualTo("PASSWORD")
        assertThat(cursor.isNull(cursor.getColumnIndex(COLUMN_KEY_FILE_FS_AUTHORITY)))
            .isTrue()
        assertThat(cursor.isNull(cursor.getColumnIndex(COLUMN_KEY_FILE_PATH)))
            .isTrue()
        assertThat(cursor.isNull(cursor.getColumnIndex(COLUMN_KEY_FILE_UID)))
            .isTrue()
        assertThat(cursor.isNull(cursor.getColumnIndex(COLUMN_KEY_FILE_NAME)))
            .isTrue()

        cursor.close()
    }

    companion object {
        private const val DB_NAME = "test-db"

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

        private const val FILE_ID = 1
        private const val FILE_FS_AUTHORITY = "{\"fsTyp\":\"SAF\"}"
        private const val FILE_PATH = "/dev/null/file.kdbx"
        private const val FILE_UID = "/dev/null/file.kdbx"
        private const val FILE_NAME = "file.kdbx"
        private val FILE_ADDED_TIME = dateInMillis(2020, 2, 2)

        private const val SELECT_STATEMENT = "SELECT * FROM used_file"
        private val INSERT_STATEMENT = """
            INSERT INTO used_file (
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
            """.trimIndent().replace("\n", "")
    }
}