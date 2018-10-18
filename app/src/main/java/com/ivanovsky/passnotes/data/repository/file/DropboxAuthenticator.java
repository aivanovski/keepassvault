package com.ivanovsky.passnotes.data.repository.file;

import android.content.Context;

import com.dropbox.core.android.Auth;
import com.ivanovsky.passnotes.BuildConfig;
import com.ivanovsky.passnotes.data.repository.SettingsRepository;

public class DropboxAuthenticator implements FileSystemAuthenticator {

	private final SettingsRepository settings;

	DropboxAuthenticator(SettingsRepository settings) {
		this.settings = settings;
	}

	String getAuthToken() {
		String token = Auth.getOAuth2Token();

		if (token == null && settings.getDropboxAuthToken() != null) {
			token = settings.getDropboxAuthToken();
		} else if (token != null) {
			settings.setDropboxAuthToken(token);
		}

		return token;
	}

	@Override
	public boolean isAuthenticationRequired() {
		return Auth.getOAuth2Token() == null && settings.getDropboxAuthToken() == null;
	}

	@Override
	public void startAuthActivity(Context context) {
		Auth.startOAuth2Authentication(context, BuildConfig.DROPBOX_APP_KEY);
	}
}
