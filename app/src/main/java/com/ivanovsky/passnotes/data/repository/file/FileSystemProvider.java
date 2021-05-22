package com.ivanovsky.passnotes.data.repository.file;

import androidx.annotation.NonNull;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface FileSystemProvider {

    @NonNull
    FileSystemAuthenticator getAuthenticator();

    @NonNull
    FileSystemSyncProcessor getSyncProcessor();

    @NonNull
    OperationResult<List<FileDescriptor>> listFiles(FileDescriptor dir);

    @NonNull
    OperationResult<FileDescriptor> getParent(FileDescriptor file);

    @NonNull
    OperationResult<FileDescriptor> getRootFile();

    @NonNull
    OperationResult<InputStream> openFileForRead(FileDescriptor file,
                                                 OnConflictStrategy onConflictStrategy,
                                                 boolean cacheOperationsEnabled);

    @NonNull
    OperationResult<OutputStream> openFileForWrite(FileDescriptor file,
                                                   OnConflictStrategy onConflictStrategy,
                                                   boolean cacheOperationsEnabled);

    @NonNull
    OperationResult<Boolean> exists(FileDescriptor file);

    @NonNull
    OperationResult<FileDescriptor> getFile(String path, boolean cacheOperationsEnabled);

    @NonNull
    OperationResult<Boolean> isStoragePermissionRequired(FileDescriptor file);
}
