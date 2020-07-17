package com.ivanovsky.passnotes.data.repository.file.dropbox;

import java.util.UUID;

class ProcessingUnit {

	final UUID processingUid;
	final ProcessingStatus status;
	final String fileUid;
	final String remotePath;

	ProcessingUnit(UUID processingUid, ProcessingStatus status, String fileUid, String remotePath) {
		this.processingUid = processingUid;
		this.status = status;
		this.fileUid = fileUid;
		this.remotePath = remotePath;
	}

	@Override
	public String toString() {
		return "ProcessingUnit{" +
				"processingUid=" + processingUid +
				", status=" + status +
				", fileUid='" + fileUid + '\'' +
				", remotePath='" + remotePath + '\'' +
				'}';
	}
}
