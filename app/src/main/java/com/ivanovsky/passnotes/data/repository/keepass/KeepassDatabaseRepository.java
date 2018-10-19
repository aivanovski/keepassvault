package com.ivanovsky.passnotes.data.repository.keepass;

import android.content.Context;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.repository.encdb.exception.EncryptedDatabaseException;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey;
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository;
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider;
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver;
import com.ivanovsky.passnotes.util.Logger;

import org.linguafranca.pwdb.Credentials;
import org.linguafranca.pwdb.Database;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class KeepassDatabaseRepository implements EncryptedDatabaseRepository {

	private volatile KeepassDatabase db;
	private final Context context;
	private final FileSystemResolver fileSystemResolver;

	public KeepassDatabaseRepository(Context context, FileSystemResolver fileSystemResolver) {
		this.context = context;
		this.fileSystemResolver = fileSystemResolver;
	}

	@Override
	public EncryptedDatabase open(EncryptedDatabaseKey key, FileDescriptor file)
			throws EncryptedDatabaseException {
		synchronized (this) {
			if (db != null) {
				close();
			}

			db = new KeepassDatabase(file, key.getKey());
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
			OutputStream out = null;

			FileSystemProvider provider = fileSystemResolver.resolveProvider(file.getFsType());

			try {
				in = new BufferedInputStream(context.getAssets().open("default.kdbx"));//TODO: make constant

				Database keepassDb = SimpleDatabase.load(defaultCredentials, in);

				out = provider.openFileForWrite(file);
				keepassDb.save(newCredentials, out);

				out.flush();

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
				if (out != null) {
					try {
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
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
