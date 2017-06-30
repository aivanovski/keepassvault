package com.ivanovsky.passnotes.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.ivanovsky.passnotes.db.dao.UsedFileDao;
import com.ivanovsky.passnotes.db.model.UsedFile;

@Database(entities = { UsedFile.class }, version = 1)
public abstract class AppDatabase extends RoomDatabase {

	public abstract UsedFileDao getUsedFileDao();
}
