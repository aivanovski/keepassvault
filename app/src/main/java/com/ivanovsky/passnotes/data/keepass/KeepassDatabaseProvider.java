package com.ivanovsky.passnotes.data.keepass;

import android.content.Context;

import com.ivanovsky.passnotes.data.safedb.DbOpenException;
import com.ivanovsky.passnotes.data.safedb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.safedb.EncryptedDatabaseKey;
import com.ivanovsky.passnotes.data.safedb.EncryptedDatabaseProvider;
import com.ivanovsky.passnotes.util.Logger;

import org.linguafranca.pwdb.Credentials;
import org.linguafranca.pwdb.Database;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.reactivex.Single;

public class KeepassDatabaseProvider implements EncryptedDatabaseProvider {

	private volatile File dbFile;
	private volatile KeepassDatabase db;
	private final Context context;
	private final Object lock;

	public KeepassDatabaseProvider(Context context) {
		this.context = context;
		this.lock = new Object();
	}

	@Override
	public boolean isOpened() {
		return db != null;
	}

	@Override
	public EncryptedDatabase getDatabase() {
		return db;
	}

	@Override
	public String getOpenedDatabasePath() {
		return dbFile != null ? dbFile.getPath() : null;
	}

	@Override
	public EncryptedDatabase open(EncryptedDatabaseKey key, File file) throws DbOpenException {
		synchronized (lock) {
			if (db != null) {
				close();
			}

			Credentials credentials = new KdbxCreds(key.getKey());

			InputStream in = null;
			Database keepassDb;

			try {
				in = new BufferedInputStream(new FileInputStream(file));
				keepassDb = SimpleDatabase.load(credentials, in);

				db = new KeepassDatabase(keepassDb);
				dbFile = file;

			} catch (Exception e) {
				Logger.printStackTrace(e);
				throw new DbOpenException(e);

			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						Logger.printStackTrace(e);
					}
				}
			}
		}

		return db;
	}

	@Override
	public Single<EncryptedDatabase> openAsync(EncryptedDatabaseKey key, File file) {
		return Single.fromCallable(() -> open(key, file));
	}

	@Override
	public boolean createNew(EncryptedDatabaseKey key, File file) {
		boolean result = false;

		synchronized (lock) {
			Credentials defaultCredentials = new KdbxCreds("123".getBytes());
			Credentials newCredentials = new KdbxCreds(key.getKey());

			InputStream in = null;

			try {
				in = new BufferedInputStream(context.getAssets().open("default.kdbx"));//TODO: make constant

				Database keepassDb = SimpleDatabase.load(defaultCredentials, in);

				keepassDb.save(newCredentials, new BufferedOutputStream(new FileOutputStream(file, false)));

				result = true;
			} catch (Exception e) {
				Logger.printStackTrace(e);
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						Logger.printStackTrace(e);
					}
				}
			}
		}

		return result;
	}

	@Override
	public void close() {
		synchronized (lock) {
			if (db != null) {
				db = null;
				dbFile = null;
			}
		}
	}
}
