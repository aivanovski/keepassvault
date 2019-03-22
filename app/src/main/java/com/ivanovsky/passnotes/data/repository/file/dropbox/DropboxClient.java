package com.ivanovsky.passnotes.data.repository.file.dropbox;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.NetworkIOException;
import com.dropbox.core.http.OkHttp3Requestor;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DownloadErrorException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.GetMetadataErrorException;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.WriteMode;
import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.repository.file.FSType;
import com.ivanovsky.passnotes.data.repository.file.dropbox.exception.DropboxApiException;
import com.ivanovsky.passnotes.data.repository.file.dropbox.exception.DropboxAuthException;
import com.ivanovsky.passnotes.data.repository.file.dropbox.exception.DropboxException;
import com.ivanovsky.passnotes.data.repository.file.dropbox.exception.DropboxInternalCacheException;
import com.ivanovsky.passnotes.data.repository.file.dropbox.exception.DropboxNetworkException;
import com.ivanovsky.passnotes.util.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.ivanovsky.passnotes.util.DateUtils.anyLastTimestamp;
import static com.ivanovsky.passnotes.util.FileUtils.getParentPath;

class DropboxClient {

	private static final String TAG = DropboxClient.class.getSimpleName();

	private static final String ERROR_SPECIFIED_FILE_IS_NOT_A_DIRECTORY = "Specified file is not a directory";
	private static final String ERROR_FILE_DOES_NOT_EXIST = "File does not exist";
	private static final String ERROR_FAILED_TO_GET_FILE_METADATA = "Failed to get file metadata: %s";
	private static final String ERROR_SPECIFIED_FILE_HAS_INCORRECT_METADATA = "Specified file has incorrect metadata: %s";
	private static final String ERROR_FAILED_TO_FIND_FILE = "Failed to find file: %s";
	private static final String ERROR_INCORRECT_MESSAGE_FROM_SERVER = "Incorrect message from server";

	private static final String PATH_NOT_FOUND_MESSAGE = "{\".tag\":\"path\",\"path\":\"not_found\"}";

	private volatile DbxClientV2 client;
	private final DropboxAuthenticator authenticator;
	private final DbxRequestConfig config;

	DropboxClient(DropboxAuthenticator authenticator) {
		this.authenticator = authenticator;
		this.config = DbxRequestConfig.newBuilder("Passnotes/Android")
				.withHttpRequestor(new OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
				.build();
	}

	private void initClientIfNeedOrThrow() throws DropboxAuthException {
		String authToken = authenticator.getAuthToken();
		if (client == null && authToken != null) {
			client = new DbxClientV2(config, authToken);
		}

		if (client == null) {
			throw new DropboxAuthException();
		}
	}

	@NonNull
	List<FileDescriptor> listFiles(FileDescriptor dir) throws DropboxException {
		List<FileDescriptor> result;

		if (!dir.isDirectory()) {
			throw new DropboxApiException(ERROR_SPECIFIED_FILE_IS_NOT_A_DIRECTORY);
		}

		initClientIfNeedOrThrow();

		try {
			result = listFilesFromDropbox(dir);
			if (result == null) {
				throw new DropboxApiException("Failed to load file list");
			}
		} catch (NetworkIOException e) {
			Logger.printStackTrace(e);
			throw new DropboxNetworkException();

		} catch (DbxException e) {
			Logger.printStackTrace(e);
			throw new DropboxApiException("Failed to load file list");
		}

		return result;
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

	private List<FileDescriptor> listFilesFromDropbox(FileDescriptor dir) throws DbxException {
		List<FileDescriptor> result = null;

		ListFolderResult remoteFiles = client.files().listFolder(formatDropboxPath(dir.getPath()));
		if (remoteFiles != null && remoteFiles.getEntries() != null) {
			result = createDescriptorsFromMetadata(remoteFiles.getEntries());
		}

		return result;
	}

	private List<FileDescriptor> createDescriptorsFromMetadata(List<Metadata> metadataList) {
		List<FileDescriptor> descriptors = new ArrayList<>();

		for (Metadata metadata : metadataList) {
			if (metadata instanceof FileMetadata) {
				descriptors.add(createDescriptorFromMetadata((FileMetadata) metadata));

			} else if (metadata instanceof FolderMetadata) {
				descriptors.add(createDescriptorFromMetadata((FolderMetadata) metadata));
			}
		}

		return descriptors;
	}

	private FileDescriptor createDescriptorFromMetadata(FileMetadata metadata) {
		FileDescriptor file = new FileDescriptor();

		file.setFsType(FSType.DROPBOX);
		file.setUid(metadata.getId());
		file.setPath(metadata.getPathDisplay());
		file.setModified(anyLastTimestamp(metadata.getClientModified(),
				metadata.getServerModified()));

		return file;
	}

	private FileDescriptor createDescriptorFromMetadata(FolderMetadata metadata) {
		FileDescriptor file = new FileDescriptor();

		file.setFsType(FSType.DROPBOX);
		file.setUid(metadata.getId());
		file.setPath(metadata.getPathDisplay());
		file.setDirectory(true);

		return file;
	}

	@NonNull
	FileDescriptor getParent(FileDescriptor file) throws DropboxException {
		FileDescriptor result;

		initClientIfNeedOrThrow();

		String parentPath = getParentPath(file.getPath());
		if (parentPath == null) {
			throw new DropboxApiException(ERROR_FILE_DOES_NOT_EXIST);
		}

		if (!parentPath.equals("/")) {
			try {
				Metadata metadata = client.files().getMetadata(formatDropboxPath(parentPath));
				if (!(metadata instanceof FolderMetadata)) {
					throw new DropboxApiException("Specified file is not a directory");
				}

				result = createDescriptorFromMetadata((FolderMetadata) metadata);
			} catch (NetworkIOException e) {
				Logger.printStackTrace(e);
				throw new DropboxNetworkException();

			} catch (DbxException e) {
				Logger.printStackTrace(e);
				throw new DropboxApiException(ERROR_FILE_DOES_NOT_EXIST);
			}
		} else {
			result = createRootDescriptor();
		}

		return result;
	}

	private FileDescriptor createRootDescriptor() {
		FileDescriptor root = new FileDescriptor();

		root.setDirectory(true);
		root.setFsType(FSType.DROPBOX);
		root.setRoot(true);
		root.setPath("/");

		return root;
	}

	@NonNull
	FileDescriptor getRoot() throws DropboxException {
		FileDescriptor result;

		initClientIfNeedOrThrow();

		try {
			ListFolderResult remoteFile = client.files().listFolder("");
			if (remoteFile == null
					|| remoteFile.getEntries() == null
					|| remoteFile.getEntries().size() == 0) {
				throw new DropboxApiException(String.format(ERROR_FAILED_TO_FIND_FILE, "/"));
			}

			result = createRootDescriptor();
		} catch (NetworkIOException e) {
			Logger.printStackTrace(e);
			throw new DropboxNetworkException();

		} catch (DbxException e) {
			Logger.printStackTrace(e);
			throw new DropboxApiException(ERROR_FILE_DOES_NOT_EXIST);
		}

		return result;
	}

	@NonNull
	FileMetadata getFileMetadataOrThrow(FileDescriptor file) throws DropboxException {
		Metadata metadata;

		initClientIfNeedOrThrow();

		try {
			metadata = client.files().getMetadata(formatDropboxPath(file.getPath()));
			if (metadata == null) {
				throw new DropboxApiException(String.format(ERROR_FAILED_TO_GET_FILE_METADATA, file.getPath()));
			}
			if (!(metadata instanceof FileMetadata)) {
				throw new DropboxApiException(String.format(ERROR_SPECIFIED_FILE_HAS_INCORRECT_METADATA, file.getPath()));
			}
		} catch (NetworkIOException e) {
			Logger.printStackTrace(e);
			throw new DropboxNetworkException();

		} catch (DbxException e) {
			Logger.printStackTrace(e);
			throw new DropboxApiException(e.getMessage());
		}

		return (FileMetadata) metadata;
	}

	@Nullable
	FileMetadata getFileMetadataOrNull(FileDescriptor file) throws DropboxException {
		Metadata metadata;

		initClientIfNeedOrThrow();

		try {
			metadata = client.files().getMetadata(formatDropboxPath(file.getPath()));
			if (metadata == null) {
				throw new DropboxApiException(String.format(ERROR_FAILED_TO_GET_FILE_METADATA, file.getPath()));
			}
			if (!(metadata instanceof FileMetadata)) {
				throw new DropboxApiException(String.format(ERROR_SPECIFIED_FILE_HAS_INCORRECT_METADATA, file.getPath()));
			}
		} catch (NetworkIOException e) {
			Logger.printStackTrace(e);
			throw new DropboxNetworkException();

		} catch (GetMetadataErrorException e) {
			if (e.getMessage().contains(PATH_NOT_FOUND_MESSAGE)) {
				metadata = null;
			} else {
				Logger.printStackTrace(e);
				throw new DropboxApiException(ERROR_INCORRECT_MESSAGE_FROM_SERVER);
			}

		} catch (DbxException e) {
			Logger.printStackTrace(e);
			throw new DropboxApiException(e.getMessage());
		}

		return (FileMetadata) metadata;
	}

	@NonNull
	FileMetadata downloadFileOrThrow(String remotePath, String destinationPath) throws DropboxException {
		FileMetadata result;

		FileOutputStream out = null;
		try {
			out = new FileOutputStream(new File(destinationPath));

			Logger.d(TAG, "Downloading dropbox file " + remotePath +
					" into " + destinationPath);

			result = client.files().download(remotePath).download(out);

			out.flush();
		} catch (IOException e) {
			Logger.printStackTrace(e);
			throw new DropboxInternalCacheException();

		} catch (NetworkIOException e) {
			Logger.printStackTrace(e);
			throw new DropboxNetworkException();

		} catch (DownloadErrorException e) {
			Logger.printStackTrace(e);
			throw new DropboxApiException(e.errorValue.toString());

		} catch (DbxException e) {
			Logger.printStackTrace(e);
			throw new DropboxApiException(e.getMessage());

		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					Logger.printStackTrace(e);
				}
			}
		}

		return result;
	}

	@NonNull
	FolderMetadata getFolderMetadataOrThrow(String path) throws DropboxException {
		Metadata metadata;

		try {
			metadata = client.files().getMetadata(formatDropboxPath(path));

			if (metadata == null) {
				throw new DropboxApiException(String.format(ERROR_FAILED_TO_GET_FILE_METADATA, path));
			}

			if (!(metadata instanceof FolderMetadata)) {
				throw new DropboxApiException(String.format(ERROR_SPECIFIED_FILE_HAS_INCORRECT_METADATA, path));
			}
		} catch (GetMetadataErrorException e) {
			if (e.getMessage().contains(PATH_NOT_FOUND_MESSAGE)) {
				throw new DropboxApiException(String.format(ERROR_FAILED_TO_GET_FILE_METADATA, path));
			} else {
				Logger.printStackTrace(e);
				throw new DropboxApiException(e.errorValue.toString());
			}
		} catch (NetworkIOException e) {
			Logger.printStackTrace(e);
			throw new DropboxNetworkException();

		} catch (DbxException e) {
			Logger.printStackTrace(e);
			throw new DropboxApiException(e.getMessage());
		}

		return (FolderMetadata) metadata;
	}

	@NonNull
	FileMetadata uploadFileOrThrow(String remotePath, String destinationPath) throws DropboxException {
		FileMetadata result;

		try {
			result = client.files().uploadBuilder(remotePath)
					.withMode(WriteMode.OVERWRITE)
					.uploadAndFinish(new BufferedInputStream(new FileInputStream(destinationPath)));
		} catch (NetworkIOException e) {
			Logger.printStackTrace(e);
			throw new DropboxNetworkException();

		} catch (DbxException e) {
			Logger.printStackTrace(e);
			throw new DropboxApiException(e.getMessage());

		} catch (IOException e) {
			Logger.printStackTrace(e);
			throw new DropboxInternalCacheException();
		}

		return result;
	}
}
