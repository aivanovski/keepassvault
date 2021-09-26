package com.ivanovsky.passnotes.data.entity

import android.os.Parcelable
import com.ivanovsky.passnotes.util.FileUtils
import com.ivanovsky.passnotes.util.FileUtils.ROOT_PATH
import java.io.File
import kotlinx.android.parcel.Parcelize

@Parcelize
data class FileDescriptor(
    val fsAuthority: FSAuthority,
    val path: String,
    val uid: String, // TODO: it can be null at some situations
    val name: String,
    val isDirectory: Boolean,
    val isRoot: Boolean,
    val modified: Long? = null
) : Parcelable {

    companion object {

        @JvmStatic
        fun fromRegularFile(file: File): FileDescriptor {
            return FileDescriptor(
                fsAuthority = FSAuthority.REGULAR_FS_AUTHORITY,
                path = file.path,
                uid = file.path,
                name = FileUtils.getFileNameFromPath(file.path),
                isDirectory = file.isDirectory,
                isRoot = (file.path == ROOT_PATH),
                modified = file.lastModified()
            )
        }
    }
}