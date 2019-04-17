package com.ivanovsky.passnotes.data.repository.file.dropbox;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.Metadata;
import com.ivanovsky.passnotes.App;
import com.ivanovsky.passnotes.data.entity.DropboxFile;
import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationError;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.DropboxFileRepository;
import com.ivanovsky.passnotes.data.repository.SettingsRepository;
import com.ivanovsky.passnotes.data.repository.file.FSType;
import com.ivanovsky.passnotes.data.repository.file.FileSystemAuthenticator;
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider;
import com.ivanovsky.passnotes.data.repository.file.dropbox.exception.DropboxApiException;
import com.ivanovsky.passnotes.data.repository.file.dropbox.exception.DropboxAuthException;
import com.ivanovsky.passnotes.data.repository.file.dropbox.exception.DropboxException;
import com.ivanovsky.passnotes.data.repository.file.dropbox.exception.DropboxNetworkException;
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
import static com.ivanovsky.passnotes.data.entity.OperationError.newAuthError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newDbVersionConflictError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newNetworkIOError;
import static com.ivanovsky.passnotes.util.DateUtils.anyLastTimestamp;
import static com.ivanovsky.passnotes.util.ObjectUtils.isNotEquals;

public class DropboxFileSystemProvider implements FileSystemProvider {

	private static final String TAG = DropboxFileSystemProvider.class.getSimpleName();

	private static final String ERROR_FAILED_TO_FIND_APP_PRIVATE_DIR = "Failed to find app private dir";
	private static final String ERROR_FAILED_TO_FIND_FILE = "Failed to find file: %s";
	private static final String ERROR_FAILED_TO_FIND_FILE_IN_CACHE = "Faile to find file in cache: %s";

	private static final long MAX_AWAITING_TIMEOUT_IN_SEC = 30;

	private final DropboxAuthenticator authenticator;
	private final DropboxClient dropboxClient;
	private final DropboxCache cache;
	private final StatusMap processingMap;
	private final Map<UUID, CountDownLatch> processingUidToLatch;
	private final Lock unitProcessingLock;

	public DropboxFileSystemProvider(SettingsRepository settings,
									 DropboxFileRepository dropboxFileRepository) {
		this.authenticator = new DropboxAuthenticator(settings);
		this.dropboxClient = new DropboxClient(authenticator);
		this.cache = new DropboxCache(dropboxFileRepository);
		this.processingMap = new StatusMap();
		this.processingUidToLatch = new HashMap<>();
		this.unitProcessingLock = new ReentrantLock();
	}

	@Override
	public FileSystemAuthenticator getAuthenticator() {
		return authenticator;
	}

	@Override
	public OperationResult<List<FileDescriptor>> listFiles(FileDescriptor dir) {
		OperationResult<List<FileDescriptor>> result = new OperationResult<>();

		try {
			result.setObj(dropboxClient.listFiles(dir));
		} catch (DropboxException e) {
			result.setError(createOperationErrorFromDropboxException(e));
		}

		return result;
	}

	private OperationError createOperationErrorFromDropboxException(DropboxException exception) {
		OperationError result;

		if (exception instanceof DropboxAuthException) {
			result = newAuthError(exception.getMessage());
		} else if (exception instanceof DropboxApiException) {
			result = newGenericIOError(exception.getMessage());
		} else if (exception instanceof DropboxNetworkException) {
			result = newNetworkIOError();
		} else {
			throw new IllegalArgumentException("Exception handling is not implemented");
		}

		return result;
	}

	@Override
	public OperationResult<FileDescriptor> getParent(FileDescriptor file) {
		OperationResult<FileDescriptor> result = new OperationResult<>();

		try {
			result.setObj(dropboxClient.getParent(file));
		} catch (DropboxException e) {
			result.setError(createOperationErrorFromDropboxException(e));
		}

		return result;
	}

	@Override
	public OperationResult<FileDescriptor> getRootFile() {
		OperationResult<FileDescriptor> result = new OperationResult<>();

		try {
			result.setObj(dropboxClient.getRoot());
		} catch (DropboxException e) {
			result.setError(createOperationErrorFromDropboxException(e));
		}

		return result;
	}

	@Override
	public OperationResult<FileDescriptor> getFile(String path) {
		OperationResult<FileDescriptor> result = new OperationResult<>();

		try {
			FileMetadata metadata = dropboxClient.getFileMetadataOrThrow(newDescriptorFromPath(path));
			result.setObj(newDescriptorFromMetadata(metadata));
		} catch (DropboxNetworkException e) {
			DropboxFile file = cache.getByRemotPath(path);

			if (file != null) {
				result.setDeferredObj(newDescriptorFromDropboxFile(file));
			} else {
				result.setError(newGenericIOError(MESSAGE_FAILED_TO_FIND_FILE));
			}
		} catch (DropboxException e) {
			result.setError(createOperationErrorFromDropboxException(e));
		}

		return result;
	}

	private FileDescriptor newDescriptorFromPath(String path) {
		FileDescriptor result = new FileDescriptor();

		result.setFsType(FSType.DROPBOX);
		result.setPath(path);

		return result;
	}

	private FileDescriptor newDescriptorFromMetadata(FileMetadata metadata) {
		FileDescriptor result = new FileDescriptor();

		result.setFsType(FSType.DROPBOX);
		result.setUid(metadata.getId());
		result.setPath(metadata.getPathLower());
		result.setDirectory(false);
		result.setModified(anyLastTimestamp(metadata.getServerModified(),
				metadata.getClientModified()));

		return result;
	}

	private FileDescriptor newDescriptorFromDropboxFile(DropboxFile file) {
		FileDescriptor result = new FileDescriptor();

		result.setFsType(FSType.DROPBOX);
		result.setUid(file.getUid());
		result.setPath(file.getRemotePath());
		result.setDirectory(false);
		result.setModified(file.getLastModificationTimestamp());

		return result;
	}

	@Override
	public OperationResult<InputStream> openFileForRead(FileDescriptor file) {
		OperationResult<InputStream> result = new OperationResult<>();

		File destinationDir = FileUtils.getRemoteFilesDir(App.getInstance());
		if (destinationDir != null) {
			ProcessingUnit unit = null;
			try {
				FileMetadata metadata = dropboxClient.getFileMetadataOrThrow(file);

				String uid = metadata.getId();
				String remoteRevision = metadata.getRev();
				String remotePath = metadata.getPathLower();

				DropboxFile cachedFile = cache.getByUid(uid);
				if (cachedFile == null) {
					// download file and add new entry to the cache
					String destinationPath = generateDestinationFilePath(destinationDir);

					unit = new ProcessingUnit(UUID.randomUUID(),
							ProcessingStatus.DOWNLOADING,
							uid,
							remotePath);

					onStartProcessingUnit(unit);

					Logger.d(TAG, "Downloading new file: remote=" + remotePath +
							", local=" + destinationPath);

					metadata = dropboxClient.downloadFileOrThrow(remotePath, destinationPath);

					cachedFile = new DropboxFile();

					cachedFile.setUid(metadata.getId());
					cachedFile.setRemotePath(metadata.getPathLower());
					cachedFile.setLocalPath(destinationPath);
					cachedFile.setRevision(metadata.getRev());
					cachedFile.setUploaded(true);
					cachedFile.setLastModificationTimestamp(
							anyLastTimestamp(metadata.getServerModified(), metadata.getClientModified()));
					cachedFile.setLastDownloadTimestamp(System.currentTimeMillis());

					cache.put(cachedFile);

					result.setObj(new FileInputStream(destinationPath));

				} else if (isNotEquals(remoteRevision, cachedFile.getRevision())) {
					if (!cachedFile.isLocallyModified()) {
						// server has new version of db
						unit = new ProcessingUnit(UUID.randomUUID(),
								ProcessingStatus.DOWNLOADING,
								uid,
								remotePath);

						onStartProcessingUnit(unit);

						Logger.d(TAG, "Updating cached file: remote=" + remotePath +
								", local=" + cachedFile.getLocalPath());

						metadata = dropboxClient.downloadFileOrThrow(remotePath, cachedFile.getLocalPath());

						cachedFile.setRemotePath(metadata.getPathLower());
						cachedFile.setRevision(metadata.getRev());
						cachedFile.setUploaded(true);
						cachedFile.setUploadFailed(false);
						cachedFile.setLocallyModified(false);
						cachedFile.setLastModificationTimestamp(
								anyLastTimestamp(metadata.getServerModified(), metadata.getClientModified()));
						cachedFile.setLastDownloadTimestamp(System.currentTimeMillis());
						cachedFile.setRetryCount(0);
						cachedFile.setLastRetryTimestamp(null);

						cache.update(cachedFile);

						result.setObj(new FileInputStream(cachedFile.getLocalPath()));

					} else {
						// user modified db
						result.setError(newDbVersionConflictError(MESSAGE_LOCAL_VERSION_CONFLICTS_WITH_REMOTE));
					}
				} else {
					// local revision is the same as in the server
					Logger.d(TAG, "Local cached file is up to date: remote=" + remotePath +
							", local=" + cachedFile.getLocalPath());

					cachedFile.setRemotePath(metadata.getPathLower());
					cachedFile.setLastModificationTimestamp(
							anyLastTimestamp(metadata.getServerModified(), metadata.getClientModified()));
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

			} catch (DropboxNetworkException e) {
				// use cached file
				DropboxFile cachedFile = cache.getByUid(file.getUid());
				if (cachedFile != null) {
					unit = new ProcessingUnit(UUID.randomUUID(),
							ProcessingStatus.DOWNLOADING,
							cachedFile.getUid(),
							cachedFile.getRemotePath());

					onStartProcessingUnit(unit);

					InputStream fileStream = newFileInputStreamOrNull(cachedFile.getLocalPath());
					if (fileStream != null) {
						result.setDeferredObj(fileStream);
					} else {
						result.setError(newGenericIOError(MESSAGE_FAILED_TO_FIND_FILE));
					}
				} else {
					result.setError(newNetworkIOError());
				}

			} catch (DropboxException e) {
				result.setError(createOperationErrorFromDropboxException(e));
			}

			if (unit != null) {
				onFinishProcessingUnit(unit.processingUid);
			}
		} else {
			result.setError(newGenericIOError(ERROR_FAILED_TO_FIND_APP_PRIVATE_DIR));
		}

		return result;
	}

	private InputStream newFileInputStreamOrNull(String path) {
		InputStream result = null;

		try {
			result = new FileInputStream(path);
		} catch (FileNotFoundException e) {
			Logger.printStackTrace(e);
		}

		return result;
	}

	private String generateDestinationFilePath(File dir) {
		// TODO: use method from FileUtils.java
		return dir.getPath() + "/" + UUID.randomUUID().toString();
	}

	@Override
	public OperationResult<OutputStream> openFileForWrite(FileDescriptor file) {
		OperationResult<OutputStream> result = new OperationResult<>();

		File destinationDir = FileUtils.getRemoteFilesDir(App.getInstance());
		if (destinationDir != null) {
			try {
				FileMetadata metadata = dropboxClient.getFileMetadataOrNull(file);
				if (metadata == null) {
					// create and upload new file
					String parentPath = FileUtils.getParentPath(file.getPath());
					if (!"/".equals(parentPath)) {
						FolderMetadata parentMetadata = dropboxClient.getFolderMetadataOrThrow(parentPath);
						parentPath = parentMetadata.getPathLower();
					}

					DropboxFile cachedFile = new DropboxFile();

					long timestamp = System.currentTimeMillis();

					cachedFile.setRemotePath(parentPath + "/" + file.getName());
					cachedFile.setLocalPath(generateDestinationFilePath(destinationDir));
					cachedFile.setLastModificationTimestamp(timestamp);
					cachedFile.setLastDownloadTimestamp(timestamp);
					cachedFile.setLocallyModified(true);

					ProcessingUnit unit = new ProcessingUnit(UUID.randomUUID(),
							ProcessingStatus.UPLOADING,
							cachedFile.getUid(),
							cachedFile.getRemotePath());

					onStartProcessingUnit(unit);

					result.from(processFileUploading(cachedFile, unit.processingUid));

					if (result.isFailed()) {
						onFinishProcessingUnit(unit.processingUid);
					}
				} else {
					// re-write existing file
					String uid = metadata.getId();
					String remotePath = metadata.getPathLower();
					Date localModified = new Date(file.getModified());
					Date serverModified = metadata.getServerModified();
					Date clientModified = metadata.getClientModified();

					DropboxFile cachedFile = cache.getByUid(uid);
					if (canResolveMergeConflict(localModified, serverModified, clientModified)) {
						if (cachedFile == null) {
							cachedFile = new DropboxFile();

							cachedFile.setRemotePath(remotePath);
							cachedFile.setLocalPath(generateDestinationFilePath(destinationDir));
							cachedFile.setLastModificationTimestamp(localModified.getTime());
							cachedFile.setLastDownloadTimestamp(localModified.getTime());
							cachedFile.setLocallyModified(true);

							ProcessingUnit unit = new ProcessingUnit(UUID.randomUUID(),
									ProcessingStatus.UPLOADING,
									cachedFile.getUid(),
									cachedFile.getRemotePath());

							onStartProcessingUnit(unit);

							result.from(processFileUploading(cachedFile, unit.processingUid));

							if (result.isFailed()) {
								onFinishProcessingUnit(unit.processingUid);
							}
						} else {
							cachedFile.setRemotePath(remotePath);
							cachedFile.setLastModificationTimestamp(localModified.getTime());
							cachedFile.setLastDownloadTimestamp(localModified.getTime());
							cachedFile.setLocallyModified(true);
							cachedFile.setUploaded(false);

							ProcessingUnit unit = new ProcessingUnit(UUID.randomUUID(),
									ProcessingStatus.UPLOADING,
									cachedFile.getUid(),
									cachedFile.getRemotePath());

							onStartProcessingUnit(unit);

							result.from(processFileUploading(cachedFile, unit.processingUid));

							if (result.isFailed()) {
								onFinishProcessingUnit(unit.processingUid);
							}
						}
					} else {
						result.setError(newDbVersionConflictError(MESSAGE_LOCAL_VERSION_CONFLICTS_WITH_REMOTE));
					}
				}
			} catch (DropboxNetworkException e) {
				// if device is offline, just write to the local file
				DropboxFile cachedFile = cache.getByUid(file.getUid());
				if (cachedFile != null) {
					cachedFile.setLastModificationTimestamp(file.getModified());
					cachedFile.setLocallyModified(true);
					cachedFile.setUploaded(false);

					ProcessingUnit unit = new ProcessingUnit(UUID.randomUUID(),
							ProcessingStatus.UPLOADING,
							cachedFile.getUid(),
							cachedFile.getRemotePath());

					onStartProcessingUnit(unit);

					try {
						result.setObj(new OfflineFileOutputStream(this, cachedFile, unit.processingUid));

						cache.update(cachedFile);
					} catch (FileNotFoundException ee) {
						result.setError(newGenericIOError(String.format(ERROR_FAILED_TO_FIND_FILE,
								cachedFile.getLocalPath())));

						onFinishProcessingUnit(unit.processingUid);
					}

				} else {
					// by some reason file is not in cache, return error
					result.setError(newGenericIOError(String.format(ERROR_FAILED_TO_FIND_FILE_IN_CACHE,
							file.toString())));
				}
			} catch (DropboxException e) {
				result.setError(createOperationErrorFromDropboxException(e));
			}
		} else {
			result.setError(newGenericIOError(ERROR_FAILED_TO_FIND_APP_PRIVATE_DIR));
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

	private void awaitProcessingUnitFinish(String fileUid, String remotePath) throws InterruptedException {
		ProcessingUnit unit = findProcessingUnit(fileUid, remotePath);

		CountDownLatch latch = null;

		Logger.d(TAG, "Waiting until operation finished: fileUid=" + fileUid +
				", remotePath=" + remotePath);

		unitProcessingLock.lock();
		try {
			if (!processingUidToLatch.containsKey(unit.processingUid)) {
				latch = new CountDownLatch(1);

				processingUidToLatch.put(unit.processingUid, latch);
			}
		} finally {
			unitProcessingLock.unlock();
		}

		if (latch != null) {
			latch.await(MAX_AWAITING_TIMEOUT_IN_SEC, TimeUnit.SECONDS);
		}

		Logger.d(TAG, "Waiting finished: fileUid=" + fileUid +
				", remotePath=" + remotePath);
	}

	private OperationResult<OutputStream> processFileUploading(DropboxFile file, UUID processingUnitUid) {
		OperationResult<OutputStream> result = new OperationResult<>();

		try {
			result.setObj(new DropboxFileOutputStream(this, dropboxClient, file, processingUnitUid));

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

	void onFileUploadFailed(DropboxFile file, UUID processingUnitUid) {
		file.setUploadFailed(true);

		cache.update(file);

		onFinishProcessingUnit(processingUnitUid);
	}

	void onFileUploadFinished(DropboxFile file, FileMetadata metadata, UUID processingUnitUid) {
		file.setUploadFailed(false);
		file.setLocallyModified(false);
		file.setUploaded(true);
		file.setLastModificationTimestamp(
				anyLastTimestamp(metadata.getServerModified(), metadata.getClientModified()));
		file.setLastDownloadTimestamp(System.currentTimeMillis());
		file.setRevision(metadata.getRev());
		file.setUid(metadata.getId());
		file.setRemotePath(metadata.getPathLower());

		cache.update(file);

		onFinishProcessingUnit(processingUnitUid);
	}

	void onOfflineWriteFailed(DropboxFile file, UUID processingUnitUid) {
		cache.update(file);

		onFinishProcessingUnit(processingUnitUid);
	}

	void onOfflineWriteFinished(DropboxFile file, UUID processingUnitUid) {
		cache.update(file);

		onFinishProcessingUnit(processingUnitUid);
	}

	private void onStartProcessingUnit(ProcessingUnit unit) {
		unitProcessingLock.lock();

		try {
			boolean running = true;
			while (running) {
				ProcessingUnit runningUnit = findProcessingUnit(unit.fileUid, unit.remotePath);
				if (runningUnit == null) {
					processingMap.put(unit);
					running = false;
				} else {
					unitProcessingLock.unlock();

					try {
						awaitProcessingUnitFinish(unit.fileUid, unit.remotePath);

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
	}

	private void onFinishProcessingUnit(UUID processingUid) {
		unitProcessingLock.lock();
		try {
			processingMap.remove(processingUid);

			if (processingUidToLatch.containsKey(processingUid)) {

			}

		} finally {
			unitProcessingLock.unlock();
		}

		// TODO: fix


		if (processingUidToLatch.containsKey(processingUid)) {
			CountDownLatch latch;



			unitProcessingLock.lock();
			try {
				latch = processingUidToLatch.get(processingUid);
				if (latch != null) {
					processingUidToLatch.remove(processingUid);
				}
			} finally {
				unitProcessingLock.unlock();
			}

			if (latch != null) {
				latch.countDown();
			}
		}
	}

	private boolean canResolveMergeConflict(Date localModified,
											Date serverModified,
											Date clientModified) {
		boolean result;

		Date server = DateUtils.anyLast(serverModified, clientModified);
		if (server != null && localModified != null) {
			result = localModified.after(server);
		} else {
			result = true;
		}

		return result;
	}

	@Override
	public OperationResult<Boolean> exists(FileDescriptor file) {
		OperationResult<Boolean> result = new OperationResult<>();

		try {
			Metadata metadata = dropboxClient.getFileMetadataOrNull(file);
			result.setObj(metadata != null);
		} catch (DropboxException e) {
			Logger.printStackTrace(e);
			result.setError(createOperationErrorFromDropboxException(e));
		}

		return result;
	}
}
