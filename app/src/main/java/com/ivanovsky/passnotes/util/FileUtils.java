package com.ivanovsky.passnotes.util;

import android.content.Context;
import android.support.annotation.Nullable;

import java.io.File;

public class FileUtils {

	@Nullable
	public static File getDatabaseDir(Context context) {
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
}
