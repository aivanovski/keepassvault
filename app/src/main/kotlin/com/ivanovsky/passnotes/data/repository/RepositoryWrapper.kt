package com.ivanovsky.passnotes.data.repository

import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase

interface RepositoryWrapper {
    fun onDatabaseOpened(db: EncryptedDatabase)
    fun onDatabaseClosed()
}