package com.ivanovsky.passnotes.data.entity;

import java.util.UUID;

public class Group {

	private UUID uid;
	private String title;

	public Group() {
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
