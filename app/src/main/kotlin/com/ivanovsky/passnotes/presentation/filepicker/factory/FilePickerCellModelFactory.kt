package com.ivanovsky.passnotes.presentation.filepicker.factory

import androidx.annotation.DrawableRes
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.presentation.core.model.BaseCellModel
import com.ivanovsky.passnotes.presentation.core.model.FileCellModel
import com.ivanovsky.passnotes.util.StringUtils
import java.text.DateFormat
import java.util.Date

class FilePickerCellModelFactory {

    fun createCellModels(
        files: List<FileDescriptor>,
        parentDir: FileDescriptor,
        dateFormat: DateFormat
    ): List<BaseCellModel> {
        return files.map { file ->
            val title = if (file == parentDir) {
                ".."
            } else {
                formatTitle(file)
            }

            FileCellModel(
                id = file.path,
                iconResId = getIconResId(file),
                title = title,
                description = formatModifiedDate(file, dateFormat),
                isSelected = false
            )
        }
    }

    @DrawableRes
    private fun getIconResId(file: FileDescriptor): Int {
        return if (file.isDirectory) {
            R.drawable.ic_folder_white_24dp
        } else {
            R.drawable.ic_file_white_24dp
        }
    }

    private fun formatTitle(file: FileDescriptor): String {
        return if (file.isDirectory) {
            file.name + "/"
        } else {
            file.name
        }
    }

    private fun formatModifiedDate(file: FileDescriptor, dateFormat: DateFormat): String {
        return if (file.modified != null) {
            dateFormat.format(Date(file.modified))
        } else {
            StringUtils.EMPTY
        }
    }
}