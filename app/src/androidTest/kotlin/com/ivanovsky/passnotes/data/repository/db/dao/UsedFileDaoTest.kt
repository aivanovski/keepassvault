package com.ivanovsky.passnotes.data.repository.db.dao

import android.support.test.runner.AndroidJUnit4
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.db.AppDatabase
import com.ivanovsky.passnotes.data.repository.file.FSType
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
	fun insert_shouldInsertItem() {
		val file = createUsedFile()

		dao.insert(file)

		val values = dao.all
		assertEquals(values.size, 1)
		assertEquals(values[0], file)
	}

	@Test
	fun update_shouldUpdateItem() {
		val first = createUsedFile()

		dao.insert(first)

		val firstModified = createModifiedUsedFile()
		dao.update(firstModified)

		val values = dao.all
		assertEquals(values[0], firstModified)
	}

	private fun createUsedFile(): UsedFile {
		val file = UsedFile()

		file.id = 1
		file.filePath = "path"
		file.fileUid = "uid"
		file.lastAccessTime = dateInMillis(2018, 1, 1)
		file.fsType = FSType.REGULAR_FS

		return file
	}

	private fun createModifiedUsedFile(): UsedFile {
		val file = createUsedFile()

		file.filePath = "modified-path"
		file.fileUid = "modified-uid"
		file.lastAccessTime = dateInMillis(2016, 1, 5)
		file.fsType = FSType.DROPBOX

		return file
	}
}