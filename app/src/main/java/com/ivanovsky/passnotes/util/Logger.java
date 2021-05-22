package com.ivanovsky.passnotes.util;

import android.util.Log;

public class Logger {

    public static void d(String tag, String message) {
        Log.d(tag, message);
    }

    public static void d(String tag, String message, Object... args) {
        Log.d(tag, String.format(message, args));
    }

    public static void e(String tag, String message, Object... args) {
        Log.e(tag, String.format(message, args));
    }

    public static void printStackTrace(Exception exception) {
        exception.printStackTrace();
    }

    private Logger() {
    }
}
