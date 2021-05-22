package com.ivanovsky.passnotes.data.repository.db.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.db.AppDatabase
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.ServerCredentials
import com.ivanovsky.passnotes.dateInMillis
import com.ivanovsky.passnotes.initInMemoryDatabase
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun insert_shouldWork() {
        // Arrange
        val file = createFirstFile()

        // Act
        val id = dao.insert(file)

        // Assert
        val values = dao.all
        assertEquals(id, 1L)
        assertEquals(values.size, 1)
        assertEquals(values[0], file)
    }

    @Test
    fun update_shouldWork() {
        // Arrange
        val file = createFirstFile()
        val modifiedFile = createSecondFile().copy(
            id = file.id
        )

        // Act
        dao.insert(file)
        dao.update(modifiedFile)

        // Assert
        val values = dao.all
        assertEquals(values[0], modifiedFile)
    }

    private fun createFirstFile() =
        UsedFile(
            id = 1,
            fsAuthority = FIRST_AUTHORITY,
            filePath = "/firsFilePath",
            fileUid = "firstFileUir",
            addedTime = dateInMillis(2018, 1, 1)
        )

    private fun createSecondFile() =
        UsedFile(
            id = 2,
            fsAuthority = SECOND_AUTHORITY,
            filePath = "/secondFilePath",
            fileUid = "secondFileUId",
            addedTime = dateInMillis(2018, 2, 2),
            lastAccessTime = dateInMillis(2018, 3, 3)
        )

    companion object {

        private val FIRST_AUTHORITY = FSAuthority(
            credentials = ServerCredentials(
                serverUrl = "firstServerUrl",
                username = "firstUsername",
                password = "firstPassword"
            ),
            type = FSType.REGULAR_FS
        )

        private val SECOND_AUTHORITY = FSAuthority(
            credentials = ServerCredentials(
                serverUrl = "secondServerUrl",
                username = "secondUsername",
                password = "secondPassword"
            ),
            type = FSType.DROPBOX
        )
    }
}