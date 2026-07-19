package com.ivanovsky.passnotes.data.repository.file.remote.exception

class RemoteFSFileNotFoundException(path: String?) :
    RemoteFSApiException(String.format("File not found %s", path))