package com.ivanovsky.passnotes.data.repository.file;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.file.exception.FileSystemException;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface FileSystemProvider {

	OperationResult<List<FileDescriptor>> listFiles(FileDescriptor dir);
	OperationResult<FileDescriptor> getParent(FileDescriptor file);
	OperationResult<FileDescriptor> getRootFile();
	FileSystemAuthenticator getAuthenticator();
	InputStream openFileForRead(FileDescriptor file) throws FileSystemException;
	OutputStream openFileForWrite(FileDescriptor file) throws FileSystemException;
	boolean exists(FileDescriptor file) throws FileSystemException;
}
