package com.ivanovsky.passnotes.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

public class DbDescriptor implements Parcelable {

	private String password;
	private File file;

	public DbDescriptor(String password, File file) {
		this.password = password;
		this.file = file;
	}

	private DbDescriptor(Parcel in) {
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

	public static Creator<DbDescriptor> CREATOR = new Creator<DbDescriptor>() {

		@Override
		public DbDescriptor createFromParcel(Parcel source) {
			return new DbDescriptor(source);
		}

		@Override
		public DbDescriptor[] newArray(int size) {
			return new DbDescriptor[size];
		}
	};
}
