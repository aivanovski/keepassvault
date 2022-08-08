package com.ivanovsky.passnotes

import androidx.room.Room
import androidx.room.migration.AutoMigrationSpec
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.ivanovsky.passnotes.data.repository.db.AppDatabase
import com.ivanovsky.passnotes.data.repository.db.converters.FSAuthorityTypeConverter
import com.ivanovsky.passnotes.utils.ClearTextDataCipher
import com.ivanovsky.passnotes.utils.DataCipherProviderImpl

object TestDatabase {

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

    fun SupportSQLiteDatabase.insertRow(
        tableName: String,
        data: Map<String, Any?>
    ) {
        val columnNames = data.keys.toList()
        val columnValues = data.values.toList()

        val sql = StringBuilder().apply {
            append("INSERT INTO $tableName (")

            for ((columnIdx, columnName) in columnNames.withIndex()) {
                append(columnName)
                if (columnIdx != columnNames.size - 1) {
                    append(",")
                }
            }

            append(") VALUES (")

            for ((columnIdx, columnValue) in columnValues.withIndex()) {
                when (columnValue) {
                    is Int -> append(columnValue)
                    is Long -> append(columnValue)
                    is String -> append("'").append(columnValue).append("'")
                    else -> append("null")
                }

                if (columnIdx != columnNames.size - 1) {
                    append(",")
                }
            }

            append(")")
        }

        execSQL(sql.toString())
    }
}
