package com.ivanovsky.passnotes.data.repository.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class MigrationFrom6To7 : Migration(6, 7) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE remote_file ADD COLUMN local_backup_path TEXT DEFAULT null")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS temporary_file (
                path TEXT NOT NULL,
                created INTEGER NOT NULL,
                modified INTEGER,
                PRIMARY KEY(path)
            )
            """.trimIndent()
        )
    }
}