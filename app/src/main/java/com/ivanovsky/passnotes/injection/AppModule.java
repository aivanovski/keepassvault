package com.ivanovsky.passnotes.injection;

import android.arch.persistence.room.Room;
import android.content.Context;

import com.ivanovsky.passnotes.data.ObserverBus;
import com.ivanovsky.passnotes.data.repository.db.AppDatabase;
import com.ivanovsky.passnotes.data.repository.file.FileResolver;
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseRepository;
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository;
import com.ivanovsky.passnotes.data.repository.UsedFileRepository;
import com.ivanovsky.passnotes.data.repository.SettingsRepository;

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
	public SettingsRepository provideSettingsManager() {
		return new SettingsRepository(context);
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
	public EncryptedDatabaseRepository provideEncryptedDBProvider(FileResolver fileResolver) {
		return new KeepassDatabaseRepository(context, fileResolver);
	}

	@Provides
	@Singleton
	public ObserverBus provideObserverBus() {
		return new ObserverBus();
	}

	@Provides
	@Singleton
	public FileResolver provideFileResolver() {
		return new FileResolver();
	}
}
