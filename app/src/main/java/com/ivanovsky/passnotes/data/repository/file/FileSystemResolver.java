package com.ivanovsky.passnotes.data.repository.file;

import com.ivanovsky.passnotes.data.repository.DropboxFileLinkRepository;
import com.ivanovsky.passnotes.data.repository.SettingsRepository;
import com.ivanovsky.passnotes.data.repository.file.dropbox.DropboxFileSystemProvider;
import com.ivanovsky.passnotes.data.repository.file.regular.RegularFileSystemProvider;

import java.util.EnumMap;
import java.util.Map;

public class FileSystemResolver {

	private final SettingsRepository settings;
	private final DropboxFileLinkRepository dropboxLinkRepository;
	private Map<FSType, FileSystemProvider> providers;

	public FileSystemResolver(SettingsRepository settings,
							  DropboxFileLinkRepository dropboxLinkRepository) {
		this.settings = settings;
		this.dropboxLinkRepository = dropboxLinkRepository;
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
			provider = new RegularFileSystemProvider();
		} else if (type == FSType.DROPBOX) {
			provider = new DropboxFileSystemProvider(settings, dropboxLinkRepository);
		} else {
			throw new IllegalArgumentException("Specified provider is not implemented: " + type);
		}

		return provider;
	}
}
