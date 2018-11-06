package com.ivanovsky.passnotes.data.repository.db.dao

import android.support.test.runner.AndroidJUnit4
import com.ivanovsky.passnotes.data.entity.DropboxFileLink
import com.ivanovsky.passnotes.data.repository.db.AppDatabase
import com.ivanovsky.passnotes.dateInMillis
import com.ivanovsky.passnotes.initInMemoryDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val ID = 1
private const val UID = "uid"
private const val INCORRECT_UID = "incorrect-uid"

@RunWith(AndroidJUnit4::class)
class DropboxFileLinkDaoTest {

	private lateinit var dao: DropboxFileLinkDao
	private lateinit var db: AppDatabase

	@Before
	fun setUp() {
		db = initInMemoryDatabase()
		dao = db.dropboxFileLinkDao
	}

	@After
	fun tearDown() {
		db.close()
	}

	@Test
	fun insert_shouldInsertItem() {
		val link = createDropboxFileLink()

		dao.insert(link)

		val links = dao.all
		assertEquals(links.size, 1)
		assertEquals(links[0], link)
	}

	@Test
	fun update_shouldUpdateItem() {
		val link = createDropboxFileLink()
		val modifiedLink = createModifiedDropboxFileLink()

		dao.insert(link)
		dao.update(modifiedLink)

		val links = dao.all
		assertEquals(links.size, 1)
		assertEquals(links[0], modifiedLink)
	}

	@Test
	fun delete_shouldDeleteItem() {
		dao.insert(createDropboxFileLink())

		dao.delete(ID)

		val links = dao.all
		assertEquals(links.size, 0)
	}

	@Test
	fun findByUid_ifSearchArgsIsCorrect_shouldReturnOneItem() {
		dao.insert(createDropboxFileLink())

		val link = dao.findByUid(UID)
		assertEquals(link, createDropboxFileLink())
	}

	@Test
	fun findByUid_ifSearchArgsIsIncorrect_shouldReturnNull() {
		dao.insert(createDropboxFileLink())

		val link = dao.findByUid(INCORRECT_UID)
		assertNull(link)
	}

	private fun createDropboxFileLink(): DropboxFileLink {
		val link = DropboxFileLink()

		link.id = ID
		link.localPath = "local-path"
		link.remotePath = "remote-path"
		link.uid = "uid"
		link.revision = "revision"
		link.isDownloaded = false
		link.lastDownloadTimestamp = dateInMillis(2018, 1, 1)
		link.lastModificationTimestamp = dateInMillis(2017, 1, 1)

		return link
	}

	private fun createModifiedDropboxFileLink(): DropboxFileLink {
		val link = createDropboxFileLink()

		link.localPath = "modified-local-path"
		link.remotePath = "modified-remote-path"
		link.uid = "modified-uid"
		link.revision = "modified-revision"
		link.isDownloaded = true
		link.lastDownloadTimestamp = dateInMillis(2018, 2, 2)
		link.lastModificationTimestamp = dateInMillis(2017, 2, 2)

		return link
	}
}