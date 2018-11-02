package com.ivanovsky.passnotes.data.repository.db.dao

import android.arch.persistence.room.Room
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.repository.db.AppDatabase
import com.ivanovsky.passnotes.data.repository.file.FSType
import com.ivanovsky.passnotes.dateInMillis
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
		db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getContext(), AppDatabase::class.java)
				.build()
		dao = db.usedFileDao
	}

	@After
	fun tearDown() {
		db.close()
	}

	@Test
	fun oneFileIsInserted() {
		val file = createFirstFile()

		dao.insert(file)

		val values = dao.all
		assertEquals(values.size, 1)
		assertEquals(values[0], file)
	}

	@Test
	fun fileIsUpdated() {
		val first = createFirstFile()
		val second = createSecondFile()

		dao.insert(first)
		dao.insert(second)

		val firstModified = modifyFirstFile()
		dao.update(firstModified)

		val values = dao.all
		assertEquals(values[0], firstModified)
		assertEquals(values[1], second)
	}

	private fun createFirstFile(): UsedFile {
		val file = UsedFile()

		file.filePath = "path-1"
		file.fileUid = "uid-1"
		file.lastAccessTime = dateInMillis(2018, 1, 1)
		file.fsType = FSType.REGULAR_FS
		file.id = 1

		return file
	}

	private fun createSecondFile(): UsedFile {
		val file = UsedFile()

		file.filePath = "path-2"
		file.fileUid = "uid-2"
		file.lastAccessTime = dateInMillis(2018, 2, 2)
		file.fsType = FSType.DROPBOX
		file.id = 2

		return file
	}

	private fun modifyFirstFile(): UsedFile {
		val file = createFirstFile()

		file.filePath = "path-1-modified"
		file.fileUid = "uid-1-modified"
		file.lastAccessTime = dateInMillis(2016, 1, 5)
		file.fsType = FSType.DROPBOX

		return file
	}
}