package com.ivanovsky.passnotes.domain.search

import com.google.common.truth.Truth.assertThat
import com.ivanovsky.passnotes.data.entity.Attachment
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.Hash
import com.ivanovsky.passnotes.data.entity.HashType
import com.ivanovsky.passnotes.data.entity.InheritableBooleanOption
import com.ivanovsky.passnotes.data.entity.Note
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.domain.entity.SearchOptions
import java.util.Date
import java.util.UUID
import org.junit.Test

class Fzf4jFuzzyEntryMatcherTest {

    private val matcher = Fzf4jFuzzyEntryMatcher()

    @Test
    fun `match should respect disabled title option for notes`() {
        // arrange
        val note = newNote(title = "Github")
        val options = SearchOptions.DEFAULT.copy(isTitleEnabled = false)

        // act
        val result = matcher.match(options, "github", listOf(note))

        // assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `match should search enabled username`() {
        // arrange
        val note = newNote(
            title = "Entry",
            properties = listOf(
                newProperty(PropertyType.USER_NAME, "Username", "john.doe@example.com")
            )
        )

        // act
        val result = matcher.match(SearchOptions.DEFAULT, "johndoe", listOf(note))

        // assert
        assertThat(result).containsExactly(note)
    }

    @Test
    fun `match should ignore disabled username`() {
        // arrange
        val note = newNote(
            title = "Entry",
            properties = listOf(
                newProperty(PropertyType.USER_NAME, "Username", "john.doe@example.com")
            )
        )
        val options = SearchOptions.DEFAULT.copy(isUsernameEnabled = false)

        // act
        val result = matcher.match(options, "johndoe", listOf(note))

        // assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `match should search custom property name when other fields are enabled`() {
        // arrange
        val note = newNote(
            title = "Entry",
            properties = listOf(
                newProperty(type = null, name = "Recovery Code", value = "123456")
            )
        )

        // act
        val result = matcher.match(SearchOptions.DEFAULT, "recovery", listOf(note))

        // assert
        assertThat(result).containsExactly(note)
    }

    @Test
    fun `match should ignore custom property when other fields are disabled`() {
        // arrange
        val note = newNote(
            title = "Entry",
            properties = listOf(
                newProperty(type = null, name = "Recovery Code", value = "123456")
            )
        )
        val options = SearchOptions.DEFAULT.copy(isOtherFieldsEnabled = false)

        // act
        val result = matcher.match(options, "recovery", listOf(note))

        // assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `match should search attachment names when other fields are enabled`() {
        // arrange
        val note = newNote(
            title = "Entry",
            attachments = listOf(newAttachment(name = "invoice.pdf"))
        )

        // act
        val result = matcher.match(SearchOptions.DEFAULT, "invoice", listOf(note))

        // assert
        assertThat(result).containsExactly(note)
    }

    @Test
    fun `match should ignore attachment names when other fields are disabled`() {
        // arrange
        val note = newNote(
            title = "Entry",
            attachments = listOf(newAttachment(name = "invoice.pdf"))
        )
        val options = SearchOptions.DEFAULT.copy(isOtherFieldsEnabled = false)

        // act
        val result = matcher.match(options, "invoice", listOf(note))

        // assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `match should always search group title`() {
        // arrange
        val group = newGroup(title = "Archive")
        val options = SearchOptions.DEFAULT.copy(isTitleEnabled = false)

        // act
        val result = matcher.match(options, "archive", listOf(group))

        // assert
        assertThat(result).containsExactly(group)
    }

    @Test
    fun `match should respect case sensitive option`() {
        // arrange
        val note = newNote(title = "Github")
        val options = SearchOptions.DEFAULT.copy(isCaseSensitive = true)

        // act
        val result = matcher.match(options, "github", listOf(note))

        // assert
        assertThat(result).isEmpty()
    }

    @Test
    fun `match should ignore case by default`() {
        // arrange
        val note = newNote(title = "Github")

        // act
        val result = matcher.match(SearchOptions.DEFAULT, "github", listOf(note))

        // assert
        assertThat(result).containsExactly(note)
    }

    private fun newNote(
        title: String,
        properties: List<Property> = emptyList(),
        attachments: List<Attachment> = emptyList()
    ): Note =
        Note(
            uid = UUID.randomUUID(),
            groupUid = UUID.randomUUID(),
            created = Date(0),
            modified = Date(0),
            expiration = null,
            title = title,
            properties = properties,
            attachments = attachments
        )

    private fun newGroup(title: String): Group =
        Group(
            uid = UUID.randomUUID(),
            parentUid = null,
            title = title,
            groupCount = 0,
            noteCount = 0,
            autotypeEnabled = InheritableBooleanOption.ENABLED,
            searchEnabled = InheritableBooleanOption.ENABLED
        )

    private fun newProperty(
        type: PropertyType?,
        name: String,
        value: String
    ): Property =
        Property(
            type = type,
            name = name,
            value = value
        )

    private fun newAttachment(name: String): Attachment =
        Attachment(
            uid = UUID.randomUUID().toString(),
            name = name,
            hash = Hash("hash".toByteArray(), HashType.SHA_256),
            data = byteArrayOf()
        )
}