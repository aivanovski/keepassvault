package com.ivanovsky.passnotes.data.repository.file;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;

import java.util.List;

public interface FileSystemSyncProcessor {

	List<FileDescriptor> getLocallyModifiedFiles();

	/** Returns updated FileDescriptor */
	OperationResult<FileDescriptor> process(FileDescriptor file,
											SyncStrategy syncStrategy,
											OnConflictStrategy onConflictStrategy);
}
