package com.ivanovsky.passnotes.injection.encdb;

import com.ivanovsky.passnotes.presentation.newgroup.NewGroupPresenter;
import com.ivanovsky.passnotes.ui.groups.GroupsActivity;
import com.ivanovsky.passnotes.ui.groups.GroupsPresenter;
import com.ivanovsky.passnotes.ui.notes.NotesPresenter;

import org.jetbrains.annotations.NotNull;

import dagger.Subcomponent;

@EncryptedDatabaseScope
@Subcomponent(modules = { EncryptedDatabaseModule.class })
public interface EncryptedDatabaseComponent {

	void inject(NewGroupPresenter newGroupPresenter);
	void inject(@NotNull GroupsActivity groupsActivity);
	void inject(@NotNull GroupsPresenter groupsPresenter);
	void inject(@NotNull NotesPresenter notesPresenter);
}
