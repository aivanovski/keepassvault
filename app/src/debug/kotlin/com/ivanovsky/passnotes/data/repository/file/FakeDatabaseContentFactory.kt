package com.ivanovsky.passnotes.data.repository.file

import android.util.Base64
import com.github.aivanovski.keepasstreebuilder.DatabaseBuilderDsl
import com.github.aivanovski.keepasstreebuilder.Fields
import com.github.aivanovski.keepasstreebuilder.converter.kotpass.KotpassDatabaseConverter
import com.github.aivanovski.keepasstreebuilder.extensions.toByteArray
import com.github.aivanovski.keepasstreebuilder.generator.EntityFactory.newBinaryFrom
import com.github.aivanovski.keepasstreebuilder.model.Binary
import com.github.aivanovski.keepasstreebuilder.model.DatabaseKey
import com.github.aivanovski.keepasstreebuilder.model.EntryEntity
import com.github.aivanovski.keepasstreebuilder.model.GroupEntity
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.util.StringUtils
import com.ivanovsky.passnotes.util.StringUtils.SPACE
import java.lang.StringBuilder
import java.time.Instant
import java.util.UUID

object FakeDatabaseContentFactory {

    fun createDefaultLocalDatabase(): ByteArray {
        return DatabaseBuilderDsl.newBuilder(KotpassDatabaseConverter())
            .key(PASSWORD_KEY)
            .content(ROOT) {
                group(GROUP_EMAIL)
                group(GROUP_INTERNET) {
                    group(GROUP_CODING) {
                        entry(ENTRY_LEETCODE)
                        entry(ENTRY_NEETCODE)
                        entry(ENTRY_GITHUB)
                    }
                    group(GROUP_GAMING) {
                        entry(ENTRY_STADIA)
                    }
                    group(GROUP_SHOPPING) {
                        entry(ENTRY_AMAZON)
                    }
                    group(GROUP_SOCIAL)

                    entry(ENTRY_GOOGLE)
                    entry(ENTRY_APPLE)
                    entry(ENTRY_MICROSOFT)
                }
                entry(ENTRY_NAS_LOGIN)
                entry(ENTRY_LAPTOP_LOGIN)
            }
            .build()
            .toByteArray()
    }

    fun createDefaultRemoteDatabase(): ByteArray {
        return DatabaseBuilderDsl.newBuilder(KotpassDatabaseConverter())
            .key(PASSWORD_KEY)
            .content(ROOT) {
                group(GROUP_EMAIL)
                group(GROUP_INTERNET) {
                    group(GROUP_CODING) {
                        entry(ENTRY_LEETCODE)
                        entry(ENTRY_NEETCODE)
                        entry(ENTRY_GITLAB)
                    }
                    group(GROUP_GAMING) {
                        entry(ENTRY_STADIA)
                    }
                    group(GROUP_SHOPPING) {
                        entry(ENTRY_AMAZON)
                    }
                    group(GROUP_SOCIAL)

                    entry(ENTRY_GOOGLE)
                    entry(ENTRY_APPLE)
                    entry(ENTRY_MICROSOFT_MODIFIED)
                }
                entry(ENTRY_NAS_LOGIN)
                entry(ENTRY_LAPTOP_LOGIN)
            }
            .build()
            .toByteArray()
    }

    fun createDatabaseWithOtpData(): ByteArray {
        return DatabaseBuilderDsl.newBuilder(KotpassDatabaseConverter())
            .key(PASSWORD_KEY)
            .content(ROOT) {
                entry(ENTRY_TOTP)
                entry(ENTRY_HOTP)
            }
            .build()
            .toByteArray()
    }

    fun createDatabaseWithKeyFile(): ByteArray {
        return DatabaseBuilderDsl.newBuilder(KotpassDatabaseConverter())
            .key(FILE_KEY)
            .content(ROOT) {
                entry(ENTRY_NAS_LOGIN)
                entry(ENTRY_LAPTOP_LOGIN)
                entry(ENTRY_MAC_BOOK_LOGIN)
            }
            .build()
            .toByteArray()
    }

    fun createDatabaseWithCombinedKey(): ByteArray {
        return DatabaseBuilderDsl.newBuilder(KotpassDatabaseConverter())
            .key(COMBINED_KEY)
            .content(ROOT) {
                entry(ENTRY_NAS_LOGIN)
                entry(ENTRY_LAPTOP_LOGIN)
                entry(ENTRY_MAC_BOOK_LOGIN)
            }
            .build()
            .toByteArray()
    }

    fun createKeyFileData(): ByteArray {
        return DEFAULT_KEY_FILE_CONTENT.toByteArray()
    }

    fun createDatabaseWithExpiredData(): ByteArray {
        return DatabaseBuilderDsl.newBuilder(KotpassDatabaseConverter())
            .key(PASSWORD_KEY)
            .content(ROOT) {
                entry(ENTRY_APPLE_EXPIRED)
                entry(ENTRY_AMAZON)
            }
            .build()
            .toByteArray()
    }

    fun createDatabaseWithHistoryData(): ByteArray {
        return DatabaseBuilderDsl.newBuilder(KotpassDatabaseConverter())
            .key(PASSWORD_KEY)
            .content(ROOT) {
                entry(ENTRY_APPLE.copy(history = APPLE_HISTORY))
                entry(ENTRY_READ_LIST.copy(history = READ_LIST_HISTORY))
                entry(ENTRY_MICROSOFT.copy(history = MICROSOFT_HISTORY))
            }
            .build()
            .toByteArray()
    }

    fun createDatabaseWithAttachmentsData(): ByteArray {
        return DatabaseBuilderDsl.newBuilder(KotpassDatabaseConverter())
            .key(PASSWORD_KEY)
            .content(ROOT) {
                entry(newEntryWithAttachments())
            }
            .build()
            .toByteArray()
    }

    fun createDemoDatabase(): ByteArray {
        return DatabaseBuilderDsl.newBuilder(KotpassDatabaseConverter())
            .key(PASSWORD_KEY)
            .content(ROOT) {
                group(GROUP_EMAIL)
                group(GROUP_INTERNET) {
                    group(GROUP_CODING) {
                        entry(ENTRY_LEETCODE)
                        entry(ENTRY_NEETCODE)
                        entry(ENTRY_GITHUB)
                    }
                    group(GROUP_GAMING) {
                        entry(ENTRY_STADIA)
                    }
                    group(GROUP_SHOPPING) {
                        entry(newDemoAmazonEntry())
                    }
                    group(GROUP_SOCIAL)

                    entry(ENTRY_GOOGLE)
                    entry(ENTRY_APPLE)
                    entry(ENTRY_MICROSOFT)
                }
                entry(ENTRY_NAS_LOGIN)
                entry(ENTRY_LAPTOP_LOGIN)
            }
            .build()
            .toByteArray()
    }

    private fun newEntry(
        created: Long,
        modified: Long,
        expires: Long? = null,
        title: String = StringUtils.EMPTY,
        username: String = StringUtils.EMPTY,
        password: String = StringUtils.EMPTY,
        url: String = StringUtils.EMPTY,
        notes: String = StringUtils.EMPTY,
        custom: Map<String, String> = emptyMap(),
        history: List<EntryEntity> = emptyList(),
        attachments: List<Binary> = emptyList()
    ): EntryEntity {
        val defaultFields = mapOf(
            PropertyType.TITLE.propertyName to title,
            PropertyType.USER_NAME.propertyName to username,
            PropertyType.PASSWORD.propertyName to password,
            PropertyType.URL.propertyName to url,
            PropertyType.NOTES.propertyName to notes
        )

        return EntryEntity(
            uuid = UUID(1L, title.hashCode().toLong()),
            created = Instant.ofEpochMilli(created),
            modified = Instant.ofEpochMilli(modified),
            expires = if (expires != null) {
                Instant.ofEpochMilli(expires)
            } else {
                null
            },
            fields = defaultFields.plus(custom),
            history = history,
            binaries = attachments
        )
    }

    private fun newGroup(
        title: String,
        uuid: UUID = UUID(0xFFL, title.hashCode().toLong())
    ): GroupEntity {
        return GroupEntity(
            uuid = uuid,
            fields = mapOf(
                Fields.TITLE to title
            )
        )
    }

    private fun newEntryWithAttachments(): EntryEntity =
        newEntry(
            title = "Entry with Attachments",
            notes = "There are some files attached",
            created = parseDate("2020-01-01"),
            modified = parseDate("2020-01-30"),
            attachments = listOf(
                newBinaryFrom(
                    name = "text.txt",
                    content = DUMMY_TEXT.toByteArray()
                ),
                newBinaryFrom(
                    name = "image.png",
                    content = Base64.decode(ENCODED_IMAGE, Base64.NO_WRAP)
                )
            )
        )

    private fun newDemoAmazonEntry(): EntryEntity {
        return ENTRY_AMAZON.copy(
            history = listOf(),
            binaries = listOf(
                newBinaryFrom(
                    name = "aws-id-rsa",
                    content = generateDummyText(1600).toByteArray()
                ),
                newBinaryFrom(
                    name = "aws-id-rsa.pub",
                    content = generateDummyText(400).toByteArray()
                )
            )
        )
    }

    private fun generateDummyText(length: Int): String {
        val sb = StringBuilder()

        while (sb.length < length) {
            val dummyLength = DUMMY_TEXT.length

            val text = if (sb.length + dummyLength < length) {
                DUMMY_TEXT
            } else {
                val delta = length - sb.length
                DUMMY_TEXT.substring(0, delta)
            }

            if (sb.isNotEmpty()) {
                sb.append(SPACE)
            }

            sb.append(text)
        }

        return sb.toString()
    }

    private const val DEFAULT_PASSWORD = "abc123"
    private const val DEFAULT_KEY_FILE_CONTENT = "abcdefg1235678"

    private val PASSWORD_KEY = DatabaseKey.PasswordKey(DEFAULT_PASSWORD)
    private val FILE_KEY = DatabaseKey.BinaryKey(DEFAULT_KEY_FILE_CONTENT.toByteArray())
    private val COMBINED_KEY = DatabaseKey.CompositeKey(
        password = DEFAULT_PASSWORD,
        binaryData = DEFAULT_KEY_FILE_CONTENT.toByteArray()
    )

    private val ROOT = newGroup(title = "Database")
    private val GROUP_EMAIL = newGroup(title = "Email")
    private val GROUP_INTERNET = newGroup(title = "Internet")
    private val GROUP_CODING = newGroup(title = "Coding")
    private val GROUP_GAMING = newGroup(title = "Gaming")
    private val GROUP_SHOPPING = newGroup(title = "Shopping")
    private val GROUP_SOCIAL = newGroup(title = "Social")

    private val TOTP_URL = """
            otpauth://totp/Example:john.doe?secret=AAAABBBBCCCCDDDD&period=30
            &digits=6&issuer=Example&algorithm=SHA1
    """.trimIndent().replace("\n", "")

    private val HOTP_URL = """
            otpauth://hotp/Example:john.doe?secret=AAAABBBBCCCCDDDD&digits=6
            &issuer=Example&algorithm=SHA1&counter=1
    """.trimIndent().replace("\n", "")

    private val ENCODED_IMAGE = """
        iVBORw0KGgoAAAANSUhEUgAAAA8AAAAPCAIAAAC0tAIdAAAAGElEQV
        R4AWMgGax5dYcYRJTqUdWjqkkFAPGInVINlnHhAAAAAElFTkSuQmCC
    """.trimIndent().replace("\n", "")

    private val DUMMY_TEXT = """
        Lorem Ipsum is simply dummy text of the printing and typesetting industry.
    """.trim()

    private val ENTRY_NAS_LOGIN = newEntry(
        title = "NAS Login",
        username = "john.doe",
        password = "abc123",
        created = parseDate("2020-01-01"),
        modified = parseDate("2020-01-01")
    )

    private val ENTRY_LAPTOP_LOGIN = newEntry(
        title = "Laptop login",
        username = "john.doe",
        password = "abc123",
        created = parseDate("2020-01-02"),
        modified = parseDate("2020-01-02")
    )

    private val ENTRY_MAC_BOOK_LOGIN = newEntry(
        title = "MacBook Login",
        username = "john.doe",
        password = "abc123",
        created = parseDate("2020-02-01"),
        modified = parseDate("2020-02-01")
    )

    private val ENTRY_GOOGLE = newEntry(
        title = "Google",
        username = "john.doe@example.com",
        password = "abc123",
        url = "https://google.com",
        created = parseDate("2020-01-03"),
        modified = parseDate("2020-01-03")
    )

    private val ENTRY_APPLE = newEntry(
        title = "Apple",
        username = "john.doe@example.com",
        password = "abc123",
        url = "https://apple.com",
        notes = "My personal Apple account",
        created = parseDateAndTime("2020-01-01 12:55:00"),
        modified = parseDateAndTime("2020-01-10 11:08:00")
    )

    private val ENTRY_MICROSOFT = newEntry(
        title = "Microsoft",
        username = "john.doe@example.com",
        password = "abc123",
        url = "https://microsoft.com",
        created = parseDate("2020-01-01"),
        modified = parseDate("2020-01-10")
    )

    private val ENTRY_MICROSOFT_MODIFIED = newEntry(
        title = "Microsoft",
        username = "john.galt@example.com",
        password = "qwerty",
        url = "https://microsoft.com",
        created = parseDate("2020-01-05"),
        modified = parseDate("2020-01-05")
    )

    private val ENTRY_LEETCODE = newEntry(
        title = "Leetcode.com",
        username = "john.doe@example.com",
        password = "abc123",
        url = "https://leetcode.com",
        created = parseDate("2020-01-06"),
        modified = parseDate("2020-01-06")
    )

    private val ENTRY_NEETCODE = newEntry(
        title = "Neetcode.com",
        username = "john.doe@example.com",
        url = "https://neetcode.io/practice",
        created = parseDate("2020-01-07"),
        modified = parseDate("2020-01-07")
    )

    private val ENTRY_GITHUB = newEntry(
        title = "Github.com",
        username = "john.doe@example.com",
        password = "abc123",
        url = "https://github.com",
        created = parseDate("2020-01-08"),
        modified = parseDate("2020-01-08")
    )

    private val ENTRY_GITLAB = newEntry(
        title = "Gitlab.com",
        username = "john.doe@example.com",
        password = "abc123",
        url = "https://gitlab.com",
        created = parseDate("2020-01-08"),
        modified = parseDate("2020-01-08")
    )

    private val ENTRY_STADIA = newEntry(
        title = "Stadia.com",
        username = "john.doe@example.com",
        password = "abc123",
        created = parseDate("2020-01-09"),
        modified = parseDate("2020-01-09")
    )

    private val ENTRY_AMAZON = newEntry(
        title = "Amazon.com",
        username = "john.doe@example.com",
        password = "abc123",
        created = parseDate("2020-01-09"),
        modified = parseDate("2020-01-09"),
        expires = parseDate("2030-01-10"),
        custom = mapOf(
            PropertyType.URL.propertyName to "https://amazon.com",
            PropertyType.OTP.propertyName to TOTP_URL
        )
    )

    private val ENTRY_TOTP = newEntry(
        title = "TOTP Entry",
        username = "john.doe@example.com",
        password = "",
        created = parseDate("2020-01-09"),
        modified = parseDate("2020-01-09"),
        custom = mapOf(
            "otp" to TOTP_URL
        )
    )

    private val ENTRY_HOTP = newEntry(
        title = "HOTP Entry",
        username = "john.doe@example.com",
        password = "",
        created = parseDate("2020-01-09"),
        modified = parseDate("2020-01-09"),
        custom = mapOf(
            "otp" to HOTP_URL
        )
    )

    private val ENTRY_APPLE_EXPIRED = newEntry(
        title = "Apple(expired)",
        username = "john.doe@example.com",
        password = "abc123",
        url = "https://apple.com",
        created = parseDate("2020-01-04"),
        modified = parseDate("2020-01-04"),
        expires = parseDate("2020-01-05")
    )

    private val MICROSOFT_HISTORY = listOf(
        ENTRY_MICROSOFT.copy(
            fields = ENTRY_MICROSOFT.fields.plus(
                PropertyType.PASSWORD.propertyName to "123"
            ),
            modified = parseDateAndTime("2020-01-01 10:04:00").toInstant()
        ),
        ENTRY_MICROSOFT.copy(
            fields = ENTRY_MICROSOFT.fields.plus(
                PropertyType.PASSWORD.propertyName to "123456"
            ),
            modified = parseDateAndTime("2020-01-02 11:12:00").toInstant()
        ),
        ENTRY_MICROSOFT.copy(
            fields = ENTRY_MICROSOFT.fields.plus(
                PropertyType.PASSWORD.propertyName to "qwerty"
            ),
            modified = parseDateAndTime("2020-01-03 12:56:00").toInstant()
        ),
        ENTRY_MICROSOFT.copy(
            fields = ENTRY_MICROSOFT.fields.plus(
                PropertyType.PASSWORD.propertyName to "abc111"
            ),
            modified = parseDateAndTime("2020-01-04 9:44:00").toInstant()
        ),
        ENTRY_MICROSOFT.copy(
            modified = parseDateAndTime("2020-01-05 17:26:00").toInstant()
        )
    )

    private val APPLE_HISTORY = listOf(
        ENTRY_APPLE.copy(
            fields = ENTRY_APPLE.fields.plus(
                PropertyType.PASSWORD.propertyName to "abc",
                PropertyType.URL.propertyName to "apple.com",
                PropertyType.NOTES.propertyName to "My personal Apple account",
                "AppleID" to "john.doe"
            ),
            modified = parseDateAndTime("2020-01-01 10:09:00").toInstant()
        ),
        ENTRY_APPLE.copy(
            fields = ENTRY_APPLE.fields.plus(
                PropertyType.PASSWORD.propertyName to "abc",
                PropertyType.URL.propertyName to "https://apple.com",
                PropertyType.NOTES.propertyName to "My personal Apple account",
                PropertyType.OTP.propertyName to TOTP_URL
            ),
            modified = parseDateAndTime("2020-01-02 14:35:00").toInstant()
        ),
        ENTRY_APPLE.copy(
            fields = ENTRY_APPLE.fields.plus(
                PropertyType.PASSWORD.propertyName to "abc",
                PropertyType.URL.propertyName to "https://apple.com",
                PropertyType.NOTES.propertyName to "My personal Apple account"
            ),
            modified = parseDateAndTime("2020-01-03 18:44:00").toInstant()
        )
    )

    private val ENTRY_READ_LIST = newEntry(
        title = "My reading list",
        notes = "The list of I've read and books I want to read",
        created = parseDate("2020-01-01"),
        modified = parseDate("2020-01-30"),
        custom = mapOf(
            "J.K. Rowling" to "Harry Potter and the Sorcerers Stone",
            "Orwell" to "1984",
            "J.R.R. Tolkien" to "The Lord of the Rings",
            "F. Scott Fitzgerald" to "The Great Gatsby; The Side of Paradise",
            "Aldous Huxley" to "Brave New World"
        )
    )

    private val READ_LIST_HISTORY = listOf(
        ENTRY_READ_LIST.copy(
            fields = ENTRY_READ_LIST.fields.plus(
                "J.K. Rowling" to "Harry Potter and the Sorcer Stone",
                "George Orwell" to "1984",
                "Harper Lee" to "To Kill a Mockingbird"
            ),
            modified = parseDate("2020-01-01").toInstant()
        ),
        ENTRY_READ_LIST.copy(
            fields = ENTRY_READ_LIST.fields.plus(
                "J.K. Rowling" to "Harry Potter and the Sorcerers Stone",
                "George Orwell" to "1984",
                "J.R.R. Tolkien" to "The Lord of the Rings",
                "F. Scott Fitzgerald" to "The Great Gatsby"
            ),
            modified = parseDate("2020-01-02").toInstant()
        ),
        ENTRY_READ_LIST.copy(
            fields = ENTRY_READ_LIST.fields.plus(
                "J.K. Rowling" to "Harry Potter and the Sorcerers Stone",
                "George Orwell" to "1984",
                "J.R.R. Tolkien" to "The Lord of the Rings",
                "F. Scott Fitzgerald" to "The Great Gatsby; The Side of Paradise"
            ),
            modified = parseDate("2020-01-03").toInstant()
        )
    )
}