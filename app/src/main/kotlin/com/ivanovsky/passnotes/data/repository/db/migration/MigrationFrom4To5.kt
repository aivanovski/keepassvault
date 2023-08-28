package com.ivanovsky.passnotes.data.repository.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class MigrationFrom4To5 : Migration(4, 5) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE used_file ADD COLUMN is_root INTEGER NOT NULL DEFAULT 0")
    }
}