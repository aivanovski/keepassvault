package com.ivanovsky.passnotes.data.repository.file.dropbox;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.NetworkIOException;
import com.dropbox.core.http.OkHttp3Requestor;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.GetMetadataErrorException;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.ivanovsky.passnotes.App;
import com.ivanovsky.passnotes.BuildConfig;
import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationError;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.entity.DropboxFileLink;
import com.ivanovsky.passnotes.data.repository.DropboxFileLinkRepository;
import com.ivanovsky.passnotes.data.repository.SettingsRepository;
import com.ivanovsky.passnotes.data.repository.file.FSType;
import com.ivanovsky.passnotes.data.repository.file.FileSystemAuthenticator;
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider;
import com.ivanovsky.passnotes.data.repository.file.exception.AuthException;
import com.ivanovsky.passnotes.data.repository.file.exception.FileSystemException;
import com.ivanovsky.passnotes.data.repository.file.exception.IOFileSystemException;
import com.ivanovsky.passnotes.data.repository.file.exception.ModificationConflictException;
import com.ivanovsky.passnotes.util.DateUtils;
import com.ivanovsky.passnotes.util.FileUtils;
import com.ivanovsky.passnotes.util.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static com.ivanovsky.passnotes.data.entity.OperationError.newAuthError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newNetworkIOError;
import static com.ivanovsky.passnotes.util.DateUtils.anyLastTimestamp;
import static com.ivanovsky.passnotes.util.FileUtils.getParentPath;
import static com.ivanovsky.passnotes.util.ObjectUtils.isNotEquals;

public class DropboxFileSystemProvider implements FileSystemProvider {

	private static final String TAG = DropboxFileSystemProvider.class.getSimpleName();

	private static final String PATH_NOT_FOUND_MESSAGE = "{\".tag\":\"path\",\"path\":\"not_found\"}";

	private static final String ERROR_FAILED_TO_GET_FILE_METADATA = "Failed to get file metadata: %s";
	private static final String ERROR_SPECIFIED_FILE_HAS_INCORRECT_METADATA = "Specified file has incorrect metadata: %s";
	private static final String ERROR_FAILED_TO_FIND_APP_PRIVATE_DIR = "Failed to find app private dir";
	private static final String ERROR_INCORRECT_FILE_PATH = "Incorrect file path";
	private static final String ERROR_FAILED_TO_FIND_FILE = "Failed to find file: %s";


	private final DropboxAuthenticator authenticator;
	private final DropboxFileLinkRepository dropboxLinkRepository;
	private DbxClientV2 client;
	private DbxRequestConfig config;
	private final List<DropboxFileLink> uploadingFiles;

	public DropboxFileSystemProvider(SettingsRepository settings,
									 DropboxFileLinkRepository dropboxLinkRepository) {
		this.dropboxLinkRepository = dropboxLinkRepository;
		authenticator = new DropboxAuthenticator(settings);
		config = DbxRequestConfig.newBuilder("Passnotes/Android")
				.withHttpRequestor(new OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
				.build();

		uploadingFiles = new LinkedList<>();
	}

	@Override
	public FileSystemAuthenticator getAuthenticator() {
		return authenticator;
	}

	@Override
	public OperationResult<List<FileDescriptor>> listFiles(FileDescriptor dir) {
		OperationResult<List<FileDescriptor>> result = new OperationResult<>();

		String authToken = authenticator.getAuthToken();
		if (authToken != null) {
			if (dir.isDirectory()) {
				initClientIfNeed(authToken);

				try {
					ListFolderResult remoteFiles = client.files().listFolder(formatDropboxPath(dir.getPath()));
					if (remoteFiles != null && remoteFiles.getEntries() != null) {
						List<FileDescriptor> files = new ArrayList<>();

						for (Metadata metadata : remoteFiles.getEntries()) {
							if (metadata instanceof FileMetadata) {
								files.add(createDescriptorFromFileMetadata((FileMetadata) metadata));

							} else if (metadata instanceof FolderMetadata) {
								files.add(createDescriptorFromFolderMetadata((FolderMetadata) metadata));
							}
						}

						result.setResult(files);
					} else {
						result.setError(newGenericIOError(OperationError.MESSAGE_FAILED_TO_LOAD_FILE));
					}

				} catch (NetworkIOException e) {
					Logger.printStackTrace(e);
					result.setError(newNetworkIOError());

				} catch (DbxException e) {
					Logger.printStackTrace(e);
					result.setError(newGenericIOError(OperationError.MESSAGE_FAILED_TO_LOAD_FILE));
				}

			} else {
				result.setError(newGenericIOError(OperationError.MESSAGE_FILE_IS_NOT_A_DIRECTORY));
			}

		} else {
			result.setError(newAuthError(OperationError.MESSAGE_AUTH_FAILED));
		}

		return result;
	}

	private void initClientIfNeed(String authToken) {
		if (client == null) {
			client = new DbxClientV2(config, authToken);
		}
	}

	private FileDescriptor createDescriptorFromFileMetadata(FileMetadata metadata) {
		FileDescriptor file = new FileDescriptor();

		file.setFsType(FSType.DROPBOX);
		file.setUid(metadata.getId());
		file.setPath(metadata.getPathDisplay());
		file.setModified(anyLastTimestamp(metadata.getClientModified(),
				metadata.getServerModified()));

		return file;
	}

	private FileDescriptor createDescriptorFromFolderMetadata(FolderMetadata metadata) {
		FileDescriptor file = new FileDescriptor();

		file.setFsType(FSType.DROPBOX);
		file.setUid(metadata.getId());
		file.setPath(metadata.getPathDisplay());
		file.setDirectory(true);

		return file;
	}

	private String formatDropboxPath(String path) {
		String result;

		if ("/".equals(path)) {
			result = "";
		} else {
			result = path;
		}

		return result;
	}

	@Override
	public OperationResult<FileDescriptor> getParent(FileDescriptor file) {
		OperationResult<FileDescriptor> result = new OperationResult<>();

		String authToken = authenticator.getAuthToken();
		if (authToken != null) {
			initClientIfNeed(authToken);

			String parentPath = getParentPath(file.getPath());
			if (parentPath != null) {
				if ("/".equals(parentPath)) {
					result.setResult(makeRootDescriptor());
				} else {
					try {
						Metadata metadata = client.files().getMetadata(formatDropboxPath(parentPath));
						if (metadata instanceof FolderMetadata) {
							result.setResult(createDescriptorFromFolderMetadata((FolderMetadata) metadata));
						} else {
							result.setError(newGenericIOError(OperationError.MESSAGE_FAILED_TO_LOAD_FILE));
						}

					} catch (NetworkIOException e) {
						Logger.printStackTrace(e);
						result.setError(newNetworkIOError());

					} catch (DbxException e) {
						Logger.printStackTrace(e);
						result.setError(newGenericIOError(OperationError.MESSAGE_FILE_DOES_NOT_EXIST));
					}
				}

			} else {
				result.setError(newGenericIOError(OperationError.MESSAGE_FILE_DOES_NOT_EXIST));
			}

		} else {
			result.setError(newAuthError(OperationError.MESSAGE_AUTH_FAILED));
		}

		return result;
	}

	@Override
	public OperationResult<FileDescriptor> getRootFile() {
		OperationResult<FileDescriptor> result = new OperationResult<>();

		String authToken = authenticator.getAuthToken();
		if (authToken != null) {
			initClientIfNeed(authToken);

			try {
				ListFolderResult remoteFiles = client.files().listFolder("");
				if (remoteFiles != null) {
					result.setResult(makeRootDescriptor());
				} else {
					result.setError(newGenericIOError(OperationError.MESSAGE_FAILED_TO_LOAD_FILE));
				}

			} catch (NetworkIOException e) {
				Logger.printStackTrace(e);
				result.setError(newNetworkIOError());

			} catch (DbxException e) {
				Logger.printStackTrace(e);
				result.setError(newGenericIOError(OperationError.MESSAGE_FAILED_TO_LOAD_FILE));
			}

		} else {
			result.setError(newAuthError(OperationError.MESSAGE_AUTH_FAILED));
		}

		return result;
	}

	private FileDescriptor makeRootDescriptor() {
		FileDescriptor root = new FileDescriptor();

		root.setDirectory(true);
		root.setFsType(FSType.DROPBOX);
		root.setRoot(true);
		root.setPath("/");

		return root;
	}

	@Override
	public InputStream openFileForRead(FileDescriptor file) throws FileSystemException {
		InputStream result;

		String authToken = authenticator.getAuthToken();
		if (authToken == null) {
			throw new AuthException();
		}

		initClientIfNeed(authToken);

		Metadata metadata;
		try {
			metadata = client.files().getMetadata(formatDropboxPath(file.getPath()));
		} catch (DbxException e) {
			throw new FileSystemException(e);
		}

		if (metadata == null) {
			throw new FileSystemException(String.format(ERROR_FAILED_TO_GET_FILE_METADATA, file.getPath()));
		}

		if (!(metadata instanceof FileMetadata)) {
			throw new FileSystemException(String.format(ERROR_SPECIFIED_FILE_HAS_INCORRECT_METADATA, file.getPath()));
		}

		File destinationDir = FileUtils.getRemoteFilesDir(App.getInstance());
		if (destinationDir == null) {
			throw new FileSystemException(ERROR_FAILED_TO_FIND_APP_PRIVATE_DIR);
		}

		FileMetadata fileMetadata = (FileMetadata) metadata;

		String uid = fileMetadata.getId();
		String revision = fileMetadata.getRev();
		String remotePath = fileMetadata.getPathLower();

		DropboxFileLink link = dropboxLinkRepository.findByUid(uid);
		if (link == null) {
			link = new DropboxFileLink();

			link.setUid(uid);
			link.setRemotePath(remotePath);
			link.setLocalPath(generateDestinationFilePath(destinationDir));
			link.setDownloaded(false);
			link.setRevision(revision);

			dropboxLinkRepository.insert(link);

		} else if (isNotEquals(revision, link.getRevision())) {
			link.setDownloaded(false);
			link.setRevision(revision);

			dropboxLinkRepository.update(link);
		}

		if (!link.isDownloaded()) {
			try {
				downloadFileAndUpdateLinkData(link);

				dropboxLinkRepository.update(link);
			} catch (DbxException | IOException e) {
				throw new FileSystemException(e);
			}
		}

		try {
			result = new FileInputStream(new File(link.getLocalPath()));
		} catch (FileNotFoundException e) {
			throw new FileSystemException(e);
		}

		return result;
	}

	private String generateDestinationFilePath(File dir) {
		return dir.getPath() + "/" + UUID.randomUUID().toString();
	}

	private void downloadFileAndUpdateLinkData(DropboxFileLink link) throws DbxException, IOException {
		OutputStream out = null;

		File destinationFile = new File(link.getLocalPath());

		try {
			out = new FileOutputStream(destinationFile);

			Logger.d(TAG, "Downloading dropbox file " + link.getRemotePath() +
					" into " + link.getLocalPath());

			FileMetadata metadata = client.files().download(link.getRemotePath())
					.download(out);

			link.setDownloaded(true);
			link.setUploaded(true);
			link.setLastModificationTimestamp(anyLastTimestamp(metadata.getServerModified(), metadata.getClientModified()));
			link.setLastDownloadTimestamp(System.currentTimeMillis());
			link.setRevision(metadata.getRev());
			link.setRemotePath(metadata.getPathLower());

			link.setRetryCount(0);
			link.setLastRetryTimestamp(null);

		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	//TODO: add NetworkIOException handling

	@Override
	public OutputStream openFileForWrite(FileDescriptor file) throws FileSystemException {
		DropboxFileOutputStream result;

		Metadata metadata;
		try {
			metadata = client.files().getMetadata(formatDropboxPath(file.getPath()));
		} catch (GetMetadataErrorException e) {
			if (e.getMessage().contains(PATH_NOT_FOUND_MESSAGE)) {
				metadata = null;
			} else {
				throw new FileSystemException(String.format(ERROR_FAILED_TO_GET_FILE_METADATA, file.getPath()));
			}

		} catch (NetworkIOException e) {
			throw new IOFileSystemException(e);

		} catch (DbxException e) {
			throw new FileSystemException(e);
		}

		FileMetadata fileMetadata = (metadata instanceof FileMetadata) ? (FileMetadata) metadata : null;

		File destinationDir = FileUtils.getRemoteFilesDir(App.getInstance());
		if (destinationDir == null) {
			throw new FileSystemException(ERROR_FAILED_TO_GET_FILE_METADATA);
		}

		if (fileMetadata == null) {
			//upload new file
			String parentPath = getParentPath(file.getPath());
			if (parentPath == null) {
				throw new FileSystemException(ERROR_INCORRECT_FILE_PATH);
			}

			if (!parentPath.equals("/")) {
				Metadata parentMetadata;

				try {
					parentMetadata = client.files().getMetadata(formatDropboxPath(parentPath));
				} catch (GetMetadataErrorException e) {
					if (e.getMessage().contains(PATH_NOT_FOUND_MESSAGE)) {
						throw new FileSystemException(String.format(ERROR_FAILED_TO_GET_FILE_METADATA, parentPath));
					} else {
						throw new FileSystemException(e);
					}
				} catch (DbxException e) {
					Logger.printStackTrace(e);
					throw new FileSystemException(e);
				}

				if (!(parentMetadata instanceof FolderMetadata)) {
					throw new FileSystemException(String.format(ERROR_SPECIFIED_FILE_HAS_INCORRECT_METADATA, parentPath));
				}

				FolderMetadata parentFolderMetadata = (FolderMetadata) parentMetadata;
				parentPath = parentFolderMetadata.getPathLower();
			}

			DropboxFileLink link = new DropboxFileLink();

			link.setLastModificationTimestamp(file.getModified());
			link.setLocalPath(generateDestinationFilePath(destinationDir));
			link.setRemotePath(parentPath + "/" + file.getName());

			dropboxLinkRepository.insert(link);

			try {
				result = new DropboxFileOutputStream(this, client, link);

				Logger.d(TAG, "Uploading file to Dropbox: from " + link.getLocalPath() +
						" to " + link.getRemotePath());

				onFileUploadStarted(link);
			} catch (IOException e) {
				Logger.printStackTrace(e);
				throw new FileSystemException(String.format(ERROR_FAILED_TO_FIND_FILE, link.getLocalPath()));
			}

		} else {
			//re-write existing file
			String uid = fileMetadata.getId();
			String revision = fileMetadata.getRev();
			String remotePath = fileMetadata.getPathLower();
			Date localModified = new Date(file.getModified());
			Date serverModified = fileMetadata.getClientModified();
			Date clientModified = fileMetadata.getClientModified();

			DropboxFileLink link = dropboxLinkRepository.findByUid(uid);

			if (!canResolveMergeConflict(localModified, serverModified, clientModified)) {
				if (link == null) {
					link = new DropboxFileLink();

					link.setUid(uid);
					link.setRemotePath(remotePath);
					link.setLocalPath(generateDestinationFilePath(destinationDir));
					link.setRevision(revision);
					link.setLastModificationTimestamp(file.getModified());

					dropboxLinkRepository.insert(link);
				}

				throw new ModificationConflictException(link.getId(),
						file.getModified(),
						(serverModified != null) ? serverModified.getTime() : null,
						(clientModified != null) ? clientModified.getTime() : null);
			}

			if (link == null) {
				link = new DropboxFileLink();

				link.setUid(uid);
				link.setRemotePath(remotePath);
				link.setLocalPath(generateDestinationFilePath(destinationDir));
				link.setRevision(revision);
				link.setLastModificationTimestamp(file.getModified());

				dropboxLinkRepository.insert(link);

			} else {
				link.setRevision(revision);
				link.setLastModificationTimestamp(file.getModified());
			}

			try {
				result = new DropboxFileOutputStream(this, client, link);

				Logger.d(TAG, "Uploading file to Dropbox: from " + link.getLocalPath() +
						" to " + link.getRemotePath());

				onFileUploadStarted(link);
			} catch (IOException e) {
				Logger.printStackTrace(e);

				throw new FileSystemException(String.format(ERROR_FAILED_TO_FIND_FILE, link.getLocalPath()));
			}
		}

		return result;
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

	private void onFileUploadStarted(DropboxFileLink link) {
		synchronized (uploadingFiles) {
			uploadingFiles.add(link);
		}
	}

	void onFileUploadFinished(DropboxFileLink link) {
		synchronized (uploadingFiles) {
			dropboxLinkRepository.update(link);

			uploadingFiles.remove(link);
		}
	}

	@Override
	public boolean exists(FileDescriptor file) throws FileSystemException {
		String authToken = authenticator.getAuthToken();
		if (authToken == null) {
			throw new AuthException();
		}

		initClientIfNeed(authToken);

		return getFileMetadata(file) != null;
	}

	private FileMetadata getFileMetadata(FileDescriptor file) throws FileSystemException {
		FileMetadata result;

		try {
			Metadata metadata = client.files().getMetadata(formatDropboxPath(file.getPath()));
			if (metadata instanceof FileMetadata) {
				result = (FileMetadata) metadata;
			} else {
				throw new FileSystemException(String.format(ERROR_SPECIFIED_FILE_HAS_INCORRECT_METADATA, file.getPath()));
			}

		} catch (GetMetadataErrorException e) {
			if (e.getMessage().contains(PATH_NOT_FOUND_MESSAGE)) {
				result = null;
			} else {
				Logger.printStackTrace(e);
				throw new FileSystemException(String.format(ERROR_FAILED_TO_GET_FILE_METADATA, file.getPath()));
			}

		} catch (DbxException e) {
			Logger.printStackTrace(e);
			throw new FileSystemException(e);
		}

		return result;
	}
}
