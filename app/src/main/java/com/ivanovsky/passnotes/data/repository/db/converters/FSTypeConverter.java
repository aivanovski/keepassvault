package com.ivanovsky.passnotes.data.repository.db.converters;

import androidx.room.TypeConverter;

import com.ivanovsky.passnotes.data.repository.file.FSType;

public class FSTypeConverter {

    @TypeConverter
    public FSType fromDatabaseValue(String value) {
        return FSType.findByValue(value);
    }

    @TypeConverter
    public String toDatabaseValue(FSType fsType) {
        return fsType != null ? fsType.getValue() : null;
    }
}
