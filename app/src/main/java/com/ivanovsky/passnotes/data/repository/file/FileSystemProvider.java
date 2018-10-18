package com.ivanovsky.passnotes.data.repository.file;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;

import java.util.List;

public interface FileSystemProvider {

	OperationResult<List<FileDescriptor>> listFiles(FileDescriptor dir);
	OperationResult<FileDescriptor> getParent(FileDescriptor file);
	OperationResult<FileDescriptor> getRootFile();
	FileSystemAuthenticator getAuthenticator();
}
