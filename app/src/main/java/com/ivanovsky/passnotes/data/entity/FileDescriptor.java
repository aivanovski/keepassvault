package com.ivanovsky.passnotes.data.entity;

import android.os.Parcel;
import android.os.Parcelable;

import com.ivanovsky.passnotes.data.repository.file.FSType;
import com.ivanovsky.passnotes.util.FileUtils;

import java.io.File;

public class FileDescriptor implements Parcelable {

	private boolean directory;
	private boolean root;
	private FSType fsType;
	private String path;

	public static FileDescriptor fromRegularFile(File file) {
		FileDescriptor result = new FileDescriptor();
		result.fsType = FSType.REGULAR_FS;
		result.path = file.getPath();
		result.directory = file.isDirectory();
		result.root = file.getPath().equals("/");
		return result;
	}

	private FileDescriptor() {
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		FileDescriptor that = (FileDescriptor) o;

		if (directory != that.directory) return false;
		if (root != that.root) return false;
		if (fsType != that.fsType) return false;
		return path != null ? path.equals(that.path) : that.path == null;
	}

	@Override
	public int hashCode() {
		int result = (directory ? 1 : 0);
		result = 31 * result + (root ? 1 : 0);
		result = 31 * result + (fsType != null ? fsType.hashCode() : 0);
		result = 31 * result + (path != null ? path.hashCode() : 0);
		return result;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(directory ? 1 : 0);
		dest.writeInt(root ? 1 : 0);
		dest.writeSerializable(fsType);
		dest.writeString(path);
	}

	private void readFromParcel(Parcel source) {
		directory = (source.readInt() == 1);
		root = (source.readInt() == 1);
		fsType = (FSType) source.readSerializable();
		path = source.readString();
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
