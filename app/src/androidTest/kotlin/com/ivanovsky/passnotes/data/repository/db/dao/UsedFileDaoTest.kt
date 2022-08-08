package com.ivanovsky.passnotes.data.repository.db.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.ivanovsky.passnotes.TestData.dateInMillis
import com.ivanovsky.passnotes.TestDatabase.initInMemoryDatabase
import com.ivanovsky.passnotes.data.entity.FSCredentials
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.db.AppDatabase
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.KeyType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UsedFileDaoTest {

    private lateinit var dao: UsedFileDao
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = initInMemoryDatabase()
        dao = db.usedFileDao
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getAll_shouldReturnAllFiles() {
        // arrange
        dao.insert(FIRST_FILE)
        dao.insert(SECOND_FILE)

        // act
        val files = dao.all

        // assert
        assertThat(files).isEqualTo(listOf(FIRST_FILE, SECOND_FILE))
    }

    @Test
    fun getById_shouldReturnFile() {
        // arrange
        dao.insert(FIRST_FILE)
        dao.insert(SECOND_FILE)

        // act
        val file = dao.getById(SECOND_FILE_ID)

        // assert
        assertThat(file).isEqualTo(SECOND_FILE)
    }

    @Test
    fun insert_shouldInsertFile() {
        // Arrange
        assertThat(dao.all).isEmpty()

        // Act
        val id = dao.insert(FIRST_FILE)

        // Assert
        assertThat(id).isEqualTo(FIRST_FILE_ID.toLong())
        assertThat(dao.all).isEqualTo(listOf(FIRST_FILE))
    }

    @Test
    fun update_shouldUpdateFile() {
        // Arrange
        dao.insert(FIRST_FILE)

        // Act
        val updatedFile = SECOND_FILE.copy(
            id = FIRST_FILE_ID
        )
        dao.update(updatedFile)

        // Assert
        assertThat(dao.all).isEqualTo(listOf(updatedFile))
    }

    @Test
    fun remove_shouldRemoveFile() {
        // arrange
        dao.insert(FIRST_FILE)
        dao.insert(SECOND_FILE)

        // act
        dao.remove(FIRST_FILE_ID)

        // assert
        assertThat(dao.all).isEqualTo(listOf(SECOND_FILE))
    }

    companion object {

        private const val FIRST_FILE_ID = 1
        private const val SECOND_FILE_ID = 2

        private val FIRST_FILE = UsedFile(
            id = FIRST_FILE_ID,
            fsAuthority = FSAuthority(
                credentials = FSCredentials.BasicCredentials(
                    url = "firstServerUrl",
                    username = "firstUsername",
                    password = "firstPassword"
                ),
                type = FSType.INTERNAL_STORAGE
            ),
            filePath = "/firsFilePath",
            fileUid = "firstFileUir",
            fileName = "firstFileName",
            addedTime = dateInMillis(2018, 1, 1),
            keyType = KeyType.PASSWORD,
            keyFileFsAuthority = FSAuthority(
                credentials = FSCredentials.BasicCredentials(
                    url = "keyFileServerUrl",
                    username = "keyFilUsername",
                    password = "keyFilePassword"
                ),
                type = FSType.INTERNAL_STORAGE
            ),
            keyFilePath = "firstKeyFilePath",
            keyFileUid = "firstKeyFileUid",
            keyFileName = "firstKeyFileName"
        )

        private val SECOND_FILE = UsedFile(
            id = SECOND_FILE_ID,
            fsAuthority = FSAuthority(
                credentials = FSCredentials.GitCredentials(
                    url = "repositoryUrl",
                    isSecretUrl = false,
                    salt = "salt"
                ),
                type = FSType.DROPBOX
            ),
            filePath = "/secondFilePath",
            fileUid = "secondFileUId",
            fileName = "secondFileName",
            addedTime = dateInMillis(2018, 2, 2),
            lastAccessTime = dateInMillis(2018, 3, 3),
            keyType = KeyType.KEY_FILE
        )
    }
}