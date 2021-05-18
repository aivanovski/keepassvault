package com.ivanovsky.passnotes.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;

import com.ivanovsky.passnotes.data.repository.file.FSType;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Entity(tableName = "used_file")
public class UsedFile {

	@PrimaryKey(autoGenerate = true)
	@ColumnInfo(name = "id")
	private int id;

	@ColumnInfo(name = "added_time")
	private long addedTime;

	@ColumnInfo(name =  "last_access_time")
	private Long lastAccessTime;

	@ColumnInfo(name = "file_path")
	private String filePath;

	@ColumnInfo(name = "file_uid")
	private String fileUid;

	@ColumnInfo(name = "fs_type")
	private FSType fsType;

	public UsedFile() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public long getAddedTime() {
		return addedTime;
	}

	public void setAddedTime(long addedTime) {
		this.addedTime = addedTime;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public Long getLastAccessTime() {
		return lastAccessTime;
	}

	public void setLastAccessTime(Long lastAccessTime) {
		this.lastAccessTime = lastAccessTime;
	}

	public String getFileUid() {
		return fileUid;
	}

	public void setFileUid(String fileUid) {
		this.fileUid = fileUid;
	}

	public FSType getFsType() {
		return fsType;
	}

	public void setFsType(FSType fsType) {
		this.fsType = fsType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;

		if (o == null || getClass() != o.getClass()) return false;

		UsedFile usedFile = (UsedFile) o;

		return new EqualsBuilder()
				.append(id, usedFile.id)
				.append(addedTime, usedFile.addedTime)
				.append(lastAccessTime, usedFile.lastAccessTime)
				.append(filePath, usedFile.filePath)
				.append(fileUid, usedFile.fileUid)
				.append(fsType, usedFile.fsType)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(id)
				.append(addedTime)
				.append(lastAccessTime)
				.append(filePath)
				.append(fileUid)
				.append(fsType)
				.toHashCode();
	}

}
