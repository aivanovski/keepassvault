package com.ivanovsky.passnotes.data.repository.db.migration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.ivanovsky.passnotes.TestData.DB_NAME
import com.ivanovsky.passnotes.TestDatabase.initMigrationHelper
import com.ivanovsky.passnotes.TestDatabase.insertRow
import com.ivanovsky.passnotes.extensions.readRow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationFrom6To7Test {

    @get:Rule
    val helper = initMigrationHelper()

    @Test
    fun shouldAddLocalBackupPathToRemoteFileTable() {
        // arrange
        val row = mapOf<String, Any?>(
            COLUMN_ID to 1L,
            COLUMN_FS_AUTHORITY to """{"fsType":"SAF"}""",
            COLUMN_LOCALLY_MODIFIED to 0L,
            COLUMN_UPLOADED to 0L,
            COLUMN_UPLOAD_FAILED to 0L,
            COLUMN_UPLOADING to 0L,
            COLUMN_DOWNLOADING to 0L,
            COLUMN_RETRY_COUNT to 1L,
            COLUMN_LAST_RETRY_TIMESTAMP to 100L,
            COLUMN_LAST_DOWNLOAD_TIMESTAMP to 200L,
            COLUMN_LAST_MODIFICATION_TIMESTAMP to 300L,
            COLUMN_LAST_REMOTE_MODIFICATION_TIMESTAMP to 400L,
            COLUMN_LOCAL_PATH to "/path/local",
            COLUMN_REMOTE_PATH to "/path/remote",
            COLUMN_UID to "uid",
            COLUMN_REVISION to "revision"
        )
        val expectedRow = row.toMutableMap().apply {
            this[COLUMN_LOCAL_BACKUP_PATH] = null
        }
        helper.createDatabase(DB_NAME, 6)
            .apply {
                insertRow(TABLE_REMOTE_FILE, row)
                close()
            }

        // act
        val db = helper.runMigrationsAndValidate(
            DB_NAME,
            7,
            true,
            MigrationFrom6To7()
        )

        // assert
        db.query("SELECT * FROM $TABLE_REMOTE_FILE")
            .use { cursor ->
                assertThat(cursor.count).isEqualTo(1)
                assertThat(cursor.readRow()).isEqualTo(expectedRow)
            }
    }

    @Test
    fun shouldCreateTemporaryFileTable() {
        // arrange
        helper.createDatabase(DB_NAME, 6).close()

        // act
        val db = helper.runMigrationsAndValidate(
            DB_NAME,
            7,
            true,
            MigrationFrom6To7()
        )
        val row = mapOf<String, Any?>(
            COLUMN_PATH to "temporary_file_path",
            COLUMN_CREATED to 100L,
            COLUMN_MODIFIED to null
        )
        db.insertRow(TABLE_TEMPORARY_FILE, row)

        // assert
        db.query("SELECT * FROM $TABLE_TEMPORARY_FILE")
            .use { cursor ->
                assertThat(cursor.count).isEqualTo(1)
                assertThat(cursor.readRow()).isEqualTo(row)
            }
    }

    companion object {
        private const val TABLE_REMOTE_FILE = "remote_file"
        private const val TABLE_TEMPORARY_FILE = "temporary_file"

        private const val COLUMN_ID = "id"
        private const val COLUMN_FS_AUTHORITY = "fs_authority"
        private const val COLUMN_LOCALLY_MODIFIED = "locally_modified"
        private const val COLUMN_UPLOADED = "uploaded"
        private const val COLUMN_UPLOAD_FAILED = "upload_failed"
        private const val COLUMN_UPLOADING = "uploading"
        private const val COLUMN_DOWNLOADING = "downloading"
        private const val COLUMN_RETRY_COUNT = "retry_count"
        private const val COLUMN_LAST_RETRY_TIMESTAMP = "last_retry_timestamp"
        private const val COLUMN_LAST_DOWNLOAD_TIMESTAMP = "last_download_timestamp"
        private const val COLUMN_LAST_MODIFICATION_TIMESTAMP = "last_modification_timestamp"
        private const val COLUMN_LAST_REMOTE_MODIFICATION_TIMESTAMP =
            "last_remote_modification_timestamp"
        private const val COLUMN_LOCAL_PATH = "local_path"
        private const val COLUMN_LOCAL_BACKUP_PATH = "local_backup_path"
        private const val COLUMN_REMOTE_PATH = "remote_path"
        private const val COLUMN_UID = "uid"
        private const val COLUMN_REVISION = "revision"

        private const val COLUMN_PATH = "path"
        private const val COLUMN_CREATED = "created"
        private const val COLUMN_MODIFIED = "modified"
    }
}