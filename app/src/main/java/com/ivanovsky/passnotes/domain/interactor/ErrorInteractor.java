package com.ivanovsky.passnotes.domain.interactor;

import android.content.Context;

import com.ivanovsky.passnotes.BuildConfig;
import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.entity.OperationError;

public class ErrorInteractor {

	private final Context context;

	public ErrorInteractor(Context context) {
		this.context = context;
	}

	public void process(OperationError error) {
		//TODO: implement method
	}

	public String processAndGetMessage(OperationError error) {
		String message;

		process(error);

		if (error.getMessage() != null) {
			message = error.getMessage();
		} else if (error.getThrowable() != null) {
			message = getDefaultMessageForException(error.getThrowable());
		} else {
			message = context.getString(R.string.error_was_occurred);
		}

		return message;
	}

	private String getDefaultMessageForException(Throwable throwable) {
		String message;

		//TODO: implement network error handling

		if (BuildConfig.DEBUG) {
			message = throwable.toString();
		} else {
			message = context.getString(R.string.error_was_occurred);
		}

		return message;
	}
}
