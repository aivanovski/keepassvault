package com.ivanovsky.passnotes.data.entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class FSAuthority(
    val credentials: ServerCredentials?,
    val type: FSType
) : Parcelable {

    // TODO: (refactor) move to extension function
    val isRequireCredentials: Boolean = (credentials == null)

    companion object {
        val REGULAR_FS_AUTHORITY = FSAuthority(null, FSType.REGULAR_FS)
        val DROPBOX_FS_AUTHORITY = FSAuthority(null, FSType.DROPBOX)
        val SAF_FS_AUTHORITY = FSAuthority(null, FSType.SAF)
    }
}