package com.ivanovsky.passnotes.data.entity;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class Note {

	private Date created;
	private Date modified;
	private UUID uid;
	private UUID groupUid;
	private String title;
	private List<Property> properties;

	public Note() {
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getModified() {
		return modified;
	}

	public void setModified(Date modified) {
		this.modified = modified;
	}

	public UUID getUid() {
		return uid;
	}

	public void setUid(UUID uid) {
		this.uid = uid;
	}

	public UUID getGroupUid() {
		return groupUid;
	}

	public void setGroupUid(UUID groupUid) {
		this.groupUid = groupUid;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<Property> getProperties() {
		return properties;
	}

	public void setProperties(List<Property> properties) {
		this.properties = properties;
	}
}
