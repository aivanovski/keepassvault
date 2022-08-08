package com.ivanovsky.passnotes.data.entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class FSAuthority(
    val credentials: FSCredentials?,
    val type: FSType
) : Parcelable {

    // TODO: (refactor) move to extension function
    val isRequireCredentials: Boolean = (credentials == null)

    companion object {
        val INTERNAL_FS_AUTHORITY = FSAuthority(null, FSType.INTERNAL_STORAGE)
        val EXTERNAL_FS_AUTHORITY = FSAuthority(null, FSType.EXTERNAL_STORAGE)
        val DROPBOX_FS_AUTHORITY = FSAuthority(null, FSType.DROPBOX)
        val SAF_FS_AUTHORITY = FSAuthority(null, FSType.SAF)
    }
}