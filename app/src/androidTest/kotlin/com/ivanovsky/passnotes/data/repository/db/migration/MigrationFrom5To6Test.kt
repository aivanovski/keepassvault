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
class MigrationFrom5To6Test {

    @get:Rule
    val helper = initMigrationHelper()

    @Test
    fun shouldMigrateDataInGitRootTable() {
        // arrange
        val row = mapOf<String, Any?>(
            COLUMN_ID to 1L,
            COLUMN_FS_AUTHORITY to """{"fsType":"SAF"}""",
            COLUMN_PATH to "file_path"
        )

        val expectedRow = row.toMutableMap()
            .apply {
                this[COLUMN_KEY_PATH] = null
                this[COLUMN_SSH_KEY_FILE_FS_AUTHORITY] = null
                this[COLUMN_SSH_KEY_FILE_PATH] = null
                this[COLUMN_SSH_KEY_FILE_UID] = null
                this[COLUMN_SSH_KEY_FILE_NAME] = null
            }

        helper.createDatabase(DB_NAME, 5)
            .apply {
                insertRow(TABLE_GIT_ROOT, row)
                close()
            }

        // act
        val db = helper.runMigrationsAndValidate(
            DB_NAME,
            6,
            true,
            MigrationFrom5To6()
        )

        // assert
        db.query("SELECT * FROM $TABLE_GIT_ROOT")
            .use { cursor ->
                assertThat(cursor.count).isEqualTo(1)
                assertThat(cursor.readRow()).isEqualTo(expectedRow)
            }
    }

    companion object {
        private const val TABLE_GIT_ROOT = "git_root"

        private const val COLUMN_ID = "id"
        private const val COLUMN_FS_AUTHORITY = "fs_authority"
        private const val COLUMN_PATH = "path"
        private const val COLUMN_KEY_PATH = "ssh_key_path"
        private const val COLUMN_SSH_KEY_FILE_FS_AUTHORITY = "ssh_key_file_fsAuthority"
        private const val COLUMN_SSH_KEY_FILE_PATH = "ssh_key_file_path"
        private const val COLUMN_SSH_KEY_FILE_UID = "ssh_key_file_uid"
        private const val COLUMN_SSH_KEY_FILE_NAME = "ssh_key_file_name"
    }
}