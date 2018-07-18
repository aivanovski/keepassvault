package com.ivanovsky.passnotes.data.safedb.model;

import java.util.UUID;

public class Group {

	private int noteCount;
	private UUID uid;
	private String title;

	public Group() {
	}

	public int getNoteCount() {
		return noteCount;
	}

	public void setNoteCount(int noteCount) {
		this.noteCount = noteCount;
	}

	public UUID getUid() {
		return uid;
	}

	public void setUid(UUID uid) {
		this.uid = uid;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
