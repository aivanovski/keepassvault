package com.ivanovsky.passnotes.data.keepass;

import com.ivanovsky.passnotes.data.keepass.dao.KeepassGroupDao;
import com.ivanovsky.passnotes.data.safedb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.safedb.EncryptedDatabaseOperationException;
import com.ivanovsky.passnotes.data.safedb.GroupRepository;
import com.ivanovsky.passnotes.util.Logger;

import org.linguafranca.pwdb.Credentials;
import org.linguafranca.pwdb.Database;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class KeepassDatabase implements EncryptedDatabase {

	private SimpleDatabase db;
	private final KeepassGroupRepository groupRepository;

	static KeepassDatabase fromFile(File file, byte[] key) throws EncryptedDatabaseOperationException {
		Credentials credentials = new KdbxCreds(key);

		InputStream in = null;
		SimpleDatabase db;

		try {
			in = new BufferedInputStream(new FileInputStream(file));
			db = SimpleDatabase.load(credentials, in);

		} catch (Exception e) {
			Logger.printStackTrace(e);
			throw new EncryptedDatabaseOperationException(e);

		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					Logger.printStackTrace(e);
				}
			}
		}

		return new KeepassDatabase(db);
	}

	private KeepassDatabase(SimpleDatabase keepassDb) {
		this.db = keepassDb;

		KeepassGroupDao groupDao = new KeepassGroupDao(keepassDb);

		this.groupRepository = new KeepassGroupRepository(groupDao);
	}

	@Override
	public GroupRepository getGroupRepository() {
		return groupRepository;
	}

	@Override
	public void commit() {

	}

	Database getKeepassDatabase() {
		return db;
	}
}
