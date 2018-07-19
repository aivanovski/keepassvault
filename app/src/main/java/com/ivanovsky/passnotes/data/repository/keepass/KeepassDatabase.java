package com.ivanovsky.passnotes.data.repository.keepass;

import com.ivanovsky.passnotes.data.repository.file.FileProvider;
import com.ivanovsky.passnotes.data.repository.keepass.dao.KeepassGroupDao;
import com.ivanovsky.passnotes.data.repository.keepass.dao.KeepassNoteDao;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseOperationException;
import com.ivanovsky.passnotes.data.repository.GroupRepository;
import com.ivanovsky.passnotes.data.repository.NoteRepository;
import com.ivanovsky.passnotes.util.Logger;

import org.linguafranca.pwdb.Credentials;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class KeepassDatabase implements EncryptedDatabase {

	private final byte[] key;
	private final FileProvider fileProvider;
	private final KeepassGroupRepository groupRepository;
	private final KeepassNoteRepository noteRepository;
	private final SimpleDatabase db;

	static KeepassDatabase fromFile(FileProvider fileProvider, byte[] key) throws EncryptedDatabaseOperationException {
		Credentials credentials = new KdbxCreds(key);

		InputStream in = null;
		SimpleDatabase db;

		try {
			in = fileProvider.createInputStream();
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

		return new KeepassDatabase(fileProvider, key, db);
	}

	private KeepassDatabase(FileProvider fileProvider, byte[] key, SimpleDatabase db) {
		this.fileProvider = fileProvider;
		this.key = key;
		this.db = db;
		this.groupRepository = new KeepassGroupRepository(new KeepassGroupDao(this));
		this.noteRepository = new KeepassNoteRepository(new KeepassNoteDao(this));
	}

	@Override
	public GroupRepository getGroupRepository() {
		return groupRepository;
	}

	@Override
	public NoteRepository getNoteRepository() {
		return noteRepository;
	}

	@Override
	public boolean commit() {
		boolean result = false;

		Credentials credentials = new KdbxCreds(key);

		OutputStream out = null;

		try {
			out = fileProvider.createOutputStream();
			db.save(credentials, out);

			result = true;
		} catch (IOException e) {
			Logger.printStackTrace(e);

		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					Logger.printStackTrace(e);
				}
			}
		}

		return result;
	}

	public SimpleDatabase getKeepassDatabase() {
		return db;
	}
}
