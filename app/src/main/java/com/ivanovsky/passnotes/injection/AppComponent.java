package com.ivanovsky.passnotes.injection;

import com.ivanovsky.passnotes.domain.interactor.storagelist.StorageListInteractor;
import com.ivanovsky.passnotes.injection.encdb.EncryptedDatabaseComponent;
import com.ivanovsky.passnotes.injection.encdb.EncryptedDatabaseModule;
import com.ivanovsky.passnotes.presentation.StartActivity;
import com.ivanovsky.passnotes.presentation.filepicker.FilePickerFragment;
import com.ivanovsky.passnotes.presentation.filepicker.FilePickerPresenter;
import com.ivanovsky.passnotes.presentation.groups.GroupsActivity;
import com.ivanovsky.passnotes.presentation.newdb.NewDatabaseActivity;
import com.ivanovsky.passnotes.presentation.newdb.NewDatabasePresenter;
import com.ivanovsky.passnotes.presentation.storagelist.StorageListFragment;
import com.ivanovsky.passnotes.presentation.storagelist.StorageListPresenter;
import com.ivanovsky.passnotes.presentation.unlock.UnlockPresenter;

import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;

import dagger.Component;

@Component(modules = { AppModule.class })
@Singleton
public interface AppComponent {

	EncryptedDatabaseComponent plus(EncryptedDatabaseModule module);
	void inject(StartActivity startActivity);
	void inject(UnlockPresenter unlockPresenter);
	void inject(NewDatabaseActivity newDatabaseActivity);
	void inject(NewDatabasePresenter newDatabasePresenter);
	void inject(GroupsActivity groupsActivity);
	void inject(@NotNull StorageListPresenter storageListPresenter);
	void inject(@NotNull FilePickerPresenter filePickerPresenter);
	void inject(@NotNull FilePickerFragment filePickerFragment);
	void inject(@NotNull StorageListInteractor storageListInteractor);
	void inject(@NotNull StorageListFragment storageListFragment);
}
