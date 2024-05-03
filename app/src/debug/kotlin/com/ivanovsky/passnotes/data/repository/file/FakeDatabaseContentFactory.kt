package com.ivanovsky.passnotes.data.repository.file

import app.keemobile.kotpass.cryptography.EncryptedValue
import app.keemobile.kotpass.database.Credentials
import app.keemobile.kotpass.database.KeePassDatabase
import app.keemobile.kotpass.database.encode
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.data.repository.file.databaseDsl.EntryEntity
import com.ivanovsky.passnotes.data.repository.file.databaseDsl.GroupEntity
import com.ivanovsky.passnotes.data.repository.file.databaseDsl.KotpassTreeDsl.newDatabase
import java.io.ByteArrayOutputStream
import java.util.UUID

object FakeDatabaseContentFactory {

    fun createDefaultLocalDatabase(): ByteArray {
        return newDatabase(PASSWORD_CREDENTIALS, ROOT) {
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
                group(GROUP_SHOPPING)
                group(GROUP_SOCIAL)

                entry(ENTRY_GOOGLE)
                entry(ENTRY_APPLE)
                entry(ENTRY_MICROSOFT)
            }
            entry(ENTRY_NAS_LOGIN)
            entry(ENTRY_LAPTOP_LOGIN)
        }
            .encodeToByteArray()
    }

    fun createDefaultRemoteDatabase(): ByteArray {
        return newDatabase(PASSWORD_CREDENTIALS, ROOT) {
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
            entry(ENTRY_MAC_BOOK_LOGIN)
        }
            .encodeToByteArray()
    }

    fun createDatabaseWithOtpData(): ByteArray {
        return newDatabase(PASSWORD_CREDENTIALS, ROOT) {
            entry(ENTRY_TOTP)
            entry(ENTRY_HOTP)
        }
            .encodeToByteArray()
    }

    fun createDatabaseWithKeyFile(): ByteArray {
        return newDatabase(KEY_FILE_CREDENTIALS, ROOT) {
            entry(ENTRY_NAS_LOGIN)
            entry(ENTRY_LAPTOP_LOGIN)
            entry(ENTRY_MAC_BOOK_LOGIN)
        }
            .encodeToByteArray()
    }

    fun createDatabaseWithCombinedKey(): ByteArray {
        return newDatabase(COMBINED_CREDENTIALS, ROOT) {
            entry(ENTRY_NAS_LOGIN)
            entry(ENTRY_LAPTOP_LOGIN)
            entry(ENTRY_MAC_BOOK_LOGIN)
        }
            .encodeToByteArray()
    }

    fun createKeyFileData(): ByteArray {
        return DEFAULT_KEY_FILE.toByteArray()
    }

    private fun KeePassDatabase.encodeToByteArray(): ByteArray {
        return ByteArrayOutputStream().use { out ->
            this.encode(out)
            out.toByteArray()
        }
    }

    private const val DEFAULT_PASSWORD = "abc123"
    private const val DEFAULT_KEY_FILE = "abcdefg1235678"

    private val PASSWORD_CREDENTIALS = Credentials.from(
        passphrase = EncryptedValue.fromString(DEFAULT_PASSWORD)
    )

    private val KEY_FILE_CREDENTIALS = Credentials.from(
        keyData = DEFAULT_KEY_FILE.toByteArray()
    )

    private val COMBINED_CREDENTIALS = Credentials.from(
        passphrase = EncryptedValue.fromString(DEFAULT_PASSWORD),
        keyData = DEFAULT_KEY_FILE.toByteArray()
    )

    private val ROOT = GroupEntity(title = "Database")
    private val GROUP_EMAIL = GroupEntity(title = "Email", uuid = UUID(100L, 1L))
    private val GROUP_INTERNET = GroupEntity(title = "Internet", uuid = UUID(100L, 2L))
    private val GROUP_CODING = GroupEntity(title = "Coding", uuid = UUID(100L, 3L))
    private val GROUP_GAMING = GroupEntity(title = "Gaming", uuid = UUID(100L, 4L))
    private val GROUP_SHOPPING = GroupEntity(title = "Shopping", uuid = UUID(100L, 5L))
    private val GROUP_SOCIAL = GroupEntity(title = "Social", uuid = UUID(100L, 7L))

    private val TOTP_URL = """
            otpauth://totp/Example:john.doe?secret=AAAABBBBCCCCDDDD&period=30
            &digits=6&issuer=Example&algorithm=SHA1
    """.trimIndent()

    private val HOTP_URL = """
            otpauth://hotp/Example:john.doe?secret=AAAABBBBCCCCDDDD&digits=6
            &issuer=Example&algorithm=SHA1&counter=1
    """.trimIndent()

    private val ENTRY_NAS_LOGIN = EntryEntity(
        title = "NAS Login",
        username = "john.doe",
        password = "abc123",
        created = parseDate("2020-01-01"),
        modified = parseDate("2020-01-01")
    )

    private val ENTRY_LAPTOP_LOGIN = EntryEntity(
        title = "Laptop login",
        username = "john.doe",
        password = "abc123",
        created = parseDate("2020-01-02"),
        modified = parseDate("2020-01-02")
    )

    private val ENTRY_MAC_BOOK_LOGIN = EntryEntity(
        title = "MacBook Login",
        username = "john.doe",
        password = "abc123",
        created = parseDate("2020-02-01"),
        modified = parseDate("2020-02-01")
    )

    private val ENTRY_GOOGLE = EntryEntity(
        title = "Google",
        username = "john.doe@example.com",
        password = "abc123",
        url = "https://google.com",
        created = parseDate("2020-01-03"),
        modified = parseDate("2020-01-03")
    )

    private val ENTRY_APPLE = EntryEntity(
        title = "Apple",
        username = "john.doe@example.com",
        password = "abc123",
        url = "https://apple.com",
        created = parseDate("2020-01-04"),
        modified = parseDate("2020-01-04")
    )

    private val ENTRY_MICROSOFT = EntryEntity(
        title = "Microsoft",
        username = "john.doe@example.com",
        password = "abc123",
        url = "https://microsoft.com",
        created = parseDate("2020-01-05"),
        modified = parseDate("2020-01-05")
    )

    private val ENTRY_MICROSOFT_MODIFIED = EntryEntity(
        title = "Microsoft",
        username = "john.galt@example.com",
        password = "qwerty",
        url = "https://microsoft.com",
        created = parseDate("2020-01-05"),
        modified = parseDate("2020-01-05")
    )

    private val ENTRY_LEETCODE = EntryEntity(
        title = "Leetcode.com",
        username = "john.doe@example.com",
        password = "abc123",
        url = "https://leetcode.com",
        created = parseDate("2020-01-06"),
        modified = parseDate("2020-01-06")
    )

    private val ENTRY_NEETCODE = EntryEntity(
        title = "Neetcode.com",
        username = "john.doe@example.com",
        url = "https://neetcode.io/practice",
        created = parseDate("2020-01-07"),
        modified = parseDate("2020-01-07")
    )

    private val ENTRY_GITHUB = EntryEntity(
        title = "Github.com",
        username = "john.doe@example.com",
        password = "abc123",
        url = "https://github.com",
        created = parseDate("2020-01-08"),
        modified = parseDate("2020-01-08")
    )

    private val ENTRY_GITLAB = EntryEntity(
        title = "Gitlab.com",
        username = "john.doe@example.com",
        password = "abc123",
        url = "https://gitlab.com",
        created = parseDate("2020-01-08"),
        modified = parseDate("2020-01-08")
    )

    private val ENTRY_STADIA = EntryEntity(
        title = "Stadia.com",
        username = "john.doe@example.com",
        password = "abc123",
        created = parseDate("2020-01-09"),
        modified = parseDate("2020-01-09")
    )

    private val ENTRY_AMAZON = EntryEntity(
        title = "Amazon.com",
        username = "john.doe@example.com",
        password = "abc123",
        created = parseDate("2020-01-09"),
        modified = parseDate("2020-01-09"),
        custom = mapOf(
            PropertyType.URL.propertyName to "https://amazon.com"
        )
    )

    private val ENTRY_TOTP = EntryEntity(
        title = "TOTP Entry",
        username = "john.doe@example.com",
        password = "",
        created = parseDate("2020-01-09"),
        modified = parseDate("2020-01-09"),
        custom = mapOf(
            "otp" to TOTP_URL
        )
    )

    private val ENTRY_HOTP = EntryEntity(
        title = "HOTP Entry",
        username = "john.doe@example.com",
        password = "",
        created = parseDate("2020-01-09"),
        modified = parseDate("2020-01-09"),
        custom = mapOf(
            "otp" to HOTP_URL
        )
    )
}