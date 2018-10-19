package com.ivanovsky.passnotes.data.repository.keepass;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider;
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver;
import com.ivanovsky.passnotes.data.repository.file.exception.FileSystemException;
import com.ivanovsky.passnotes.data.repository.keepass.dao.KeepassGroupDao;
import com.ivanovsky.passnotes.data.repository.keepass.dao.KeepassNoteDao;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.exception.EncryptedDatabaseException;
import com.ivanovsky.passnotes.data.repository.GroupRepository;
import com.ivanovsky.passnotes.data.repository.NoteRepository;
import com.ivanovsky.passnotes.injection.Injector;
import com.ivanovsky.passnotes.util.Logger;

import org.linguafranca.pwdb.Credentials;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Inject;

public class KeepassDatabase implements EncryptedDatabase {

	@Inject
	FileSystemResolver fileSystemResolver;

	private final byte[] key;
	private final KeepassGroupRepository groupRepository;
	private final KeepassNoteRepository noteRepository;
	private final SimpleDatabase db;
	private final FileDescriptor file;

	public KeepassDatabase(FileDescriptor file, byte[] key) throws EncryptedDatabaseException {
		Injector.getInstance().getAppComponent().inject(this);

		this.db = readDatabaseFile(file, key);
		this.file = file;
		this.key = key;
		this.groupRepository = new KeepassGroupRepository(new KeepassGroupDao(this));
		this.noteRepository = new KeepassNoteRepository(new KeepassNoteDao(this));
	}

	private SimpleDatabase readDatabaseFile(FileDescriptor file, byte[] key) throws EncryptedDatabaseException {
		SimpleDatabase result;

		Credentials credentials = new KdbxCreds(key);

		FileSystemProvider provider = fileSystemResolver.resolveProvider(file.getFsType());

		InputStream in = null;

		try {
			in = provider.openFileForRead(file);
			result = SimpleDatabase.load(credentials, in);

		} catch (Exception e) {
			Logger.printStackTrace(e);
			throw new EncryptedDatabaseException(e);

		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					Logger.printStackTrace(e);
				}
			}
		}

		return result;
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
	public void commit() throws EncryptedDatabaseException {
		FileSystemProvider provider = fileSystemResolver.resolveProvider(file.getFsType());

		Credentials credentials = new KdbxCreds(key);

		OutputStream out = null;

		try {
			out = provider.openFileForWrite(file);
			db.save(credentials, out);
			out.flush();
		} catch (FileSystemException | IOException e) {
			throw new EncryptedDatabaseException(e);

		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					Logger.printStackTrace(e);
				}
			}
		}
	}

	public SimpleDatabase getKeepassDatabase() {
		return db;
	}
}
