package com.ivanovsky.passnotes.data.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@SuppressWarnings("WeakerAccess")
@Entity(tableName = "used_file")
public class UsedFile {

	@PrimaryKey(autoGenerate = true)
	int id;

	@ColumnInfo(name = "file_path")
	String filePath;

	@ColumnInfo(name =  "last_access_time")
	long lastAccessTime;

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

	public long getLastAccessTime() {
		return lastAccessTime;
	}

	public void setLastAccessTime(long lastAccessTime) {
		this.lastAccessTime = lastAccessTime;
	}
}
