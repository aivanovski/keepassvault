package com.ivanovsky.passnotes.data.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverter;

import com.ivanovsky.passnotes.data.repository.file.FSType;

@SuppressWarnings("WeakerAccess")
@Entity(tableName = "used_file")
public class UsedFile {

	@PrimaryKey(autoGenerate = true)
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

		if (id != usedFile.id) return false;
		if (addedTime != usedFile.addedTime) return false;
		if (lastAccessTime != null ? !lastAccessTime.equals(usedFile.lastAccessTime) : usedFile.lastAccessTime != null)
			return false;
		if (filePath != null ? !filePath.equals(usedFile.filePath) : usedFile.filePath != null)
			return false;
		if (fileUid != null ? !fileUid.equals(usedFile.fileUid) : usedFile.fileUid != null)
			return false;
		return fsType == usedFile.fsType;
	}

	@Override
	public int hashCode() {
		int result = id;
		result = 31 * result + (int) (addedTime ^ (addedTime >>> 32));
		result = 31 * result + (lastAccessTime != null ? lastAccessTime.hashCode() : 0);
		result = 31 * result + (filePath != null ? filePath.hashCode() : 0);
		result = 31 * result + (fileUid != null ? fileUid.hashCode() : 0);
		result = 31 * result + (fsType != null ? fsType.hashCode() : 0);
		return result;
	}

	public static class FSTypeConverter {

		@TypeConverter
		public FSType fromDatabaseValue(int value) {
			switch (value) {
				case 1:
					return FSType.REGULAR_FS;
				case 2:
					return FSType.DROPBOX;
				default:
					throw new IllegalArgumentException("Failed to determine FSType " +
							"corresponding to value: " + value);
			}
		}

		@TypeConverter
		public int toDatabaseValue(FSType fsType) {
			switch (fsType) {
				case REGULAR_FS:
					return 1;
				case DROPBOX:
					return 2;
				default:
					return 0;
			}
		}
	}
}
