package com.ivanovsky.passnotes.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;

public class OperationError implements Serializable {

    public static final String MESSAGE_FAILED_TO_FIND_GROUP = "Failed to find group";
    public static final String MESSAGE_FAILED_TO_FIND_NOTE = "Failed to find note";
    public static final String MESSAGE_UNKNOWN_ERROR = "Unknown error";
    public static final String MESSAGE_FILE_ACCESS_IS_FORBIDDEN = "File access is forbidden";
    public static final String MESSAGE_FILE_IS_NOT_A_DIRECTORY = "File is not a directory";
    public static final String MESSAGE_INCORRECT_FILE_SYSTEM_CREDENTIALS =
            "Incorrect file system credentials";
    public static final String MESSAGE_IO_ERROR = "IO error";
    public static final String MESSAGE_RECORD_IS_ALREADY_EXISTS = "Record is already exists";
    public static final String MESSAGE_FAILED_TO_OPEN_DB_FILE = "Failed to open DB file";
    public static final String MESSAGE_LOCAL_VERSION_CONFLICTS_WITH_REMOTE =
            "Local version conflicts with remote";
    public static final String MESSAGE_FAILED_TO_FIND_FILE = "Failed to find file";
    public static final String MESSAGE_FAILED_TO_ACCESS_TO_PRIVATE_STORAGE =
            "Failed to access to private storage";
    public static final String MESSAGE_FAILED_TO_ACCESS_TO_FILE = "Failed to access to file";
    public static final String MESSAGE_FAILED_TO_GET_DATABASE = "Failed to get database";
    public static final String MESSAGE_DEFERRED_OPERATIONS_ARE_NOT_SUPPORTED =
            "Deferred operations are not supported";
    public static final String MESSAGE_FAILED_TO_FIND_CACHED_FILE = "Failed to find cached file";
    public static final String MESSAGE_FAILED_TO_FIND_ROOT_GROUP = "Failed to find root group";
    public static final String MESSAGE_UID_IS_NULL = "Uid is null";
    public static final String MESSAGE_PARENT_UID_IS_NULL = "Parent uid is null";
    public static final String MESSAGE_FAILED_TO_GET_PARENT_PATH = "Failed to get parent path";
    public static final String MESSAGE_FILE_IS_NOT_MODIFIED = "File is not modified";
    public static final String MESSAGE_INCORRECT_SYNC_STATUS = "Incorrect sync status";
    public static final String MESSAGE_INCORRECT_USE_CASE = "Incorrect use case";
    public static final String MESSAGE_WRITE_OPERATION_IS_NOT_SUPPORTED =
            "Write operation is not supported";
    public static final String MESSAGE_FAILED_TO_MOVE_GROUP_INSIDE_ITS_OWN_TREE =
            "Failed to move group inside its owdn tree";
    public static final String MESSAGE_FAILED_TO_REMOVE_FILE = "Failed to remove file";
    public static final String MESSAGE_FAILED_TO_READ_KEY_FILE = "Failed to read key file";
    public static final String MESSAGE_FAILED_TO_ENCODE_DATA = "Failed to encode data";
    public static final String MESSAGE_FAILED_TO_DECODE_DATA = "Failed to decode data";
    public static final String MESSAGE_INVALID_PASSWORD = "Invalid password";
    public static final String MESSAGE_INVALID_KEY_FILE = "Invalid key file";
    public static final String MESSAGE_FAILED_TO_CREATE_A_DIRECTORY =
            "Failed to create a directory";
    public static final String MESSAGE_SYNCHRONIZATION_TAKES_TOO_LONG =
            "Synchronization takes too long";
    public static final String MESSAGE_UNSUPPORTED_DATABASE_TYPE = "Unsupported database type";

    public static final String GENERIC_MESSAGE_NOT_FOUND = "%s not found";
    public static final String GENERIC_MESSAGE_GROUP_IS_ALREADY_EXIST = "Group '%s' already exists";
    public static final String GENERIC_MESSAGE_FAILED_TO_RETRIEVE_DATA_BY_URI =
            "Failed to retrive data by uri: %s";
    public static final String GENERIC_MESSAGE_FAILED_TO_FIND_COLUMN = "Failed to find column: %s";
    public static final String GENERIC_MESSAGE_FAILED_TO_GET_ACCESS_RIGHT_TO_URI =
            "Failed to get access to: %s";
    public static final String GENERIC_MESSAGE_FAILED_TO_FIND_FILE = "Failed to find file: %s";
    public static final String GENERIC_MESSAGE_FAILED_TO_FIND_ENTITY_BY_UID =
            "Failed to find '%s' in db: uid=%s";
    public static final String GENERIC_MESSAGE_FAILED_TO_FIND_ENTITY_BY_ID =
            "Failed to find '%s' in db: id=%s";
    public static final String GENERIC_MESSAGE_FAILED_TO_GET_REFERENCE_TO =
            "Failed to get reference to: %s";
    public static final String GENERIC_MESSAGE_FAILED_TO_GET_PARENT_FOR =
            "Failed to get parent for: %s";
    public static final String GENERIC_MESSAGE_FILE_IS_NOT_A_DIRECTORY =
            "File is not a directory: %s";
    public static final String GENERIC_INVALID_DATABASE_ENTRY = "Invalid db entry: %s";
    public static final String GENERIC_FILE_ALREADY_EXISTS =
            "File with identical name already exists: %s";

    @NonNull
    private final Type type;

    @Nullable
    private final String message;

    @Nullable
    private final Throwable throwable;

    public static OperationError newDbError(String message, Stacktrace stacktrace) {
        return new OperationError(Type.DB_ERROR, message, stacktrace);
    }

    public static OperationError newDbError(String message, Exception exception) {
        return new OperationError(Type.DB_ERROR, message, exception);
    }

    public static OperationError newDbVersionConflictError(String message, Stacktrace stacktrace) {
        return new OperationError(Type.DB_VERSION_CONFLICT_ERROR, message, stacktrace);
    }

    public static OperationError newGenericError(String message, Stacktrace stacktrace) {
        return new OperationError(Type.GENERIC_ERROR, message, stacktrace);
    }

    public static OperationError newGenericError(Throwable cause) {
        return new OperationError(Type.GENERIC_ERROR, null, cause);
    }

    public static OperationError newGenericError(String message, Throwable throwable) {
        return new OperationError(Type.GENERIC_ERROR, message, throwable);
    }

    public static OperationError newFileAccessError(String message, Throwable throwable) {
        return new OperationError(Type.FILE_ACCESS_ERROR, message, throwable);
    }

    public static OperationError newFileAccessError(String message, Stacktrace stacktrace) {
        return new OperationError(Type.FILE_ACCESS_ERROR, message, stacktrace);
    }

    public static OperationError newFileNotFoundError(Exception exception) {
        return new OperationError(Type.FILE_NOT_FOUND_ERROR, null, exception);
    }

    public static OperationError newFileNotFoundError(Stacktrace stacktrace) {
        return new OperationError(Type.FILE_NOT_FOUND_ERROR, null, stacktrace);
    }

    public static OperationError newFileNotFoundError(String message, Stacktrace stacktrace) {
        return new OperationError(Type.FILE_NOT_FOUND_ERROR, message, stacktrace);
    }

    public static OperationError newGenericIOError(String message, Stacktrace stacktrace) {
        return new OperationError(Type.GENERIC_IO_ERROR, message, stacktrace);
    }

    public static OperationError newGenericIOError(String message, Throwable throwable) {
        return new OperationError(Type.GENERIC_IO_ERROR, message, throwable);
    }

    public static OperationError newGenericIOError(Throwable throwable) {
        return new OperationError(Type.GENERIC_IO_ERROR, null, throwable);
    }

    public static OperationError newAuthError(String message, Exception exception) {
        return new OperationError(Type.AUTH_ERROR, message, exception);
    }

    public static OperationError newAuthError(Exception exception) {
        return new OperationError(Type.AUTH_ERROR, null, exception);
    }

    public static OperationError newAuthError(Stacktrace stacktrace) {
        return new OperationError(Type.AUTH_ERROR, null, stacktrace);
    }

    public static OperationError newNetworkIOError(Stacktrace stacktrace) {
        return new OperationError(Type.NETWORK_IO_ERROR, null, stacktrace);
    }

    public static OperationError newNetworkIOError(Exception cause) {
        return new OperationError(Type.NETWORK_IO_ERROR, null, cause);
    }

    public static OperationError newFileAlreadyExistsError(String message, Stacktrace stacktrace) {
        return new OperationError(Type.FILE_IS_ALREADY_EXISTS, message, stacktrace);
    }

    public static OperationError newFileAlreadyExistsError(Stacktrace stacktrace) {
        return new OperationError(Type.FILE_IS_ALREADY_EXISTS, null, stacktrace);
    }

    public static OperationError newCacheError(String message, Stacktrace stacktrace) {
        return new OperationError(Type.CACHE_ERROR, message, stacktrace);
    }

    public static OperationError newRemoteApiError(String message, Stacktrace stacktrace) {
        return new OperationError(Type.REMOTE_API_ERROR, message, stacktrace);
    }

    public static OperationError newRemoteApiError(String message, Throwable throwable) {
        return new OperationError(Type.REMOTE_API_ERROR, message, throwable);
    }

    public static OperationError newPermissionError(String message, Stacktrace stacktrace) {
        return new OperationError(Type.FILE_PERMISSION_ERROR, message, stacktrace);
    }

    public static OperationError newErrorMessage(String message, Stacktrace stacktrace) {
        return new OperationError(Type.ERROR_MESSAGE, message, stacktrace);
    }

    public static OperationError newBiometricDataError(Stacktrace stacktrace) {
        return new OperationError(Type.BIOMETRIC_DATA_INVALIDATED_ERROR, null, stacktrace);
    }

    public static OperationError newBiometricDataError(Exception cause) {
        return new OperationError(Type.BIOMETRIC_DATA_INVALIDATED_ERROR, null, cause);
    }

    public enum Type {
        GENERIC_ERROR,
        DB_AUTH_ERROR,
        DB_ERROR,
        DB_VERSION_CONFLICT_ERROR, // if user modified db
        FILE_ACCESS_ERROR,
        FILE_PERMISSION_ERROR,
        FILE_NOT_FOUND_ERROR,
        GENERIC_IO_ERROR,
        AUTH_ERROR,
        NETWORK_IO_ERROR,
        FILE_IS_ALREADY_EXISTS,
        // inconsistent cached data,
        CACHE_ERROR,
        REMOTE_API_ERROR,
        // not important error that can be ignored
        ERROR_MESSAGE,
        // if user modified biometric data on phone (e.g. add new finger)
        BIOMETRIC_DATA_INVALIDATED_ERROR
    }

    private OperationError(@NonNull Type type,
                           @Nullable String message,
                           @Nullable Throwable throwable) {
        this.type = type;
        this.message = message;
        this.throwable = throwable;
    }

    @NonNull
    public Type getType() {
        return type;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @Nullable
    public Throwable getThrowable() {
        return throwable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        OperationError that = (OperationError) o;

        return new EqualsBuilder()
                .append(type, that.type)
                .append(message, that.message)
                .append(throwable, that.throwable)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(type)
                .append(message)
                .append(throwable)
                .toHashCode();
    }
}
