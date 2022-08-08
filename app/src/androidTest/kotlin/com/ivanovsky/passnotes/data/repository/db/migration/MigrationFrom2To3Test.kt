package com.ivanovsky.passnotes.data.repository.db.migration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.ivanovsky.passnotes.TestData.DB_NAME
import com.ivanovsky.passnotes.TestData.dateInMillis
import com.ivanovsky.passnotes.TestDatabase.initMigrationHelper
import com.ivanovsky.passnotes.TestDatabase.insertRow
import com.ivanovsky.passnotes.extensions.readRow
import com.ivanovsky.passnotes.utils.Base64DataCipher
import com.ivanovsky.passnotes.utils.DataCipherProviderImpl
import com.ivanovsky.passnotes.utils.NullDataCipher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationFrom2To3Test {

    @get:Rule
    val helper = initMigrationHelper()

    @Test
    fun shouldMigrateDataInUsedFileTable() {
        // arrange
        val row = mapOf<String, Any?>(
            COLUMN_ID to FILE_ID,
            COLUMN_FS_AUTHORITY to WEBDAV_FS_AUTHORITY_OLD,
            COLUMN_FILE_PATH to FILE_PATH,
            COLUMN_FILE_UID to FILE_UID,
            COLUMN_FILE_NAME to FILE_NAME,
            COLUMN_ADDED_TIME to TIMESTAMP,
            COLUMN_LAST_ACCESS_TIME to null,
            COLUMN_KEY_TYPE to KEY_FILE,
            COLUMN_KEY_FILE_FS_AUTHORITY to WEBDAV_FS_AUTHORITY_OLD,
            COLUMN_KEY_FILE_PATH to null,
            COLUMN_KEY_FILE_UID to null,
            COLUMN_KEY_FILE_NAME to null
        )
        val expectedRow = row.toMutableMap()
            .apply {
                this[COLUMN_FS_AUTHORITY] = WEBDAV_FS_AUTHORITY_NEW
                this[COLUMN_KEY_FILE_FS_AUTHORITY] = WEBDAV_FS_AUTHORITY_NEW
            }
        helper.createDatabase(DB_NAME, 2)
            .apply {
                insertRow(TABLE_USED_FILE, row)
                close()
            }

        // act
        val db = helper.runMigrationsAndValidate(
            DB_NAME,
            3,
            true,
            MigrationFrom2To3(DataCipherProviderImpl(Base64DataCipher()))
        )

        // assert
        db.query("SELECT * FROM $TABLE_USED_FILE")
            .use { cursor ->
                assertThat(cursor.count).isEqualTo(1)
                assertThat(cursor.readRow()).isEqualTo(expectedRow)
            }
    }

    @Test
    fun shouldLeaveDataAsIsForUsedFileTable() {
        // arrange
        val row = mapOf<String, Any?>(
            COLUMN_ID to FILE_ID,
            COLUMN_FS_AUTHORITY to SAF_FS_AUTHORITY,
            COLUMN_FILE_PATH to FILE_PATH,
            COLUMN_FILE_UID to FILE_UID,
            COLUMN_FILE_NAME to FILE_NAME,
            COLUMN_ADDED_TIME to TIMESTAMP,
            COLUMN_LAST_ACCESS_TIME to null,
            COLUMN_KEY_TYPE to PASSWORD,
            COLUMN_KEY_FILE_FS_AUTHORITY to null,
            COLUMN_KEY_FILE_PATH to null,
            COLUMN_KEY_FILE_UID to null,
            COLUMN_KEY_FILE_NAME to null
        )
        helper.createDatabase(DB_NAME, 2)
            .apply {
                insertRow(TABLE_USED_FILE, row)
                close()
            }

        // act
        val db = helper.runMigrationsAndValidate(
            DB_NAME,
            3,
            true,
            MigrationFrom2To3(DataCipherProviderImpl(Base64DataCipher()))
        )

        // assert
        db.query("SELECT * FROM $TABLE_USED_FILE")
            .use { cursor ->
                assertThat(cursor.count).isEqualTo(1)
                assertThat(cursor.readRow()).isEqualTo(row)
            }
    }

    @Test
    fun shouldRemoveRowFromUsedFileTable() {
        // arrange
        val row = mapOf<String, Any?>(
            COLUMN_ID to FILE_ID,
            COLUMN_FS_AUTHORITY to WEBDAV_FS_AUTHORITY_OLD,
            COLUMN_FILE_PATH to FILE_PATH,
            COLUMN_FILE_UID to FILE_UID,
            COLUMN_FILE_NAME to FILE_NAME,
            COLUMN_ADDED_TIME to TIMESTAMP,
            COLUMN_LAST_ACCESS_TIME to null,
            COLUMN_KEY_TYPE to PASSWORD,
            COLUMN_KEY_FILE_FS_AUTHORITY to null,
            COLUMN_KEY_FILE_PATH to null,
            COLUMN_KEY_FILE_UID to null,
            COLUMN_KEY_FILE_NAME to null
        )
        helper.createDatabase(DB_NAME, 2)
            .apply {
                insertRow(TABLE_USED_FILE, row)
                close()
            }

        // act
        val db = helper.runMigrationsAndValidate(
            DB_NAME,
            3,
            true,
            MigrationFrom2To3(DataCipherProviderImpl(NullDataCipher()))
        )

        // assert
        db.query("SELECT * FROM $TABLE_USED_FILE")
            .use { cursor ->
                assertThat(cursor.count).isEqualTo(0)
            }
    }

    @Test
    fun shouldMigrateDataInRemoteFileTable() {
        // arrange
        val row = mapOf<String, Any?>(
            COLUMN_ID to FILE_ID,
            COLUMN_FS_AUTHORITY to WEBDAV_FS_AUTHORITY_OLD,
            COLUMN_LOCALLY_MODIFIED to 0L,
            COLUMN_UPLOADED to 0L,
            COLUMN_UPLOAD_FAILED to 0L,
            COLUMN_UPLOADING to 0L,
            COLUMN_DOWNLOADING to 0L,
            COLUMN_RETRY_COUNT to RETRY_COUNT,
            COLUMN_LAST_RETRY_TIMESTAMP to TIMESTAMP,
            COLUMN_LAST_DOWNLOAD_TIMESTAMP to TIMESTAMP,
            COLUMN_LAST_MODIFICATION_TIMESTAMP to TIMESTAMP,
            COLUMN_LAST_REMOTE_MODIFICATION_TIMESTAMP to TIMESTAMP,
            COLUMN_LOCAL_PATH to FILE_PATH,
            COLUMN_REMOTE_PATH to REMOTE_FILE_PATH,
            COLUMN_UID to FILE_UID,
            COLUMN_REVISION to REVISION
        )
        val expectedRow = row.toMutableMap()
            .apply {
                this[COLUMN_FS_AUTHORITY] = WEBDAV_FS_AUTHORITY_NEW
            }

        helper.createDatabase(DB_NAME, 2)
            .apply {
                insertRow(TABLE_REMOTE_FILE, row)
                close()
            }

        // act
        val db = helper.runMigrationsAndValidate(
            DB_NAME,
            3,
            true,
            MigrationFrom2To3(DataCipherProviderImpl(Base64DataCipher()))
        )

        // assert
        db.query("SELECT * FROM $TABLE_REMOTE_FILE")
            .use { cursor ->
                assertThat(cursor.count).isEqualTo(1)
                assertThat(cursor.readRow()).isEqualTo(expectedRow)
            }
    }

    @Test
    fun shouldLeaveDataAsIsForRemoteFileTable() {
        // arrange
        val row = mapOf<String, Any?>(
            COLUMN_ID to FILE_ID,
            COLUMN_FS_AUTHORITY to SAF_FS_AUTHORITY,
            COLUMN_LOCALLY_MODIFIED to 0L,
            COLUMN_UPLOADED to 0L,
            COLUMN_UPLOAD_FAILED to 0L,
            COLUMN_UPLOADING to 0L,
            COLUMN_DOWNLOADING to 0L,
            COLUMN_RETRY_COUNT to RETRY_COUNT,
            COLUMN_LAST_RETRY_TIMESTAMP to TIMESTAMP,
            COLUMN_LAST_DOWNLOAD_TIMESTAMP to TIMESTAMP,
            COLUMN_LAST_MODIFICATION_TIMESTAMP to TIMESTAMP,
            COLUMN_LAST_REMOTE_MODIFICATION_TIMESTAMP to TIMESTAMP,
            COLUMN_LOCAL_PATH to FILE_PATH,
            COLUMN_REMOTE_PATH to REMOTE_FILE_PATH,
            COLUMN_UID to FILE_UID,
            COLUMN_REVISION to REVISION
        )

        helper.createDatabase(DB_NAME, 2)
            .apply {
                insertRow(TABLE_REMOTE_FILE, row)
                close()
            }

        // act
        val db = helper.runMigrationsAndValidate(
            DB_NAME,
            3,
            true,
            MigrationFrom2To3(DataCipherProviderImpl(Base64DataCipher()))
        )

        // assert
        db.query("SELECT * FROM $TABLE_REMOTE_FILE")
            .use { cursor ->
                assertThat(cursor.count).isEqualTo(1)
                assertThat(cursor.readRow()).isEqualTo(row)
            }
    }

    @Test
    fun shouldREmoteRowFromRemoteFileTable() {
        // arrange
        val row = mapOf<String, Any?>(
            COLUMN_ID to FILE_ID,
            COLUMN_FS_AUTHORITY to WEBDAV_FS_AUTHORITY_OLD,
            COLUMN_LOCALLY_MODIFIED to 0L,
            COLUMN_UPLOADED to 0L,
            COLUMN_UPLOAD_FAILED to 0L,
            COLUMN_UPLOADING to 0L,
            COLUMN_DOWNLOADING to 0L,
            COLUMN_RETRY_COUNT to RETRY_COUNT,
            COLUMN_LAST_RETRY_TIMESTAMP to TIMESTAMP,
            COLUMN_LAST_DOWNLOAD_TIMESTAMP to TIMESTAMP,
            COLUMN_LAST_MODIFICATION_TIMESTAMP to TIMESTAMP,
            COLUMN_LAST_REMOTE_MODIFICATION_TIMESTAMP to TIMESTAMP,
            COLUMN_LOCAL_PATH to FILE_PATH,
            COLUMN_REMOTE_PATH to REMOTE_FILE_PATH,
            COLUMN_UID to FILE_UID,
            COLUMN_REVISION to REVISION
        )

        helper.createDatabase(DB_NAME, 2)
            .apply {
                insertRow(TABLE_REMOTE_FILE, row)
                close()
            }

        // act
        val db = helper.runMigrationsAndValidate(
            DB_NAME,
            3,
            true,
            MigrationFrom2To3(DataCipherProviderImpl(NullDataCipher()))
        )

        // assert
        db.query("SELECT * FROM $TABLE_REMOTE_FILE")
            .use { cursor ->
                assertThat(cursor.count).isEqualTo(0)
            }
    }

    companion object {
        private const val TABLE_USED_FILE = "used_file"
        private const val TABLE_REMOTE_FILE = "remote_file"

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
        private const val COLUMN_REMOTE_PATH = "remote_path"
        private const val COLUMN_UID = "uid"
        private const val COLUMN_REVISION = "revision"

        private val ENCODED_CREDENTIALS_OLD = """{
                "serverUrl": "testUrl",
                "username": "testUsername",
                "password": "testPassword"
            }
            """.collapseJson().let { Base64DataCipher().encode(it) }
        private val WEBDAV_FS_AUTHORITY_OLD = """{
            "fsType": "WEBDAV",
            "credentials": "$ENCODED_CREDENTIALS_OLD"
            }
            """.collapseJson()

        private val ENCODED_CREDENTIALS_NEW = """{
                "type": "BasicCredentials",
                "url": "testUrl",
                "username": "testUsername",
                "password": "testPassword"
            }
            """.collapseJson().let { Base64DataCipher().encode(it) }
        private val WEBDAV_FS_AUTHORITY_NEW = """{
            "fsType": "WEBDAV",
            "credentials": "$ENCODED_CREDENTIALS_NEW"
            }
            """.collapseJson()
        private const val SAF_FS_AUTHORITY = """{"fsType":"SAF"}"""

        private const val FILE_ID = 1L
        private const val RETRY_COUNT = 10L
        private const val FILE_PATH = "/dev/null/file.kdbx"
        private const val REMOTE_FILE_PATH = "/dir/file.kdbx"
        private const val FILE_UID = "testUid"
        private const val FILE_NAME = "file.kdbx"
        private const val REVISION = "testRevision"
        private const val PASSWORD = "PASSWORD"
        private const val KEY_FILE = "KEY_FILE"
        private val TIMESTAMP = dateInMillis(2020, 2, 2)

        private fun String.collapseJson(): String {
            return this.replace("\n", "").replace(" ", "")
        }
    }
}