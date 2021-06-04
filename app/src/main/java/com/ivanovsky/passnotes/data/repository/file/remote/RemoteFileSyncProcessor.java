package com.ivanovsky.passnotes.data.repository.file.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy;
import com.ivanovsky.passnotes.data.entity.OperationError;
import com.ivanovsky.passnotes.data.entity.RemoteFile;
import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo;
import com.ivanovsky.passnotes.data.entity.SyncResolution;
import com.ivanovsky.passnotes.data.entity.SyncStatus;
import com.ivanovsky.passnotes.data.repository.file.FileSystemSyncProcessor;
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy;
import com.ivanovsky.passnotes.data.repository.file.SyncStrategy;
import com.ivanovsky.passnotes.domain.FileHelper;
import com.ivanovsky.passnotes.domain.SyncStrategyResolver;
import com.ivanovsky.passnotes.extensions.RemoteFileExtKt;
import com.ivanovsky.passnotes.util.InputOutputUtils;
import com.ivanovsky.passnotes.util.Logger;
import com.ivanovsky.passnotes.util.LongExtKt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_ACCESS_TO_FILE;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_FIND_CACHED_FILE;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FILE_IS_NOT_MODIFIED;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_INCORRECT_SYNC_STATUS;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_LOCAL_VERSION_CONFLICTS_WITH_REMOTE;
import static com.ivanovsky.passnotes.data.entity.OperationError.newCacheError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newDbVersionConflictError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newFileAccessError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newGenericError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newNetworkIOError;
import static com.ivanovsky.passnotes.util.InputOutputUtils.newFileInputStreamOrNull;

public class RemoteFileSyncProcessor implements FileSystemSyncProcessor {

    private final RemoteFileSystemProvider provider;
    private final RemoteFileCache cache;
    private final FileHelper fileHelper;
    private final SyncStrategyResolver syncResolver;

    public RemoteFileSyncProcessor(RemoteFileSystemProvider provider,
                                   RemoteFileCache cache,
                                   FileHelper fileHelper) {
        this.provider = provider;
        this.cache = cache;
        this.fileHelper = fileHelper;
        this.syncResolver = new SyncStrategyResolver();
    }

    @NonNull
    @Override
    public List<FileDescriptor> getLocallyModifiedFiles() {
        List<FileDescriptor> result = new ArrayList<>();

        List<RemoteFile> remoteFiles = cache.getLocallyModifiedFiles();
        for (RemoteFile file : remoteFiles) {
            FileDescriptor descriptor = RemoteFileExtKt.toFileDescriptor(file);
            result.add(descriptor);
        }

        return result;
    }

    @NonNull
    @Override
    public SyncStatus getSyncStatusForFile(@NonNull String uid) {
        RemoteFile cachedFile = cache.getByUid(uid);
        if (cachedFile == null) {
            return SyncStatus.NO_CHANGES;
        }

        OperationResult<FileDescriptor> getFile = provider.getFile(
                cachedFile.getRemotePath(),
                false);
        if (getFile.isFailed()) {
            boolean networkError =
                    (getFile.getError().getType() == OperationError.Type.NETWORK_IO_ERROR);

            if (networkError) {
                if (cachedFile.isLocallyModified()) {
                    return SyncStatus.LOCAL_CHANGES_NO_NETWORK;
                } else {
                    return SyncStatus.NO_NETWORK;
                }
            } else {
                return SyncStatus.CONFLICT;
            }
        }

        Long localModified = cachedFile.getLastModificationTimestamp();
        Long remoteModified = getFile.getObj().getModified();

        if (cachedFile.isLocallyModified()) {
            SyncResolution resolution = syncResolver.resolve(localModified,
                    cachedFile.getLastRemoteModificationTimestamp(),
                    remoteModified,
                    SyncStrategy.LAST_REMOTE_MODIFICATION_WINS);

            switch (resolution) {
                case LOCAL:
                    return SyncStatus.LOCAL_CHANGES;

                case REMOTE:
                    return SyncStatus.REMOTE_CHANGES;

                case EQUALS:
                    return SyncStatus.NO_CHANGES;

                case ERROR:
                default:
                    return SyncStatus.CONFLICT;
            }

        } if (LongExtKt.isNewerThan(remoteModified, cachedFile.getLastRemoteModificationTimestamp())) {
            return SyncStatus.REMOTE_CHANGES;
        } else {
            return SyncStatus.NO_CHANGES;
        }
    }

    @NonNull
    @Override
    public OperationResult<SyncConflictInfo> getSyncConflictForFile(@NonNull String uid) {
        RemoteFile cachedFile = cache.getByUid(uid);
        if (cachedFile == null) {
            return OperationResult.error(newCacheError(MESSAGE_FAILED_TO_FIND_CACHED_FILE));
        }

        OperationResult<FileDescriptor> getFile = provider.getFile(
                cachedFile.getRemotePath(),
                false);
        if (getFile.isFailed()) {
            return getFile.takeError();
        }

        Long localModified = cachedFile.getLastModificationTimestamp();
        Long remoteModified = getFile.getObj().getModified();

        if (!cachedFile.isLocallyModified()) {
            return OperationResult.error(newGenericError(MESSAGE_FILE_IS_NOT_MODIFIED));
        }

        SyncResolution resolution = syncResolver.resolve(localModified,
                cachedFile.getLastRemoteModificationTimestamp(),
                remoteModified,
                SyncStrategy.LAST_REMOTE_MODIFICATION_WINS);
        if (resolution != SyncResolution.ERROR) {
            return OperationResult.error(newGenericError(MESSAGE_INCORRECT_SYNC_STATUS));
        }

        SyncConflictInfo info = new SyncConflictInfo(RemoteFileExtKt.toFileDescriptor(cachedFile),
                getFile.getObj());

        return OperationResult.success(info);
    }

    @NonNull
    @Override
    public OperationResult<FileDescriptor> process(@NonNull FileDescriptor file,
                                                   @NonNull SyncStrategy syncStrategy,
                                                   @Nullable ConflictResolutionStrategy resolutionStrategy) {
        RemoteFile cachedFile = cache.getByUid(file.getUid());
        if (cachedFile == null) {
            return OperationResult.error(newCacheError(MESSAGE_FAILED_TO_FIND_CACHED_FILE));
        }

        FileDescriptor localFile = RemoteFileExtKt.toFileDescriptor(cachedFile);

        OperationResult<FileDescriptor> getFile = provider.getFile(
                localFile.getPath(),
                false);
        if (getFile.isFailed()) {
            return getFile.takeError();
        }

        FileDescriptor remoteDescriptor = getFile.getObj();

        Long localModified = localFile.getModified();
        Long remoteModified = remoteDescriptor.getModified();

        SyncResolution resolution = syncResolver.resolve(localModified,
                cachedFile.getLastRemoteModificationTimestamp(),
                remoteModified,
                syncStrategy);

        switch (resolution) {
            case LOCAL:
                return uploadLocalFile(cachedFile, localFile);

            case REMOTE:
            case EQUALS:
                return downloadFile(cachedFile, localFile);

            case ERROR:
                if (resolutionStrategy == ConflictResolutionStrategy.RESOLVE_WITH_LOCAL_FILE) {
                    return uploadLocalFile(cachedFile, localFile);

                } else if (resolutionStrategy == ConflictResolutionStrategy.RESOLVE_WITH_REMOTE_FILE) {
                    return downloadFile(cachedFile, localFile);

                } else {
                    return OperationResult.error(newDbVersionConflictError(MESSAGE_LOCAL_VERSION_CONFLICTS_WITH_REMOTE));
                }

            default:
                return OperationResult.error(newDbVersionConflictError(MESSAGE_LOCAL_VERSION_CONFLICTS_WITH_REMOTE));
        }
    }

    private OperationResult<FileDescriptor> uploadLocalFile(RemoteFile cachedFile,
                                                            FileDescriptor localDescriptor) {
        OperationResult<FileDescriptor> result = new OperationResult<>();

        OperationResult<OutputStream> outResult = provider.openFileForWrite(localDescriptor,
                OnConflictStrategy.REWRITE,
                false);

        if (outResult.isSucceeded()) {
            OutputStream out = outResult.getObj();

            // Create buffer which will contains file data, because 'out' linked to the same file,
            // that 'in'

            OperationResult<InputStream> openBufferResult = copyFileAndOpen(new File(cachedFile.getLocalPath()));
            InputStream in = openBufferResult.getObj();

            try {
                InputOutputUtils.copy(in, out, true);

                RemoteFile updatedCachedFile = cache.getByUid(cachedFile.getUid());
                if (updatedCachedFile != null) {
                    result.setObj(RemoteFileExtKt.toFileDescriptor(updatedCachedFile));
                } else {
                    result.setError(newCacheError(MESSAGE_FAILED_TO_FIND_CACHED_FILE));
                }
            } catch (IOException e) {
                Logger.printStackTrace(e);
                result.setError(newNetworkIOError());
            }
        } else {
            result.setError(outResult.getError());
        }

        return result;
    }

    private OperationResult<InputStream> copyFileAndOpen(File file) {
        OperationResult<InputStream> result = new OperationResult<>();

        File buffer = fileHelper.generateDestinationFileForRemoteFile();
        if (buffer != null) {
            try {
                fileHelper.duplicateFile(file, buffer);

                InputStream in = newFileInputStreamOrNull(buffer);
                if (in != null) {
                    result.setObj(in);
                } else {
                    result.setError(newGenericIOError(MESSAGE_FAILED_TO_ACCESS_TO_FILE));
                }
            } catch (IOException e) {
                Logger.printStackTrace(e);

                result.setError(newGenericIOError(MESSAGE_FAILED_TO_ACCESS_TO_FILE));
            }
        } else {
            result.setError(newGenericIOError(MESSAGE_FAILED_TO_ACCESS_TO_FILE));
        }

        return result;
    }

    private OperationResult<FileDescriptor> downloadFile(RemoteFile cachedFile,
                                                         FileDescriptor localDescriptor) {
        OperationResult<FileDescriptor> result = new OperationResult<>();

        OperationResult<InputStream> inResult = provider.openFileForRead(localDescriptor,
                OnConflictStrategy.REWRITE,
                false);

        if (inResult.isSucceeded()) {
            InputStream in = inResult.getObj();

            try {
                in.close();

                RemoteFile updatedCachedFile = cache.getByUid(cachedFile.getUid());
                if (updatedCachedFile != null) {
                    result.setObj(RemoteFileExtKt.toFileDescriptor(updatedCachedFile));
                } else {
                    result.setError(newCacheError(MESSAGE_FAILED_TO_FIND_CACHED_FILE));
                }
            } catch (IOException e) {
                e.printStackTrace();
                result.setError(newFileAccessError(MESSAGE_FAILED_TO_ACCESS_TO_FILE));
            }
        } else {
            result.setError(inResult.getError());
        }

        return result;
    }
}
