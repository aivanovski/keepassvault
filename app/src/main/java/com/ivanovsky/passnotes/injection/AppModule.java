package com.ivanovsky.passnotes.injection;

import android.arch.persistence.room.Room;
import android.content.Context;

import com.ivanovsky.passnotes.data.db.AppDatabase;
import com.ivanovsky.passnotes.data.keepass.KeepassDatabaseProvider;
import com.ivanovsky.passnotes.data.safedb.EncryptedDatabaseProvider;
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
		this.db = Room.databaseBuilder(context.getApplicationContext(),
				AppDatabase.class, AppDatabase.FILE_NAME).build();
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
		return db;
	}

	@Provides
	@Singleton
	public UsedFileRepository providesUsedFileRepository() {
		return usedFileRepository;
	}

	@Provides
	@Singleton
	public EncryptedDatabaseProvider provideEncryptedDBProvider() {
		return new KeepassDatabaseProvider(context);
	}
}
