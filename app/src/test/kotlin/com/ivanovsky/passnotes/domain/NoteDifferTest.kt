package com.ivanovsky.passnotes.domain

import com.google.common.truth.Truth.assertThat
import com.ivanovsky.passnotes.data.entity.Attachment
import com.ivanovsky.passnotes.data.entity.Hash
import com.ivanovsky.passnotes.data.entity.HashType
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.util.asDate
import java.util.UUID
import org.junit.Test

class NoteDifferTest {

    @Test
    fun `getAttachmentsDiff should return empty list`() {
        // arrange
        val oldNote = newNote(
            listOf(Attachments.FIRST.copy(), Attachments.SECOND.copy())
        )
        val newNote = newNote(
            listOf(Attachments.SECOND.copy(), Attachments.FIRST.copy())
        )

        // act
        val diff = NoteDiffer().getAttachmentsDiff(oldNote, newNote)

        // assert
        assertThat(diff).isEmpty()
    }

    @Test
    fun `getAttachmentsDiff should return items to insert`() {
        // arrange
        val oldNote = newNote(emptyList())
        val newNote = newNote(
            listOf(Attachments.FIRST.copy(), Attachments.SECOND.copy())
        )

        // act
        val diff = NoteDiffer().getAttachmentsDiff(oldNote, newNote)

        // assert
        assertThat(diff).isEqualTo(
            listOf(
                Pair(NoteDiffer.DiffAction.INSERT, Attachments.FIRST),
                Pair(NoteDiffer.DiffAction.INSERT, Attachments.SECOND)
            )
        )
    }

    @Test
    fun `getAttachmentsDiff should return items to remove`() {
        // arrange
        val oldNote = newNote(
            listOf(Attachments.FIRST.copy(), Attachments.SECOND.copy())
        )
        val newNote = newNote(
            emptyList()
        )

        // act
        val diff = NoteDiffer().getAttachmentsDiff(oldNote, newNote)

        // assert
        assertThat(diff).isEqualTo(
            listOf(
                Pair(NoteDiffer.DiffAction.REMOVE, Attachments.FIRST),
                Pair(NoteDiffer.DiffAction.REMOVE, Attachments.SECOND)
            )
        )
    }

    @Test
    fun `getAttachmentsDiff should return items to remove and to insert`() {
        // arrange
        val oldNote = newNote(
            listOf(Attachments.FIRST.copy(), Attachments.SECOND.copy())
        )
        val newNote = newNote(
            listOf(Attachments.FIRST.copy(), Attachments.THIRD.copy())
        )

        // act
        val diff = NoteDiffer().getAttachmentsDiff(oldNote, newNote)

        // assert
        assertThat(diff).isEqualTo(
            listOf(
                Pair(NoteDiffer.DiffAction.REMOVE, Attachments.SECOND),
                Pair(NoteDiffer.DiffAction.INSERT, Attachments.THIRD)
            )
        )
    }

    private fun newNote(attachments: List<Attachment>): Note =
        Note(
            uid = UUID(1, 1),
            groupUid = UUID(2, 2),
            created = "2020-01-10".asDate(),
            modified = "2020-01-11".asDate(),
            expiration = null,
            title = "note",
            properties = emptyList(),
            attachments = attachments
        )

    private object Attachments {
        val FIRST = Attachment(
            uid = "file1-uid",
            name = "file1.txt",
            hash = Hash("hash1".toByteArray(), HashType.SHA_256),
            data = "file1-content".toByteArray()
        )

        val SECOND = Attachment(
            uid = "file2-uid",
            name = "file2.txt",
            hash = Hash("hash2".toByteArray(), HashType.SHA_256),
            data = "file2-content".toByteArray()
        )

        val THIRD = Attachment(
            uid = "file3-uid",
            name = "file3.txt",
            hash = Hash("hash3".toByteArray(), HashType.SHA_256),
            data = "file3-content".toByteArray()
        )

        val FOURTH = Attachment(
            uid = "file4-uid",
            name = "file4.txt",
            hash = Hash("hash4".toByteArray(), HashType.SHA_256),
            data = "file4-content".toByteArray()
        )
    }

    companion object {
    }
}