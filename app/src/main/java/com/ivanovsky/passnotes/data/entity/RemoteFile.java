package com.ivanovsky.passnotes.data.entity;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jetbrains.annotations.NotNull;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.ivanovsky.passnotes.data.repository.file.FSType;

@Entity(tableName = "remote_file")
public class RemoteFile {

	@ColumnInfo(name = "fs_type")
	private FSType fsType;

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
	@ColumnInfo(name = "id")
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

	public RemoteFile() {
	}

	public FSType getFsType() {
		return fsType;
	}

	public void setFsType(FSType fsType) {
		this.fsType = fsType;
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

		RemoteFile that = (RemoteFile) o;

		return new EqualsBuilder()
				.append(locallyModified, that.locallyModified)
				.append(uploaded, that.uploaded)
				.append(uploadFailed, that.uploadFailed)
				.append(uploading, that.uploading)
				.append(downloading, that.downloading)
				.append(retryCount, that.retryCount)
				.append(fsType, that.fsType)
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
				.append(fsType)
				.append(locallyModified)
				.append(uploaded)
				.append(uploadFailed)
				.append(uploading)
				.append(downloading)
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

	@Override
	public String toString() {
		return "RemoteFile{" +
				"fsType=" + fsType +
				", locallyModified=" + locallyModified +
				", uploaded=" + uploaded +
				", uploadFailed=" + uploadFailed +
				", uploading=" + uploading +
				", downloading=" + downloading +
				", id=" + id +
				", retryCount=" + retryCount +
				", lastRetryTimestamp=" + lastRetryTimestamp +
				", lastDownloadTimestamp=" + lastDownloadTimestamp +
				", lastModificationTimestamp=" + lastModificationTimestamp +
				", localPath='" + localPath + '\'' +
				", remotePath='" + remotePath + '\'' +
				", uid='" + uid + '\'' +
				", revision='" + revision + '\'' +
				'}';
	}
}
