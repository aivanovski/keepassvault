package com.ivanovsky.passnotes.data.repository.file;

import com.ivanovsky.passnotes.data.repository.RemoteFileRepository;
import com.ivanovsky.passnotes.data.repository.SettingsRepository;
import com.ivanovsky.passnotes.data.repository.file.dropbox.DropboxAuthenticator;
import com.ivanovsky.passnotes.data.repository.file.dropbox.DropboxClient;
import com.ivanovsky.passnotes.data.repository.file.remote.RemoteApiClient;
import com.ivanovsky.passnotes.data.repository.file.remote.RemoteFileSystemProvider;
import com.ivanovsky.passnotes.data.repository.file.regular.RegularFileSystemProvider;
import com.ivanovsky.passnotes.domain.FileHelper;
import com.ivanovsky.passnotes.domain.PermissionHelper;

import java.util.EnumMap;
import java.util.Map;

public class FileSystemResolver {

	private final SettingsRepository settings;
	private final RemoteFileRepository remoteFileRepository;
	private final FileHelper fileHelper;
	private final PermissionHelper permissionHelper;
	private final Map<FSType, FileSystemProvider> providers;

	public FileSystemResolver(SettingsRepository settings,
							  RemoteFileRepository remoteFileRepository,
							  FileHelper fileHelper,
							  PermissionHelper permissionHelper) {
		this.settings = settings;
		this.remoteFileRepository = remoteFileRepository;
		this.fileHelper = fileHelper;
		this.permissionHelper = permissionHelper;
		this.providers = new EnumMap<>(FSType.class);
	}

	public FileSystemProvider resolveProvider(FSType type) {
		FileSystemProvider provider;

		synchronized (providers) {
			provider = providers.get(type);
			if (provider == null) {
				provider = instantiateProvider(type);
				providers.put(type, provider);
			}
		}

		return provider;
	}

	private FileSystemProvider instantiateProvider(FSType type) {
		FileSystemProvider provider;

		if (type == FSType.REGULAR_FS) {
			provider = new RegularFileSystemProvider(permissionHelper);
		} else if (type == FSType.DROPBOX) {
		    DropboxAuthenticator authenticator = new DropboxAuthenticator(settings);
		    RemoteApiClient client = new DropboxClient(authenticator);
			provider = new RemoteFileSystemProvider(authenticator, client, remoteFileRepository,
					fileHelper, type);
		} else {
			throw new IllegalArgumentException("Specified provider is not implemented: " + type);
		}

		return provider;
	}
}
