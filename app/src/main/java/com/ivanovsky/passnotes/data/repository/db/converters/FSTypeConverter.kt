package com.ivanovsky.passnotes.data.repository.db.converters

import androidx.room.TypeConverter
import com.ivanovsky.passnotes.data.entity.FSType
import com.ivanovsky.passnotes.data.entity.FSType.Companion.findByValue

object FSTypeConverter {

    @TypeConverter
    fun fromDatabaseValue(value: String): FSType {
        return findByValue(value) ?: FSType.REGULAR_FS
    }

    @TypeConverter
    fun toDatabaseValue(fsType: FSType): String {
        return fsType.value
    }
}