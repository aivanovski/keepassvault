package com.ivanovsky.passnotes.data.repository.file;

import com.ivanovsky.passnotes.data.repository.DropboxFileRepository;
import com.ivanovsky.passnotes.data.repository.SettingsRepository;
import com.ivanovsky.passnotes.data.repository.file.dropbox.DropboxFileSystemProvider;
import com.ivanovsky.passnotes.data.repository.file.regular.RegularFileSystemProvider;
import com.ivanovsky.passnotes.domain.FileHelper;
import com.ivanovsky.passnotes.domain.PermissionHelper;

import java.util.EnumMap;
import java.util.Map;

public class FileSystemResolver {

	private final SettingsRepository settings;
	private final DropboxFileRepository dropboxFileRepository;
	private final FileHelper fileHelper;
	private final PermissionHelper permissionHelper;
	private Map<FSType, FileSystemProvider> providers;

	public FileSystemResolver(SettingsRepository settings,
							  DropboxFileRepository dropboxFileRepository,
							  FileHelper fileHelper,
							  PermissionHelper permissionHelper) {
		this.settings = settings;
		this.dropboxFileRepository = dropboxFileRepository;
		this.fileHelper = fileHelper;
		this.permissionHelper = permissionHelper;
		this.providers = new EnumMap<>(FSType.class);
	}

	public FileSystemProvider resolveProvider(FSType type) {
		FileSystemProvider provider = providers.get(type);

		if (provider == null) {
			provider = instantiateProvider(type);
			providers.put(type, provider);
		}

		return provider;
	}

	private FileSystemProvider instantiateProvider(FSType type) {
		FileSystemProvider provider;

		if (type == FSType.REGULAR_FS) {
			provider = new RegularFileSystemProvider(permissionHelper);
		} else if (type == FSType.DROPBOX) {
			provider = new DropboxFileSystemProvider(settings, dropboxFileRepository, fileHelper);
		} else {
			throw new IllegalArgumentException("Specified provider is not implemented: " + type);
		}

		return provider;
	}
}
