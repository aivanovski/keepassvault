package com.ivanovsky.passnotes.data.repository.file;

import com.ivanovsky.passnotes.data.repository.SettingsRepository;

import java.util.EnumMap;
import java.util.Map;

public class FileSystemResolver {

	private final SettingsRepository settings;
	private Map<FSType, FileSystemProvider> providers;

	public FileSystemResolver(SettingsRepository settings) {
		this.settings = settings;
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
			provider = new DropboxFileSystemProvider(settings);
		} else {
			throw new IllegalArgumentException("Specified provider is not implemented: " + type);
		}

		return provider;
	}
}
