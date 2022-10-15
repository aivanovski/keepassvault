package com.ivanovsky.passnotes.data.repository.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class MigrationFrom3To4 : Migration(3, 4) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE used_file ADD COLUMN biometric_data TEXT DEFAULT null")
    }
}