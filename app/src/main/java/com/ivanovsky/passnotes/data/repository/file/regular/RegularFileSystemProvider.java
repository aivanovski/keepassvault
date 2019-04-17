package com.ivanovsky.passnotes.data.repository.file.regular;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationError;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.file.FileSystemAuthenticator;
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider;
import com.ivanovsky.passnotes.util.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FILE_NOT_FOUND;
import static com.ivanovsky.passnotes.data.entity.OperationError.newFileAccessError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError;

public class RegularFileSystemProvider implements FileSystemProvider {

	private final Lock lock;

	public RegularFileSystemProvider() {
		this.lock = new ReentrantLock();
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

					result.setObj(files);
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
				result.setObj(FileDescriptor.fromRegularFile(parentFile));
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
			result.setObj(FileDescriptor.fromRegularFile(root));
		} else {
			result.setError(newGenericIOError(OperationError.MESSAGE_FILE_NOT_FOUND));
		}

		return result;
	}

	@Override
	public OperationResult<InputStream> openFileForRead(FileDescriptor file) {
		OperationResult<InputStream> result = new OperationResult<>();

		lock.lock();
		try {
			InputStream in = new BufferedInputStream(new FileInputStream(file.getPath()));
			result.setObj(in);
		} catch (FileNotFoundException e) {
			Logger.printStackTrace(e);
			result.setError(newGenericIOError(e.getMessage()));
		} finally {
			lock.unlock();
		}

		return result;
	}

	@Override
	public OperationResult<OutputStream> openFileForWrite(FileDescriptor file) {
		OperationResult<OutputStream> result = new OperationResult<>();

		lock.lock();
		try {
			OutputStream out = new BufferedOutputStream(new FileOutputStream(file.getPath()));
			result.setObj(out);
		} catch (FileNotFoundException e) {
			Logger.printStackTrace(e);
			result.setError(newGenericIOError(e.getMessage()));
		} finally {
			lock.unlock();
		}

		return result;
	}

	@Override
	public OperationResult<Boolean> exists(FileDescriptor file) {
		boolean exists = new File(file.getPath()).exists();
		return OperationResult.success(exists);
	}

	@Override
	public OperationResult<FileDescriptor> getFile(String path) {
		OperationResult<FileDescriptor> result = new OperationResult<>();

		File file = new File(path);
		if (file.exists()) {
			result.setObj(FileDescriptor.fromRegularFile(file));
		} else {
			result.setError(newGenericIOError(MESSAGE_FILE_NOT_FOUND));
		}

		return result;
	}
}
