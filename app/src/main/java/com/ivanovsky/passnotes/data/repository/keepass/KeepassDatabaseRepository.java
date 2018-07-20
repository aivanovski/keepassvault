package com.ivanovsky.passnotes.data.repository.keepass;

import android.content.Context;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.repository.encdb.exception.EncryptedDatabaseException;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey;
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository;
import com.ivanovsky.passnotes.data.repository.file.FileProvider;
import com.ivanovsky.passnotes.data.repository.file.FileResolver;
import com.ivanovsky.passnotes.util.Logger;

import org.linguafranca.pwdb.Credentials;
import org.linguafranca.pwdb.Database;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import io.reactivex.Single;

public class KeepassDatabaseRepository implements EncryptedDatabaseRepository {

	private volatile KeepassDatabase db;
	private final Context context;
	private final FileResolver fileResolver;

	public KeepassDatabaseRepository(Context context, FileResolver fileResolver) {
		this.context = context;
		this.fileResolver = fileResolver;
	}

	@Override
	public Single<EncryptedDatabase> openAsync(EncryptedDatabaseKey key, FileDescriptor file) {
		return Single.fromCallable(() -> open(key, file));
	}

	@Override
	public EncryptedDatabase open(EncryptedDatabaseKey key, FileDescriptor file)
			throws EncryptedDatabaseException {
		synchronized (this) {
			if (db != null) {
				close();
			}

			FileProvider fileProvider = fileResolver.resolveProvider(file);

			db = KeepassDatabase.fromFile(fileProvider, key.getKey());
		}

		return db;
	}

	@Override
	public boolean createNew(EncryptedDatabaseKey key, FileDescriptor file) {
		boolean result = false;

		synchronized (this) {
			Credentials defaultCredentials = new KdbxCreds("123".getBytes());
			Credentials newCredentials = new KdbxCreds(key.getKey());

			InputStream in = null;

			FileProvider fileProvider = fileResolver.resolveProvider(file);

			try {
				in = new BufferedInputStream(context.getAssets().open("default.kdbx"));//TODO: make constant

				Database keepassDb = SimpleDatabase.load(defaultCredentials, in);

				keepassDb.save(newCredentials, fileProvider.createOutputStream());

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
		synchronized (this) {
			if (db != null) {
				db = null;
			}
		}
	}
}
