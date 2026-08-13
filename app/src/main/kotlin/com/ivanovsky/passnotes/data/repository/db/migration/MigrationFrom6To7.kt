package com.ivanovsky.passnotes.data.repository.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class MigrationFrom6To7 : Migration(6, 7) {

    // TODO: add test

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE remote_file ADD COLUMN local_backup_path TEXT DEFAULT null")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS temporary_file (
                file_path TEXT NOT NULL,
                created_time INTEGER NOT NULL,
                PRIMARY KEY(file_path)
            )
            """.trimIndent()
        )
    }
}