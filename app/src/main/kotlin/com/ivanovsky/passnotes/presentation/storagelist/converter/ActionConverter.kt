package com.ivanovsky.passnotes.presentation.storagelist.converter

import com.ivanovsky.passnotes.presentation.storagelist.Action

fun Action.toFilePickerAction(): com.ivanovsky.passnotes.presentation.filepicker.Action {
    return when (this) {
        Action.PICK_FILE -> com.ivanovsky.passnotes.presentation.filepicker.Action.PICK_FILE
        Action.PICK_STORAGE -> com.ivanovsky.passnotes.presentation.filepicker.Action.PICK_DIRECTORY
    }
}