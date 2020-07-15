package com.ivanovsky.passnotes.data.repository.keepass;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.TemplateRepository;
import com.ivanovsky.passnotes.data.repository.encdb.exception.FailedToWriteDBException;
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider;
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver;
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy;
import com.ivanovsky.passnotes.data.repository.file.RemoteFileOutputStream;
import com.ivanovsky.passnotes.data.repository.keepass.dao.KeepassGroupDao;
import com.ivanovsky.passnotes.data.repository.keepass.dao.KeepassNoteDao;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.exception.EncryptedDatabaseException;
import com.ivanovsky.passnotes.data.repository.GroupRepository;
import com.ivanovsky.passnotes.data.repository.NoteRepository;
import com.ivanovsky.passnotes.injection.Injector;
import com.ivanovsky.passnotes.util.InputOutputUtils;
import com.ivanovsky.passnotes.util.Logger;

import org.linguafranca.pwdb.Credentials;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Inject;

import static com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError;

public class KeepassDatabase implements EncryptedDatabase {

	// TODO: add lock

	@Inject
	FileSystemResolver fileSystemResolver;

	private final byte[] key;
	private final FileDescriptor file;
	private final KeepassGroupRepository groupRepository;
	private final KeepassNoteRepository noteRepository;
	private final KeepassTemplateRepository templateRepository;
	private final SimpleDatabase db;
	private final Object lock;

	public KeepassDatabase(FileDescriptor file, InputStream in, byte[] key) throws EncryptedDatabaseException {
		Injector.getInstance().getAppComponent().inject(this);

		this.file = file;
		this.key = key;
		this.lock = new Object();
		this.db = readDatabaseFile(in, key);

		KeepassNoteDao noteDao = new KeepassNoteDao(this);
		KeepassGroupDao groupDao = new KeepassGroupDao(this);

		this.groupRepository = new KeepassGroupRepository(groupDao);
		this.noteRepository = new KeepassNoteRepository(noteDao);
		this.templateRepository = new KeepassTemplateRepository(groupDao, noteDao);

		db.enableRecycleBin(false);

		templateRepository.findTemplateNotes();
	}

	private SimpleDatabase readDatabaseFile(InputStream in, byte[] key) throws EncryptedDatabaseException {
		SimpleDatabase result;

		Credentials credentials = new KdbxCreds(key);

		synchronized (lock) {
			try {
				result = SimpleDatabase.load(credentials, in);
			} catch (IllegalStateException e) {
				Logger.printStackTrace(e);
				throw new EncryptedDatabaseException(e);

			} catch (IOException e) {
				throw new FailedToWriteDBException();

			} catch (Exception e) {
				Logger.printStackTrace(e);
				throw new EncryptedDatabaseException(e);

			} finally {
				InputOutputUtils.close(in);
			}
		}

		return result;
	}

	@Override
	public Object getLock() {
		return lock;
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
	public TemplateRepository getTemplateRepository() {
		return templateRepository;
	}

	@Override
	public OperationResult<Boolean> commit() {
		OperationResult<Boolean> result = new OperationResult<>();

		synchronized (lock) {
			FileSystemProvider provider = fileSystemResolver.resolveProvider(file.getFsType());

			Credentials credentials = new KdbxCreds(key);

			file.setModified(System.currentTimeMillis());

			OutputStream out = null;
			try {
				OperationResult<OutputStream> outResult = provider.openFileForWrite(file,
						OnConflictStrategy.CANCEL,
						true);
				if (outResult.isFailed()) {
					return outResult.takeError();
				}

				out = outResult.getObj();

				// method 'SimpleDatabase.save' closes output stream after work is done
				if (out instanceof RemoteFileOutputStream) {
					RemoteFileOutputStream remoteOut = (RemoteFileOutputStream) out;

					File localFile = remoteOut.getOutputFile();

					BufferedOutputStream localOut = new BufferedOutputStream(new FileOutputStream(localFile));
					db.save(credentials, localOut);

					InputOutputUtils.copy(new FileInputStream(localFile), out, false);

					out.close();
				} else {
					db.save(credentials, out);
				}

				if (outResult.isDeferred()) {
					result.setDeferredObj(true);
				} else {
					result.setObj(true);
				}

			} catch (IOException e) {
				InputOutputUtils.close(out);

				result.setError(newGenericIOError(e));
			}
		}

		return result;
	}

	public SimpleDatabase getKeepassDatabase() {
		return db;
	}
}
