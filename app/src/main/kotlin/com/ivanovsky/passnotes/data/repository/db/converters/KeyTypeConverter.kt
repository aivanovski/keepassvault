package com.ivanovsky.passnotes.data.repository.db.converters

import androidx.room.TypeConverter
import com.ivanovsky.passnotes.data.entity.KeyType

object KeyTypeConverter {

    @TypeConverter
    fun fromDatabaseValue(value: String): KeyType {
        return KeyType.getByName(value) ?: KeyType.PASSWORD
    }

    @TypeConverter
    fun toDatabaseValue(type: KeyType): String = type.name
}