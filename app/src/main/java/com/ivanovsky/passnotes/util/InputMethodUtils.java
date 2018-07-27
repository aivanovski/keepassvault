package com.ivanovsky.passnotes.util;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class InputMethodUtils {

	public static void hideSoftInput(@Nullable Activity activity) {
		if (activity == null) return;

		View focusedView = activity.getWindow().getCurrentFocus();
		if (focusedView != null && focusedView.getWindowToken() != null) {
			InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
			if (imm != null) {
				imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
			}
		}
	}

	public static void showSoftInput(Context context, View view) {
		if (context == null || view == null) return;

		view.requestFocus();
		view.postDelayed(() -> {
			InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
			if (imm != null) {
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 100);
	}

	private InputMethodUtils() {
	}
}
