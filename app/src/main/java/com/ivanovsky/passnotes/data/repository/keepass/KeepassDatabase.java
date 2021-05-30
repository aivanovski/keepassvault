package com.ivanovsky.passnotes.data.repository.keepass;

import androidx.annotation.NonNull;

import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.TemplateRepository;
import com.ivanovsky.passnotes.data.repository.encdb.exception.FailedToWriteDBException;
import com.ivanovsky.passnotes.data.repository.file.FileSystemProvider;
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver;
import com.ivanovsky.passnotes.data.repository.file.OnConflictStrategy;
import com.ivanovsky.passnotes.data.repository.keepass.dao.KeepassGroupDao;
import com.ivanovsky.passnotes.data.repository.keepass.dao.KeepassNoteDao;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.exception.EncryptedDatabaseException;
import com.ivanovsky.passnotes.data.repository.GroupRepository;
import com.ivanovsky.passnotes.data.repository.NoteRepository;
import com.ivanovsky.passnotes.util.InputOutputUtils;
import com.ivanovsky.passnotes.util.Logger;

import org.linguafranca.pwdb.Credentials;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.simple.SimpleDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.ivanovsky.passnotes.data.entity.OperationError.newGenericIOError;

public class KeepassDatabase implements EncryptedDatabase {

    private final byte[] key;
    private final FileDescriptor file;
    private final KeepassGroupRepository groupRepository;
    private final KeepassNoteRepository noteRepository;
    private final KeepassTemplateRepository templateRepository;
    private final FileSystemResolver fileSystemResolver;
    private final SimpleDatabase db;
    private final Object lock;

    public KeepassDatabase(FileSystemResolver fileSystemResolver, FileDescriptor file,
                           InputStream in, byte[] key) throws EncryptedDatabaseException {
        this.fileSystemResolver = fileSystemResolver;
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

    private SimpleDatabase readDatabaseFile(InputStream in,
                                            byte[] key) throws EncryptedDatabaseException {
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

    @NonNull
    @Override
    public Object getLock() {
        return lock;
    }

    @NonNull
    @Override
    public GroupRepository getGroupRepository() {
        return groupRepository;
    }

    @NonNull
    @Override
    public NoteRepository getNoteRepository() {
        return noteRepository;
    }

    @NonNull
    @Override
    public TemplateRepository getTemplateRepository() {
        return templateRepository;
    }

    @NonNull
    @Override
    public OperationResult<Boolean> commit() {
        OperationResult<Boolean> result = new OperationResult<>();

        synchronized (lock) {
            FileSystemProvider provider = fileSystemResolver.resolveProvider(file.getFsAuthority());

            Credentials credentials = new KdbxCreds(key);

            FileDescriptor updatedFile = file.copy(file.getFsAuthority(),
                    file.getPath(),
                    file.getUid(),
                    file.isDirectory(),
                    file.isRoot(),
                    System.currentTimeMillis());

            OutputStream out = null;
            try {
                OperationResult<OutputStream> outResult = provider.openFileForWrite(updatedFile,
                        OnConflictStrategy.CANCEL,
                        true);
                if (outResult.isFailed()) {
                    return outResult.takeError();
                }

                out = outResult.getObj();

                // method 'SimpleDatabase.save' closes output stream after work is done
                db.save(credentials, out);

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
