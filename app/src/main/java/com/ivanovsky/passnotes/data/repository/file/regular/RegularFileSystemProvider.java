package com.ivanovsky.passnotes.data.repository.file.regular;

import androidx.annotation.NonNull;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationError;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.file.FSOptions;
import com.ivanovsky.passnotes.data.repository.file.FileSystemAuthenticator;
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider;
import com.ivanovsky.passnotes.data.repository.file.FileSystemSyncProcessor;
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy;
import com.ivanovsky.passnotes.util.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FILE_NOT_FOUND;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_WRITE_OPERATION_IS_NOT_SUPPORTED;
import static com.ivanovsky.passnotes.data.entity.OperationError.newFileAccessError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError;

public class RegularFileSystemProvider implements FileSystemProvider {

    private final Lock lock;
    private final FileSystemAuthenticator authenticator;
    private final FileSystemSyncProcessor syncProcessor;

    public RegularFileSystemProvider() {
        this.lock = new ReentrantLock();
        this.authenticator = new RegularFileSystemAuthenticator();
        this.syncProcessor = new RegularFileSystemSyncProcessor();
    }

    @NonNull
    @Override
    public FileSystemAuthenticator getAuthenticator() {
        return authenticator;
    }

    @NonNull
    @Override
    public FileSystemSyncProcessor getSyncProcessor() {
        return syncProcessor;
    }

    @NonNull
    @Override
    public OperationResult<List<FileDescriptor>> listFiles(@NonNull FileDescriptor dir) {
        OperationResult<List<FileDescriptor>> result = new OperationResult<>();

        if (dir.isDirectory()) {
            File file = new File(dir.getPath());
            if (file.exists()) {
                List<FileDescriptor> files = new ArrayList<>();

                try {
                    File[] childFiles = file.listFiles();
                    if (childFiles != null && childFiles.length != 0) {
                        for (File childFile : childFiles) {
                            files.add(FileDescriptor.fromRegularFile(childFile));
                        }
                    }

                    result.setObj(files);
                } catch (SecurityException e) {
                    result.setError(newFileAccessError(OperationError.MESSAGE_FILE_ACCESS_IS_FORBIDDEN, e));
                }
            } else {
                result.setError(newGenericIOError(OperationError.MESSAGE_FILE_NOT_FOUND));
            }
        } else {
            result.setError(newGenericIOError(OperationError.MESSAGE_FILE_IS_NOT_A_DIRECTORY));
        }

        return result;
    }

    @NonNull
    @Override
    public OperationResult<FileDescriptor> getParent(@NonNull FileDescriptor fileDescriptor) {
        OperationResult<FileDescriptor> result = new OperationResult<>();

        File file = new File(fileDescriptor.getPath());
        if (file.exists()) {
            File parentFile = file.getParentFile();

            if (parentFile != null) {
                result.setObj(FileDescriptor.fromRegularFile(parentFile));
            } else {
                result.setError(newGenericIOError(OperationError.MESSAGE_FILE_DOES_NOT_EXIST));
            }
        } else {
            result.setError(newGenericIOError(OperationError.MESSAGE_FILE_NOT_FOUND));
        }

        return result;
    }

    @NonNull
    @Override
    public OperationResult<FileDescriptor> getRootFile() {
        OperationResult<FileDescriptor> result = new OperationResult<>();

        File root = new File("/");
        if (root.exists()) {
            result.setObj(FileDescriptor.fromRegularFile(root));
        } else {
            result.setError(newGenericIOError(OperationError.MESSAGE_FILE_NOT_FOUND));
        }

        return result;
    }

    @NonNull
    @Override
    public OperationResult<InputStream> openFileForRead(@NonNull FileDescriptor file,
                                                        @NonNull OnConflictStrategy onConflictStrategy,
                                                        @NonNull FSOptions options) {
        OperationResult<InputStream> result = new OperationResult<>();

        lock.lock();
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(file.getPath()));
            result.setObj(in);
        } catch (FileNotFoundException e) {
            Logger.printStackTrace(e);
            result.setError(newGenericIOError(e.getMessage()));
        } finally {
            lock.unlock();
        }

        return result;
    }

    @NonNull
    @Override
    public OperationResult<OutputStream> openFileForWrite(@NonNull FileDescriptor file,
                                                          @NonNull OnConflictStrategy onConflictStrategy,
                                                          @NonNull FSOptions options) {
        if (!options.isWriteEnabled()) {
            return OperationResult.error(newGenericIOError(MESSAGE_WRITE_OPERATION_IS_NOT_SUPPORTED));
        }

        OperationResult<OutputStream> result = new OperationResult<>();

        lock.lock();
        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(file.getPath()));
            result.setObj(out);
        } catch (FileNotFoundException e) {
            Logger.printStackTrace(e);
            result.setError(newGenericIOError(e.getMessage()));
        } finally {
            lock.unlock();
        }

        return result;
    }

    @NonNull
    @Override
    public OperationResult<Boolean> exists(@NonNull FileDescriptor file) {
        boolean exists = new File(file.getPath()).exists();
        return OperationResult.success(exists);
    }

    @NonNull
    @Override
    public OperationResult<FileDescriptor> getFile(@NonNull String path,
                                                   @NonNull FSOptions options) {
        OperationResult<FileDescriptor> result = new OperationResult<>();

        File file = new File(path);
        if (file.exists()) {
            result.setObj(FileDescriptor.fromRegularFile(file));
        } else {
            result.setError(newGenericIOError(MESSAGE_FILE_NOT_FOUND));
        }

        return result;
    }
}
