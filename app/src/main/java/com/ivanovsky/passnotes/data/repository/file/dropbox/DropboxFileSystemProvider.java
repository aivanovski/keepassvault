package com.ivanovsky.passnotes.data.repository.file.dropbox;

import com.dropbox.core.v2.files.FileMetadata;
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

	private final DropboxAuthenticator authenticator;
	private final DropboxClient dropboxClient;
	private final DropboxCache cache;

	public DropboxFileSystemProvider(SettingsRepository settings,
									 DropboxFileRepository dropboxFileRepository) {
		this.authenticator = new DropboxAuthenticator(settings);
		this.dropboxClient = new DropboxClient(authenticator);
		this.cache = new DropboxCache(dropboxFileRepository);
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
					if (!cachedFile.isModifiedLocally()) {
						// server has new version of db
						metadata = dropboxClient.downloadFileOrThrow(remotePath, cachedFile.getLocalPath());

						cachedFile.setRemotePath(metadata.getPathLower());
						cachedFile.setRevision(metadata.getRev());
						cachedFile.setUploaded(true);
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
					cachedFile.setRetryCount(0);
					cachedFile.setLastRetryTimestamp(null);

					cache.update(cachedFile);

					result.setObj(new FileInputStream(cachedFile.getLocalPath()));
				}
			} catch (FileNotFoundException e) {
				result.setError(newGenericIOError(MESSAGE_FAILED_TO_FIND_FILE));

			} catch (DropboxNetworkException e) {
				// use cached file
				DropboxFile cachedFile = cache.findByPath(file.getPath());
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
//		DropboxFileOutputStream result;
//
//		Metadata metadata;
//		try {
//			metadata = client.files().getMetadata(formatDropboxPath(file.getPath()));
//		} catch (GetMetadataErrorException e) {
//			if (e.getMessage().contains(PATH_NOT_FOUND_MESSAGE)) {
//				metadata = null;
//			} else {
//				throw new FileSystemException(String.format(ERROR_FAILED_TO_GET_FILE_METADATA, file.getPath()));
//			}
//
//		} catch (NetworkIOException e) {
//			throw new IOFileSystemException(e);
//
//		} catch (DbxException e) {
//			throw new FileSystemException(e);
//		}
//
//		FileMetadata fileMetadata = (metadata instanceof FileMetadata) ? (FileMetadata) metadata : null;
//
//		File destinationDir = FileUtils.getRemoteFilesDir(App.getInstance());
//		if (destinationDir == null) {
//			throw new FileSystemException(ERROR_FAILED_TO_GET_FILE_METADATA);
//		}
//
//		if (fileMetadata == null) {
//			//upload new file
//			String parentPath = getParentPath(file.getPath());
//			if (parentPath == null) {
//				throw new FileSystemException(ERROR_INCORRECT_FILE_PATH);
//			}
//
//			if (!parentPath.equals("/")) {
//				Metadata parentMetadata;
//
//				try {
//					parentMetadata = client.files().getMetadata(formatDropboxPath(parentPath));
//				} catch (GetMetadataErrorException e) {
//					if (e.getMessage().contains(PATH_NOT_FOUND_MESSAGE)) {
//						throw new FileSystemException(String.format(ERROR_FAILED_TO_GET_FILE_METADATA, parentPath));
//					} else {
//						throw new FileSystemException(e);
//					}
//				} catch (DbxException e) {
//					Logger.printStackTrace(e);
//					throw new FileSystemException(e);
//				}
//
//				if (!(parentMetadata instanceof FolderMetadata)) {
//					throw new FileSystemException(String.format(ERROR_SPECIFIED_FILE_HAS_INCORRECT_METADATA, parentPath));
//				}
//
//				FolderMetadata parentFolderMetadata = (FolderMetadata) parentMetadata;
//				parentPath = parentFolderMetadata.getPathLower();
//			}
//
//			DropboxFileLink link = new DropboxFileLink();
//
//			link.setLastModificationTimestamp(file.getModified());
//			link.setLocalPath(generateDestinationFilePath(destinationDir));
//			link.setRemotePath(parentPath + "/" + file.getName());
//
//			dropboxLinkRepository.insert(link);
//
//			try {
//				result = new DropboxFileOutputStream(this, client, link);
//
//				Logger.d(TAG, "Uploading file to Dropbox: from " + link.getLocalPath() +
//						" to " + link.getRemotePath());
//
//				onFileUploadStarted(link);
//			} catch (IOException e) {
//				Logger.printStackTrace(e);
//				throw new FileSystemException(String.format(ERROR_FAILED_TO_FIND_FILE, link.getLocalPath()));
//			}
//
//		} else {
//			//re-write existing file
//			String uid = fileMetadata.getId();
//			String revision = fileMetadata.getRev();
//			String remotePath = fileMetadata.getPathLower();
//			Date localModified = new Date(file.getModified());
//			Date serverModified = fileMetadata.getClientModified();
//			Date clientModified = fileMetadata.getClientModified();
//
//			DropboxFileLink link = dropboxLinkRepository.findByUid(uid);
//
//			if (!canResolveMergeConflict(localModified, serverModified, clientModified)) {
//				if (link == null) {
//					link = new DropboxFileLink();
//
//					link.setUid(uid);
//					link.setRemotePath(remotePath);
//					link.setLocalPath(generateDestinationFilePath(destinationDir));
//					link.setRevision(revision);
//					link.setLastModificationTimestamp(file.getModified());
//
//					dropboxLinkRepository.insert(link);
//				}
//
//				throw new ModificationConflictException(link.getId(),
//						file.getModified(),
//						(serverModified != null) ? serverModified.getTime() : null,
//						(clientModified != null) ? clientModified.getTime() : null);
//			}
//
//			if (link == null) {
//				link = new DropboxFileLink();
//
//				link.setUid(uid);
//				link.setRemotePath(remotePath);
//				link.setLocalPath(generateDestinationFilePath(destinationDir));
//				link.setRevision(revision);
//				link.setLastModificationTimestamp(file.getModified());
//
//				dropboxLinkRepository.insert(link);
//
//			} else {
//				link.setRevision(revision);
//				link.setLastModificationTimestamp(file.getModified());
//			}
//
//			try {
//				result = new DropboxFileOutputStream(this, client, link);
//
//				Logger.d(TAG, "Uploading file to Dropbox: from " + link.getLocalPath() +
//						" to " + link.getRemotePath());
//
//				onFileUploadStarted(link);
//			} catch (IOException e) {
//				Logger.printStackTrace(e);
//
//				throw new FileSystemException(String.format(ERROR_FAILED_TO_FIND_FILE, link.getLocalPath()));
//			}
//		}
//
//		return OperationResult.success(result);
		return OperationResult.success(null);
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
