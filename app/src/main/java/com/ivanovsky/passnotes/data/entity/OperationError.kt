package com.ivanovsky.passnotes.data.entity

import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace
import java.io.Serializable

class OperationError private constructor(
    val type: Type,
    val message: String?,
    val throwable: Throwable?
) : Serializable {

    enum class Type {
        GENERIC_ERROR,
        DB_AUTH_ERROR,
        DB_ERROR,
        DB_VERSION_CONFLICT_ERROR,
        FILE_ACCESS_ERROR,
        FILE_PERMISSION_ERROR,
        FILE_NOT_FOUND_ERROR,
        GENERIC_IO_ERROR,
        AUTH_ERROR,
        NETWORK_IO_ERROR,
        FILE_ALREADY_EXISTS,
        CACHE_ERROR,
        REMOTE_API_ERROR,
        ERROR_MESSAGE,
        BIOMETRIC_DATA_INVALIDATED_ERROR
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) return false

        other as OperationError
        return type == other.type && message == other.message && throwable == other.throwable
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + throwable.hashCode()
        return result
    }

    companion object {
        const val MESSAGE_FAILED_TO_FIND_GROUP = "Failed to find group"
        const val MESSAGE_FAILED_TO_FIND_NOTE = "Failed to find note"
        const val MESSAGE_UNKNOWN_ERROR = "Unknown error"
        const val MESSAGE_FILE_ACCESS_IS_FORBIDDEN = "File access is forbidden"
        const val MESSAGE_FILE_IS_NOT_A_DIRECTORY = "File is not a directory"
        const val MESSAGE_INCORRECT_FILE_SYSTEM_CREDENTIALS = "Incorrect file system credentials"
        const val MESSAGE_IO_ERROR = "IO error"
        const val MESSAGE_RECORD_IS_ALREADY_EXISTS = "Record is already exists"
        const val MESSAGE_FAILED_TO_OPEN_DB_FILE = "Failed to open DB file"
        const val MESSAGE_FAILED_TO_DECODE_DB_FILE = "Failed to decode DB file"
        const val MESSAGE_LOCAL_VERSION_CONFLICTS_WITH_REMOTE =
            "Local version conflicts with remote"
        const val MESSAGE_FAILED_TO_FIND_FILE = "Failed to find file"
        const val MESSAGE_FAILED_TO_ACCESS_TO_PRIVATE_STORAGE =
            "Failed to access to private storage"
        const val MESSAGE_FAILED_TO_ACCESS_TO_FILE = "Failed to access to file"
        const val MESSAGE_FAILED_TO_GET_DATABASE = "Failed to get database"
        const val MESSAGE_DEFERRED_OPERATIONS_ARE_NOT_SUPPORTED =
            "Deferred operations are not supported"
        const val MESSAGE_FAILED_TO_FIND_CACHED_FILE = "Failed to find cached file"
        const val MESSAGE_FAILED_TO_FIND_ROOT_GROUP = "Failed to find root group"
        const val MESSAGE_UID_IS_NULL = "Uid is null"
        const val MESSAGE_PARENT_UID_IS_NULL = "Parent uid is null"
        const val MESSAGE_FAILED_TO_GET_PARENT_PATH = "Failed to get parent path"
        const val MESSAGE_FILE_IS_NOT_MODIFIED = "File is not modified"
        const val MESSAGE_INCORRECT_SYNC_STATUS = "Incorrect sync status"
        const val MESSAGE_INCORRECT_USE_CASE = "Incorrect use case"
        const val MESSAGE_WRITE_OPERATION_IS_NOT_SUPPORTED = "Write operation is not supported"
        const val MESSAGE_FAILED_TO_MOVE_GROUP_INSIDE_ITS_OWN_TREE =
            "Failed to move group inside its owdn tree"
        const val MESSAGE_FAILED_TO_REMOVE_FILE = "Failed to remove file"
        const val MESSAGE_FAILED_TO_READ_KEY_FILE = "Failed to read key file"
        const val MESSAGE_FAILED_TO_ENCODE_DATA = "Failed to encode data"
        const val MESSAGE_FAILED_TO_DECODE_DATA = "Failed to decode data"
        const val MESSAGE_INVALID_PASSWORD = "Invalid password"
        const val MESSAGE_INVALID_KEY_FILE = "Invalid key file"
        const val MESSAGE_FAILED_TO_CREATE_A_DIRECTORY = "Failed to create a directory"
        const val MESSAGE_SYNCHRONIZATION_TAKES_TOO_LONG = "Synchronization takes too long"
        const val MESSAGE_UNSUPPORTED_DATABASE_TYPE = "Unsupported database type"
        const val MESSAGE_FAILED_TO_PARSE_UUID = "Failed to parse UUID"

        const val GENERIC_MESSAGE_NOT_FOUND = "%s not found"
        const val GENERIC_MESSAGE_GROUP_IS_ALREADY_EXIST = "Group '%s' already exists"
        const val GENERIC_MESSAGE_FAILED_TO_RETRIEVE_DATA_BY_URI =
            "Failed to retrive data by uri: %s"
        const val GENERIC_MESSAGE_FAILED_TO_FIND_COLUMN = "Failed to find column: %s"
        const val GENERIC_MESSAGE_FAILED_TO_GET_ACCESS_RIGHT_TO_URI =
            "Failed to get access to: %s"
        const val GENERIC_MESSAGE_FAILED_TO_FIND_FILE = "Failed to find file: %s"
        const val GENERIC_MESSAGE_FAILED_TO_FIND_ENTITY_BY_UID =
            "Failed to find '%s' in db: uid=%s"
        const val GENERIC_MESSAGE_FAILED_TO_FIND_ENTITY_BY_ID =
            "Failed to find '%s' in db: id=%s"
        const val GENERIC_MESSAGE_FAILED_TO_GET_REFERENCE_TO = "Failed to get reference to: %s"
        const val GENERIC_MESSAGE_FAILED_TO_GET_PARENT_FOR = "Failed to get parent for: %s"
        const val GENERIC_MESSAGE_FILE_IS_NOT_A_DIRECTORY = "File is not a directory: %s"
        const val GENERIC_INVALID_DATABASE_ENTRY = "Invalid db entry: %s"
        const val GENERIC_FILE_ALREADY_EXISTS = "File with identical name already exists: %s"

        @JvmStatic
        fun newDbError(message: String?, stacktrace: Stacktrace?) =
            OperationError(Type.DB_ERROR, message, stacktrace)

        @JvmStatic
        fun newDbError(message: String?, exception: Exception?) =
            OperationError(Type.DB_ERROR, message, exception)

        @JvmStatic
        fun newDbVersionConflictError(message: String?, stacktrace: Stacktrace?) =
            OperationError(Type.DB_VERSION_CONFLICT_ERROR, message, stacktrace)

        @JvmStatic
        fun newGenericError(message: String?, stacktrace: Stacktrace?) =
            OperationError(Type.GENERIC_ERROR, message, stacktrace)

        @JvmStatic
        fun newGenericError(cause: Throwable?) = OperationError(Type.GENERIC_ERROR, null, cause)

        @JvmStatic
        fun newGenericError(message: String?, throwable: Throwable?) =
            OperationError(Type.GENERIC_ERROR, message, throwable)

        @JvmStatic
        fun newFileAccessError(message: String?, throwable: Throwable?) =
            OperationError(Type.FILE_ACCESS_ERROR, message, throwable)

        @JvmStatic
        fun newFileAccessError(message: String?, stacktrace: Stacktrace?) =
            OperationError(Type.FILE_ACCESS_ERROR, message, stacktrace)

        @JvmStatic
        fun newFileNotFoundError(exception: Exception?) =
            OperationError(Type.FILE_NOT_FOUND_ERROR, null, exception)

        @JvmStatic
        fun newFileNotFoundError(stacktrace: Stacktrace?) =
            OperationError(Type.FILE_NOT_FOUND_ERROR, null, stacktrace)

        @JvmStatic
        fun newFileNotFoundError(message: String?, stacktrace: Stacktrace?) =
            OperationError(Type.FILE_NOT_FOUND_ERROR, message, stacktrace)

        @JvmStatic
        fun newGenericIOError(message: String?, stacktrace: Stacktrace?) =
            OperationError(Type.GENERIC_IO_ERROR, message, stacktrace)

        @JvmStatic
        fun newGenericIOError(message: String?, throwable: Throwable?) =
            OperationError(Type.GENERIC_IO_ERROR, message, throwable)

        @JvmStatic
        fun newGenericIOError(throwable: Throwable?) =
            OperationError(Type.GENERIC_IO_ERROR, null, throwable)

        @JvmStatic
        fun newAuthError(message: String?, exception: Exception?) =
            OperationError(Type.AUTH_ERROR, message, exception)

        @JvmStatic
        fun newAuthError(exception: Exception?) = OperationError(Type.AUTH_ERROR, null, exception)

        @JvmStatic
        fun newAuthError(stacktrace: Stacktrace?) = OperationError(
            Type.AUTH_ERROR,
            null,
            stacktrace
        )

        @JvmStatic
        fun newNetworkIOError(stacktrace: Stacktrace?) =
            OperationError(Type.NETWORK_IO_ERROR, null, stacktrace)

        @JvmStatic
        fun newNetworkIOError(cause: Exception?) =
            OperationError(Type.NETWORK_IO_ERROR, null, cause)

        @JvmStatic
        fun newFileAlreadyExistsError(message: String?, stacktrace: Stacktrace?) =
            OperationError(Type.FILE_ALREADY_EXISTS, message, stacktrace)

        @JvmStatic
        fun newFileAlreadyExistsError(stacktrace: Stacktrace?) =
            OperationError(Type.FILE_ALREADY_EXISTS, null, stacktrace)

        @JvmStatic
        fun newCacheError(message: String?, stacktrace: Stacktrace?) =
            OperationError(Type.CACHE_ERROR, message, stacktrace)

        @JvmStatic
        fun newRemoteApiError(message: String?, stacktrace: Stacktrace?) =
            OperationError(Type.REMOTE_API_ERROR, message, stacktrace)

        @JvmStatic
        fun newRemoteApiError(message: String?, throwable: Throwable?) =
            OperationError(Type.REMOTE_API_ERROR, message, throwable)

        @JvmStatic
        fun newPermissionError(message: String?, stacktrace: Stacktrace?) =
            OperationError(Type.FILE_PERMISSION_ERROR, message, stacktrace)

        @JvmStatic
        fun newErrorMessage(message: String?, stacktrace: Stacktrace?) =
            OperationError(Type.ERROR_MESSAGE, message, stacktrace)

        @JvmStatic
        fun newBiometricDataError(stacktrace: Stacktrace?) =
            OperationError(Type.BIOMETRIC_DATA_INVALIDATED_ERROR, null, stacktrace)

        @JvmStatic
        fun newBiometricDataError(cause: Exception?) =
            OperationError(Type.BIOMETRIC_DATA_INVALIDATED_ERROR, null, cause)
    }
}