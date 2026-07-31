package com.ivanovsky.passnotes.data.repository.file.remote.exception

class RemoteFSFileNotFoundException(path: String?) :
    RemoteFSApiException("File not found $path")