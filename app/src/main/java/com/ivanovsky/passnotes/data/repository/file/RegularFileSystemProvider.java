package com.ivanovsky.passnotes.data.repository.file;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationError;
import com.ivanovsky.passnotes.data.entity.OperationResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.ivanovsky.passnotes.data.entity.OperationError.newFileAccessError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError;

public class RegularFileSystemProvider implements FileSystemProvider {

	public RegularFileSystemProvider() {
	}

	@Override
	public FileSystemAuthenticator getAuthenticator() {
		return null;
	}

	@Override
	public OperationResult<List<FileDescriptor>> listFiles(FileDescriptor dir) {
		OperationResult<List<FileDescriptor>> result = new OperationResult<>();

		if (dir.isDirectory()) {
			File file = new File(dir.getPath());
			if (file.exists()) {
				List<FileDescriptor> files = new ArrayList<>();

				try {
					File[] childFiles = file.listFiles();
					if (childFiles != null && childFiles.length != 0) {
						for (File childFile : childFiles) {
							files.add(FileDescriptor.fromRegularFile(childFile));
						}
					}

					result.setResult(files);
				} catch (SecurityException e) {
					result.setError(newFileAccessError(OperationError.MESSAGE_FILE_ACCESS_IS_FORBIDDEN, e));
				}
			} else {
				result.setError(newGenericIOError(OperationError.MESSAGE_FILE_NOT_FOUND));
			}
		} else {
			result.setError(newGenericIOError(OperationError.MESSAGE_FILE_IS_NOT_A_DIRECTORY));
		}

		return result;
	}

	@Override
	public OperationResult<FileDescriptor> getParent(FileDescriptor fileDescriptor) {
		OperationResult<FileDescriptor> result = new OperationResult<>();

		File file = new File(fileDescriptor.getPath());
		if (file.exists()) {
			File parentFile = file.getParentFile();

			if (parentFile != null) {
				result.setResult(FileDescriptor.fromRegularFile(parentFile));
			} else {
				result.setError(newGenericIOError(OperationError.MESSAGE_FILE_DOES_NOT_EXIST));
			}
		} else {
			result.setError(newGenericIOError(OperationError.MESSAGE_FILE_NOT_FOUND));
		}

		return result;
	}

	@Override
	public OperationResult<FileDescriptor> getRootFile() {
		OperationResult<FileDescriptor> result = new OperationResult<>();

		File root = new File("/");
		if (root.exists()) {
			result.setResult(FileDescriptor.fromRegularFile(root));
		} else {
			result.setError(newGenericIOError(OperationError.MESSAGE_FILE_NOT_FOUND));
		}

		return result;
	}
}
