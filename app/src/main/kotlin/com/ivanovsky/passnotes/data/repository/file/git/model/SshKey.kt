package com.ivanovsky.passnotes.data.repository.file.git.model

data class SshKey(
    val keyPath: String,
    val password: String?
)