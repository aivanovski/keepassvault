package com.ivanovsky.passnotes.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashMap;
import java.util.Map;

public class SettingsRepository {

	private static final Map<String, DefaultValue> DEFAULT_VALUES = createDefaultValuesMap();

	private static final String DROPBOX_AUTH_TOKEN = "dropboxAuthToken";
	private static final String IS_EXTERNAL_STORAGE_CACHE_ENABLED = "isExternalStorageCacheEnabled";

	private SharedPreferences preferences;

	private static Map<String, DefaultValue> createDefaultValuesMap() {
		Map<String, DefaultValue> map = new HashMap<>();

		map.put(DROPBOX_AUTH_TOKEN, new DefaultValue<>(null, String.class));
		map.put(IS_EXTERNAL_STORAGE_CACHE_ENABLED, new DefaultValue<>(false, Boolean.class));

		return map;
	}

	public SettingsRepository(Context context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public String getDropboxAuthToken() {
		return preferences.getString(DROPBOX_AUTH_TOKEN, getDefaultValue(DROPBOX_AUTH_TOKEN, String.class));
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

	public void clean() {
		SharedPreferences.Editor editor = preferences.edit();
		editor.clear();
		editor.apply();
	}

	private boolean getBoolean(String key) {
		return preferences.getBoolean(key, getDefaultValue(key, Boolean.class));
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

	@SuppressWarnings("UnusedParameters")
	private <T> T getDefaultValue(String name, Class<T> type) {
		//noinspection unchecked
		return (T) DEFAULT_VALUES.get(name).value;
	}

	private static class DefaultValue<T> {

		final Object value;
		final Class type;

		DefaultValue(T value, Class type) {
			this.value = value;
			this.type = type;
		}
	}
}
