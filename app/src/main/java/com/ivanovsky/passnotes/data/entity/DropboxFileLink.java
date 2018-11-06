package com.ivanovsky.passnotes.data.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "dropbox_file_link")
public class DropboxFileLink {

	@ColumnInfo(name = "donwloaded")
	private boolean downloaded;

	@PrimaryKey(autoGenerate = true)
	private Integer id;

	@ColumnInfo(name = "last_download_timestamp")
	private Long lastDownloadTimestamp;

	@ColumnInfo(name = "last_modification_timestamp")
	private Long lastModificationTimestamp;

	@ColumnInfo(name = "local_path")
	private String localPath;

	@ColumnInfo(name = "remote_path")
	private String remotePath;

	@ColumnInfo(name = "uid")
	private String uid;

	@ColumnInfo(name = "revision")
	private String revision;

	public DropboxFileLink() {
	}

	public boolean isDownloaded() {
		return downloaded;
	}

	public void setDownloaded(boolean downloaded) {
		this.downloaded = downloaded;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Long getLastDownloadTimestamp() {
		return lastDownloadTimestamp;
	}

	public void setLastDownloadTimestamp(Long lastDownloadTimestamp) {
		this.lastDownloadTimestamp = lastDownloadTimestamp;
	}

	public Long getLastModificationTimestamp() {
		return lastModificationTimestamp;
	}

	public void setLastModificationTimestamp(Long lastModificationTimestamp) {
		this.lastModificationTimestamp = lastModificationTimestamp;
	}

	public String getLocalPath() {
		return localPath;
	}

	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}

	public String getRemotePath() {
		return remotePath;
	}

	public void setRemotePath(String remotePath) {
		this.remotePath = remotePath;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getRevision() {
		return revision;
	}

	public void setRevision(String revision) {
		this.revision = revision;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		DropboxFileLink that = (DropboxFileLink) o;

		if (downloaded != that.downloaded) return false;
		if (id != null ? !id.equals(that.id) : that.id != null) return false;
		if (lastDownloadTimestamp != null ? !lastDownloadTimestamp.equals(that.lastDownloadTimestamp) : that.lastDownloadTimestamp != null)
			return false;
		if (lastModificationTimestamp != null ? !lastModificationTimestamp.equals(that.lastModificationTimestamp) : that.lastModificationTimestamp != null)
			return false;
		if (localPath != null ? !localPath.equals(that.localPath) : that.localPath != null)
			return false;
		if (remotePath != null ? !remotePath.equals(that.remotePath) : that.remotePath != null)
			return false;
		if (uid != null ? !uid.equals(that.uid) : that.uid != null) return false;
		return revision != null ? revision.equals(that.revision) : that.revision == null;
	}

	@Override
	public int hashCode() {
		int result = (downloaded ? 1 : 0);
		result = 31 * result + (id != null ? id.hashCode() : 0);
		result = 31 * result + (lastDownloadTimestamp != null ? lastDownloadTimestamp.hashCode() : 0);
		result = 31 * result + (lastModificationTimestamp != null ? lastModificationTimestamp.hashCode() : 0);
		result = 31 * result + (localPath != null ? localPath.hashCode() : 0);
		result = 31 * result + (remotePath != null ? remotePath.hashCode() : 0);
		result = 31 * result + (uid != null ? uid.hashCode() : 0);
		result = 31 * result + (revision != null ? revision.hashCode() : 0);
		return result;
	}
}
