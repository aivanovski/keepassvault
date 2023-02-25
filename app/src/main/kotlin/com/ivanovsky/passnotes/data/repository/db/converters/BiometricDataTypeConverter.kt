package com.ivanovsky.passnotes.data.repository.db.converters

import androidx.room.TypeConverter
import com.ivanovsky.passnotes.data.crypto.entity.Base64SecretData
import com.ivanovsky.passnotes.data.crypto.entity.BiometricData
import com.ivanovsky.passnotes.data.crypto.entity.toBase64SecretData
import com.ivanovsky.passnotes.data.crypto.entity.toBiometricData
import com.ivanovsky.passnotes.data.crypto.entity.toSecretData

object BiometricDataTypeConverter {

    @TypeConverter
    fun fromDatabaseValue(value: String?): BiometricData? {
        return value?.let { Base64SecretData.parse(it) }
            ?.toSecretData()
            ?.toBiometricData()
    }

    @TypeConverter
    fun toDatabaseValue(value: BiometricData?): String? {
        return value
            ?.toSecretData()
            ?.toBase64SecretData()
            ?.toString()
    }
}