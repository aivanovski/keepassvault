package com.ivanovsky.passnotes.db.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "used_file")
public class UsedFile {

	@PrimaryKey
	int id;

	@ColumnInfo(name = "file_path")
	String filePath;

	public UsedFile() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
}
