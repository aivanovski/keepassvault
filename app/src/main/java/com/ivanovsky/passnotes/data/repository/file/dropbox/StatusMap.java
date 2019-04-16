package com.ivanovsky.passnotes.data.repository.file.dropbox;

import java.util.ArrayList;
import java.util.List;

class StatusMap {

	private final List<ProcessingUnit> entries;

	StatusMap() {
		this.entries = new ArrayList<>();
	}

	ProcessingUnit getByUid(String uid) {
		if (uid == null) return null;

		ProcessingUnit result = null;

		for (ProcessingUnit entry : entries) {
			if (uid.equals(entry.fileUid)) {
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
}
