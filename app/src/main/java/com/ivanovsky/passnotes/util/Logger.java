package com.ivanovsky.passnotes.util;

import android.util.Log;

public class Logger {

	public static void d(String tag, String message) {
		Log.d(tag, message);
	}

	public static void printStackTrace(Exception exception) {
		exception.printStackTrace();
	}
}
