package com.ivanovsky.passnotes.data.repository.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.entity.RemoteFile
import androidx.room.TypeConverters
import com.ivanovsky.passnotes.data.repository.db.converters.FSAuthorityTypeConverter
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.ivanovsky.passnotes.data.crypto.DataCipherProvider
import com.ivanovsky.passnotes.data.entity.GitRoot
import com.ivanovsky.passnotes.data.repository.db.dao.GitRootDao
import com.ivanovsky.passnotes.data.repository.db.dao.UsedFileDao
import com.ivanovsky.passnotes.data.repository.db.dao.RemoteFileDao
import com.ivanovsky.passnotes.data.repository.db.migration.MigrationFrom1To2
import com.ivanovsky.passnotes.data.repository.db.migration.MigrationFrom2To3

// TODO(improvement): Unused data from should be removed from database

@Database(
    entities = [
        UsedFile::class,
        RemoteFile::class,
        GitRoot::class
    ],
    version = 3
)
@TypeConverters(
    FSAuthorityTypeConverter::class,
)
abstract class AppDatabase : RoomDatabase() {

    abstract val usedFileDao: UsedFileDao
    abstract val remoteFileDao: RemoteFileDao
    abstract val gitRootDao: GitRootDao

    companion object {

        private const val FILE_NAME = "passnotes.db"

        fun buildDatabase(
            context: Context,
            cipherProvider: DataCipherProvider
        ): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                FILE_NAME
            )
                .addTypeConverter(FSAuthorityTypeConverter(cipherProvider))
                .addMigrations(*createMigrations(cipherProvider))
                .build()
        }

        private fun createMigrations(cipherProvider: DataCipherProvider): Array<Migration> {
            return arrayOf(
                MigrationFrom1To2(),
                MigrationFrom2To3(cipherProvider)
            )
        }
    }
}