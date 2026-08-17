package com.ivanovsky.passnotes.data.entity

data class MergeFiles(
    val base: FileDescriptor,
    val local: FileDescriptor,
    val remote: FileDescriptor,
    val output: FileDescriptor
)