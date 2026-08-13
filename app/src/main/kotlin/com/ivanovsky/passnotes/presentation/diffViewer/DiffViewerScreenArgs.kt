package com.ivanovsky.passnotes.presentation.diffViewer

import android.os.Parcelable
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey
import com.ivanovsky.passnotes.presentation.diffViewer.model.DiffEntity
import kotlinx.parcelize.Parcelize

@Parcelize
data class DiffViewerScreenArgs(
    val mode: DiffViewerMode,
    /**
     * If true, then database should not be locked
     */
    val isHoldDatabaseInteraction: Boolean
) : Parcelable

sealed interface DiffViewerMode : Parcelable {

    @Parcelize
    data class Compare(
        val left: DiffEntity,
        val right: DiffEntity
    ) : DiffViewerMode

    @Parcelize
    data class Merge(
        val key: EncryptedDatabaseKey,
        val base: FileDescriptor,
        val local: FileDescriptor,
        val remote: FileDescriptor,
        val output: FileDescriptor
    ) : DiffViewerMode
}