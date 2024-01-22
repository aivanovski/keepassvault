package com.ivanovsky.passnotes.presentation.diffViewer.model

import android.os.Parcelable
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey
import kotlinx.parcelize.Parcelize

sealed class DiffEntity : Parcelable {

    @Parcelize
    object OpenedDatabase : DiffEntity()

    @Parcelize
    object SelectFile : DiffEntity()

    @Parcelize
    data class File(
        val key: EncryptedDatabaseKey,
        val file: FileDescriptor
    ) : DiffEntity()
}