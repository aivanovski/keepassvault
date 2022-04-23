package com.ivanovsky.passnotes.data.repository.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.data.entity.RemoteFile
import androidx.room.TypeConverters
import com.ivanovsky.passnotes.data.repository.db.converters.FSAuthorityTypeConverter
import androidx.room.RoomDatabase
import com.ivanovsky.passnotes.data.crypto.DataCipherProvider
import com.ivanovsky.passnotes.data.repository.db.dao.UsedFileDao
import com.ivanovsky.passnotes.data.repository.db.dao.RemoteFileDao
import com.ivanovsky.passnotes.data.repository.db.migration.MigrationFrom1To2

@Database(
    entities = [
        UsedFile::class,
        RemoteFile::class
    ],
    version = 2
)
@TypeConverters(
    FSAuthorityTypeConverter::class,
)
abstract class AppDatabase : RoomDatabase() {

    abstract val usedFileDao: UsedFileDao
    abstract val remoteFileDao: RemoteFileDao

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
                .addMigrations(MigrationFrom1To2())
                .build()
        }
    }
}