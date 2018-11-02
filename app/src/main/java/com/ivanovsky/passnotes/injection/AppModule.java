package com.ivanovsky.passnotes.injection;

import android.arch.persistence.room.Room;
import android.content.Context;

import com.ivanovsky.passnotes.data.ObserverBus;
import com.ivanovsky.passnotes.data.repository.db.AppDatabase;
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver;
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseRepository;
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository;
import com.ivanovsky.passnotes.data.repository.UsedFileRepository;
import com.ivanovsky.passnotes.data.repository.SettingsRepository;
import com.ivanovsky.passnotes.domain.ClipboardHelper;
import com.ivanovsky.passnotes.domain.PermissionHelper;
import com.ivanovsky.passnotes.domain.ResourceHelper;
import com.ivanovsky.passnotes.domain.interactor.filepicker.FilePickerInteractor;
import com.ivanovsky.passnotes.domain.interactor.newdb.NewDatabaseInteractor;
import com.ivanovsky.passnotes.domain.interactor.storagelist.StorageListInteractor;
import com.ivanovsky.passnotes.domain.interactor.unlock.UnlockInteractor;
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor;

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
	SettingsRepository provideSettingsManager() {
		return new SettingsRepository(context);
	}

	@Provides
	@Singleton
	AppDatabase provideAppDatabase() {
		return db;
	}

	@Provides
	@Singleton
	UsedFileRepository provideUsedFileRepository() {
		return usedFileRepository;
	}

	@Provides
	@Singleton
	EncryptedDatabaseRepository provideEncryptedDBProvider(FileSystemResolver fileSystemResolver) {
		return new KeepassDatabaseRepository(context, fileSystemResolver);
	}

	@Provides
	@Singleton
	ObserverBus provideObserverBus() {
		return new ObserverBus();
	}

	@Provides
	@Singleton
	FileSystemResolver provideFilSystemResolver(SettingsRepository settings) {
		return new FileSystemResolver(settings);
	}

	@Provides
	@Singleton
	UnlockInteractor provideUnlockInteractor(EncryptedDatabaseRepository dbRepository,
											 UsedFileRepository usedFileRepository,
											 ObserverBus observerBus) {
		return new UnlockInteractor(usedFileRepository, dbRepository, observerBus);
	}

	@Provides
	@Singleton
	ErrorInteractor provideErrorInteractor() {
		return new ErrorInteractor(context);
	}

	@Provides
	@Singleton
	NewDatabaseInteractor provideNewDatabaseInteractor(EncryptedDatabaseRepository dbRepository,
													   UsedFileRepository usedFileRepository,
													   FileSystemResolver fileSystemResolver,
													   ObserverBus observerBus) {
		return new NewDatabaseInteractor(dbRepository, usedFileRepository, fileSystemResolver, observerBus);
	}

	@Provides
	@Singleton
	ClipboardHelper provideClipboardHelper() {
		return new ClipboardHelper(context);
	}

	@Provides
	@Singleton
	StorageListInteractor provideStorageInteractor() {
		return new StorageListInteractor(context);
	}

	@Provides
	@Singleton
	FilePickerInteractor provideFilePickerInteractor(FileSystemResolver fileSystemResolver) {
		return new FilePickerInteractor(fileSystemResolver);
	}

	@Provides
	@Singleton
	PermissionHelper providerPermisionHelper() {
		return new PermissionHelper(context);
	}

	@Provides
	@Singleton
	ResourceHelper providerResourceHelper() {
		return new ResourceHelper(context);
	}
}
