package com.ivanovsky.passnotes.domain.interactor.debugmenu

import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.data.entity.OperationError.*
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.GroupRepository
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey
import com.ivanovsky.passnotes.domain.FileHelper
import com.ivanovsky.passnotes.domain.interactor.server_login.GetDebugCredentialsUseCase
import com.ivanovsky.passnotes.util.InputOutputUtils
import com.ivanovsky.passnotes.util.InputOutputUtils.newFileInputStreamOrNull
import com.ivanovsky.passnotes.util.InputOutputUtils.newFileOutputStreamOrNull
import com.ivanovsky.passnotes.util.Logger
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class DebugMenuInteractor(
    private val fileSystemResolver: FileSystemResolver,
    private val dbRepository: EncryptedDatabaseRepository,
    private val fileHelper: FileHelper,
    private val getDebugCredentialsUseCase: GetDebugCredentialsUseCase
) {

    fun getDebugWebDavCredentials() = getDebugCredentialsUseCase.getDebugWebDavCredentials()

    fun getFileContent(file: FileDescriptor): OperationResult<Pair<FileDescriptor, File>> {
        val result = OperationResult<Pair<FileDescriptor, File>>()

        val provider = fileSystemResolver.resolveProvider(file.fsAuthority)

        val descriptorResult = provider.getFile(file.path, true)

        if (descriptorResult.isSucceededOrDeferred) {
            val descriptor = descriptorResult.obj
            val contentResult =
                provider.openFileForRead(descriptor, OnConflictStrategy.REWRITE, true)

            if (contentResult.isSucceededOrDeferred) {
                val destinationResult = createNewLocalDestinationStream()
                if (destinationResult.isSucceededOrDeferred) {
                    val content = contentResult.obj
                    val destinationFile = destinationResult.obj.first
                    val destinationStream = destinationResult.obj.second

                    try {
                        InputOutputUtils.copy(content, destinationStream, true)
                        result.obj = Pair(descriptor, destinationFile)
                    } catch (e: Exception) {
                        Logger.printStackTrace(e)
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

        val outFile = fileHelper.generateDestinationFileForRemoteFile()
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
        password: String,
        file: FileDescriptor
    ): OperationResult<Pair<FileDescriptor, File>> {
        val result = OperationResult<Pair<FileDescriptor, File>>()

        val anyFsProvider = fileSystemResolver.resolveProvider(file.fsAuthority)

        val existsResult = anyFsProvider.exists(file)
        if (existsResult.isSucceeded) {
            val exists = existsResult.obj

            if (!exists) {
                val creationResult = createNewDatabaseInPrivateStorage(password)

                if (creationResult.isSucceededOrDeferred) {
                    val openResult = anyFsProvider.openFileForWrite(
                        file,
                        OnConflictStrategy.CANCEL,
                        true
                    )
                    if (openResult.isSucceededOrDeferred) {
                        val inputFile = creationResult.obj.first
                        val inputStream = creationResult.obj.second
                        val outStream = openResult.obj
                        val copyResult = copyStreamToStream(inputStream, outStream)

                        if (copyResult.isSucceededOrDeferred) {
                            val fileResult = anyFsProvider.getFile(file.path, true)

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

    private fun createNewDatabaseInPrivateStorage(password: String): OperationResult<Pair<File, InputStream>> {
        val result = OperationResult<Pair<File, InputStream>>()

        val dbFile = fileHelper.generateDestinationFileForRemoteFile()
        if (dbFile != null) {
            val dbDescriptor = FileDescriptor.fromRegularFile(dbFile)
            val key = KeepassDatabaseKey(password)

            val creationResult = dbRepository.createNew(key, dbDescriptor)
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
            InputOutputUtils.copy(inStream, outStream, true)
            result.obj = true
        } catch (e: Exception) {
            Logger.printStackTrace(e)
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
            true
        )

        if (openResult.isSucceededOrDeferred) {
            val outStream = openResult.obj
            val inStream = newFileInputStreamOrNull(inFile)

            if (inStream != null) {
                try {
                    InputOutputUtils.copy(inStream, outStream, true)
                    result.obj = Pair(outFile, inFile)
                } catch (e: Exception) {
                    Logger.printStackTrace(e)
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

    fun openDbFile(password: String, file: File): OperationResult<Boolean> {
        val result = OperationResult<Boolean>()

        val key = KeepassDatabaseKey(password)

        val openResult = dbRepository.open(key, FileDescriptor.fromRegularFile(file))
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
            return OperationResult.error(newDbError(MESSAGE_DB_IS_NOT_OPENED))
        }

        val newGroupTitle = generateNewGroupTitle(db.groupRepository)
        if (newGroupTitle == null) {
            return OperationResult.error(newDbError(MESSAGE_UNKNOWN_ERROR))
        }

        val rootGroupResult = db.groupRepository.rootGroup
        if (rootGroupResult.isFailed) {
            return rootGroupResult.takeError()
        }

        val rootGroupUid = rootGroupResult.obj.uid
        val newGroup = Group()

        newGroup.title = newGroupTitle

        val insertResult = db.groupRepository.insert(newGroup, rootGroupUid)
        if (insertResult.isFailed) {
            return insertResult.takeError()
        }

        return insertResult.takeStatusWith(true)
    }

    private fun generateNewGroupTitle(groupRepository: GroupRepository): String? {
        var title: String? = null

        val groupsResult = groupRepository.allGroup
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
}