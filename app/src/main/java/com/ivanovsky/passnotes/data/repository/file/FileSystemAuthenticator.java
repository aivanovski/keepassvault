package com.ivanovsky.passnotes.data.repository.file;

import android.content.Context;

public interface FileSystemAuthenticator {

	boolean isAuthenticationRequired();
	void startAuthActivity(Context context);
}
