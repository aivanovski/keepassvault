package com.ivanovsky.passnotes.domain.interactor;

import android.content.Context;

import com.ivanovsky.passnotes.R;

public class ErrorInteractor {

	private final Context context;

	public ErrorInteractor(Context context) {
		this.context = context;
	}

	public String getErrorMessage(Throwable throwable) {
		return context.getString(R.string.error_was_occurred);
	}
}
