package com.ivanovsky.passnotes.presentation.unlock;

import android.arch.lifecycle.LiveData;

import com.ivanovsky.passnotes.data.entity.UsedFile;
import com.ivanovsky.passnotes.presentation.core.BasePresenter;
import com.ivanovsky.passnotes.presentation.core.BaseView;
import com.ivanovsky.passnotes.presentation.core.ScreenState;

import java.io.File;
import java.util.List;

class UnlockContract {

	interface View extends BaseView<Presenter> {
	}

	interface Presenter extends BasePresenter {
		void loadData();
		LiveData<List<UsedFile>> getRecentlyUsedFilesData();
		LiveData<ScreenState> getScreenStateData();
		LiveData<Void> getShowGroupsScreenAction();
		LiveData<Void> getShowNewDatabaseScreenAction();
		LiveData<Void> getHideKeyboardAction();
		void onUnlockButtonClicked(String password, File dbFile);
	}
}