package com.ivanovsky.passnotes.data.repository.keepass.dao;

import androidx.annotation.NonNull;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.ivanovsky.passnotes.data.entity.Group;
import com.ivanovsky.passnotes.data.entity.Note;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.entity.Property;
import com.ivanovsky.passnotes.data.entity.PropertyType;
import com.ivanovsky.passnotes.data.repository.encdb.dao.NoteDao;
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabase;

import org.linguafranca.pwdb.kdbx.simple.SimpleEntry;
import org.linguafranca.pwdb.kdbx.simple.SimpleGroup;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.ivanovsky.passnotes.data.entity.OperationError.GENERIC_MESSAGE_NOT_FOUND;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_DUPLICATED_NOTE;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_ADD_ENTRY;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_COMPLETE_OPERATION;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_FIND_GROUP;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_FIND_NOTE;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_UID_IS_NULL;
import static com.ivanovsky.passnotes.data.entity.OperationError.newDbError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newGenericError;

import static java.util.Collections.singletonList;

import android.util.Pair;

public class KeepassNoteDao implements NoteDao {

    private static final String PROPERTY_TITLE = "Title";
    private static final String PROPERTY_PASSWORD = "Password";
    private static final String PROPERTY_URL = "URL";
    private static final String PROPERTY_USER_NAME = "UserName";
    private static final String PROPERTY_NOTES = "Notes";

    private final KeepassDatabase db;
    private final List<OnNoteUpdateListener> updateListeners;
    private final List<OnNoteInsertListener> insertListeners;
    private final List<OnNoteRemoveListener> removeListeners;

    public interface OnNoteUpdateListener {
        void onNoteChanged(UUID groupUid, UUID oldNoteUid, UUID newNoteUid);
    }

    public interface OnNoteInsertListener {
        void onNoteCreated(List<Pair<UUID, UUID>> groupAndNoteUids);
    }

    public interface OnNoteRemoveListener {
        void onNoteRemove(UUID groupUid, UUID noteUid);
    }

    public KeepassNoteDao(KeepassDatabase db) {
        this.db = db;
        this.updateListeners = new CopyOnWriteArrayList<>();
        this.insertListeners = new CopyOnWriteArrayList<>();
        this.removeListeners = new CopyOnWriteArrayList<>();
    }

    public void addOnNoteChangeListener(OnNoteUpdateListener updateListener) {
        updateListeners.add(updateListener);
    }

    public void addOnNoteInsertListener(OnNoteInsertListener insertListener) {
        insertListeners.add(insertListener);
    }

    public void addOnNoteRemoveListener(OnNoteRemoveListener removeListener) {
        removeListeners.add(removeListener);
    }

    @NonNull
    @Override
    public OperationResult<List<Note>> getAll() {
        List<Note> allNotes = new ArrayList<>();

        synchronized (db.getLock()) {
            OperationResult<List<Group>> allGroupsResult = db.getGroupRepository().getAllGroup();
            if (allGroupsResult.isFailed()) {
                return allGroupsResult.takeError();
            }

            List<Group> allGroups = allGroupsResult.getObj();
            for (Group group : allGroups) {
                OperationResult<List<Note>> notesResult = getNotesByGroupUid(group.getUid());
                if (notesResult.isFailed()) {
                    return notesResult.takeError();
                }

                allNotes.addAll(notesResult.getObj());
            }
        }

        return OperationResult.success(allNotes);
    }

    @NonNull
    @Override
    public OperationResult<List<Note>> getNotesByGroupUid(UUID groupUid) {
        List<Note> notes = new ArrayList<>();

        synchronized (db.getLock()) {
            SimpleGroup group = db.findGroupByUid(groupUid);
            if (group == null) {
                return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_GROUP));
            }

            List<SimpleEntry> entries = group.getEntries();
            if (entries != null) {
                notes.addAll(createNotesFromEntries(group.getEntries()));
            }
        }

        return OperationResult.success(notes);
    }

    private List<Note> createNotesFromEntries(List<SimpleEntry> entries) {
        List<Note> notes = new ArrayList<>();

        if (entries != null) {
            for (SimpleEntry entry : entries) {
                notes.add(createNoteFromEntry(entry));
            }
        }

        return notes;
    }

    private Note createNoteFromEntry(SimpleEntry entry) {
        UUID uid;
        UUID groupUid;
        String title;
        Date created;
        Date modified;

        // TODO: add field validation

        uid = entry.getUuid();
        groupUid = entry.getParent().getUuid();
        title = entry.getTitle();
        created = entry.getCreationTime();

        if (entry.getLastModificationTime() != null) {
            modified = entry.getLastModificationTime();
        } else {
            modified = entry.getCreationTime();
        }

        List<Property> properties = new ArrayList<>();
        List<String> propertyNames = entry.getPropertyNames();
        if (propertyNames != null) {
            for (String propertyName : propertyNames) {
                String propertyValue = entry.getProperty(propertyName);
                boolean isProtected = entry.isPropertyProtected(propertyName);

                Property property = createProperty(propertyName, propertyValue, isProtected);
                if (property != null) {
                    properties.add(property);
                }
            }
        }

        return new Note(uid, groupUid, created, modified, title, properties);
    }

    private Property createProperty(String name, String value, boolean isProtected) {
        if (name == null) return null;

        return new Property(parsePropertyType(name), name, value, isProtected);
    }

    private PropertyType parsePropertyType(String type) {
        switch (type) {
            case PROPERTY_TITLE:
                return PropertyType.TITLE;
            case PROPERTY_PASSWORD:
                return PropertyType.PASSWORD;
            case PROPERTY_USER_NAME:
                return PropertyType.USER_NAME;
            case PROPERTY_URL:
                return PropertyType.URL;
            case PROPERTY_NOTES:
                return PropertyType.NOTES;
            default:
                return null;
        }
    }

    @NonNull
    @Override
    public OperationResult<UUID> insert(Note note) {
        return insert(note, true, true);
    }

    private OperationResult<UUID> insert(Note note, boolean notifyListener, boolean doCommit) {
        SimpleEntry newEntry;

        synchronized (db.getLock()) {
            SimpleGroup group = db.findGroupByUid(note.getGroupUid());
            if (group == null) {
                return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_GROUP));
            }

            newEntry = group.addEntry(createEntryFromNote(note));
            if (newEntry == null) {
                return OperationResult.error(newDbError(MESSAGE_FAILED_TO_ADD_ENTRY));
            }

            if (doCommit) {
                OperationResult<Boolean> commitResult = db.commit();
                if (commitResult.isFailed()) {
                    group.removeEntry(newEntry);
                    return commitResult.takeError();
                }
            }
        }

        UUID newEntryUid = newEntry.getUuid();

        if (notifyListener) {
            notifyNoteInserted(singletonList(
                    new Pair<>(note.getGroupUid(), newEntryUid)));
        }

        return OperationResult.success(newEntryUid);
    }

    @NonNull
    @Override
    public OperationResult<Boolean> insert(List<Note> notes) {
        List<Pair<Note, OperationResult<UUID>>> results = Stream.of(notes)
                .map(note -> new Pair<>(note, insert(note, false, false)))
                .collect(Collectors.toList());

        boolean success = Stream.of(results)
                .allMatch(noteToResultPair -> noteToResultPair.second.isSucceededOrDeferred());

        if (success) {
            OperationResult<Boolean> commitResult = db.commit();
            if (commitResult.isFailed()) {
                return commitResult.takeError();
            }

            if (insertListeners.size() != 0) {
                List<Pair<UUID, UUID>> groupAndNoteUids = Stream.of(results)
                        .map(noteToResultPair -> new Pair<>(
                                noteToResultPair.first.getGroupUid(),
                                noteToResultPair.second.getObj()
                        ))
                        .collect(Collectors.toList());

                notifyNoteInserted(groupAndNoteUids);
            }

            return OperationResult.success(true);
        } else {
            OperationResult<UUID> failedOperation = Stream.of(results)
                    .filter(noteToResultPair -> noteToResultPair.second.isFailed())
                    .map(noteToResultPair -> noteToResultPair.second)
                    .findFirst()
                    .orElse(null);

            if (failedOperation == null) {
                return OperationResult.error(newDbError(
                        String.format(GENERIC_MESSAGE_NOT_FOUND, "Operation")));
            }

            return failedOperation.takeError();
        }
    }

    private SimpleEntry createEntryFromNote(Note note) {
        SimpleEntry entry = SimpleEntry.createEntry(db.getKeepassDatabase());

        // TODO: add protected properties

        for (Property property : note.getProperties()) {
            entry.setProperty(property.getName(), property.getValue(), property.isProtected());
        }

        entry.setTitle(note.getTitle());

        return entry;
    }

    @NonNull
    @Override
    public OperationResult<Note> getNoteByUid(UUID noteUid) {
        SimpleEntry note;
        synchronized (db.getLock()) {
            SimpleGroup rootGroup = db.getKeepassDatabase().getRootGroup();

            List<? extends SimpleEntry> entries = rootGroup.findEntries(
                    entry -> entry.getUuid().equals(noteUid),
                    true);
            if (entries.size() == 0) {
                return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_NOTE));
            }

            note = entries.get(0);
        }

        return OperationResult.success(createNoteFromEntry(note));
    }

    @NonNull
    @Override
    public OperationResult<UUID> update(Note note) {
        UUID uid = note.getUid();
        if (uid == null) {
            return OperationResult.error(newGenericError(MESSAGE_UID_IS_NULL));
        }

        synchronized (db.getLock()) {
            SimpleGroup group = db.findGroupByUid(note.getGroupUid());
            if (group == null) {
                return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_GROUP));
            }

            List<? extends SimpleEntry> entries =
                    group.findEntries(entry -> uid.equals(entry.getUuid()), false);
            if (entries.size() == 0) {
                return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_NOTE));
            } else if (entries.size() > 1) {
                return OperationResult.error(newDbError(MESSAGE_DUPLICATED_NOTE));
            }

            SimpleEntry oldEntry = entries.get(0);
            SimpleEntry newEntry = createEntryFromNote(note);

            updateEntryValues(oldEntry, newEntry);

            OperationResult<Boolean> commitResult = db.commit();
            if (commitResult.isFailed()) {
                group.removeEntry(newEntry);
                return commitResult.takeError();
            }
        }

        notifyNoteUpdated(note.getGroupUid(), uid, uid);

        return OperationResult.success(uid);
    }

    private void updateEntryValues(SimpleEntry oldEntry, SimpleEntry newEntry) {
        oldEntry.setTitle(newEntry.getTitle());
        oldEntry.setUsername(newEntry.getUsername());
        oldEntry.setPassword(newEntry.getPassword());
        oldEntry.setUrl(newEntry.getUrl());
        oldEntry.setNotes(newEntry.getNotes());

        List<String> oldNames = oldEntry.getPropertyNames();
        for (String propertyName : oldNames) {
            PropertyType type = parsePropertyType(propertyName);
            if (!PropertyType.DEFAULT_TYPES.contains(type)) {
                oldEntry.removeProperty(propertyName);
            }
        }

        List<String> newNames = newEntry.getPropertyNames();
        for (String propertyName : newNames) {
            PropertyType type = parsePropertyType(propertyName);
            if (!PropertyType.DEFAULT_TYPES.contains(type)) {
                oldEntry.setProperty(propertyName, newEntry.getProperty(propertyName),
                        newEntry.isPropertyProtected(propertyName));
            }
        }
    }

    @NonNull
    @Override
    public OperationResult<Boolean> remove(UUID noteUid) {
        boolean deleted;
        Note note;
        synchronized (db.getLock()) {
            OperationResult<Note> getNote = getNoteByUid(noteUid);
            if (getNote.isFailed()) {
                return getNote.takeError();
            }

            note = getNote.getObj();

            deleted = db.getKeepassDatabase().deleteEntry(noteUid);
            if (!deleted) {
                return OperationResult.error(newDbError(MESSAGE_FAILED_TO_COMPLETE_OPERATION));
            }

            OperationResult<Boolean> commitResult = db.commit();
            if (commitResult.isFailed()) {
                return commitResult.takeError();
            }
        }

        notifyNoteRemoved(note.getGroupUid(), noteUid);

        return OperationResult.success(true);
    }

    private void notifyNoteUpdated(UUID groupUid, UUID oldNoteUid, UUID newNoteUid) {
        for (OnNoteUpdateListener listener : updateListeners) {
            listener.onNoteChanged(groupUid, oldNoteUid, newNoteUid);
        }
    }

    private void notifyNoteInserted(List<Pair<UUID, UUID>> groupAndNoteUids) {
        for (OnNoteInsertListener listener : insertListeners) {
            listener.onNoteCreated(groupAndNoteUids);
        }
    }

    private void notifyNoteRemoved(UUID groupUid, UUID noteUid) {
        for (OnNoteRemoveListener listener : removeListeners) {
            listener.onNoteRemove(groupUid, noteUid);
        }
    }
}
