package com.ivanovsky.passnotes.data.repository.file.remote;

import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_ACCESS_TO_FILE;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_FIND_CACHED_FILE;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_FIND_FILE;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ivanovsky.passnotes.data.ObserverBus;
import com.ivanovsky.passnotes.data.entity.ConflictResolutionStrategy;
import com.ivanovsky.passnotes.data.entity.FSAuthority;
import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationError;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.entity.RemoteFile;
import com.ivanovsky.passnotes.data.entity.RemoteFileMetadata;
import com.ivanovsky.passnotes.data.entity.SyncConflictInfo;
import com.ivanovsky.passnotes.data.entity.SyncProgressStatus;
import com.ivanovsky.passnotes.data.entity.SyncResolution;
import com.ivanovsky.passnotes.data.entity.SyncStatus;
import com.ivanovsky.passnotes.data.repository.file.BaseRemoteFileOutputStream;
import com.ivanovsky.passnotes.data.repository.file.FSOptions;
import com.ivanovsky.passnotes.data.repository.file.FileSystemSyncProcessor;
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy;
import com.ivanovsky.passnotes.data.repository.file.RemoteFileInputStream;
import com.ivanovsky.passnotes.data.repository.file.SyncStrategy;
import com.ivanovsky.passnotes.domain.FileHelper;
import com.ivanovsky.passnotes.domain.SyncStrategyResolver;
import com.ivanovsky.passnotes.domain.entity.exception.Stacktrace;
import com.ivanovsky.passnotes.extensions.RemoteFileExtKt;
import com.ivanovsky.passnotes.util.FileUtils;
import com.ivanovsky.passnotes.util.InputOutputUtils;
import com.ivanovsky.passnotes.util.LongExtKt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import timber.log.Timber;

public class RemoteFileSyncProcessor implements FileSystemSyncProcessor {

    private final FSAuthority fsAuthority;
    private final RemoteFileSystemProvider provider;
    private final RemoteFileCache cache;
    private final FileHelper fileHelper;
    private final SyncStrategyResolver syncResolver;
    private final ObserverBus observerBus;
    private final Map<String, SyncProgressStatus> progressStatuses;
    private final Map<String, SyncStatus> statuses;

    public RemoteFileSyncProcessor(
            RemoteFileSystemProvider provider,
            RemoteFileCache cache,
            FileHelper fileHelper,
            ObserverBus observerBus,
            FSAuthority fsAuthority) {
        this.provider = provider;
        this.cache = cache;
        this.fileHelper = fileHelper;
        this.observerBus = observerBus;
        this.fsAuthority = fsAuthority;
        this.syncResolver = new SyncStrategyResolver();
        this.progressStatuses = new ConcurrentHashMap<>();
        this.statuses = new ConcurrentHashMap<>();
    }

    @Nullable
    @Override
    public FileDescriptor getCachedFile(@NonNull String uid) {
        FileDescriptor result = null;

        RemoteFile file = cache.getByUid(uid);
        if (file != null) {
            result = RemoteFileExtKt.toFileDescriptor(file);
        }

        return result;
    }

    @NonNull
    @Override
    public SyncProgressStatus getSyncProgressStatusForFile(@NonNull String uid) {
        SyncProgressStatus status = progressStatuses.get(uid);

        return (status != null) ? status : SyncProgressStatus.IDLE;
    }

    @NonNull
    @Override
    public SyncStatus getSyncStatusForFile(@NonNull String uid) {
        SyncStatus cachedStatus = statuses.get(uid);
        if (cachedStatus != null) {
            return cachedStatus;
        }

        RemoteFile cachedFile = cache.getByUid(uid);
        if (cachedFile == null) {
            return SyncStatus.NO_CHANGES;
        }

        OperationResult<FileDescriptor> getFile =
                provider.getFile(cachedFile.getRemotePath(), FSOptions.noCache());
        if (getFile.isFailed()) {
            OperationError.Type errorType = getFile.getError().getType();

            if (errorType == OperationError.Type.NETWORK_IO_ERROR) {
                if (cachedFile.isLocallyModified()) {
                    return SyncStatus.LOCAL_CHANGES_NO_NETWORK;
                } else {
                    return SyncStatus.NO_NETWORK;
                }
            } else if (errorType == OperationError.Type.AUTH_ERROR) {
                return SyncStatus.AUTH_ERROR;
            } else {
                return SyncStatus.ERROR;
            }
        }

        Long localModified = cachedFile.getLastModificationTimestamp();
        Long remoteModified = getFile.getObj().getModified();

        if (cachedFile.isLocallyModified()) {
            SyncResolution resolution =
                    syncResolver.resolve(
                            localModified,
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
        }
        if (LongExtKt.isNewerThan(
                remoteModified, cachedFile.getLastRemoteModificationTimestamp())) {
            return SyncStatus.REMOTE_CHANGES;
        } else {
            return SyncStatus.NO_CHANGES;
        }
    }

    @Nullable
    @Override
    public String getRevision(@NonNull String uid) {
        String revision = null;

        RemoteFile file = cache.getByUid(uid);
        if (file != null) {
            revision = file.getRevision();
        }

        return revision;
    }

    @NonNull
    @Override
    public OperationResult<SyncConflictInfo> getSyncConflictForFile(@NonNull String uid) {
        RemoteFile cachedFile = cache.getByUid(uid);
        if (cachedFile == null) {
            return OperationResult.error(
                    newCacheError(MESSAGE_FAILED_TO_FIND_CACHED_FILE, new Stacktrace()));
        }

        OperationResult<FileDescriptor> getFile =
                provider.getFile(cachedFile.getRemotePath(), FSOptions.noCache());
        if (getFile.isFailed()) {
            return getFile.takeError();
        }

        Long localModified = cachedFile.getLastModificationTimestamp();
        Long remoteModified = getFile.getObj().getModified();

        if (!cachedFile.isLocallyModified()) {
            return OperationResult.error(
                    newGenericError(MESSAGE_FILE_IS_NOT_MODIFIED, new Stacktrace()));
        }

        SyncResolution resolution =
                syncResolver.resolve(
                        localModified,
                        cachedFile.getLastRemoteModificationTimestamp(),
                        remoteModified,
                        SyncStrategy.LAST_REMOTE_MODIFICATION_WINS);
        if (resolution != SyncResolution.ERROR) {
            return OperationResult.error(
                    newGenericError(MESSAGE_INCORRECT_SYNC_STATUS, new Stacktrace()));
        }

        SyncConflictInfo info =
                new SyncConflictInfo(
                        RemoteFileExtKt.toFileDescriptor(cachedFile), getFile.getObj());

        return OperationResult.success(info);
    }

    @NonNull
    @Override
    public OperationResult<FileDescriptor> process(
            @NonNull FileDescriptor file,
            @NonNull SyncStrategy syncStrategy,
            @Nullable ConflictResolutionStrategy resolutionStrategy) {
        Timber.d(
                "process: file=%s, strategy=%s, conflictStrategy=%s",
                file, syncStrategy, resolutionStrategy);

        updateProgressStatusForFile(file.getUid(), SyncProgressStatus.SYNCING);

        RemoteFile cachedFile = cache.getByUid(file.getUid());
        if (cachedFile == null) {
            Timber.d("Unable to process file, no cached file");

            updateProgressStatusForFile(file.getUid(), SyncProgressStatus.IDLE);
            return OperationResult.error(
                    newCacheError(MESSAGE_FAILED_TO_FIND_CACHED_FILE, new Stacktrace()));
        }

        FileDescriptor localFile = RemoteFileExtKt.toFileDescriptor(cachedFile);

        OperationResult<FileDescriptor> getFile =
                provider.getFile(localFile.getPath(), FSOptions.noCache());
        if (getFile.isFailed()) {
            Timber.d("Unable to process file, failed to get file info");

            updateProgressStatusForFile(file.getUid(), SyncProgressStatus.IDLE);
            return getFile.takeError();
        }

        FileDescriptor remoteDescriptor = getFile.getObj();

        Long localModified = localFile.getModified();
        Long remoteModified = remoteDescriptor.getModified();

        SyncResolution resolution =
                syncResolver.resolve(
                        localModified,
                        cachedFile.getLastRemoteModificationTimestamp(),
                        remoteModified,
                        syncStrategy);
        SyncStatus status = convertResolutionToStatus(resolution);
        updateSyncStatusForFile(file.getUid(), status);

        Timber.d(
                "process: remoteFile=%s, localModified=%s, remoteModified=%s, resolution=%s",
                remoteDescriptor, localModified, remoteModified, resolution);

        switch (resolution) {
            case LOCAL:
                return uploadLocalFile(cachedFile, localFile, remoteDescriptor);

            case REMOTE:
            case EQUALS:
                return downloadFile(cachedFile, localFile, remoteDescriptor);

            case ERROR:
                if (resolutionStrategy == ConflictResolutionStrategy.RESOLVE_WITH_LOCAL_FILE) {
                    return uploadLocalFile(cachedFile, localFile, remoteDescriptor);

                } else if (resolutionStrategy
                        == ConflictResolutionStrategy.RESOLVE_WITH_REMOTE_FILE) {
                    return downloadFile(cachedFile, localFile, remoteDescriptor);

                } else {
                    return OperationResult.error(
                            newDbVersionConflictError(
                                    MESSAGE_LOCAL_VERSION_CONFLICTS_WITH_REMOTE, new Stacktrace()));
                }

            default:
                return OperationResult.error(
                        newDbVersionConflictError(
                                MESSAGE_LOCAL_VERSION_CONFLICTS_WITH_REMOTE, new Stacktrace()));
        }
    }

    private OperationResult<FileDescriptor> uploadLocalFile(
            RemoteFile cachedFile,
            FileDescriptor localDescriptor,
            FileDescriptor remoteDescriptor) {
        updateProgressStatusForFile(cachedFile.getUid(), SyncProgressStatus.UPLOADING);

        OperationResult<OutputStream> outResult =
                provider.openFileForWrite(
                        localDescriptor, OnConflictStrategy.REWRITE, FSOptions.noCache());
        if (outResult.isFailed()) {
            Timber.d("Failed to open file for write, error=%s", outResult.getError());
            return outResult.takeError();
        }

        if (!(outResult.getObj() instanceof BaseRemoteFileOutputStream)) {
            Timber.d("Incorrect result");
            return OperationResult.error(
                    newGenericIOError(MESSAGE_FAILED_TO_FIND_FILE, new Stacktrace()));
        }
        BaseRemoteFileOutputStream out = (BaseRemoteFileOutputStream) outResult.getObj();

        // Create buffer which will contains file data, because 'out' linked to the same file,
        // that 'in'
        OperationResult<InputStream> openBufferResult =
                copyFileAndOpen(new File(cachedFile.getLocalPath()));
        if (openBufferResult.isFailed()) {
            Timber.d("Failed to copy file, uid=%s", cachedFile.getUid());
            return openBufferResult.takeError();
        }

        InputStream in = openBufferResult.getObj();
        try {
            InputOutputUtils.copyOrThrow(in, out, true);
        } catch (IOException e) {
            Timber.d("Failed to copy file, uid=%s, error=%s", cachedFile.getUid(), e.toString());
            Timber.d(e);
            return OperationResult.error(newNetworkIOError(new Stacktrace()));
        }

        RemoteFile updatedCachedFile = cache.getByUid(cachedFile.getUid());
        if (updatedCachedFile == null) {
            Timber.d("Failed to find file in cache, uid=%s", cachedFile.getUid());
            return OperationResult.error(
                    newCacheError(MESSAGE_FAILED_TO_FIND_CACHED_FILE, new Stacktrace()));
        }

        OperationResult<RemoteFileMetadata> metadataResult =
                provider.getFileMetadata(remoteDescriptor);
        if (metadataResult.isFailed()) {
            Timber.d("Failed to get metadata, error=%s", metadataResult.getError());
            return metadataResult.takeError();
        }

        RemoteFileMetadata metadata = metadataResult.getObj();

        updatedCachedFile.setUid(metadata.getUid());
        updatedCachedFile.setLocalPath(out.getOutputFile().getPath());
        updatedCachedFile.setRemotePath(metadata.getPath());
        updatedCachedFile.setRevision(metadata.getRevision());
        updatedCachedFile.setLastModificationTimestamp(metadata.getServerModified().getTime());
        updatedCachedFile.setLastRemoteModificationTimestamp(
                metadata.getServerModified().getTime());
        updatedCachedFile.setLastDownloadTimestamp(System.currentTimeMillis());
        updatedCachedFile.setUploaded(true);
        updatedCachedFile.setLocallyModified(false);

        cache.update(updatedCachedFile);

        updateProgressStatusForFile(cachedFile.getUid(), SyncProgressStatus.IDLE);
        removeSyncStatusForFile(cachedFile.getUid());

        return OperationResult.success(RemoteFileExtKt.toFileDescriptor(updatedCachedFile));
    }

    private OperationResult<InputStream> copyFileAndOpen(File file) {
        OperationResult<InputStream> result = new OperationResult<>();

        File buffer = fileHelper.generateDestinationFileOrNull();
        if (buffer != null) {
            try {
                FileUtils.copyFile(file, buffer);

                InputStream in = newFileInputStreamOrNull(buffer);
                if (in != null) {
                    result.setObj(in);
                } else {
                    result.setError(
                            newGenericIOError(MESSAGE_FAILED_TO_ACCESS_TO_FILE, new Stacktrace()));
                }
            } catch (IOException e) {
                Timber.d(e);

                result.setError(
                        newGenericIOError(MESSAGE_FAILED_TO_ACCESS_TO_FILE, new Stacktrace()));
            }
        } else {
            result.setError(
                    newGenericIOError(MESSAGE_FAILED_TO_ACCESS_TO_FILE, new Stacktrace()));
        }

        return result;
    }

    private OperationResult<FileDescriptor> downloadFile(
            RemoteFile cachedFile,
            FileDescriptor localDescriptor,
            FileDescriptor remoteDescriptor) {
        Timber.d("downloadFile: file=%s", localDescriptor);

        updateProgressStatusForFile(cachedFile.getUid(), SyncProgressStatus.DOWNLOADING);

        OperationResult<InputStream> inResult =
                provider.openFileForRead(
                        localDescriptor, OnConflictStrategy.REWRITE, FSOptions.noCache());
        if (inResult.isFailed()) {
            Timber.d("Failed to download, error=%s", inResult.getError());
            return inResult.takeError();
        }

        if (!(inResult.getObj() instanceof RemoteFileInputStream)) {
            Timber.d("Failed to open file");
            return OperationResult.error(
                    newGenericIOError(MESSAGE_FAILED_TO_FIND_FILE, new Stacktrace()));
        }

        RemoteFileInputStream input = (RemoteFileInputStream) inResult.getObj();
        try {
            input.close();
        } catch (IOException e) {
            Timber.d(e);
            return OperationResult.error(
                    newFileAccessError(MESSAGE_FAILED_TO_ACCESS_TO_FILE, new Stacktrace()));
        }

        RemoteFile updatedCachedFile = cache.getByUid(cachedFile.getUid());
        if (updatedCachedFile == null) {
            Timber.d("Failed to find file in cache, uid=%s", cachedFile.getUid());
            return OperationResult.error(
                    newCacheError(MESSAGE_FAILED_TO_FIND_CACHED_FILE, new Stacktrace()));
        }

        OperationResult<RemoteFileMetadata> metadataResult =
                provider.getFileMetadata(remoteDescriptor);
        if (metadataResult.isFailed()) {
            Timber.d("Failed to get metadata, error=%s", metadataResult.getError());
            return metadataResult.takeError();
        }

        RemoteFileMetadata metadata = metadataResult.getObj();

        updatedCachedFile.setUid(metadata.getUid());
        updatedCachedFile.setLocalPath(input.getPath());
        updatedCachedFile.setRemotePath(metadata.getPath());
        updatedCachedFile.setRevision(metadata.getRevision());
        updatedCachedFile.setLastModificationTimestamp(metadata.getServerModified().getTime());
        updatedCachedFile.setLastRemoteModificationTimestamp(
                metadata.getServerModified().getTime());
        updatedCachedFile.setLastDownloadTimestamp(System.currentTimeMillis());
        updatedCachedFile.setUploaded(true);
        updatedCachedFile.setLocallyModified(false);

        cache.update(updatedCachedFile);

        updateProgressStatusForFile(cachedFile.getUid(), SyncProgressStatus.IDLE);
        removeSyncStatusForFile(cachedFile.getUid());

        return OperationResult.success(RemoteFileExtKt.toFileDescriptor(updatedCachedFile));
    }

    private void updateProgressStatusForFile(String uid, SyncProgressStatus status) {
        Timber.d("updateStatusForFile: status=%s, uid=%s", status, uid);
        SyncProgressStatus oldStatus = progressStatuses.getOrDefault(uid, SyncProgressStatus.IDLE);

        if (status != oldStatus) {
            observerBus.notifySyncProgressStatusChanged(fsAuthority, uid, status);

            if (status != SyncProgressStatus.IDLE) {
                progressStatuses.put(uid, status);
            } else {
                progressStatuses.remove(uid);
            }
        }
    }

    private void updateSyncStatusForFile(String uid, SyncStatus status) {
        statuses.put(uid, status);
    }

    private void removeSyncStatusForFile(String uid) {
        statuses.remove(uid);
    }

    private SyncStatus convertResolutionToStatus(SyncResolution resolution) {
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
    }
}
