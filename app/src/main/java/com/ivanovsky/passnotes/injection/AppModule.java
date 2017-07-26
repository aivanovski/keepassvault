package com.ivanovsky.passnotes.injection;

import android.arch.persistence.room.Room;
import android.content.Context;

import com.ivanovsky.passnotes.data.db.AppDatabase;
import com.ivanovsky.passnotes.data.db.dao.UsedFileDao;
import com.ivanovsky.passnotes.data.db.model.UsedFile;
import com.ivanovsky.passnotes.data.repository.UsedFileRepository;
import com.ivanovsky.passnotes.settings.SettingsManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {

	private final Context context;
	private final AppDatabase db;
	private final UsedFileRepository usedFileRepository;

	public AppModule(Context context) {
		this.context = context;
		this.db = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "passnotes").build();
		this.usedFileRepository = new UsedFileRepository(db);
	}

	@Provides
	@Singleton
	public SettingsManager provideSettingsManager() {
		return new SettingsManager(context);
	}

	@Provides
	@Singleton
	public AppDatabase provideAppDatabase() {
		//TODO: remove test values
		new Thread(() -> {
			UsedFileDao dao = db.getUsedFileDao();
			if (dao.getAll().size() == 0) {
				UsedFile usedFile = new UsedFile();

				usedFile.setFilePath("/sdcard/test.db");

				dao.insert(usedFile);
			}

			usedFileRepository.notifyDataSetChanged();
		}).start();

		return db;
	}

	@Provides
	@Singleton
	public UsedFileRepository providesUsedFileRepository() {
		return usedFileRepository;
	}
}
