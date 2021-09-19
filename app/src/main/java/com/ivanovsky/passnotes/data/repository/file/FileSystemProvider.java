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
    OperationResult<List<FileDescriptor>> listFiles(@NonNull FileDescriptor dir);

    @NonNull
    OperationResult<FileDescriptor> getParent(@NonNull FileDescriptor file);

    @NonNull
    OperationResult<FileDescriptor> getRootFile();

    @NonNull
    OperationResult<InputStream> openFileForRead(@NonNull FileDescriptor file,
                                                 @NonNull OnConflictStrategy onConflictStrategy,
                                                 @NonNull FSOptions options);

    @NonNull
    OperationResult<OutputStream> openFileForWrite(@NonNull FileDescriptor file,
                                                   @NonNull OnConflictStrategy onConflictStrategy,
                                                   @NonNull FSOptions options);

    @NonNull
    OperationResult<Boolean> exists(@NonNull FileDescriptor file);

    @NonNull
    OperationResult<FileDescriptor> getFile(@NonNull String path,
                                            @NonNull FSOptions options);
}
