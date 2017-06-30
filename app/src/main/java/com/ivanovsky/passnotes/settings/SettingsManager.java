package com.ivanovsky.passnotes.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashMap;
import java.util.Map;

public class SettingsManager {

	private static final Map<String, DefaultValue> DEFAULT_VALUES = createDefaultValuesMap();

	private SharedPreferences preferences;

	private static Map<String, DefaultValue> createDefaultValuesMap() {
		Map<String, DefaultValue> map = new HashMap<>();

//		map.put(IS_LOGGED_IN, new DefaultValue<>(false, Integer.class));

		return map;
	}

	public SettingsManager(Context context) {
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
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
