package com.ivanovsky.passnotes.data.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.UUID;

// TODO: Move to Kotlin
public class Group implements Parcelable {

    private UUID uid;
    private String title;

    public Group() {
    }

    protected Group(Parcel in) {
        title = in.readString();
        uid = (UUID) in.readSerializable();
    }

    public UUID getUid() {
        return uid;
    }

    public void setUid(UUID uid) {
        this.uid = uid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeSerializable(uid);
    }

    public static final Creator<Group> CREATOR = new Creator<Group>() {
        @Override
        public Group createFromParcel(Parcel in) {
            return new Group(in);
        }

        @Override
        public Group[] newArray(int size) {
            return new Group[size];
        }
    };
}
