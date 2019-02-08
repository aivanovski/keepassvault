package com.ivanovsky.passnotes.data.repository.file.exception;

public class ModificationConflictException extends FileSystemException {

	private final int fileLinkId;
	private final long localModified;
	private final Long serverModified;
	private final Long clientModified;

	public ModificationConflictException(int fileLinkId, long localModified, Long serverModified, Long clientModified) {
		super("conflict in modification timestamp");
		this.fileLinkId = fileLinkId;
		this.localModified = localModified;
		this.serverModified = serverModified;
		this.clientModified = clientModified;
	}
}
