package com.ivanovsky.passnotes.data.entity;

import java.io.File;

public class FileDescriptor {

	private Type type;
	private String path;

	public enum Type {
		REGULAR_FILE
	}

	public static FileDescriptor fromRegularFile(File file) {
		FileDescriptor result = new FileDescriptor();
		result.type = Type.REGULAR_FILE;
		result.path = file.getPath();
		return result;
	}

	private FileDescriptor() {
	}

	public Type getType() {
		return type;
	}

	public String getPath() {
		return path;
	}
}
