package com.ivanovsky.passnotes.data.repository.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class MigrationFrom5To6 : Migration(5, 6) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE git_root ADD COLUMN ssh_key_path TEXT DEFAULT null")
        db.execSQL("ALTER TABLE git_root ADD COLUMN ssh_key_file_fsAuthority TEXT DEFAULT null")
        db.execSQL("ALTER TABLE git_root ADD COLUMN ssh_key_file_path TEXT DEFAULT null")
        db.execSQL("ALTER TABLE git_root ADD COLUMN ssh_key_file_uid TEXT DEFAULT null")
        db.execSQL("ALTER TABLE git_root ADD COLUMN ssh_key_file_name TEXT DEFAULT null")
    }
}