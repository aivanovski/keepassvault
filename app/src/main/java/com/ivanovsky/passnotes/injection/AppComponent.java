package com.ivanovsky.passnotes.injection;

import com.ivanovsky.passnotes.ui.StartActivity;
import com.ivanovsky.passnotes.ui.newdb.NewDatabaseActivity;
import com.ivanovsky.passnotes.ui.newdb.NewDatabasePresenter;
import com.ivanovsky.passnotes.ui.newgroup.NewGroupPresenter;
import com.ivanovsky.passnotes.ui.groups.GroupsActivity;
import com.ivanovsky.passnotes.ui.groups.GroupsPresenter;
import com.ivanovsky.passnotes.ui.notes.NotesActivity;
import com.ivanovsky.passnotes.ui.notes.NotesPresenter;
import com.ivanovsky.passnotes.ui.unlock.UnlockPresenter;

import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;

import dagger.Component;

@Component(modules = { AppModule.class })
@Singleton
public interface AppComponent {

	void inject(StartActivity startActivity);
	void inject(UnlockPresenter unlockPresenter);
	void inject(NewDatabaseActivity newDatabaseActivity);
	void inject(NewDatabasePresenter newDatabasePresenter);
	void inject(GroupsPresenter groupsPresenter);
	void inject(NewGroupPresenter newGroupPresenter);
	void inject(GroupsActivity groupsActivity);
	void inject(@NotNull NotesActivity notesActivity);
	void inject(@NotNull NotesPresenter notesPresenter);
}
