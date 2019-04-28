package com.ivanovsky.passnotes.data.entity;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "dropbox_file")
public class DropboxFile {

	@ColumnInfo(name = "locally_modified")
	private boolean locallyModified;

	@ColumnInfo(name = "uploaded")
	private boolean uploaded;

	@ColumnInfo(name = "upload_failed")
	private boolean uploadFailed;

	@ColumnInfo(name = "uploading")
	private boolean uploading;

	@ColumnInfo(name = "downloading")
	private boolean downloading;

	@PrimaryKey(autoGenerate = true)
	private Long id;

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

	public DropboxFile() {
	}

	public boolean isLocallyModified() {
		return locallyModified;
	}

	public void setLocallyModified(boolean locallyModified) {
		this.locallyModified = locallyModified;
	}

	public boolean isUploaded() {
		return uploaded;
	}

	public void setUploaded(boolean uploaded) {
		this.uploaded = uploaded;
	}

	public boolean isUploadFailed() {
		return uploadFailed;
	}

	public void setUploadFailed(boolean uploadFailed) {
		this.uploadFailed = uploadFailed;
	}

	public boolean isUploading() {
		return uploading;
	}

	public void setUploading(boolean uploading) {
		this.uploading = uploading;
	}

	public boolean isDownloading() {
		return downloading;
	}

	public void setDownloading(boolean downloading) {
		this.downloading = downloading;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
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

		DropboxFile that = (DropboxFile) o;

		return new EqualsBuilder()
				.append(locallyModified, that.locallyModified)
				.append(uploaded, that.uploaded)
				.append(uploadFailed, that.uploadFailed)
				.append(retryCount, that.retryCount)
				.append(id, that.id)
				.append(lastRetryTimestamp, that.lastRetryTimestamp)
				.append(lastDownloadTimestamp, that.lastDownloadTimestamp)
				.append(lastModificationTimestamp, that.lastModificationTimestamp)
				.append(localPath, that.localPath)
				.append(remotePath, that.remotePath)
				.append(uid, that.uid)
				.append(revision, that.revision)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(locallyModified)
				.append(uploaded)
				.append(uploadFailed)
				.append(id)
				.append(retryCount)
				.append(lastRetryTimestamp)
				.append(lastDownloadTimestamp)
				.append(lastModificationTimestamp)
				.append(localPath)
				.append(remotePath)
				.append(uid)
				.append(revision)
				.toHashCode();
	}
}
