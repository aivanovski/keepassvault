package com.ivanovsky.passnotes.util;

import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;

import java.util.Locale;

public class LocaleUtils {

	@NonNull
	public static Locale getSystemLocale(@NonNull Context context) {
		Locale result;

		if (Build.VERSION.SDK_INT >= 24) {
			result = context.getResources().getConfiguration().getLocales().get(0);
		} else {
			result = context.getResources().getConfiguration().locale;
		}

		return result;
	}
}
