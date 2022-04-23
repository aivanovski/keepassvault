package com.ivanovsky.passnotes.data.repository.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class MigrationFrom1To2 : Migration(1, 2) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE used_file ADD COLUMN key_type TEXT NOT NULL DEFAULT 'PASSWORD'")
        database.execSQL("ALTER TABLE used_file ADD COLUMN key_file_fs_authority TEXT DEFAULT null")
        database.execSQL("ALTER TABLE used_file ADD COLUMN key_file_path TEXT DEFAULT null")
        database.execSQL("ALTER TABLE used_file ADD COLUMN key_file_uid TEXT DEFAULT null")
        database.execSQL("ALTER TABLE used_file ADD COLUMN key_file_name TEXT DEFAULT null")
    }
}