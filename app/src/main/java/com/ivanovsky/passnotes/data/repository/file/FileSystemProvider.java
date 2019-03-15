package com.ivanovsky.passnotes.data.repository.file;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface FileSystemProvider {

	FileSystemAuthenticator getAuthenticator();
	OperationResult<List<FileDescriptor>> listFiles(FileDescriptor dir);
	OperationResult<FileDescriptor> getParent(FileDescriptor file);
	OperationResult<FileDescriptor> getRootFile();
	OperationResult<InputStream> openFileForRead(FileDescriptor file);
	OperationResult<OutputStream> openFileForWrite(FileDescriptor file);
	OperationResult<Boolean> exists(FileDescriptor file);
}
