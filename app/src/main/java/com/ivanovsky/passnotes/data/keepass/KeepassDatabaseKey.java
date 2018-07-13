package com.ivanovsky.passnotes.data.keepass;

import com.ivanovsky.passnotes.data.safedb.EncryptedDatabaseKey;

public class KeepassDatabaseKey implements EncryptedDatabaseKey {

	private final String password;

	public KeepassDatabaseKey(String password) {
		this.password = password;
	}

	@Override
	public byte[] getKey() {
		return password.getBytes();
	}
}
