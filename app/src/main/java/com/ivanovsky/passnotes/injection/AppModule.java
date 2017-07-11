package com.ivanovsky.passnotes.injection;

import android.arch.persistence.room.Room;
import android.content.Context;

import com.ivanovsky.passnotes.db.AppDatabase;
import com.ivanovsky.passnotes.db.dao.UsedFileDao;
import com.ivanovsky.passnotes.db.model.UsedFile;
import com.ivanovsky.passnotes.settings.SettingsManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {

	private final Context context;

	public AppModule(Context context) {
		this.context = context;
	}

	@Provides
	@Singleton
	public SettingsManager provideSettingsManager() {
		return new SettingsManager(context);
	}

	@Provides
	@Singleton
	public AppDatabase provideAppDatabase() {
		AppDatabase db = Room.databaseBuilder(context.getApplicationContext(),
				AppDatabase.class, "passnotes").build();

		//TODO: remove test values
		new Thread(() -> {
			UsedFileDao dao = db.getUsedFileDao();
			if (dao.getAll().size() == 0) {
				UsedFile usedFile = new UsedFile();

				usedFile.setFilePath("/sdcard/test.db");

				dao.insert(usedFile);
			}
		}).start();

		return db;
	}
}
