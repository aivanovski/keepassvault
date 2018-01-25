package com.ivanovsky.passnotes.data.encrdb.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@SuppressWarnings("WeakerAccess")
@Entity(tableName = "notepad")
public class Notepad {

	@PrimaryKey
	int id;

	@ColumnInfo(name = "title")
	String title;

	public Notepad() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
