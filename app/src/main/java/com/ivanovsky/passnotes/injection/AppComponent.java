package com.ivanovsky.passnotes.injection;

import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabase;
import com.ivanovsky.passnotes.domain.interactor.storagelist.StorageListInteractor;
import com.ivanovsky.passnotes.presentation.StartActivity;
import com.ivanovsky.passnotes.presentation.debugmenu.DebugMenuPresenter;
import com.ivanovsky.passnotes.presentation.filepicker.FilePickerFragment;
import com.ivanovsky.passnotes.presentation.filepicker.FilePickerPresenter;
import com.ivanovsky.passnotes.presentation.group.GroupPresenter;
import com.ivanovsky.passnotes.presentation.groups.GroupsActivity;
import com.ivanovsky.passnotes.presentation.groups.GroupsPresenter;
import com.ivanovsky.passnotes.presentation.groups.NewEntryDialog;
import com.ivanovsky.passnotes.presentation.newdb.NewDatabaseActivity;
import com.ivanovsky.passnotes.presentation.newdb.NewDatabasePresenter;
import com.ivanovsky.passnotes.presentation.note.NotePresenter;
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorPresenter;
import com.ivanovsky.passnotes.presentation.note_editor.view.NoteEditorDataTransformer;
import com.ivanovsky.passnotes.presentation.notes.NotesPresenter;
import com.ivanovsky.passnotes.presentation.storagelist.StorageListFragment;
import com.ivanovsky.passnotes.presentation.storagelist.StorageListPresenter;
import com.ivanovsky.passnotes.presentation.unlock.UnlockPresenter;

import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;

import dagger.Component;

@Component(modules = { AppModule.class })
@Singleton
public interface AppComponent {

	void inject(StartActivity startActivity);
	void inject(UnlockPresenter unlockPresenter);
	void inject(NewDatabaseActivity newDatabaseActivity);
	void inject(@NotNull NewDatabasePresenter newDatabasePresenter);
	void inject(@NotNull StorageListPresenter storageListPresenter);
	void inject(@NotNull FilePickerPresenter filePickerPresenter);
	void inject(@NotNull FilePickerFragment filePickerFragment);
	void inject(@NotNull StorageListInteractor storageListInteractor);
	void inject(@NotNull StorageListFragment storageListFragment);
	void inject(KeepassDatabase keepassDatabase);
	void inject(@NotNull DebugMenuPresenter debugMenuPresenter);
    void inject(@NotNull NewEntryDialog newEntryDialog);
	void inject(@NotNull GroupsActivity groupsActivity);
	void inject(@NotNull GroupsPresenter groupsPresenter);
	void inject(@NotNull NotesPresenter notesPresenter);
	void inject(@NotNull NotePresenter notePresenter);
	void inject(@NotNull GroupPresenter groupPresenter);
	void inject(@NotNull NoteEditorPresenter noteEditorPresenter);
    void inject(@NotNull NoteEditorDataTransformer noteEditorDataTransformer);
}
