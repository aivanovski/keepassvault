package com.ivanovsky.passnotes.data.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.ivanovsky.passnotes.data.repository.file.FSType;
import com.ivanovsky.passnotes.util.FileUtils;

import java.io.File;
import java.util.Date;

public class FileDescriptor implements Parcelable {

	private boolean directory;
	private boolean root;
	private Date modified;
	private FSType fsType;
	private String path;
	private String uid;

	public static FileDescriptor fromRegularFile(File file) {
		FileDescriptor result = new FileDescriptor();

		result.fsType = FSType.REGULAR_FS;
		result.path = file.getPath();
		result.directory = file.isDirectory();
		result.modified = new Date(file.lastModified());
		result.root = file.getPath().equals("/");
		result.uid = result.path;

		return result;
	}

	public static FileDescriptor fromParent(FileDescriptor parent, String filename) {
		FileDescriptor result = new FileDescriptor();

		result.fsType = parent.fsType;
		result.path = parent.path + "/" + filename;
		result.directory = false;
		result.modified = null;
		result.root = false;
		result.uid = result.path;

		return result;
	}

	public FileDescriptor() {
	}

	private FileDescriptor(Parcel source) {
		readFromParcel(source);
	}

	public boolean isDirectory() {
		return directory;
	}

	public void setDirectory(boolean directory) {
		this.directory = directory;
	}

	public boolean isRoot() {
		return root;
	}

	public void setRoot(boolean root) {
		this.root = root;
	}

	public Date getModified() {
		return modified;
	}

	public void setModified(Date modified) {
		this.modified = modified;
	}

	public FSType getFsType() {
		return fsType;
	}

	public void setFsType(FSType fsType) {
		this.fsType = fsType;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getName() {
		return FileUtils.getFileNameFromPath(path);
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		FileDescriptor that = (FileDescriptor) o;

		if (directory != that.directory) return false;
		if (root != that.root) return false;
		if (modified != null ? !modified.equals(that.modified) : that.modified != null)
			return false;
		if (fsType != that.fsType) return false;
		if (path != null ? !path.equals(that.path) : that.path != null) return false;
		return uid != null ? uid.equals(that.uid) : that.uid == null;
	}

	@Override
	public int hashCode() {
		int result = (directory ? 1 : 0);
		result = 31 * result + (root ? 1 : 0);
		result = 31 * result + (modified != null ? modified.hashCode() : 0);
		result = 31 * result + (fsType != null ? fsType.hashCode() : 0);
		result = 31 * result + (path != null ? path.hashCode() : 0);
		result = 31 * result + (uid != null ? uid.hashCode() : 0);
		return result;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(directory ? 1 : 0);
		dest.writeInt(root ? 1 : 0);
		dest.writeSerializable(modified);
		dest.writeSerializable(fsType);
		dest.writeString(path);
		dest.writeString(uid);
	}

	private void readFromParcel(Parcel source) {
		directory = (source.readInt() == 1);
		root = (source.readInt() == 1);
		modified = (Date) source.readSerializable();
		fsType = (FSType) source.readSerializable();
		path = source.readString();
		uid = source.readString();
	}

	public static Creator<FileDescriptor> CREATOR = new Creator<FileDescriptor>() {

		@Override
		public FileDescriptor createFromParcel(Parcel source) {
			return new FileDescriptor(source);
		}

		@Override
		public FileDescriptor[] newArray(int size) {
			return new FileDescriptor[size];
		}
	};
}
