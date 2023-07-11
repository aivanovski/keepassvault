package com.ivanovsky.passnotes.data.repository.file

import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.util.FileUtils
import java.text.SimpleDateFormat
import java.util.Locale

class FakeFileFactory(
    private val fsAuthority: FSAuthority
) {

    fun createRootFile(): FileDescriptor {
        return create(fsAuthority, FileUid.ROOT, Time.ROOT)
    }

    fun createNoChangesFile(): FileDescriptor {
        return create(fsAuthority, FileUid.NO_CHANGES, Time.NO_CHANGES)
    }

    fun createConflictLocalFile(): FileDescriptor {
        return create(fsAuthority, FileUid.CONFLICT, Time.CONFLICT_LOCAL)
    }

    fun createConflictRemoteFile(): FileDescriptor {
        return create(fsAuthority, FileUid.CONFLICT, Time.CONFLICT_REMOTE)
    }

    private fun create(
        fsAuthority: FSAuthority,
        uid: String,
        modified: Long = System.currentTimeMillis()
    ): FileDescriptor {
        val path = pathFromUid(uid)

        return FileDescriptor(
            fsAuthority = fsAuthority,
            path = path,
            uid = uid,
            name = FileUtils.getFileNameFromPath(path),
            isDirectory = (uid == FileUid.ROOT),
            isRoot = (uid == FileUid.ROOT),
            modified = modified
        )
    }

    private fun pathFromUid(uid: String): String {
        return when (uid) {
            FileUid.NO_CHANGES -> "/test-no-changes.kdbx"
            FileUid.CONFLICT -> "/test-conflict.kdbx"
            FileUid.ROOT -> "/"
            else -> throw IllegalArgumentException("Unknown uid: $uid")
        }
    }

    object FileUid {
        const val NO_CHANGES = "no-changes"
        const val CONFLICT = "conflict"
        const val ROOT = "/"
    }

    private object Time {

        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val ROOT = parseDate("2020-01-01")
        val NO_CHANGES = parseDate("2020-02-01")
        val CONFLICT_LOCAL = parseDate("2020-03-01")
        val CONFLICT_REMOTE = parseDate("2020-03-02")

        private fun parseDate(str: String): Long {
            return DATE_FORMAT.parse(str)?.time ?: throw IllegalArgumentException()
        }
    }
}