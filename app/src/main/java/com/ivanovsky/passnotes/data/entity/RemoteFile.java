package com.ivanovsky.passnotes.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Objects;

@Entity(tableName = "remote_file")
public class RemoteFile {

    @ColumnInfo(name = "fs_authority")
    private FSAuthority fsAuthority;

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

    @ColumnInfo(name = "last_remote_modification_timestamp")
    private Long lastRemoteModificationTimestamp;

    @ColumnInfo(name = "local_path")
    private String localPath;

    @ColumnInfo(name = "local_backup_path")
    private String localBackupPath;

    @ColumnInfo(name = "remote_path")
    private String remotePath;

    @ColumnInfo(name = "uid")
    private String uid;

    @ColumnInfo(name = "revision")
    private String revision;

    public RemoteFile() {}

    public FSAuthority getFsAuthority() {
        return fsAuthority;
    }

    public void setFsAuthority(FSAuthority fsAuthority) {
        this.fsAuthority = fsAuthority;
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

    public Long getLastRemoteModificationTimestamp() {
        return lastRemoteModificationTimestamp;
    }

    public void setLastRemoteModificationTimestamp(Long lastRemoteModificationTimestamp) {
        this.lastRemoteModificationTimestamp = lastRemoteModificationTimestamp;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getLocalBackupPath() {
        return localBackupPath;
    }

    public void setLocalBackupPath(String localBackupPath) {
        this.localBackupPath = localBackupPath;
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
        if (o == null || getClass() != o.getClass()) return false;

        RemoteFile that = (RemoteFile) o;
        return locallyModified == that.locallyModified
                && uploaded == that.uploaded
                && uploadFailed == that.uploadFailed
                && uploading == that.uploading
                && downloading == that.downloading
                && retryCount == that.retryCount
                && Objects.equals(fsAuthority, that.fsAuthority)
                && Objects.equals(id, that.id)
                && Objects.equals(lastRetryTimestamp, that.lastRetryTimestamp)
                && Objects.equals(lastDownloadTimestamp, that.lastDownloadTimestamp)
                && Objects.equals(lastModificationTimestamp, that.lastModificationTimestamp)
                && Objects.equals(
                        lastRemoteModificationTimestamp, that.lastRemoteModificationTimestamp)
                && Objects.equals(localPath, that.localPath)
                && Objects.equals(localBackupPath, that.localBackupPath)
                && Objects.equals(remotePath, that.remotePath)
                && Objects.equals(uid, that.uid)
                && Objects.equals(revision, that.revision);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(fsAuthority);
        result = 31 * result + Boolean.hashCode(locallyModified);
        result = 31 * result + Boolean.hashCode(uploaded);
        result = 31 * result + Boolean.hashCode(uploadFailed);
        result = 31 * result + Boolean.hashCode(uploading);
        result = 31 * result + Boolean.hashCode(downloading);
        result = 31 * result + Objects.hashCode(id);
        result = 31 * result + retryCount;
        result = 31 * result + Objects.hashCode(lastRetryTimestamp);
        result = 31 * result + Objects.hashCode(lastDownloadTimestamp);
        result = 31 * result + Objects.hashCode(lastModificationTimestamp);
        result = 31 * result + Objects.hashCode(lastRemoteModificationTimestamp);
        result = 31 * result + Objects.hashCode(localPath);
        result = 31 * result + Objects.hashCode(localBackupPath);
        result = 31 * result + Objects.hashCode(remotePath);
        result = 31 * result + Objects.hashCode(uid);
        result = 31 * result + Objects.hashCode(revision);
        return result;
    }

    @Override
    public String toString() {
        return "RemoteFile{"
                + "fsAuthority="
                + fsAuthority
                + ", locallyModified="
                + locallyModified
                + ", uploaded="
                + uploaded
                + ", uploadFailed="
                + uploadFailed
                + ", uploading="
                + uploading
                + ", downloading="
                + downloading
                + ", id="
                + id
                + ", retryCount="
                + retryCount
                + ", lastRetryTimestamp="
                + lastRetryTimestamp
                + ", lastDownloadTimestamp="
                + lastDownloadTimestamp
                + ", lastModificationTimestamp="
                + lastModificationTimestamp
                + ", lastServerModificationTimestamp="
                + lastRemoteModificationTimestamp
                + ", localPath='"
                + localPath
                + '\''
                + ", lastRemoteFilePath='"
                + localBackupPath
                + '\''
                + ", remotePath='"
                + remotePath
                + '\''
                + ", uid='"
                + uid
                + '\''
                + ", revision='"
                + revision
                + '\''
                + '}';
    }
}
