package com.ivanovsky.passnotes.data.repository.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.ivanovsky.passnotes.data.entity.DropboxFileLink;
import com.ivanovsky.passnotes.data.repository.db.dao.DropboxFileLinkDao;
import com.ivanovsky.passnotes.data.repository.db.dao.UsedFileDao;
import com.ivanovsky.passnotes.data.entity.UsedFile;

@Database(entities = { UsedFile.class, DropboxFileLink.class }, version = 1)
@TypeConverters(UsedFile.FSTypeConverter.class)
public abstract class AppDatabase extends RoomDatabase {

	public static final String FILE_NAME = "passnotes.db";

	public abstract UsedFileDao getUsedFileDao();
	public abstract DropboxFileLinkDao getDropboxFileLinkDao();
}
