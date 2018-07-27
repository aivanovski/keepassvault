package com.ivanovsky.passnotes.data.repository.file;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RegularFileProvider implements FileProvider {

	private final File file;

	RegularFileProvider(FileDescriptor fileDescriptor) {
		this.file = new File(fileDescriptor.getPath());
	}

	@Override
	public InputStream createInputStream() throws IOException {
		return new BufferedInputStream(new FileInputStream(file));
	}

	@Override
	public OutputStream createOutputStream() throws IOException {
		return new BufferedOutputStream(new FileOutputStream(file));
	}
}
