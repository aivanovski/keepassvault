package com.ivanovsky.passnotes.presentation.filepicker

import androidx.annotation.DrawableRes
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import java.text.DateFormat
import java.util.*

class ViewItemMapper(
    private val dateFormat: DateFormat
) {

    fun map(file: FileDescriptor): FilePickerAdapter.Item {
        return FilePickerAdapter.Item(
            getIconResId(file.isDirectory),
            formatItemTitle(file),
            formatModifiedDate(file.modified),
            false
        )
    }

    fun map(files: List<FileDescriptor>): List<FilePickerAdapter.Item> {
        return files.map { file -> map(file) }
    }

    @DrawableRes
    private fun getIconResId(isDirectory: Boolean): Int {
        return if (isDirectory) R.drawable.ic_folder_white_24dp else R.drawable.ic_file_white_24dp
    }

    private fun formatItemTitle(file: FileDescriptor): String {
        return if (file.isDirectory) file.name + "/" else file.name
    }

    private fun formatModifiedDate(modified: Long?): String {
        return if (modified != null) {
            dateFormat.format(Date(modified))
        } else {
            ""
        }
    }
}