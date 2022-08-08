package com.ivanovsky.passnotes.data.repository.file.git.model

import com.ivanovsky.passnotes.util.FileUtils
import java.io.File

data class VersionedFile(
    val localPath: String
) {
    val name = FileUtils.getFileNameFromPath(localPath)

    fun toFile(): File = File(localPath)
}