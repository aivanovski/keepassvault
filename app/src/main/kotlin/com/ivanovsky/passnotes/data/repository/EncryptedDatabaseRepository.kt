package com.ivanovsky.passnotes.data.repository

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.keepass.KeepassImplementation

interface EncryptedDatabaseRepository {

    fun isOpened(): Boolean

    fun getDatabase(): EncryptedDatabase?

    /** Reads a database from [file] and saves it as the currently opened database. */
    fun open(
        type: KeepassImplementation,
        key: EncryptedDatabaseKey,
        file: FileDescriptor,
        options: FSOptions
    ): OperationResult<EncryptedDatabase>

    fun createNew(
        type: KeepassImplementation,
        key: EncryptedDatabaseKey,
        file: FileDescriptor,
        addTemplates: Boolean
    ): OperationResult<Boolean>

    fun reload(): OperationResult<Boolean>

    fun close(): OperationResult<Boolean>

    /** Reads and returns a database from [file] without making it the currently opened one. */
    fun read(
        type: KeepassImplementation,
        key: EncryptedDatabaseKey,
        file: FileDescriptor,
        options: FSOptions
    ): OperationResult<EncryptedDatabase>
}