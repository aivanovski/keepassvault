package com.ivanovsky.passnotes

import androidx.room.Room
import androidx.room.migration.AutoMigrationSpec
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.ivanovsky.passnotes.data.repository.db.AppDatabase
import com.ivanovsky.passnotes.data.repository.db.converters.FSAuthorityTypeConverter
import com.ivanovsky.passnotes.utils.ClearTextDataCipher
import com.ivanovsky.passnotes.utils.DataCipherProviderImpl
import java.util.Calendar

fun dateInMillis(year: Int, month: Int, day: Int): Long {
    val cal = Calendar.getInstance()

    cal.set(year, month, day, 0, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)

    return cal.timeInMillis
}

fun initInMemoryDatabase(): AppDatabase {
    return Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().context,
        AppDatabase::class.java
    )
        .addTypeConverter(FSAuthorityTypeConverter(DataCipherProviderImpl(ClearTextDataCipher())))
        .build()
}

fun initMigrationHelper(): MigrationTestHelper =
    MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList<AutoMigrationSpec>(),
        FrameworkSQLiteOpenHelperFactory()
    )