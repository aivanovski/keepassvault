package com.ivanovsky.passnotes.data.entity;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class OperationError {

    public static final String MESSAGE_FAILED_TO_FIND_GROUP = "Failed to find group";
    public static final String MESSAGE_FAILED_TO_FIND_NOTE = "Failed to find note";
    public static final String MESSAGE_FAILED_TO_COMMIT = "Failed to commit";
    public static final String MESSAGE_UNKNOWN_ERROR = "Unknown error";
    public static final String MESSAGE_FILE_ACCESS_IS_FORBIDDEN = "File access is forbidden";
    public static final String MESSAGE_FILE_IS_NOT_A_DIRECTORY = "File is not a directory";
    public static final String MESSAGE_FILE_NOT_FOUND = "File not found";
    public static final String MESSAGE_FILE_DOES_NOT_EXIST = "File doesn't exist";
    public static final String MESSAGE_AUTH_FAILED = "Auth failed";
    public static final String MESSAGE_INCORRECT_FILE_SYSTEM_CREDENTIALS = "Incorrect file system credentials";
    public static final String MESSAGE_FAILED_TO_LOAD_FILE = "Failed to load file";
    public static final String MESSAGE_FAILED_TO_LOAD_FILE_LIST = "Failed to load file list";
    public static final String MESSAGE_IO_ERROR = "IO error";
    public static final String MESSAGE_RECORD_IS_ALREADY_EXISTS = "Record is already exists";
    public static final String MESSAGE_FILE_IS_ALREADY_EXISTS = "File is already exists";
    public static final String MESSAGE_FAILED_TO_OPEN_DB_FILE = "Failed to open DB file";
    public static final String MESSAGE_FAILED_TO_OPEN_DEFAULT_DB_FILE = "Failed to open default DB file";
    public static final String MESSAGE_LOCAL_VERSION_CONFLICTS_WITH_REMOTE = "Local version conflicts with remote";
    public static final String MESSAGE_FAILED_TO_FIND_FILE = "Failed to find file";
    public static final String MESSAGE_FAILED_TO_ACCESS_TO_PRIVATE_STORAGE = "Failed to access to private storage";
    public static final String MESSAGE_FAILED_TO_ACCESS_TO_FILE = "Failed to access to file";
    public static final String MESSAGE_FAILED_TO_GET_DATABASE = "Failed to get database";
    public static final String MESSAGE_DEFERRED_OPERATIONS_ARE_NOT_SUPPORTED = "Deferred operations are not supported";
    public static final String MESSAGE_FAILED_TO_FIND_CACHED_FILE = "Failed to find cached file";
    public static final String MESSAGE_FAILED_TO_FIND_ROOT_GROUP = "Failed to find root group";
    public static final String MESSAGE_FAILED_TO_FIND_PARENT_GROUP = "Failed to find parent group";
    public static final String MESSAGE_FAILED_TO_FIND_NEW_PARENT_GROUP = "Failed to find new parent group";
    public static final String MESSAGE_UID_IS_NULL = "Uid is null";
    public static final String MESSAGE_PARENT_UID_IS_NULL = "Parent uid is null";
    public static final String MESSAGE_DUPLICATED_NOTE = "Duplicated note";
    public static final String MESSAGE_FAILED_TO_ADD_ENTRY = "Failed to add entry";
    public static final String MESSAGE_FAILED_TO_REMOVE_ROOT_GROUP = "Failed to remove root group";
    public static final String MESSAGE_FAILED_TO_GET_PARENT_PATH = "Failed to get paretn path";
    public static final String MESSAGE_FAILED_TO_RESOLVE_SYNC_PROCESSOR = "Failed to resolve sync processot";
    public static final String MESSAGE_FILE_IS_NOT_MODIFIED = "File is not modified";
    public static final String MESSAGE_INCORRECT_SYNC_STATUS = "Incorrect sync status";
    public static final String MESSAGE_INCORRECT_USE_CASE = "Incorrect use case";
    public static final String MESSAGE_WRITE_OPERATION_IS_NOT_SUPPORTED = "Write operation is not supported";
    public static final String MESSAGE_UNSUPPORTED_CONFIG_TYPE = "Unsupported config type";
    public static final String MESSAGE_FAILED_TO_COMPLETE_OPERATION = "Failed to complete operation";
    public static final String MESSAGE_FAILED_TO_MOVE_GROUP_INSIDE_ITS_OWN_TREE = "Failed to move group inside its owdn tree";
    public static final String MESSAGE_FAILED_TO_REMOVE_FILE = "Failed to remove file";

    public static final String GENERIC_MESSAGE_NOT_FOUND = "%s not found";
    public static final String GENERIC_MESSAGE_GROUP_IS_ALREADY_EXIST = "Group '%s' already exists";
    public static final String GENERIC_MESSAGE_FAILED_TO_RETRIEVE_DATA_BY_URI = "Failed to retrive data by uri: %s";
    public static final String GENERIC_MESSAGE_FAILED_TO_FIND_COLUMN = "Failed to find column: %s";
    public static final String GENERIC_MESSAGE_FAILED_TO_GET_ACCESS_RIGHT_TO_URI = "Failed to get access to: %s";
    public static final String GENERIC_MESSAGE_FAILED_TO_FIND_FILE = "Failed to find file: %s";
    public static final String GENERIC_MESSAGE_FAILED_TO_FIND_ENTITY_BY_UID = "Failed to find '%s' in db: uid=%s";

    private Type type;
    private String message;
    private Throwable throwable;

    public static OperationError newDbError(String message) {
        OperationError error = new OperationError(Type.DB_ERROR);
        error.message = message;
        return error;
    }

    public static OperationError newDbError(String message, Exception exception) {
        OperationError error = new OperationError(Type.DB_ERROR);
        error.message = message;
        error.throwable = exception;
        return error;
    }

    public static OperationError newFailedToGetDbError() {
        OperationError error = new OperationError(Type.DB_ERROR);
        error.message = MESSAGE_FAILED_TO_GET_DATABASE;
        return error;
    }

    public static OperationError newDbVersionConflictError(String message) {
        OperationError error = new OperationError(Type.DB_VERSION_CONFLICT_ERROR);
        error.message = message;
        return error;
    }

    public static OperationError newGenericError(String message) {
        OperationError error = new OperationError(Type.GENERIC_ERROR);
        error.message = message;
        return error;
    }

    public static OperationError newGenericError(String message, Throwable throwable) {
        OperationError error = new OperationError(Type.GENERIC_ERROR);
        error.message = message;
        error.throwable = throwable;
        return error;
    }

    public static OperationError newFileAccessError(String message, Throwable throwable) {
        OperationError error = new OperationError(Type.FILE_ACCESS_ERROR);
        error.message = message;
        error.throwable = throwable;
        return error;
    }

    public static OperationError newFileAccessError(String message) {
        OperationError error = new OperationError(Type.FILE_ACCESS_ERROR);
        error.message = message;
        return error;
    }

    public static OperationError newFileNotFoundError() {
        OperationError error = new OperationError(Type.FILE_NOT_FOUND_ERROR);
        error.message = MESSAGE_FILE_NOT_FOUND;
        return error;
    }

    public static OperationError newFileNotFoundError(String message) {
        OperationError error = new OperationError(Type.FILE_NOT_FOUND_ERROR);
        error.message = message;
        return error;
    }

    public static OperationError newGenericIOError(String message) {
        OperationError error = new OperationError(Type.GENERIC_IO_ERROR);
        error.message = message;
        return error;
    }

    public static OperationError newGenericIOError(String message, Throwable throwable) {
        OperationError error = new OperationError(Type.GENERIC_IO_ERROR);
        error.message = message;
        error.throwable = throwable;
        return error;
    }

    public static OperationError newGenericIOError(Throwable throwable) {
        OperationError error = new OperationError(Type.GENERIC_IO_ERROR);
        error.message = throwable.toString();
        error.throwable = throwable;
        return error;
    }

    public static OperationError newAuthError(String message) {
        OperationError error = new OperationError(Type.AUTH_ERROR);
        error.message = message;
        return error;
    }

    public static OperationError newAuthError() {
        OperationError error = new OperationError(Type.AUTH_ERROR);
        error.message = MESSAGE_AUTH_FAILED;
        return error;
    }

    public static OperationError newNetworkIOError() {
        OperationError error = new OperationError(Type.NETWORK_IO_ERROR);
        error.message = MESSAGE_IO_ERROR;
        return error;
    }

    public static OperationError newFileIsAlreadyExistsError() {
        OperationError error = new OperationError(Type.FILE_IS_ALREADY_EXISTS);
        error.message = MESSAGE_FILE_IS_ALREADY_EXISTS;
        return error;
    }

    public static OperationError newCacheError(String message) {
        OperationError error = new OperationError(Type.CACHE_ERROR);
        error.message = message;
        return error;
    }

    public static OperationError newRemoteApiError(String message) {
        OperationError error = new OperationError(Type.REMOTE_API_ERROR);
        error.message = message;
        return error;
    }

    public enum Type {
        GENERIC_ERROR,
        DB_AUTH_ERROR,
        DB_ERROR,
        DB_VERSION_CONFLICT_ERROR,// if user modified db
        FILE_ACCESS_ERROR,
        FILE_NOT_FOUND_ERROR,
        GENERIC_IO_ERROR,
        AUTH_ERROR,
        NETWORK_IO_ERROR,
        FILE_IS_ALREADY_EXISTS,
        CACHE_ERROR, // inconsistent cached data
        REMOTE_API_ERROR
    }

    //TODO: remove unnecessary constructors and refactor

    public OperationError(Type type) {
        this.type = type;
    }

    public OperationError(Type type, String message) {
        this.type = type;
        this.message = message;
    }

    public OperationError(Type type, Throwable throwable) {
        this.type = type;
        this.throwable = throwable;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
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
