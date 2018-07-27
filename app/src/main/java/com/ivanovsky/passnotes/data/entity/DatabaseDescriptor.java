package com.ivanovsky.passnotes.data.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

public class DatabaseDescriptor implements Parcelable {

	private String password;
	private File file;

	public DatabaseDescriptor(String password, File file) {
		this.password = password;
		this.file = file;
	}

	private DatabaseDescriptor(Parcel in) {
		readFromParcel(in);
	}

	public String getPassword() {
		return password;
	}

	public File getFile() {
		return file;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(password);
		dest.writeString(file.getPath());
	}

	private void readFromParcel(Parcel source) {
		password = source.readString();
		file = new File(source.readString());
	}

	public static Creator<DatabaseDescriptor> CREATOR = new Creator<DatabaseDescriptor>() {

		@Override
		public DatabaseDescriptor createFromParcel(Parcel source) {
			return new DatabaseDescriptor(source);
		}

		@Override
		public DatabaseDescriptor[] newArray(int size) {
			return new DatabaseDescriptor[size];
		}
	};
}
