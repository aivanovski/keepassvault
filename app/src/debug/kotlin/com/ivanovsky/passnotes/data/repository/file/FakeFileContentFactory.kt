package com.ivanovsky.passnotes.data.repository.file

import app.keemobile.kotpass.database.KeePassDatabase
import app.keemobile.kotpass.database.encode
import com.ivanovsky.passnotes.data.repository.file.databaseDsl.EntryEntity
import com.ivanovsky.passnotes.data.repository.file.databaseDsl.GroupEntity
import com.ivanovsky.passnotes.data.repository.file.databaseDsl.KotpassTreeDsl
import java.io.ByteArrayOutputStream
import java.util.UUID

class FakeFileContentFactory {

    fun createDefaultLocalDatabase(): ByteArray {
        return KotpassTreeDsl.tree(ROOT) {
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
            entry(ENTRY_LOCAL)
        }
            .toByteArray()
    }

    fun createDefaultRemoteDatabase(): ByteArray {
        return KotpassTreeDsl.tree(ROOT) {
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
                group(GROUP_SHOPPING)
                group(GROUP_SOCIAL)

                entry(ENTRY_GOOGLE)
                entry(ENTRY_APPLE)
                entry(ENTRY_MICROSOFT)
            }
            entry(ENTRY_NAS_LOGIN)
            entry(ENTRY_LAPTOP_LOGIN)
            entry(ENTRY_MAC_BOOK_LOGIN)
            entry(ENTRY_REMOTE)
        }
            .toByteArray()
    }

    private fun KeePassDatabase.toByteArray(): ByteArray {
        return ByteArrayOutputStream().use { out ->
            this.encode(out)
            out.toByteArray()
        }
    }

    companion object {
        private val ROOT = GroupEntity(title = "Database")
        private val GROUP_EMAIL = GroupEntity(title = "Email", uuid = UUID(100L, 1L))
        private val GROUP_INTERNET = GroupEntity(title = "Internet", uuid = UUID(100L, 2L))
        private val GROUP_CODING = GroupEntity(title = "Coding", uuid = UUID(100L, 3L))
        private val GROUP_GAMING = GroupEntity(title = "Gaming", uuid = UUID(100L, 4L))
        private val GROUP_SHOPPING = GroupEntity(title = "Shopping", uuid = UUID(100L, 5L))
        private val GROUP_SOCIAL = GroupEntity(title = "Social", uuid = UUID(100L, 6L))

        private val ENTRY_LOCAL = EntryEntity(
            title = "Local",
            username = "john.doe",
            password = "abc123",
            created = parseDate("2020-01-01"),
            modified = parseDate("2020-01-01")
        )

        private val ENTRY_REMOTE = EntryEntity(
            title = "Remote",
            username = "john.doe",
            password = "abc123",
            created = parseDate("2020-01-01"),
            modified = parseDate("2020-01-01")
        )

        private val ENTRY_NAS_LOGIN = EntryEntity(
            title = "My NAS Login",
            username = "john.doe",
            password = "abc123",
            created = parseDate("2020-01-01"),
            modified = parseDate("2020-01-01")
        )

        private val ENTRY_LAPTOP_LOGIN = EntryEntity(
            title = "My Laptop Login",
            username = "john.doe",
            password = "abc123",
            created = parseDate("2020-01-02"),
            modified = parseDate("2020-01-02")
        )

        private val ENTRY_MAC_BOOK_LOGIN = EntryEntity(
            title = "My Mac Book Login",
            username = "john.doe",
            password = "abc123",
            created = parseDate("2020-02-01"),
            modified = parseDate("2020-02-01")
        )

        private val ENTRY_GOOGLE = EntryEntity(
            title = "My Google Login",
            username = "john.doe@example.com",
            password = "abc123",
            url = "https://google.com",
            created = parseDate("2020-01-03"),
            modified = parseDate("2020-01-03")
        )

        private val ENTRY_APPLE = EntryEntity(
            title = "My Apple Login",
            username = "john.doe@example.com",
            password = "abc123",
            url = "https://apple.com",
            created = parseDate("2020-01-04"),
            modified = parseDate("2020-01-04")
        )

        private val ENTRY_MICROSOFT = EntryEntity(
            title = "My Microsoft Login",
            username = "john.doe@example.com",
            password = "abc123",
            url = "https://microsoft.com",
            created = parseDate("2020-01-05"),
            modified = parseDate("2020-01-05")
        )

        private val ENTRY_LEETCODE = EntryEntity(
            title = "My LeetCode Login",
            username = "john.doe@example.com",
            password = "abc123",
            url = "https://leetcode.com",
            created = parseDate("2020-01-06"),
            modified = parseDate("2020-01-06")
        )

        private val ENTRY_NEETCODE = EntryEntity(
            title = "My NeetCode Login",
            username = "john.doe@example.com",
            url = "https://neetcode.io/practice",
            created = parseDate("2020-01-07"),
            modified = parseDate("2020-01-07")
        )

        private val ENTRY_GITHUB = EntryEntity(
            title = "My GitHub Login",
            username = "john.doe@example.com",
            password = "abc123",
            url = "https://github.com",
            created = parseDate("2020-01-08"),
            modified = parseDate("2020-01-08")
        )

        private val ENTRY_GITLAB = EntryEntity(
            title = "My GitLab Login",
            username = "john.doe@example.com",
            password = "abc123",
            url = "https://gitlab.com",
            created = parseDate("2020-01-08"),
            modified = parseDate("2020-01-08")
        )

        private val ENTRY_STADIA = EntryEntity(
            title = "My Stadia Login",
            username = "john.doe@example.com",
            password = "abc123",
            created = parseDate("2020-01-09"),
            modified = parseDate("2020-01-09")
        )
    }
}