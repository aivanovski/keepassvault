package com.ivanovsky.passnotes.data.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "dropbox_file_link")
public class DropboxFileLink {

	@ColumnInfo(name = "donwloaded")
	private boolean downloaded;

	@ColumnInfo(name = "uploaded")
	private boolean uploaded;

	@PrimaryKey(autoGenerate = true)
	private Integer id;

	@ColumnInfo(name = "retry_count")
	private int retryCount;

	@ColumnInfo(name = "last_retry_timestamp")
	private Long lastRetryTimestamp;

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

	public boolean isUploaded() {
		return uploaded;
	}

	public void setUploaded(boolean uploaded) {
		this.uploaded = uploaded;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public int getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}

	public void incrementRetryCount() {
		retryCount++;
	}

	public Long getLastRetryTimestamp() {
		return lastRetryTimestamp;
	}

	public void setLastRetryTimestamp(Long lastRetryTimestamp) {
		this.lastRetryTimestamp = lastRetryTimestamp;
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

		DropboxFileLink link = (DropboxFileLink) o;

		if (downloaded != link.downloaded) return false;
		if (uploaded != link.uploaded) return false;
		if (retryCount != link.retryCount) return false;
		if (id != null ? !id.equals(link.id) : link.id != null) return false;
		if (lastRetryTimestamp != null ? !lastRetryTimestamp.equals(link.lastRetryTimestamp) : link.lastRetryTimestamp != null)
			return false;
		if (lastDownloadTimestamp != null ? !lastDownloadTimestamp.equals(link.lastDownloadTimestamp) : link.lastDownloadTimestamp != null)
			return false;
		if (lastModificationTimestamp != null ? !lastModificationTimestamp.equals(link.lastModificationTimestamp) : link.lastModificationTimestamp != null)
			return false;
		if (localPath != null ? !localPath.equals(link.localPath) : link.localPath != null)
			return false;
		if (remotePath != null ? !remotePath.equals(link.remotePath) : link.remotePath != null)
			return false;
		if (uid != null ? !uid.equals(link.uid) : link.uid != null) return false;
		return revision != null ? revision.equals(link.revision) : link.revision == null;
	}

	@Override
	public int hashCode() {
		int result = (downloaded ? 1 : 0);
		result = 31 * result + (uploaded ? 1 : 0);
		result = 31 * result + (id != null ? id.hashCode() : 0);
		result = 31 * result + retryCount;
		result = 31 * result + (lastRetryTimestamp != null ? lastRetryTimestamp.hashCode() : 0);
		result = 31 * result + (lastDownloadTimestamp != null ? lastDownloadTimestamp.hashCode() : 0);
		result = 31 * result + (lastModificationTimestamp != null ? lastModificationTimestamp.hashCode() : 0);
		result = 31 * result + (localPath != null ? localPath.hashCode() : 0);
		result = 31 * result + (remotePath != null ? remotePath.hashCode() : 0);
		result = 31 * result + (uid != null ? uid.hashCode() : 0);
		result = 31 * result + (revision != null ? revision.hashCode() : 0);
		return result;
	}
}
