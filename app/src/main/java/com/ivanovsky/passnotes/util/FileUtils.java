package com.ivanovsky.passnotes.util;

import androidx.annotation.Nullable;

public class FileUtils {

	@Nullable
	public static String getFileNameFromPath(@Nullable String filePath) {
		if (filePath == null) {
			return null;
		}

		String fileName = "";

		int idx = filePath.lastIndexOf("/");
		if (idx >= 0 && idx < filePath.length() - 1) {
			fileName = filePath.substring(idx + 1);

		} else if (idx == 0 && filePath.length() == 1) {
			fileName = filePath;
		}

		return fileName;
	}

	@Nullable
	public static String getParentPath(@Nullable String path) {
		if (path == null) {
			return null;
		}

		String parentPath = null;

		int idx = path.lastIndexOf("/");
		if (idx > 0) {
			parentPath = path.substring(0, idx);
		} else if (idx == 0) {
			parentPath = "/";
		}

		return parentPath;
	}

	@Nullable
	public static String getFileNameWithoutExtensionFromPath(@Nullable String filePath) {
		String result = null;

		String fileName = getFileNameFromPath(filePath);
		if (fileName == null) {
			return null;
		}

		int idx = fileName.lastIndexOf(".");
		if (idx > 0
				&& idx + 1 < fileName.length()) {
			result = fileName.substring(0, idx);
		}

		return result;
	}

	private FileUtils() {
	}
}
