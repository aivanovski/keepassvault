package com.ivanovsky.passnotes.injection;

import androidx.room.Room;
import android.content.Context;

import com.ivanovsky.passnotes.data.ObserverBus;
import com.ivanovsky.passnotes.data.repository.DropboxFileRepository;
import com.ivanovsky.passnotes.data.repository.db.AppDatabase;
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver;
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseRepository;
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository;
import com.ivanovsky.passnotes.data.repository.UsedFileRepository;
import com.ivanovsky.passnotes.data.repository.SettingsRepository;
import com.ivanovsky.passnotes.domain.ClipboardHelper;
import com.ivanovsky.passnotes.domain.FileHelper;
import com.ivanovsky.passnotes.domain.FileSyncHelper;
import com.ivanovsky.passnotes.domain.NoteDiffer;
import com.ivanovsky.passnotes.domain.PermissionHelper;
import com.ivanovsky.passnotes.domain.ResourceHelper;
import com.ivanovsky.passnotes.domain.globalsnackbar.GlobalSnackbarBus;
import com.ivanovsky.passnotes.domain.interactor.debugmenu.DebugMenuInteractor;
import com.ivanovsky.passnotes.domain.interactor.filepicker.FilePickerInteractor;
import com.ivanovsky.passnotes.domain.interactor.group.GroupInteractor;
import com.ivanovsky.passnotes.domain.interactor.groups.GroupsInteractor;
import com.ivanovsky.passnotes.domain.interactor.newdb.NewDatabaseInteractor;
import com.ivanovsky.passnotes.domain.interactor.note.NoteInteractor;
import com.ivanovsky.passnotes.domain.interactor.note_editor.NoteEditorInteractor;
import com.ivanovsky.passnotes.domain.interactor.notes.NotesInteractor;
import com.ivanovsky.passnotes.domain.interactor.storagelist.StorageListInteractor;
import com.ivanovsky.passnotes.domain.interactor.unlock.UnlockInteractor;
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor;

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
	SettingsRepository provideSettingsManager() {
		return new SettingsRepository(context);
	}

	@Provides
	@Singleton
	AppDatabase provideAppDatabase() {
		return Room.databaseBuilder(context.getApplicationContext(),
				AppDatabase.class, AppDatabase.FILE_NAME).build();
	}

	@Provides
	@Singleton
	UsedFileRepository provideUsedFileRepository(AppDatabase db) {
		return new UsedFileRepository(db);
	}

	@Provides
	@Singleton
	DropboxFileRepository provideDropboxFileRepository(AppDatabase db) {
		return new DropboxFileRepository(db.getDropboxFileDao());
	}

	@Provides
	@Singleton
	EncryptedDatabaseRepository provideEncryptedDatabaseRepository(FileSystemResolver fileSystemResolver) {
		return new KeepassDatabaseRepository(context, fileSystemResolver);
	}

	@Provides
	@Singleton
	ObserverBus provideObserverBus() {
		return new ObserverBus();
	}

	@Provides
	@Singleton
	FileSystemResolver provideFilSystemResolver(SettingsRepository settings,
												DropboxFileRepository dropboxFileRepository,
												FileHelper fileHelper) {
		return new FileSystemResolver(settings, dropboxFileRepository, fileHelper);
	}

	@Provides
	@Singleton
	FileSyncHelper provideFileSyncHelper(FileSystemResolver resolver) {
		return new FileSyncHelper(resolver);
	}

	@Provides
	@Singleton
	UnlockInteractor provideUnlockInteractor(EncryptedDatabaseRepository dbRepository,
											 UsedFileRepository usedFileRepository,
											 ObserverBus observerBus,
											 FileSyncHelper fileSyncHelper) {
		return new UnlockInteractor(usedFileRepository, dbRepository, observerBus, fileSyncHelper);
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
	DebugMenuInteractor provideDebugMenuInteractor(FileSystemResolver fileSystemResolver,
												   EncryptedDatabaseRepository dbRepository,
												   FileHelper fileHelper) {
		return new DebugMenuInteractor(fileSystemResolver, dbRepository, fileHelper);
	}

	@Provides
	@Singleton
	PermissionHelper providerPermissionHelper() {
		return new PermissionHelper(context);
	}

	@Provides
	@Singleton
	ResourceHelper providerResourceHelper() {
		return new ResourceHelper(context);
	}

	@Provides
	@Singleton
	GlobalSnackbarBus provideGlobalSnackbarBus() {
		return new GlobalSnackbarBus();
	}

	@Provides
	@Singleton
	FileHelper provideFileHelper(SettingsRepository settings) {
		return new FileHelper(context, settings);
	}

	@Provides
	@Singleton
	GroupsInteractor provideGroupsInteractor(EncryptedDatabaseRepository dbRepo) {
		return new GroupsInteractor(dbRepo);
	}

	@Provides
	@Singleton
	NotesInteractor provideNotesInteractor(EncryptedDatabaseRepository dbRepo) {
		return new NotesInteractor(dbRepo);
	}

	@Provides
	@Singleton
	GroupInteractor provideNewGroupInteractor(EncryptedDatabaseRepository dbRepo,
											  ResourceHelper resourceHelper,
											  ObserverBus observerBus) {
		return new GroupInteractor(dbRepo, resourceHelper, observerBus);
	}

	@Provides
	@Singleton
	NoteInteractor provideAddEditNoteInteractor(EncryptedDatabaseRepository dbRepo,
												ClipboardHelper clipboardHelper) {
		return new NoteInteractor(dbRepo, clipboardHelper);
	}

	@Provides
	@Singleton
	NoteEditorInteractor providerNoteEditorInteractor(EncryptedDatabaseRepository dbRepo,
													  ObserverBus observerBus) {
		return new NoteEditorInteractor(dbRepo, observerBus);
	}

	@Provides
	@Singleton
	NoteDiffer provideNoteDiffer() {
		return new NoteDiffer();
	}
}
