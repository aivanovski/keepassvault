package com.ivanovsky.passnotes.data.repository.keepass.dao;

import com.ivanovsky.passnotes.data.entity.Note;
import com.ivanovsky.passnotes.data.entity.OperationError;
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

import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_DUPLICATED_NOTE;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_ADD_ENTRY;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_FIND_GROUP;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_FIND_NOTE;
import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_UID_IS_NULL;
import static com.ivanovsky.passnotes.data.entity.OperationError.newDbError;
import static com.ivanovsky.passnotes.data.entity.OperationError.newGenericError;

public class KeepassNoteDao implements NoteDao {

	private static final String PROPERTY_TITLE = "Title";
	private static final String PROPERTY_PASSWORD = "Password";
	private static final String PROPERTY_URL = "URL";
	private static final String PROPERTY_USER_NAME = "UserName";
	private static final String PROPERTY_NOTES = "Notes";

	private final KeepassDatabase db;
	private volatile OnNoteUpdateListener updateListener;
	private volatile OnNoteInsertListener insertListener;

	public interface OnNoteUpdateListener {
		void onNoteChanged(UUID groupUid, UUID oldNoteUid, UUID newNoteUid);
	}

	public interface OnNoteInsertListener {
		void onNoteCreated(UUID groupUid, UUID noteUid);
	}

	public KeepassNoteDao(KeepassDatabase db) {
		this.db = db;
	}

	public void setOnNoteChangeListener(OnNoteUpdateListener updateListener) {
		this.updateListener = updateListener;
	}

	public void setOnNoteInsertListener(OnNoteInsertListener insertListener) {
		this.insertListener = insertListener;
	}

	@Override
	public OperationResult<List<Note>> getNotesByGroupUid(UUID groupUid) {
		SimpleGroup group = db.getKeepassDatabase().findGroup(groupUid);
		if (group == null) {
			return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_GROUP));
		}

		List<Note> notes = new ArrayList<>();

		List<SimpleEntry> entries = group.getEntries();
		if (entries != null) {
			notes.addAll(createNotesFromEntries(group.getEntries()));
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

	@Override
	public OperationResult<UUID> insert(Note note) {
		SimpleGroup group = db.getKeepassDatabase().findGroup(note.getGroupUid());
		if (group == null) {
			return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_GROUP));
		}

		SimpleEntry newEntry = group.addEntry(createEntryFromNote(note));
		if (newEntry == null) {
			return OperationResult.error(newDbError(MESSAGE_FAILED_TO_ADD_ENTRY));
		}

		OperationResult<Boolean> commitResult = db.commit();
		if (commitResult.isFailed()) {
			group.removeEntry(newEntry);
			return commitResult.takeError();
		}

		if (insertListener != null) {
			insertListener.onNoteCreated(note.getGroupUid(), newEntry.getUuid());
		}

		return commitResult.takeStatusWith(newEntry.getUuid());
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

	@Override
	public OperationResult<Note> getNoteByUid(UUID noteUid) {
		OperationResult<Note> result = new OperationResult<>();

		SimpleGroup rootGroup = db.getKeepassDatabase().getRootGroup();

		List<? extends SimpleEntry> entries = rootGroup.findEntries(
				entry -> entry.getUuid().equals(noteUid),
				true);
		if (entries.size() != 0) {
			result.setObj(createNoteFromEntry(entries.get(0)));
		} else {
			result.setError(newDbError(OperationError.MESSAGE_FAILED_TO_FIND_NOTE));
		}

		return result;
	}

	@Override
	public OperationResult<UUID> update(Note note) {
		UUID oldUid = note.getUid();
		if (oldUid == null) {
			return OperationResult.error(newGenericError(MESSAGE_UID_IS_NULL));
		}

		SimpleGroup group = db.getKeepassDatabase().findGroup(note.getGroupUid());
		if (group == null) {
			return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_GROUP));
		}

		List<? extends SimpleEntry> entries =
				group.findEntries(entry -> oldUid.equals(entry.getUuid()), false);
		if (entries.size() == 0) {
			return OperationResult.error(newDbError(MESSAGE_FAILED_TO_FIND_NOTE));
		} else if (entries.size() > 1) {
			return OperationResult.error(newDbError(MESSAGE_DUPLICATED_NOTE));
		}

		SimpleEntry oldEntry = entries.get(0);
		group.removeEntry(oldEntry);

		// TODO: entry insertion can be reused from insert() method

		SimpleEntry newEntry = group.addEntry(createEntryFromNote(note));
		if (newEntry == null) {
			return OperationResult.error(newDbError(MESSAGE_FAILED_TO_ADD_ENTRY));
		}

		UUID newUid = newEntry.getUuid();

		OperationResult<Boolean> commitResult = db.commit();
		if (commitResult.isFailed()) {
			group.removeEntry(newEntry);
			return commitResult.takeError();
		}

		if (updateListener != null) {
			updateListener.onNoteChanged(note.getGroupUid(), oldUid, newUid);
		}

		return commitResult.takeStatusWith(newUid);
	}
}
