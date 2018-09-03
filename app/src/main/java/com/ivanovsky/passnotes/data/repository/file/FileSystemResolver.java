package com.ivanovsky.passnotes.data.repository.file;

import java.util.EnumMap;
import java.util.Map;

public class FileSystemResolver {

	private Map<FSType, FileSystemProvider> providers;

	public FileSystemResolver() {
		providers = new EnumMap<>(FSType.class);
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
		} else {
			throw new IllegalArgumentException("Specified provider is not implemented: " + type);
		}

		return provider;
	}
}
