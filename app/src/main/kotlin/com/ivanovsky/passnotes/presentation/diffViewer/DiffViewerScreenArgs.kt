package com.ivanovsky.passnotes.presentation.diffViewer

import android.os.Parcelable
import com.ivanovsky.passnotes.presentation.diffViewer.model.DiffEntity
import kotlinx.parcelize.Parcelize

@Parcelize
data class DiffViewerScreenArgs(
    val left: DiffEntity,
    val right: DiffEntity,
    /**
     * If true, then database will not be locked
     */
    val isHoldDatabaseInteraction: Boolean
) : Parcelable