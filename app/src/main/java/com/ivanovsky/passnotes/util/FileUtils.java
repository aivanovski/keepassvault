package com.ivanovsky.passnotes.util;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

public class FileUtils {

	@Nullable
	public static File getRemoteFilesDir(@NonNull Context context) {
		return getPrivateDir(context, "remote-files");
	}

	@Nullable
	private static File getPrivateDir(@NonNull Context context, @NonNull String name) {
		if (name == null) return null;

		File result = null;

		File dbDir = context.getDir(name, Context.MODE_PRIVATE);
		if (dbDir != null && dbDir.exists()) {
			result = dbDir;
		}

		return result;
	}

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

	public static boolean isLocatedInPrivateStorage(@NonNull File file, @NonNull Context context) {
		boolean result = false;

		File privateDir = context.getFilesDir();
		if (privateDir != null) {
			File dataDir = privateDir.getParentFile();
			if (dataDir != null) {
				String dataDirPath = dataDir.getPath();

				result = file.getPath().startsWith(dataDirPath);
			}
		}

		return result;
	}

	private FileUtils() {
	}
}
