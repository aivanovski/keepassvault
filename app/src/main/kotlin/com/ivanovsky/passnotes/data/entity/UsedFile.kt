package com.ivanovsky.passnotes.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ivanovsky.passnotes.data.crypto.entity.BiometricData
import com.ivanovsky.passnotes.data.repository.db.converters.BiometricDataTypeConverter
import com.ivanovsky.passnotes.data.repository.db.converters.KeyTypeConverter

@Entity(tableName = "used_file")
@TypeConverters(KeyTypeConverter::class, BiometricDataTypeConverter::class)
data class UsedFile(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int? = null,

    @ColumnInfo(name = "fs_authority")
    val fsAuthority: FSAuthority,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "file_uid")
    val fileUid: String,

    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "added_time")
    val addedTime: Long,

    @ColumnInfo(name = "last_access_time")
    val lastAccessTime: Long? = null,

    @ColumnInfo(name = "key_type")
    val keyType: KeyType,

    @ColumnInfo(name = "key_file_fs_authority")
    val keyFileFsAuthority: FSAuthority? = null,

    @ColumnInfo(name = "key_file_path")
    val keyFilePath: String? = null,

    @ColumnInfo(name = "key_file_uid")
    val keyFileUid: String? = null,

    @ColumnInfo(name = "key_file_name")
    val keyFileName: String? = null,

    @ColumnInfo(name = "biometric_data")
    val biometricData: BiometricData? = null
)