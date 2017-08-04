package com.ivanovsky.passnotes.ui.newdb;

import android.widget.EditText;

import com.ivanovsky.passnotes.ui.core.BasePresenter;
import com.ivanovsky.passnotes.ui.core.BaseView;

public class NewDatabaseContract {

	interface View extends BaseView<Presenter> {
		EditText getFilenameEditText();
		EditText getPasswordEditText();
		EditText getPasswordConfirmationEditText();
		void showHomeActivity();
		void askForPermission();
	}

	interface Presenter extends BasePresenter {
		boolean onOptionsItemSelected(int menuItemId);
		boolean validateFieldsData();
		void onDoneMenuClicked();
		void onPermissionResult(boolean granted);
	}
}
