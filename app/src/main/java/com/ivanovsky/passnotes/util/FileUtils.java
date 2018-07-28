package com.ivanovsky.passnotes.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;

public class FileUtils {

	@Nullable
	public static File getDatabaseDir(@NonNull Context context) {
		File result = null;

		File filesDir = context.getFilesDir();
		if (filesDir != null && filesDir.getParentFile() != null) {

			File dataDir = filesDir.getParentFile();
			if (dataDir.exists()) {

				File databaseDir = new File(dataDir.getPath() + File.separator + "databases");
				if (databaseDir.exists()) {
					result = databaseDir;
				}
			}
		}

		return result;
	}

	@Nullable
	public static File getKeepassDir(@NonNull Context context) {
		File result = null;

		File dbDir = context.getDir("keepass-databases", Context.MODE_PRIVATE);
		if (dbDir != null && dbDir.exists()) {
			result = dbDir;
		}

		return result;
	}

	@Nullable
	public static String getFileNameFromPath(@Nullable String filePath) {
		if (filePath == null) {
			return null;
		}

		String fileName = "";

		int idx = filePath.lastIndexOf("/");
		if (idx > 0
				&& idx + 1 < filePath.length()) {
			fileName = filePath.substring(idx + 1, filePath.length());
		}

		return fileName;
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
