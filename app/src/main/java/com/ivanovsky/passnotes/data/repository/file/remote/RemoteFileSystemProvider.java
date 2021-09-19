package com.ivanovsky.passnotes.data.repository.file.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ivanovsky.passnotes.data.entity.FSAuthority;
import com.ivanovsky.passnotes.data.entity.RemoteFile;
import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationError;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.entity.RemoteFileMetadata;
import com.ivanovsky.passnotes.data.repository.RemoteFileRepository;
import com.ivanovsky.passnotes.data.repository.file.FSOptions;
import com.ivanovsky.passnotes.data.repository.file.FileSystemAuthenticator;
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider;
import com.ivanovsky.passnotes.data.repository.file.FileSystemSyncProcessor;
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy;
import com.ivanovsky.passnotes.data.repository.file.remote.exception.RemoteFSApiException;
import com.ivanovsky.passnotes.data.repository.file.remote.exception.RemoteFSAuthException;
import com.ivanovsky.passnotes.data.repository.file.remote.exception.RemoteFSException;
import com.ivanovsky.passnotes.data.repository.file.remote.exception.RemoteFSFileNotFoundException;
import com.ivanovsky.passnotes.data.repository.file.remote.exception.RemoteFSNetworkException;
import com.ivanovsky.passnotes.domain.FileHelper;
import com.ivanovsky.passnotes.extensions.RemoteFileExtKt;
import com.ivanovsky.passnotes.extensions.RemoteFileMetadataExtKt;
import com.ivanovsky.passnotes.util.DateUtils;
import com.ivanovsky.passnotes.util.FileUtils;
import com.ivanovsky.passnotes.util.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_FIND_FILE;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_LOCAL_VERSION_CONFLICTS_WITH_REMOTE;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_WRITE_OPERATION_IS_NOT_SUPPORTED;
import static com.ivanovsky.passnotes.data.entity.OperationError.newAuthError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newDbVersionConflictError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newNetworkIOError;
import static com.ivanovsky.passnotes.util.DateUtils.anyLastTimestamp;
import static com.ivanovsky.passnotes.util.InputOutputUtils.newFileInputStreamOrNull;
import static com.ivanovsky.passnotes.util.ObjectUtils.isNotEquals;

public class RemoteFileSystemProvider implements FileSystemProvider {

    private static final String TAG = RemoteFileSystemProvider.class.getSimpleName();

    private static final String ERROR_FAILED_TO_FIND_APP_PRIVATE_DIR = "Failed to find app private dir";
    private static final String ERROR_FAILED_TO_FIND_FILE = "Failed to find file: %s";
    private static final String ERROR_FAILED_TO_FIND_FILE_IN_CACHE = "Faile to find file in cache: %s";
    private static final String ERROR_FAILED_TO_START_PROCESSING_UNIT = "Failed to start processing unit";

    private static final long MAX_AWAITING_TIMEOUT_IN_SEC = 30;

    private final FileSystemAuthenticator authenticator;
    private final RemoteApiClient client;
    private final RemoteFileCache cache;
    private final StatusMap processingMap;
    private final Map<UUID, CountDownLatch> processingUidToLatch;
    private final Lock unitProcessingLock;
    private final FileHelper fileHelper;
    private final RemoteFileSyncProcessor syncProcessor;
    private final FSAuthority fsAuthority;

    public RemoteFileSystemProvider(FileSystemAuthenticator authenticator,
                                    RemoteApiClient client,
                                    RemoteFileRepository remoteFileRepository,
                                    FileHelper fileHelper,
                                    FSAuthority fsAuthority) {
        this.authenticator = authenticator;
        this.client = client;
        this.cache = new RemoteFileCache(remoteFileRepository, fsAuthority);
        this.processingMap = new StatusMap();
        this.processingUidToLatch = new HashMap<>();
        this.unitProcessingLock = new ReentrantLock();
        this.fileHelper = fileHelper;
        this.syncProcessor = new RemoteFileSyncProcessor(this, cache, fileHelper);
        this.fsAuthority = fsAuthority;
    }

    @NonNull
    @Override
    public FileSystemAuthenticator getAuthenticator() {
        return authenticator;
    }

    @NonNull
    @Override
    public OperationResult<List<FileDescriptor>> listFiles(@NonNull FileDescriptor dir) {
        if (client instanceof RemoteApiClientAdapter) {
            RemoteApiClientAdapter clientAdapter = (RemoteApiClientAdapter) client;
            return clientAdapter.getBaseClient().listFiles(dir);
        }

        // TODO: deprecated RemoteApiClient usage
        OperationResult<List<FileDescriptor>> result = new OperationResult<>();

        try {
            result.setObj(client.listFiles(dir));
        } catch (RemoteFSException e) {
            result.setError(createOperationErrorFromException(e));
        }

        return result;
    }

    private OperationError createOperationErrorFromException(RemoteFSException exception) {
        OperationError result;

        if (exception instanceof RemoteFSAuthException) {
            result = newAuthError(exception.getMessage());
        } else if (exception instanceof RemoteFSNetworkException) {
            result = newNetworkIOError();
        } else if (exception instanceof RemoteFSFileNotFoundException) {
            result = newGenericIOError(exception.getMessage());
        } else if (exception instanceof RemoteFSApiException) {
            result = newGenericIOError(exception.getMessage());
        } else {
            throw new IllegalArgumentException("Exception handling is not implemented: exception=" + exception);
        }

        return result;
    }

    @NonNull
    @Override
    public OperationResult<FileDescriptor> getParent(@NonNull FileDescriptor file) {
        if (client instanceof RemoteApiClientAdapter) {
            RemoteApiClientAdapter clientAdapter = (RemoteApiClientAdapter) client;
            return clientAdapter.getBaseClient().getParent(file);
        }

        // TODO: deprecated RemoteApiClient usage
        OperationResult<FileDescriptor> result = new OperationResult<>();

        try {
            result.setObj(client.getParent(file));
        } catch (RemoteFSException e) {
            result.setError(createOperationErrorFromException(e));
        }

        return result;
    }

    @NonNull
    @Override
    public OperationResult<FileDescriptor> getRootFile() {
        if (client instanceof RemoteApiClientAdapter) {
            RemoteApiClientAdapter clientAdapter = (RemoteApiClientAdapter) client;
            return clientAdapter.getBaseClient().getRoot();
        }

        // TODO: deprecated RemoteApiClient usage

        OperationResult<FileDescriptor> result = new OperationResult<>();

        try {
            result.setObj(client.getRoot());
        } catch (RemoteFSException e) {
            result.setError(createOperationErrorFromException(e));
        }

        return result;
    }

    private OperationResult<FileDescriptor> getDeferredFileFromCache(@NonNull String path,
                                                                     @Nullable OperationError error) {
        RemoteFile cachedFile = cache.getByRemotePath(path);

        if (cachedFile != null) {
            return OperationResult.deferred(newDescriptorFromRemoteFile(cachedFile), error);
        } else {
            return OperationResult.error(newGenericIOError(MESSAGE_FAILED_TO_FIND_FILE));
        }
    }

    @NonNull
    @Override
    public OperationResult<FileDescriptor> getFile(@NonNull String path,
                                                   @NonNull FSOptions options) {
        if (options.isCacheOnly()) {
            return getDeferredFileFromCache(path, null);
        }

        if (client instanceof RemoteApiClientAdapter) {
            RemoteApiClientAdapter clientAdapter = (RemoteApiClientAdapter) client;

            OperationResult<RemoteFileMetadata> metadata =
                    clientAdapter.getBaseClient().getFileMetadata(newDescriptorFromPath(path));
            if (metadata.isFailedDueToNetwork() && options.isCacheEnabled()) {
                return getDeferredFileFromCache(path, metadata.getError());
            }

            if (metadata.isFailed()) {
                return metadata.takeError();
            }

            return OperationResult.success(newDescriptorFromMetadata(metadata.getObj()));
        }

        // TODO: deprecated RemoteApiClient usage
        OperationResult<FileDescriptor> result = new OperationResult<>();

        try {
            RemoteFileMetadata metadata = client.getFileMetadataOrThrow(newDescriptorFromPath(path));
            result.setObj(newDescriptorFromMetadata(metadata));
        } catch (RemoteFSNetworkException e) {
            if (!options.isCacheEnabled()) {
                return OperationResult.error(createOperationErrorFromException(e));
            }

            return getDeferredFileFromCache(path, createOperationErrorFromException(e));

        } catch (RemoteFSException e) {
            result.setError(createOperationErrorFromException(e));
        }

        return result;
    }

    private FileDescriptor newDescriptorFromPath(String path) {
        return new FileDescriptor(fsAuthority,
                path,
                path,
                false,
                false,
                null);
    }

    private FileDescriptor newDescriptorFromMetadata(RemoteFileMetadata metadata) {
        return RemoteFileMetadataExtKt.toFileDescriptor(metadata, fsAuthority);
    }

    private FileDescriptor newDescriptorFromRemoteFile(RemoteFile file) {
        return RemoteFileExtKt.toFileDescriptor(file);
    }

    private OperationResult<InputStream> openDeferredFileForReadFromCache(@NonNull FileDescriptor file,
                                                                          @Nullable OperationError error) {
        RemoteFile cachedFile = cache.getByUid(file.getUid());
        if (cachedFile == null) {
            String message = String.format(ERROR_FAILED_TO_FIND_FILE_IN_CACHE, file.getPath());
            return OperationResult.error(newGenericIOError(message));
        }

        try {
            return OperationResult.deferred(new FileInputStream(cachedFile.getLocalPath()), error);
        } catch (FileNotFoundException e) {
            return OperationResult.error(newGenericIOError(MESSAGE_FAILED_TO_FIND_FILE));
        }
    }

    @NonNull
    @Override
    public OperationResult<InputStream> openFileForRead(@NonNull FileDescriptor file,
                                                        @NonNull OnConflictStrategy onConflictStrategy,
                                                        @NonNull FSOptions options) {
        OperationResult<InputStream> result = new OperationResult<>();

        File destinationDir = fileHelper.getRemoteFilesDir();
        if (destinationDir == null) {
            return OperationResult.error(newGenericIOError(ERROR_FAILED_TO_FIND_APP_PRIVATE_DIR));
        }

        if (options.isCacheOnly()) {
            return openDeferredFileForReadFromCache(file, null);
        }

        ProcessingUnit unit = null;
        try {
            RemoteFileMetadata metadata = client.getFileMetadataOrThrow(file);

            String uid = metadata.getUid();
            String remoteRevision = metadata.getRevision();
            String remotePath = metadata.getPath();

            RemoteFile cachedFile = cache.getByUid(uid);

            if (cachedFile == null || !options.isCacheEnabled()) {
                // download file and add new entry to the cache
                String destinationPath = generateDestinationFilePath(destinationDir);

                unit = new ProcessingUnit(UUID.randomUUID(),
                        ProcessingStatus.DOWNLOADING,
                        uid,
                        remotePath);

                if (startProcessingUnit(unit)) {
                    Logger.d(TAG, "Downloading new file: remote=%s, local=%s", remotePath, destinationPath);

                    metadata = client.downloadFileOrThrow(remotePath, destinationPath);

                    if (options.isCacheEnabled()) {
                        cachedFile = new RemoteFile();

                        cachedFile.setFsAuthority(fsAuthority);
                        cachedFile.setUid(metadata.getUid());
                        cachedFile.setRemotePath(metadata.getPath());
                        cachedFile.setLocalPath(destinationPath);
                        cachedFile.setRevision(metadata.getRevision());
                        cachedFile.setUploaded(true);
                        cachedFile.setLastModificationTimestamp(
                                anyLastTimestamp(metadata.getServerModified(), metadata.getClientModified()));
                        cachedFile.setLastRemoteModificationTimestamp(metadata.getServerModified().getTime());
                        cachedFile.setLastDownloadTimestamp(System.currentTimeMillis());

                        cache.put(cachedFile);
                    }

                    result.setObj(new FileInputStream(destinationPath));
                } else {
                    result.setError(newGenericIOError(ERROR_FAILED_TO_START_PROCESSING_UNIT));
                }

            } else if (isNotEquals(remoteRevision, cachedFile.getRevision())) {
                if (canResolveDownloadConflict(cachedFile, onConflictStrategy)) {
                    // server has new version of db
                    unit = new ProcessingUnit(UUID.randomUUID(),
                            ProcessingStatus.DOWNLOADING,
                            uid,
                            remotePath);

                    if (startProcessingUnit(unit)) {
                        Logger.d(TAG, "Updating cached file: remote=%s, local=%s", remotePath, cachedFile.getLocalPath());

                        metadata = client.downloadFileOrThrow(remotePath, cachedFile.getLocalPath());

                        cachedFile.setRemotePath(metadata.getPath());
                        cachedFile.setRevision(metadata.getRevision());
                        cachedFile.setUploaded(true);
                        cachedFile.setUploadFailed(false);
                        cachedFile.setLocallyModified(false);
                        cachedFile.setLastModificationTimestamp(
                                anyLastTimestamp(metadata.getServerModified(), metadata.getClientModified()));
                        cachedFile.setLastRemoteModificationTimestamp(metadata.getServerModified().getTime());
                        cachedFile.setLastDownloadTimestamp(System.currentTimeMillis());
                        cachedFile.setRetryCount(0);
                        cachedFile.setLastRetryTimestamp(null);

                        cache.update(cachedFile);

                        result.setObj(new FileInputStream(cachedFile.getLocalPath()));
                    } else {
                        result.setError(newGenericIOError(ERROR_FAILED_TO_START_PROCESSING_UNIT));
                    }
                } else {
                    // user modified db
                    result.setError(newDbVersionConflictError(MESSAGE_LOCAL_VERSION_CONFLICTS_WITH_REMOTE));
                }
            } else {
                // local revision is the same as in the server
                Logger.d(TAG, "Local cached file is up to date: remote=%s, local=%s",
                        remotePath, cachedFile.getLocalPath());

                cachedFile.setRemotePath(metadata.getPath());
                cachedFile.setLastModificationTimestamp(
                        anyLastTimestamp(metadata.getServerModified(), metadata.getClientModified()));
                cachedFile.setLastRemoteModificationTimestamp(metadata.getServerModified().getTime());
                cachedFile.setUploaded(true);
                cachedFile.setUploadFailed(false);
                cachedFile.setLocallyModified(false);
                cachedFile.setRetryCount(0);
                cachedFile.setLastRetryTimestamp(null);

                cache.update(cachedFile);

                result.setObj(new FileInputStream(cachedFile.getLocalPath()));
            }
        } catch (FileNotFoundException e) {
            result.setError(newGenericIOError(MESSAGE_FAILED_TO_FIND_FILE));

        } catch (RemoteFSNetworkException e) {
            // use cached file
            RemoteFile cachedFile = cache.getByUid(file.getUid());
            if (cachedFile != null) {
                unit = new ProcessingUnit(UUID.randomUUID(),
                        ProcessingStatus.DOWNLOADING,
                        cachedFile.getUid(),
                        cachedFile.getRemotePath());

                if (startProcessingUnit(unit)) {
                    InputStream fileStream = newFileInputStreamOrNull(new File(cachedFile.getLocalPath()));
                    if (fileStream != null) {
                        result.setDeferredObj(fileStream);
                    } else {
                        result.setError(newGenericIOError(MESSAGE_FAILED_TO_FIND_FILE));
                    }
                } else {
                    result.setError(newGenericIOError(ERROR_FAILED_TO_START_PROCESSING_UNIT));
                }

            } else {
                result.setError(newNetworkIOError());
            }

        } catch (RemoteFSException e) {
            result.setError(createOperationErrorFromException(e));
        }

        if (unit != null) {
            onFinishProcessingUnit(unit.getProcessingUid());
        }

        return result;
    }

    private String generateDestinationFilePath(File dir) {
        // TODO: use method from FileUtils.java
        return dir.getPath() + "/" + UUID.randomUUID().toString();
    }

    private boolean canResolveDownloadConflict(RemoteFile cachedFile,
                                               OnConflictStrategy onConflictStrategy) {
        boolean result;

        if (cachedFile.isLocallyModified() && onConflictStrategy == OnConflictStrategy.CANCEL) {
            result = false;
        } else {
            result = true;
        }

        return result;
    }

    private OperationResult<OutputStream> openCachedFileForWrite(@NonNull FileDescriptor file,
                                                                 @Nullable OperationError error) {
        RemoteFile cachedFile = cache.getByUid(file.getUid());
        if (cachedFile == null) {
            String message = String.format(ERROR_FAILED_TO_FIND_FILE_IN_CACHE, file.getPath());
            return OperationResult.error(newGenericIOError(message));
        }

        cachedFile.setLastModificationTimestamp(file.getModified());
        cachedFile.setLocallyModified(true);
        cachedFile.setUploaded(false);

        ProcessingUnit unit = new ProcessingUnit(UUID.randomUUID(),
                ProcessingStatus.UPLOADING,
                cachedFile.getUid(),
                cachedFile.getRemotePath());

        if (startProcessingUnit(unit)) {
            try {
                OutputStream out = new OfflineFileOutputStream(this, cachedFile, unit.getProcessingUid());

                cache.update(cachedFile);

                return OperationResult.deferred(out, error);
            } catch (FileNotFoundException ee) {
                onFinishProcessingUnit(unit.getProcessingUid());

                return OperationResult.error(newGenericIOError(String.format(ERROR_FAILED_TO_FIND_FILE,
                        cachedFile.getLocalPath())));
            }
        } else {
            return OperationResult.error(newGenericIOError(ERROR_FAILED_TO_START_PROCESSING_UNIT));
        }
    }

    @NonNull
    @Override
    public OperationResult<OutputStream> openFileForWrite(@NonNull FileDescriptor file,
                                                          @NonNull OnConflictStrategy onConflict,
                                                          @NonNull FSOptions options) {
        if (!options.isWriteEnabled()) {
            return OperationResult.error(newGenericIOError(MESSAGE_WRITE_OPERATION_IS_NOT_SUPPORTED));
        }

        OperationResult<OutputStream> result = new OperationResult<>();

        File destinationDir = fileHelper.getRemoteFilesDir();
        if (destinationDir == null) {
            return OperationResult.error(newGenericIOError(ERROR_FAILED_TO_FIND_APP_PRIVATE_DIR));
        }

        if (options.isCacheOnly()) {
            return openCachedFileForWrite(file, null);
        }

        try {
            RemoteFileMetadata metadata;

            try {
                metadata = client.getFileMetadataOrThrow(file);
            } catch (RemoteFSFileNotFoundException e) {
                metadata = null;
            }

            if (metadata == null) {
                // create and upload new file
                String parentPath = FileUtils.getParentPath(file.getPath());
                if (!"/".equals(parentPath)) {
                    FileDescriptor parent = client.getParent(file);
                    parentPath = parent.getPath();
                }

                RemoteFile cachedFile = new RemoteFile();

                long timestamp = System.currentTimeMillis();

                cachedFile.setFsAuthority(fsAuthority);
                cachedFile.setRemotePath(parentPath + "/" + file.getName());
                cachedFile.setLocalPath(generateDestinationFilePath(destinationDir));
                cachedFile.setUid(cachedFile.getRemotePath());
                cachedFile.setLastModificationTimestamp(timestamp);
                cachedFile.setLastRemoteModificationTimestamp(null);
                cachedFile.setLastDownloadTimestamp(timestamp);
                cachedFile.setLocallyModified(true);

                ProcessingUnit unit = new ProcessingUnit(UUID.randomUUID(),
                        ProcessingStatus.UPLOADING,
                        cachedFile.getUid(),
                        cachedFile.getRemotePath());

                Logger.d(TAG, "Uploading to new file: remote=%s, local=%s",
                        cachedFile.getRemotePath(), cachedFile.getLocalPath());

                if (startProcessingUnit(unit)) {
                    result.from(processFileUploading(cachedFile, unit.getProcessingUid()));

                    if (result.isFailed()) {
                        onFinishProcessingUnit(unit.getProcessingUid());
                    }
                } else {
                    result.setError(newGenericIOError(ERROR_FAILED_TO_START_PROCESSING_UNIT));
                }

            } else {
                // re-write existing file
                String uid = metadata.getUid();
                String remotePath = metadata.getPath();
                Date localModified = new Date(file.getModified());
                Date serverModified = metadata.getServerModified();
                Date clientModified = metadata.getClientModified();

                RemoteFile cachedFile = cache.getByUid(uid);
                if (canResolveMergeConflict(localModified, serverModified, clientModified, onConflict)) {
                    if (cachedFile == null) {
                        cachedFile = new RemoteFile();

                        cachedFile.setFsAuthority(fsAuthority);
                        cachedFile.setRemotePath(remotePath);
                        cachedFile.setLocalPath(generateDestinationFilePath(destinationDir));
                        cachedFile.setLastModificationTimestamp(localModified.getTime());
                        cachedFile.setLastRemoteModificationTimestamp(serverModified.getTime());
                        cachedFile.setLastDownloadTimestamp(localModified.getTime());
                        cachedFile.setLocallyModified(true);

                        ProcessingUnit unit = new ProcessingUnit(UUID.randomUUID(),
                                ProcessingStatus.UPLOADING,
                                cachedFile.getUid(),
                                cachedFile.getRemotePath());

                        Logger.d(TAG, "Uploading to existing file: remote=%s, local=%s",
                                cachedFile.getRemotePath(), cachedFile.getLocalPath());

                        if (startProcessingUnit(unit)) {
                            result.from(processFileUploading(cachedFile, unit.getProcessingUid()));

                            if (result.isFailed()) {
                                onFinishProcessingUnit(unit.getProcessingUid());
                            }
                        } else {
                            result.setError(newGenericIOError(ERROR_FAILED_TO_START_PROCESSING_UNIT));
                        }

                    } else {
                        cachedFile.setRemotePath(remotePath);
                        cachedFile.setLastModificationTimestamp(localModified.getTime());
                        cachedFile.setLastRemoteModificationTimestamp(serverModified.getTime());
                        cachedFile.setLastDownloadTimestamp(localModified.getTime());
                        cachedFile.setLocallyModified(true);
                        cachedFile.setUploaded(false);

                        ProcessingUnit unit = new ProcessingUnit(UUID.randomUUID(),
                                ProcessingStatus.UPLOADING,
                                cachedFile.getUid(),
                                cachedFile.getRemotePath());

                        if (startProcessingUnit(unit)) {
                            result.from(processFileUploading(cachedFile, unit.getProcessingUid()));

                            if (result.isFailed()) {
                                onFinishProcessingUnit(unit.getProcessingUid());
                            }
                        } else {
                            result.setError(newGenericIOError(ERROR_FAILED_TO_START_PROCESSING_UNIT));
                        }
                    }
                } else {
                    result.setError(newDbVersionConflictError(MESSAGE_LOCAL_VERSION_CONFLICTS_WITH_REMOTE));
                }
            }
        } catch (RemoteFSNetworkException e) {
            // if device is offline, just write to the local file
            if (!options.isCacheEnabled()) {
                return OperationResult.error(createOperationErrorFromException(e));
            }

            return openCachedFileForWrite(file, createOperationErrorFromException(e));

        } catch (RemoteFSException e) {
            result.setError(createOperationErrorFromException(e));
        }

        return result;
    }

    private ProcessingUnit findProcessingUnit(String fileUid, String remotePath) {
        ProcessingUnit entry;

        if (fileUid == null) {
            entry = processingMap.getByRemotePath(remotePath);
        } else {
            entry = processingMap.getByFileUid(fileUid);
        }

        return entry;
    }

    private void awaitProcessingUnitFinish(String fileUid,
                                           String remotePath) throws InterruptedException {
        ProcessingUnit unit = findProcessingUnit(fileUid, remotePath);

        CountDownLatch latch;

        Logger.d(TAG, "Waiting until operation finished: fileUid=" + fileUid +
                ", remotePath=" + remotePath);

        unitProcessingLock.lock();
        try {
            if (!processingUidToLatch.containsKey(unit.getProcessingUid())) {
                latch = new CountDownLatch(1);

                processingUidToLatch.put(unit.getProcessingUid(), latch);
            } else {
                latch = processingUidToLatch.get(unit.getProcessingUid());
            }
        } finally {
            unitProcessingLock.unlock();
        }

        if (latch != null) {
            Logger.d(TAG, "Awaiting on latch: 0x" + Integer.toHexString(latch.hashCode()));

            latch.await(MAX_AWAITING_TIMEOUT_IN_SEC, TimeUnit.SECONDS);
        }

        Logger.d(TAG, "Waiting finished: fileUid=" + fileUid +
                ", remotePath=" + remotePath);
    }

    private OperationResult<OutputStream> processFileUploading(RemoteFile file,
                                                               UUID processingUnitUid) {
        OperationResult<OutputStream> result = new OperationResult<>();

        try {
            result.setObj(new RemoteFileOutputStream(this, client, file, processingUnitUid));

            if (file.getId() != null) {
                cache.update(file);
            } else {
                cache.put(file);
            }
        } catch (FileNotFoundException e) {
            result.setError(newGenericIOError(String.format(ERROR_FAILED_TO_FIND_FILE,
                    file.getLocalPath())));
        }

        return result;
    }

    public void onFileUploadFailed(RemoteFile file, UUID processingUnitUid) {
        Logger.d(TAG, "onFileUploadFailed: unitUid=%s, file=%s", processingUnitUid, file);

        file.setUploadFailed(true);

        cache.update(file);

        onFinishProcessingUnit(processingUnitUid);
    }

    public void onFileUploadFinished(RemoteFile file, RemoteFileMetadata metadata,
                                     UUID processingUnitUid) {
        Logger.d(TAG, "onFileUploadFinished: unitUid=%s, file=%s", processingUnitUid, file);

        Long modifiedTimestamp = anyLastTimestamp(metadata.getServerModified(), metadata.getClientModified());

        file.setUploadFailed(false);
        file.setLocallyModified(false);
        file.setUploaded(true);
        file.setLastModificationTimestamp(modifiedTimestamp);
        file.setLastRemoteModificationTimestamp(metadata.getServerModified().getTime());
        file.setLastDownloadTimestamp(System.currentTimeMillis());
        file.setRevision(metadata.getRevision());
        file.setUid(metadata.getUid());
        file.setRemotePath(metadata.getPath());

        cache.update(file);

        onFinishProcessingUnit(processingUnitUid);
    }

    public void onOfflineWriteFailed(RemoteFile file, UUID processingUnitUid) {
        Logger.d(TAG, "onOfflineWriteFailed: unitUid=%s, file=%s", processingUnitUid, file);

        cache.update(file);

        onFinishProcessingUnit(processingUnitUid);
    }

    public void onOfflineWriteFinished(RemoteFile file, UUID processingUnitUid) {
        Logger.d(TAG, "onOfflineWriteFinished: unitUid=%s, file=%s", processingUnitUid, file);

        cache.update(file);

        onFinishProcessingUnit(processingUnitUid);
    }

    private boolean startProcessingUnit(ProcessingUnit unit) {
        boolean unitStarted = false;

        unitProcessingLock.lock();

        Logger.d(TAG, "Starting processing unit: %s", unit);

        try {
            boolean running = true;
            while (running) {
                ProcessingUnit runningUnit = findProcessingUnit(unit.getFileUid(), unit.getRemotePath());
                if (runningUnit == null) {
                    processingMap.put(unit);
                    unitStarted = true;
                    running = false;
                } else {
                    unitProcessingLock.unlock();

                    try {
                        awaitProcessingUnitFinish(unit.getFileUid(), unit.getRemotePath());

                        unitProcessingLock.lock();
                    } catch (InterruptedException e) {
                        Logger.d(TAG, "Can't await job finish, timeout has occurred.");
                        running = false;
                    }
                }
            }
        } finally {
            unitProcessingLock.unlock();
        }

        return unitStarted;
    }

    private void onFinishProcessingUnit(UUID processingUid) {
        CountDownLatch latch = null;

        unitProcessingLock.lock();
        try {
            processingMap.remove(processingUid);

            if (processingUidToLatch.containsKey(processingUid)) {
                latch = processingUidToLatch.remove(processingUid);
            }
        } finally {
            unitProcessingLock.unlock();
        }

        if (latch != null) {
            latch.countDown();
        }
    }

    private boolean canResolveMergeConflict(Date localModified,
                                            Date serverModified,
                                            Date clientModified,
                                            OnConflictStrategy onConflictStrategy) {
        boolean result;

        Date lastServerModified = DateUtils.anyLast(serverModified, clientModified);
        if (lastServerModified != null && localModified != null) {
            if (localModified.after(lastServerModified)) {
                result = true;
            } else {
                result = (onConflictStrategy == OnConflictStrategy.REWRITE);
            }
        } else {
            result = true;
        }

        return result;
    }

    @NonNull
    @Override
    public OperationResult<Boolean> exists(@NonNull FileDescriptor file) {
        OperationResult<Boolean> result = new OperationResult<>();

        try {
            client.getFileMetadataOrThrow(file);
            result.setObj(true);
        } catch (RemoteFSFileNotFoundException e) {
            result.setObj(false);
        } catch (RemoteFSException e) {
            Logger.printStackTrace(e);
            result.setError(createOperationErrorFromException(e));
        }

        return result;
    }

    @NonNull
    @Override
    public FileSystemSyncProcessor getSyncProcessor() {
        return syncProcessor;
    }
}
