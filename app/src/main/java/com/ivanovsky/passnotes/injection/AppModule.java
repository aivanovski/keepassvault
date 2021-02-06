package com.ivanovsky.passnotes.injection;

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
import com.ivanovsky.passnotes.domain.DispatcherProvider;
import com.ivanovsky.passnotes.domain.FileHelper;
import com.ivanovsky.passnotes.domain.FileSyncHelper;
import com.ivanovsky.passnotes.domain.LocaleProvider;
import com.ivanovsky.passnotes.domain.NoteDiffer;
import com.ivanovsky.passnotes.domain.PermissionHelper;
import com.ivanovsky.passnotes.domain.ResourceProvider;
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
	private final SharedModule module;

	public AppModule(Context context, SharedModule module) {
		this.context = context;
		this.module = module;
	}

	@Provides
	@Singleton
	SettingsRepository provideSettingsManager() {
		return module.getSettings();
	}

	@Provides
	@Singleton
	AppDatabase provideAppDatabase() {
		return module.getDatabase();
	}

	@Provides
	@Singleton
	UsedFileRepository provideUsedFileRepository() {
		return module.getUsedFileRepository();
	}

	@Provides
	@Singleton
	DropboxFileRepository provideDropboxFileRepository() {
		return module.getDropboxFileRepository();
	}

	@Provides
	@Singleton
	EncryptedDatabaseRepository provideEncryptedDatabaseRepository() {
		return module.getEncryptedDatabaseRepository();
	}

	@Provides
	@Singleton
	ObserverBus provideObserverBus() {
		return module.getObserverBus();
	}

	@Provides
	@Singleton
	FileSystemResolver provideFilSystemResolver() {
		return module.getFileSystemResolver();
	}

	@Provides
	@Singleton
	FileSyncHelper provideFileSyncHelper() {
		return module.getFileSyncHelper();
	}

	@Provides
	@Singleton
	ErrorInteractor provideErrorInteractor() {
		return module.getErrorInteractor();
	}

	@Provides
	@Singleton
	ClipboardHelper provideClipboardHelper() {
		return new ClipboardHelper(context);
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
		return module.getPermissionHelper();
	}

	@Provides
	@Singleton
	ResourceProvider providerResourceHelper() {
		return module.getResourceProvider();
	}

	@Provides
	@Singleton
	GlobalSnackbarBus provideGlobalSnackbarBus() {
		return new GlobalSnackbarBus();
	}

	@Provides
	@Singleton
	FileHelper provideFileHelper(SettingsRepository settings) {
		return module.getFileHelper();
	}

	@Provides
	@Singleton
	GroupsInteractor provideGroupsInteractor(EncryptedDatabaseRepository dbRepo, ObserverBus observerBus) {
		return new GroupsInteractor(dbRepo, observerBus);
	}

	@Provides
	@Singleton
	NotesInteractor provideNotesInteractor(EncryptedDatabaseRepository dbRepo) {
		return new NotesInteractor(dbRepo);
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

	@Provides
	@Singleton
	LocaleProvider provideLocaleProvider() {
	    return module.getLocaleProvider();
	}

	@Provides
	@Singleton
	DispatcherProvider provideDispatcherProvider() {
		return module.getDispatcherProvider();
	}
}
