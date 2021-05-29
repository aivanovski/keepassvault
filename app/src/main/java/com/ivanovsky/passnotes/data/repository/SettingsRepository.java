package com.ivanovsky.passnotes.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SettingsRepository {

    private static final String TAG = SettingsRepository.class.getSimpleName();

    private static final String DROPBOX_AUTH_TOKEN = "dropboxAuthToken";
    private static final String IS_EXTERNAL_STORAGE_CACHE_ENABLED = "isExternalStorageCacheEnabled";
    private static final String AUTO_LOCK_DELAY_IN_MS = "autoLockDelay";
    private static final String IS_LOCK_NOTIFICATION_VISIBLE = "isLockNotificationVisible";

    private static final int DEFAULT_AUTO_LOCK_DELAY = (int) TimeUnit.MINUTES.toMillis(5);

    private static final Map<String, DefaultValue<?>> DEFAULT_VALUES = createDefaultValuesMap();

    private final SharedPreferences preferences;

    private static Map<String, DefaultValue<?>> createDefaultValuesMap() {
        Map<String, DefaultValue<?>> map = new HashMap<>();

        map.put(DROPBOX_AUTH_TOKEN, new DefaultValue<>(null, String.class));
        map.put(IS_EXTERNAL_STORAGE_CACHE_ENABLED, new DefaultValue<>(false, Boolean.class));
        map.put(AUTO_LOCK_DELAY_IN_MS, new DefaultValue<>(DEFAULT_AUTO_LOCK_DELAY, Integer.class));
        map.put(IS_LOCK_NOTIFICATION_VISIBLE, new DefaultValue<>(true, Boolean.class));

        return map;
    }

    public SettingsRepository(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public String getDropboxAuthToken() {
        return getString(DROPBOX_AUTH_TOKEN);
    }

    public void setDropboxAuthToken(String value) {
        putString(DROPBOX_AUTH_TOKEN, value);
    }

    public boolean isExternalStorageCacheEnabled() {
        return getBoolean(IS_EXTERNAL_STORAGE_CACHE_ENABLED);
    }

    public void setExternalStorageCacheEnabled(boolean externalStorageCacheEnabled) {
        putBoolean(IS_EXTERNAL_STORAGE_CACHE_ENABLED, externalStorageCacheEnabled);
    }

    public Integer getAutoLockDelayInMs() {
        int value = getInt(AUTO_LOCK_DELAY_IN_MS);
        return (value != -1) ? value : null;
    }

    public void setAutoLockDelayInMs(Integer delayInMs) {
        putInt(AUTO_LOCK_DELAY_IN_MS, (delayInMs != null) ? delayInMs : -1);
    }

    public boolean isLockNotificationVisible() {
        return getBoolean(IS_LOCK_NOTIFICATION_VISIBLE);
    }

    public void setLockNotificationVisible(boolean lockNotificationVisible) {
        putBoolean(IS_LOCK_NOTIFICATION_VISIBLE, lockNotificationVisible);
    }

    public void clean() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }

    private boolean getBoolean(String key) {
        return preferences.getBoolean(key, getDefaultValue(key, Boolean.class));
    }

    private String getString(String key) {
        return preferences.getString(key, getDefaultValue(key, String.class));
    }

    private int getInt(String key) {
        return preferences.getInt(key, getDefaultValue(key, Integer.class));
    }

    private void putBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private void putString(String key, String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private void putInt(String key, int value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    @SuppressWarnings({"UnusedParameters", "unchecked", "ConstantConditions"})
    private <T> T getDefaultValue(String name, Class<T> type) {
        return (T) DEFAULT_VALUES.get(name).value;
    }

    private static class DefaultValue<T> {

        final Object value;
        final Class<?> type;

        DefaultValue(T value, Class<?> type) {
            this.value = value;
            this.type = type;
        }
    }
}
