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
import com.ivanovsky.passnotes.data.entity.FSAuthority;
import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.RemoteFileMetadata;
import com.ivanovsky.passnotes.data.repository.file.remote.exception.RemoteFSApiException;
import com.ivanovsky.passnotes.data.repository.file.remote.exception.RemoteFSAuthException;
import com.ivanovsky.passnotes.data.repository.file.remote.exception.RemoteFSException;
import com.ivanovsky.passnotes.data.repository.file.remote.exception.RemoteFSFileNotFoundException;
import com.ivanovsky.passnotes.data.repository.file.remote.exception.InternalCacheException;
import com.ivanovsky.passnotes.data.repository.file.remote.exception.RemoteFSNetworkException;
import com.ivanovsky.passnotes.data.repository.file.remote.RemoteApiClient;
import com.ivanovsky.passnotes.util.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

import static com.ivanovsky.passnotes.util.DateUtils.anyLastTimestamp;
import static com.ivanovsky.passnotes.util.FileUtils.ROOT_PATH;
import static com.ivanovsky.passnotes.util.FileUtils.getParentPath;

public class DropboxClient implements RemoteApiClient {

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
	private final FSAuthority fsAuthority;

	public DropboxClient(DropboxAuthenticator authenticator) {
		this.authenticator = authenticator;
		this.config = DbxRequestConfig.newBuilder("Passnotes/Android")
				.withHttpRequestor(new OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
				.build();
		this.fsAuthority = FSAuthority.Companion.getDROPBOX_FS_AUTHORITY();
	}

	private void initClientIfNeedOrThrow() throws RemoteFSAuthException {
		String authToken = authenticator.getAuthToken();
		if (client == null && authToken != null) {
			client = new DbxClientV2(config, authToken);
		}

		if (client == null) {
			throw new RemoteFSAuthException();
		}
	}

	@Override
	@NonNull
	public List<FileDescriptor> listFiles(FileDescriptor dir) throws RemoteFSException {
		List<FileDescriptor> result;

		if (!dir.isDirectory()) {
			throw new RemoteFSApiException(ERROR_SPECIFIED_FILE_IS_NOT_A_DIRECTORY);
		}

		initClientIfNeedOrThrow();

		try {
			result = listFilesFromDropbox(dir);
			if (result == null) {
				throw new RemoteFSApiException("Failed to load file list");
			}
		} catch (NetworkIOException e) {
			Logger.printStackTrace(e);
			throw new RemoteFSNetworkException();

		} catch (DbxException e) {
			Logger.printStackTrace(e);
			throw new RemoteFSApiException("Failed to load file list");
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
		String path = metadata.getPathDisplay();
		Long modified = anyLastTimestamp(metadata.getClientModified(), metadata.getServerModified());

		return new FileDescriptor(fsAuthority,
				path,
				metadata.getId(),
				false,
				false,
				modified);
	}

	private FileDescriptor createDescriptorFromMetadata(FolderMetadata metadata) {
		String path = metadata.getPathDisplay();
		boolean isRoot = ROOT_PATH.equals(path);

		return new FileDescriptor(fsAuthority,
				path,
				metadata.getId(),
				true,
				isRoot,
				null);
	}

	@Override
	@NonNull
	public FileDescriptor getParent(FileDescriptor file) throws RemoteFSException {
		FileDescriptor result;

		initClientIfNeedOrThrow();

		String parentPath = getParentPath(file.getPath());
		if (parentPath == null) {
			throw new RemoteFSApiException(ERROR_FILE_DOES_NOT_EXIST);
		}

		if (!parentPath.equals("/")) {
			try {
				Metadata metadata = client.files().getMetadata(formatDropboxPath(parentPath));
				if (!(metadata instanceof FolderMetadata)) {
					throw new RemoteFSApiException("Specified file is not a directory");
				}

				result = createDescriptorFromMetadata((FolderMetadata) metadata);
			} catch (NetworkIOException e) {
				Logger.printStackTrace(e);
				throw new RemoteFSNetworkException();

			} catch (DbxException e) {
				Logger.printStackTrace(e);
				throw new RemoteFSApiException(ERROR_FILE_DOES_NOT_EXIST);
			}
		} else {
			result = createRootDescriptor();
		}

		return result;
	}

	private FileDescriptor createRootDescriptor() {
		return new FileDescriptor(fsAuthority,
				ROOT_PATH,
				ROOT_PATH,
				true,
				true,
				null);
	}

	@Override
	@NonNull
	public FileDescriptor getRoot() throws RemoteFSException {
		FileDescriptor result;

		initClientIfNeedOrThrow();

		try {
			ListFolderResult remoteFile = client.files().listFolder("");
			if (remoteFile == null
					|| remoteFile.getEntries() == null
					|| remoteFile.getEntries().size() == 0) {
				throw new RemoteFSApiException(String.format(ERROR_FAILED_TO_FIND_FILE, "/"));
			}

			result = createRootDescriptor();
		} catch (NetworkIOException e) {
			Logger.printStackTrace(e);
			throw new RemoteFSNetworkException();

		} catch (DbxException e) {
			Logger.printStackTrace(e);
			throw new RemoteFSApiException(ERROR_FILE_DOES_NOT_EXIST);
		}

		return result;
	}

	@Override
	@NonNull
	public RemoteFileMetadata getFileMetadataOrThrow(FileDescriptor file) throws RemoteFSException {
		Metadata metadata;

		initClientIfNeedOrThrow();

		try {
			metadata = client.files().getMetadata(formatDropboxPath(file.getPath()));
			if (metadata == null) {
				throw new RemoteFSApiException(String.format(ERROR_FAILED_TO_GET_FILE_METADATA, file.getPath()));
			}
			if (!(metadata instanceof FileMetadata)) {
				throw new RemoteFSApiException(String.format(ERROR_SPECIFIED_FILE_HAS_INCORRECT_METADATA, file.getPath()));
			}
		} catch (NetworkIOException e) {
			Logger.printStackTrace(e);
			throw new RemoteFSNetworkException();
		} catch (GetMetadataErrorException e) {
			if (e.getMessage().contains(PATH_NOT_FOUND_MESSAGE)) {
			    throw new RemoteFSFileNotFoundException(file.getPath());
			} else {
				Logger.printStackTrace(e);
				throw new RemoteFSApiException(ERROR_INCORRECT_MESSAGE_FROM_SERVER);
			}

		} catch (DbxException e) {
			Logger.printStackTrace(e);
			throw new RemoteFSApiException(e.getMessage());
		}

		return convertMetadata((FileMetadata) metadata);
	}

	@Override
	@NonNull
	public RemoteFileMetadata downloadFileOrThrow(String remotePath, String destinationPath) throws RemoteFSException {
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
			throw new InternalCacheException();

		} catch (NetworkIOException e) {
			Logger.printStackTrace(e);
			throw new RemoteFSNetworkException();

		} catch (DownloadErrorException e) {
			Logger.printStackTrace(e);
			throw new RemoteFSApiException(e.errorValue.toString());

		} catch (DbxException e) {
			Logger.printStackTrace(e);
			throw new RemoteFSApiException(e.getMessage());

		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					Logger.printStackTrace(e);
				}
			}
		}

		return convertMetadata(result);
	}

	@Override
	@NonNull
	public RemoteFileMetadata uploadFileOrThrow(String remotePath, String localPath) throws RemoteFSException {
		FileMetadata result;

		try {
			result = client.files().uploadBuilder(remotePath)
					.withMode(WriteMode.OVERWRITE)
					.uploadAndFinish(new BufferedInputStream(new FileInputStream(localPath)));
		} catch (NetworkIOException e) {
			Logger.printStackTrace(e);
			throw new RemoteFSNetworkException();

		} catch (DbxException e) {
			Logger.printStackTrace(e);
			throw new RemoteFSApiException(e.getMessage());

		} catch (IOException e) {
			Logger.printStackTrace(e);
			throw new InternalCacheException();
		}

		return convertMetadata(result);
	}

	private RemoteFileMetadata convertMetadata(FileMetadata metadata) {
		return new RemoteFileMetadata(metadata.getId(), metadata.getPathLower(),
				metadata.getServerModified(), metadata.getClientModified(), metadata.getRev());
	}
}
