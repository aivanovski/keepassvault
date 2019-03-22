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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

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

	private final DropboxAuthenticator authenticator;
	private final DropboxClient dropboxClient;
	private final DropboxCache cache;
	private final List<DropboxFile> uploadingFiles;
	private final List<DropboxFile> offlineFiles;

	public DropboxFileSystemProvider(SettingsRepository settings,
									 DropboxFileRepository dropboxFileRepository) {
		this.authenticator = new DropboxAuthenticator(settings);
		this.dropboxClient = new DropboxClient(authenticator);
		this.cache = new DropboxCache(dropboxFileRepository);
		this.uploadingFiles = new CopyOnWriteArrayList<>();
		this.offlineFiles = new CopyOnWriteArrayList<>();
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
	public OperationResult<InputStream> openFileForRead(FileDescriptor file) {
		OperationResult<InputStream> result = new OperationResult<>();

		File destinationDir = FileUtils.getRemoteFilesDir(App.getInstance());
		if (destinationDir != null) {
			try {
				FileMetadata metadata = dropboxClient.getFileMetadataOrThrow(file);

				String uid = metadata.getId();
				String remoteRevision = metadata.getRev();
				String remotePath = metadata.getPathLower();

				DropboxFile cachedFile = cache.getByUid(uid);
				if (cachedFile == null) {
					// download file and add new entry to the cache
					String destinationPath = generateDestinationFilePath(destinationDir);

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
				DropboxFile cachedFile = cache.findByPath(file.getPath());// TODO: maybe cachedFile should be found by uid
				if (cachedFile != null) {
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

					try {
						result.setObj(new DropboxFileOutputStream(this, dropboxClient, cachedFile));

						cache.put(cachedFile);
					} catch (FileNotFoundException e) {
						result.setError(newGenericIOError(String.format(ERROR_FAILED_TO_FIND_FILE,
								cachedFile.getLocalPath())));
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

							try {
								result.setObj(new DropboxFileOutputStream(this, dropboxClient, cachedFile));

								cache.put(cachedFile);
							} catch (FileNotFoundException e) {
								result.setError(newGenericIOError(String.format(ERROR_FAILED_TO_FIND_FILE,
										cachedFile.getLocalPath())));
							}
						} else {
							cachedFile.setRemotePath(remotePath);
							cachedFile.setLastModificationTimestamp(localModified.getTime());
							cachedFile.setLastDownloadTimestamp(localModified.getTime());
							cachedFile.setLocallyModified(true);
							cachedFile.setUploaded(false);

							try {
								result.setObj(new DropboxFileOutputStream(this, dropboxClient, cachedFile));

								cache.update(cachedFile);
							} catch (FileNotFoundException e) {
								result.setError(newGenericIOError(String.format(ERROR_FAILED_TO_FIND_FILE,
										cachedFile.getLocalPath())));
							}
						}
					} else {
						result.setError(newDbVersionConflictError(MESSAGE_LOCAL_VERSION_CONFLICTS_WITH_REMOTE));
					}
				}
			} catch (DropboxNetworkException e) {
				// if device is offline, just write to the local file
				DropboxFile cachedFile = cache.findByUid(file.getUid());
				if (cachedFile != null) {

					cachedFile.setLastModificationTimestamp(file.getModified());
					cachedFile.setLocallyModified(true);
					cachedFile.setUploaded(false);

					try {
						result.setObj(new OfflineFileOutputStream(this, cachedFile));

						cache.update(cachedFile);
					} catch (FileNotFoundException ee) {
						result.setError(newGenericIOError(String.format(ERROR_FAILED_TO_FIND_FILE,
								cachedFile.getLocalPath())));
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

	void onFileUploadFailed(DropboxFile file) {
		// TODO
		file.setUploadFailed(true);

		cache.update(file);
	}

	void onFileUploadFinished(DropboxFile file, FileMetadata metadata) {
		file.setLocallyModified(false);
		file.setUploaded(true);
		file.setLastModificationTimestamp(
				anyLastTimestamp(metadata.getServerModified(), metadata.getClientModified()));
		file.setLastDownloadTimestamp(System.currentTimeMillis());

		cache.update(file);
	}

	void onOfflineWriteFailed(DropboxFile file) {
		// TODO
	}

	void onOfflineWriteFinished(DropboxFile file) {
		// TODO

		cache.update(file);
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
