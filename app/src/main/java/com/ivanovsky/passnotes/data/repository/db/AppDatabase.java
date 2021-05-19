package com.ivanovsky.passnotes.data.repository.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.ivanovsky.passnotes.data.repository.db.converters.FSAuthorityTypeConverter;
import com.ivanovsky.passnotes.data.repository.db.converters.FSTypeConverter;
import com.ivanovsky.passnotes.data.entity.RemoteFile;
import com.ivanovsky.passnotes.data.repository.db.dao.RemoteFileDao;
import com.ivanovsky.passnotes.data.repository.db.dao.UsedFileDao;
import com.ivanovsky.passnotes.data.entity.UsedFile;

//TODO: param exportSchema should be true, this should be fixed before release
@Database(entities = {
		UsedFile.class,
		RemoteFile.class
},
		version = 1,
		exportSchema = false)
@TypeConverters(FSAuthorityTypeConverter.class)
public abstract class AppDatabase extends RoomDatabase {

	public static final String FILE_NAME = "passnotes.db";

	public abstract UsedFileDao getUsedFileDao();
	public abstract RemoteFileDao getRemoteFileDao();
}
