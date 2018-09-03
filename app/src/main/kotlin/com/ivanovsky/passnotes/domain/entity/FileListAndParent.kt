package com.ivanovsky.passnotes.domain.entity

import com.ivanovsky.passnotes.data.entity.FileDescriptor

class FileListAndParent(val files: List<FileDescriptor>, val parent: FileDescriptor?)