package com.ivanovsky.passnotes.data.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.ivanovsky.passnotes.data.repository.file.FSType;
import com.ivanovsky.passnotes.util.FileUtils;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.File;

public class FileDescriptor implements Parcelable {

	private boolean directory;
	private boolean root;
	private Long modified;
	private FSType fsType;
	private String path;
	private String uid;

	public static FileDescriptor fromRegularFile(File file) {
		FileDescriptor result = new FileDescriptor();

		result.fsType = FSType.REGULAR_FS;
		result.path = file.getPath();
		result.directory = file.isDirectory();
		result.modified = file.lastModified();
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
		result.uid = null;

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

	public Long getModified() {
		return modified;
	}

	public void setModified(Long modified) {
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

		return new EqualsBuilder()
				.append(directory, that.directory)
				.append(root, that.root)
				.append(modified, that.modified)
				.append(fsType, that.fsType)
				.append(path, that.path)
				.append(uid, that.uid)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(directory)
				.append(root)
				.append(modified)
				.append(fsType)
				.append(path)
				.append(uid)
				.toHashCode();
	}

	@Override
	public String toString() {
		return "FileDescriptor{" +
				"directory=" + directory +
				", root=" + root +
				", modified=" + modified +
				", fsType=" + fsType +
				", path='" + path + '\'' +
				", uid='" + uid + '\'' +
				'}';
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
		modified = (Long) source.readSerializable();
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
