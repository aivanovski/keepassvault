package com.ivanovsky.passnotes.data.repository.file.dropbox;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class StatusMap {

	private final List<ProcessingUnit> entries;

	StatusMap() {
		this.entries = new ArrayList<>();
	}

	ProcessingUnit getByFileUid(String fileUid) {
		if (fileUid == null) return null;

		ProcessingUnit result = null;

		for (ProcessingUnit entry : entries) {
			if (fileUid.equals(entry.fileUid)) {
				result = entry;
				break;
			}
		}

		return result;
	}

	ProcessingUnit getByRemotePath(String remotePath) {
		if (remotePath == null) return null;

		ProcessingUnit result = null;

		for (ProcessingUnit entry : entries) {
			if (remotePath.equals(entry.remotePath)) {
				result = entry;
				break;
			}
		}

		return result;
	}

	void put(ProcessingUnit unit) {
		entries.add(unit);
	}

	void remove(UUID processingUid) {
		for (int idx = 0; idx < entries.size(); idx++) {
			ProcessingUnit entry = entries.get(idx);
			if (entry.processingUid.equals(processingUid)) {
				entries.remove(idx);
				break;
			}
		}
	}
}
