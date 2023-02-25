package com.ivanovsky.passnotes.domain.interactor.debugmenu

import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.GroupEntity
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_ACCESS_TO_PRIVATE_STORAGE
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_GET_DATABASE
import com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_UNKNOWN_ERROR
import com.ivanovsky.passnotes.data.entity.OperationError.newDbError
import com.ivanovsky.passnotes.data.entity.OperationError.newFileIsAlreadyExistsError
import com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.encdb.dao.GroupDao
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy
import com.ivanovsky.passnotes.data.repository.keepass.KeepassImplementation
import com.ivanovsky.passnotes.data.repository.keepass.PasswordKeepassKey
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.FileHelper
import com.ivanovsky.passnotes.domain.usecases.test.GetTestCredentialsUseCase
import com.ivanovsky.passnotes.extensions.toFileDescriptor
import com.ivanovsky.passnotes.util.InputOutputUtils
import com.ivanovsky.passnotes.util.InputOutputUtils.newFileInputStreamOrNull
import com.ivanovsky.passnotes.util.InputOutputUtils.newFileOutputStreamOrNull
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.withContext
import timber.log.Timber

class DebugMenuInteractor(
    private val fileSystemResolver: FileSystemResolver,
    private val dbRepository: EncryptedDatabaseRepository,
    private val fileHelper: FileHelper,
    private val getTestCredentialsUseCase: GetTestCredentialsUseCase,
    private val dispatchers: DispatcherProvider
) {

    fun getTestWebDavCredentials() = getTestCredentialsUseCase.getDebugWebDavCredentials()

    fun getTestGitCredentials() = getTestCredentialsUseCase.getDebugGitCredentials()

    fun getFileContent(file: FileDescriptor): OperationResult<Pair<FileDescriptor, File>> {
        val result = OperationResult<Pair<FileDescriptor, File>>()

        val provider = fileSystemResolver.resolveProvider(file.fsAuthority)

        val descriptorResult = provider.getFile(file.path, FSOptions.DEFAULT)

        if (descriptorResult.isSucceededOrDeferred) {
            val descriptor = descriptorResult.obj
            val contentResult =
                provider.openFileForRead(descriptor, OnConflictStrategy.REWRITE, FSOptions.DEFAULT)

            if (contentResult.isSucceededOrDeferred) {
                val destinationResult = createNewLocalDestinationStream()
                if (destinationResult.isSucceededOrDeferred) {
                    val content = contentResult.obj
                    val destinationFile = destinationResult.obj.first
                    val destinationStream = destinationResult.obj.second

                    try {
                        InputOutputUtils.copyOrThrow(content, destinationStream, true)
                        result.obj = Pair(descriptor, destinationFile)
                    } catch (e: Exception) {
                        Timber.d(e)
                        result.error = newGenericIOError(e.toString())
                    }
                } else {
                    result.error = destinationResult.error
                }
            } else {
                result.error = contentResult.error
            }
        } else {
            result.error = descriptorResult.error
        }

        return result
    }

    private fun createNewLocalDestinationStream(): OperationResult<Pair<File, OutputStream>> {
        val result = OperationResult<Pair<File, OutputStream>>()

        val outFile = fileHelper.generateDestinationFileOrNull()
        if (outFile != null) {
            val outStream = newFileOutputStreamOrNull(outFile)
            if (outStream != null) {
                result.obj = Pair(outFile, outStream)
            } else {
                result.error = newGenericIOError(MESSAGE_FAILED_TO_ACCESS_TO_PRIVATE_STORAGE)
            }
        } else {
            result.error = newGenericIOError(MESSAGE_FAILED_TO_ACCESS_TO_PRIVATE_STORAGE)
        }

        return result
    }

    fun newDbFile(
        type: KeepassImplementation,
        password: String,
        file: FileDescriptor
    ): OperationResult<Pair<FileDescriptor, File>> {
        val result = OperationResult<Pair<FileDescriptor, File>>()

        val anyFsProvider = fileSystemResolver.resolveProvider(file.fsAuthority)

        val existsResult = anyFsProvider.exists(file)
        if (existsResult.isSucceeded) {
            val exists = existsResult.obj

            if (!exists) {
                val creationResult = createNewDatabaseInPrivateStorage(type, password)

                if (creationResult.isSucceededOrDeferred) {
                    val openResult = anyFsProvider.openFileForWrite(
                        file,
                        OnConflictStrategy.CANCEL,
                        FSOptions.DEFAULT
                    )
                    if (openResult.isSucceededOrDeferred) {
                        val inputFile = creationResult.obj.first
                        val inputStream = creationResult.obj.second
                        val outStream = openResult.obj
                        val copyResult = copyStreamToStream(inputStream, outStream)

                        if (copyResult.isSucceededOrDeferred) {
                            val fileResult = anyFsProvider.getFile(file.path, FSOptions.DEFAULT)

                            if (fileResult.isSucceededOrDeferred) {
                                val outDescriptor = fileResult.obj

                                result.obj = Pair(outDescriptor, inputFile)
                            } else {
                                result.error = fileResult.error
                            }
                        } else {
                            result.error = copyResult.error
                        }
                    } else {
                        result.error = openResult.error
                    }
                } else {
                    result.error = creationResult.error
                }
            } else {
                result.error = newFileIsAlreadyExistsError()
            }
        } else {
            result.error = existsResult.error
        }

        return result
    }

    private fun createNewDatabaseInPrivateStorage(
        type: KeepassImplementation,
        password: String
    ): OperationResult<Pair<File, InputStream>> {
        val result = OperationResult<Pair<File, InputStream>>()

        val dbFile = fileHelper.generateDestinationFileOrNull()
        if (dbFile != null) {
            val dbDescriptor = dbFile.toFileDescriptor(
                fsAuthority = getFsAuthorityForFile(dbFile)
            )
            val key = PasswordKeepassKey(password)

            val creationResult = dbRepository.createNew(
                type,
                key,
                dbDescriptor,
                false
            )
            if (creationResult.isSucceededOrDeferred) {
                val dbStream = newFileInputStreamOrNull(dbFile)
                if (dbStream != null) {
                    result.obj = Pair(dbFile, dbStream)
                } else {
                    result.error = newGenericIOError(MESSAGE_FAILED_TO_ACCESS_TO_PRIVATE_STORAGE)
                }
            } else {
                result.error = creationResult.error
            }
        } else {
            result.error = newGenericIOError(MESSAGE_FAILED_TO_ACCESS_TO_PRIVATE_STORAGE)
        }

        return result
    }

    private fun copyStreamToStream(
        inStream: InputStream,
        outStream: OutputStream
    ): OperationResult<Boolean> {
        val result = OperationResult<Boolean>()

        try {
            InputOutputUtils.copyOrThrow(inStream, outStream, true)
            result.obj = true
        } catch (e: Exception) {
            Timber.d(e)
            result.error = newGenericIOError(e.toString())
        }

        return result
    }

    fun writeDbFile(
        inFile: File,
        outFile: FileDescriptor
    ): OperationResult<Pair<FileDescriptor, File>> {
        val result = OperationResult<Pair<FileDescriptor, File>>()

        val provider = fileSystemResolver.resolveProvider(outFile.fsAuthority)

        val openResult = provider.openFileForWrite(
            outFile,
            OnConflictStrategy.REWRITE,
            FSOptions.DEFAULT
        )

        if (openResult.isSucceededOrDeferred) {
            val outStream = openResult.obj
            val inStream = newFileInputStreamOrNull(inFile)

            if (inStream != null) {
                try {
                    InputOutputUtils.copyOrThrow(inStream, outStream, true)
                    result.obj = Pair(outFile, inFile)
                } catch (e: Exception) {
                    Timber.d(e)
                    result.error = newGenericIOError(e.toString())
                }
            } else {
                result.error = newGenericIOError(MESSAGE_FAILED_TO_ACCESS_TO_PRIVATE_STORAGE)
            }
        } else {
            result.error = openResult.error
        }

        return result
    }

    fun openDbFile(
        type: KeepassImplementation,
        password: String,
        file: File
    ): OperationResult<Boolean> {
        val result = OperationResult<Boolean>()

        val key = PasswordKeepassKey(password)

        val descriptor = file.toFileDescriptor(
            fsAuthority = getFsAuthorityForFile(file)
        )
        val openResult = dbRepository.open(
            type,
            key,
            descriptor,
            FSOptions.DEFAULT
        )
        if (openResult.isSucceededOrDeferred) {
            result.obj = true
        } else {
            result.error = openResult.error
        }

        return result
    }

    fun closeDbFile(file: File): OperationResult<Boolean> {
        val result = OperationResult<Boolean>()

        val closeResult = dbRepository.close()
        if (closeResult.isSucceededOrDeferred) {
            file.setLastModified(System.currentTimeMillis())
            result.obj = closeResult.obj
        } else {
            result.error = closeResult.error
        }

        return result
    }

    @Suppress("FoldInitializerAndIfToElvis")
    fun addEntryToDb(): OperationResult<Boolean> {
        val db = dbRepository.database
        if (db == null) {
            return OperationResult.error(newDbError(MESSAGE_FAILED_TO_GET_DATABASE))
        }

        val newGroupTitle = generateNewGroupTitle(db.groupDao)
        if (newGroupTitle == null) {
            return OperationResult.error(newDbError(MESSAGE_UNKNOWN_ERROR))
        }

        val rootGroupResult = db.groupDao.rootGroup
        if (rootGroupResult.isFailed) {
            return rootGroupResult.takeError()
        }

        val rootGroup = rootGroupResult.obj
        val rootGroupUid = rootGroup.uid
        val newGroup = GroupEntity(
            parentUid = rootGroupUid,
            title = newGroupTitle,
            autotypeEnabled = rootGroup.autotypeEnabled.inherit(),
            searchEnabled = rootGroup.searchEnabled.inherit()
        )

        val insertResult = db.groupDao.insert(newGroup)
        if (insertResult.isFailed) {
            return insertResult.takeError()
        }

        return insertResult.takeStatusWith(true)
    }

    suspend fun isFileExists(file: FileDescriptor): OperationResult<Boolean> {
        return withContext(dispatchers.IO) {
            fileSystemResolver
                .resolveProvider(file.fsAuthority)
                .exists(file)
        }
    }

    suspend fun getFileByPath(
        path: String,
        fsAuthority: FSAuthority
    ): OperationResult<FileDescriptor> =
        withContext(dispatchers.IO) {
            fileSystemResolver
                .resolveProvider(fsAuthority)
                .getFile(path, FSOptions.DEFAULT)
        }

    suspend fun getRootFile(fsAuthority: FSAuthority): OperationResult<FileDescriptor> =
        withContext(dispatchers.IO) {
            fileSystemResolver
                .resolveProvider(fsAuthority)
                .rootFile
        }

    private fun generateNewGroupTitle(groupDao: GroupDao): String? {
        var title: String? = null

        val groupsResult = groupDao.all
        if (groupsResult.isSucceededOrDeferred) {
            val indexesInGroups = extractIndexesFromGroups(groupsResult.obj)
            val nextGroupIndex = findNextAvailableIndex(indexesInGroups)
            title = "Group $nextGroupIndex"
        }

        return title
    }

    private fun extractIndexesFromGroups(groups: List<Group>): List<Int> {
        return groups.filter { group -> group.title != null && group.title.startsWith("Group ") }
            .mapNotNull { group -> extractIndexFromGroupTitle(group.title) }
            .toSet()
            .sorted()
    }

    private fun extractIndexFromGroupTitle(title: String): Int? {
        var index: Int? = null

        val spaceIdx = title.indexOf(" ")
        if (spaceIdx + 1 < title.length) {
            val titleIndexStr = title.substring(spaceIdx + 1, title.length)
            if (titleIndexStr.all { ch -> ch.isDigit() }) {
                index = Integer.parseInt(titleIndexStr)
            }
        }

        return index
    }

    private fun findNextAvailableIndex(indexes: List<Int>): Int {
        var result = 1

        for (idx in 0 until indexes.size) {
            if (indexes[idx] > result) {
                break
            } else {
                result = indexes[idx] + 1
            }
        }

        return result
    }

    private fun getFsAuthorityForFile(file: File): FSAuthority {
        return if (fileHelper.isLocatedInInternalStorage(file)) {
            FSAuthority.INTERNAL_FS_AUTHORITY
        } else {
            FSAuthority.EXTERNAL_FS_AUTHORITY
        }
    }
}