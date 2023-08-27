package com.ivanovsky.passnotes.data.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FSAuthority(
    val credentials: FSCredentials?,
    val type: FSType,
    val isBrowsable: Boolean
) : Parcelable {

    // TODO: (refactor) move to extension function
    val isRequireCredentials: Boolean = (credentials == null)

    companion object {
        val INTERNAL_FS_AUTHORITY = FSAuthority(
            credentials = null,
            type = FSType.INTERNAL_STORAGE,
            isBrowsable = true
        )

        val EXTERNAL_FS_AUTHORITY = FSAuthority(
            credentials = null,
            type = FSType.EXTERNAL_STORAGE,
            isBrowsable = true
        )

        val SAF_FS_AUTHORITY = FSAuthority(
            credentials = null,
            type = FSType.SAF,
            isBrowsable = false
        )
    }
}