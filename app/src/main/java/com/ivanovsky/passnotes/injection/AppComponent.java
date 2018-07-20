package com.ivanovsky.passnotes.injection;

import com.ivanovsky.passnotes.injection.encdb.EncryptedDatabaseComponent;
import com.ivanovsky.passnotes.injection.encdb.EncryptedDatabaseModule;
import com.ivanovsky.passnotes.presentation.StartActivity;
import com.ivanovsky.passnotes.ui.groups.GroupsActivity;
import com.ivanovsky.passnotes.ui.groups.GroupsPresenter;
import com.ivanovsky.passnotes.presentation.newdb.NewDatabaseActivity;
import com.ivanovsky.passnotes.presentation.newdb.NewDatabasePresenter;
import com.ivanovsky.passnotes.presentation.newgroup.NewGroupPresenter;
import com.ivanovsky.passnotes.ui.notes.NotesActivity;
import com.ivanovsky.passnotes.ui.notes.NotesPresenter;
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
	void inject(NewDatabasePresenter newDatabasePresenter);
	void inject(GroupsActivity groupsActivity);
	EncryptedDatabaseComponent plus(EncryptedDatabaseModule module);
}
