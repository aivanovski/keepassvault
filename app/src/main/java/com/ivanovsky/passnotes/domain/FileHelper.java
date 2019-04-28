package com.ivanovsky.passnotes.domain;

import android.content.Context;

import com.ivanovsky.passnotes.data.repository.SettingsRepository;
import com.ivanovsky.passnotes.util.InputOutputUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FileHelper {

	private final Context context;
	private final SettingsRepository settings;

	public FileHelper(Context context, SettingsRepository settings) {
		this.context = context;
		this.settings = settings;
	}

	@Nullable
	public File generateDestinationFileForRemoteFile() {
		File result = null;

		String destination = generateDestinationForRemoteFile();
		if (destination != null) {
			result = new File(destination);
		}

		return result;
	}

	@Nullable
	public String generateDestinationForRemoteFile() {
		String result = null;

		File dir = getRemoteFilesDir();
		if (dir != null) {
			result = dir.getPath() + "/" + UUID.randomUUID().toString();
		}

		return result;
	}

	@Nullable
	public File getRemoteFilesDir() {
		File result;

		if (settings.isExternalStorageCacheEnabled()) {
			result = getExternalStorageDir("remote-files");
		} else {
			result = getPrivateDir("remote-files");
		}

		return result;
	}

	@Nullable
	private File getPrivateDir(@NonNull String name) {
		File result = null;

		File dir = context.getDir(name, Context.MODE_PRIVATE);
		if (dir != null && dir.exists()) {
			result = dir;
		}

		return result;
	}

	private File getExternalStorageDir(@NonNull String name) {
		File result = null;

		File dir = context.getExternalCacheDir();
		if (dir != null && dir.exists()) {
			File subDir = new File(dir, name);
			if (subDir.exists() || (!subDir.exists() && subDir.mkdirs())) {
				result = subDir;
			}
		}

		return result;
	}

	@Nullable
	public File getDatabaseDir() {
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

	public boolean isLocatedInPrivateStorage(@NonNull File file) {
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

	@Nullable
	public void duplicateFile(@NonNull File src, @NonNull File destination)
			throws IOException {
		InputStream in = new BufferedInputStream(new FileInputStream(src));
		OutputStream out = new BufferedOutputStream(new FileOutputStream(destination));

		InputOutputUtils.copy(in, out, true);
	}
}
