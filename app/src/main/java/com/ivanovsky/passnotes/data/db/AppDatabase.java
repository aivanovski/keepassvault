package com.ivanovsky.passnotes.data.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.ivanovsky.passnotes.data.db.dao.UsedFileDao;
import com.ivanovsky.passnotes.data.db.model.UsedFile;

@Database(entities = { UsedFile.class }, version = 1)
public abstract class AppDatabase extends RoomDatabase {

	public abstract UsedFileDao getUsedFileDao();
}
