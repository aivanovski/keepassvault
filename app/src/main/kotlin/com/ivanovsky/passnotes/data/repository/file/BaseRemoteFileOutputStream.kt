package com.ivanovsky.passnotes.data.repository.file

import java.io.File
import java.io.OutputStream

abstract class BaseRemoteFileOutputStream : OutputStream() {
    abstract val outputFile: File
}