package com.ivanovsky.passnotes.data.repository.file;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;

public class FileResolver {

	public FileResolver() {
	}

	public FileProvider resolveProvider(FileDescriptor fileDescriptor) {
		FileProvider provider;

		if (fileDescriptor.getFsType() == FSType.REGULAR_FS) {
			provider = new RegularFileProvider(fileDescriptor);
		} else {
			throw new IllegalArgumentException("Incorrect FileDescriptor type");
		}

		return provider;
	}
}