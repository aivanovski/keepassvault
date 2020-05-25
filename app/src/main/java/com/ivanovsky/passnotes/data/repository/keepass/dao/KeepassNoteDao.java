package com.ivanovsky.passnotes.data.repository.keepass.dao;

import com.ivanovsky.passnotes.data.entity.OperationError;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.entity.PropertyType;
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.dao.NoteDao;
import com.ivanovsky.passnotes.data.entity.Note;
import com.ivanovsky.passnotes.data.entity.Property;

import org.linguafranca.pwdb.kdbx.simple.SimpleEntry;
import org.linguafranca.pwdb.kdbx.simple.SimpleGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.ivanovsky.passnotes.data.entity.OperationError.MESSAGE_FAILED_TO_FIND_GROUP;
import static com.ivanovsky.passnotes.data.entity.OperationError.newDbError;

public class KeepassNoteDao implements NoteDao {

	private static final String PROPERTY_TITLE = "Title";
	private static final String PROPERTY_PASSWORD = "Password";
	private static final String PROPERTY_URL = "URL";
	private static final String PROPERTY_USER_NAME = "UserName";
	private static final String PROPERTY_NOTES = "Notes";

	private final KeepassDatabase db;

	public KeepassNoteDao(KeepassDatabase db) {
		this.db = db;
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

			for (Note note : notes) {
				note.setGroupUid(groupUid);
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
		Note note = new Note();

		note.setUid(entry.getUuid());
		note.setTitle(entry.getTitle());
		note.setCreated(entry.getCreationTime());

		if (entry.getLastModificationTime() != null) {
			note.setModified(entry.getLastModificationTime());
		} else {
			note.setModified(entry.getCreationTime());
		}

		List<Property> properties = new ArrayList<>();
		List<String> propertyNames = entry.getPropertyNames();
		if (propertyNames != null) {
			for (String propertyName : propertyNames) {
				String propertyValue = entry.getProperty(propertyName);

				Property property = createProperty(propertyName, propertyValue);
				if (property != null) {
					properties.add(property);
				}
			}
		}

		note.setProperties(properties);

		return note;
	}

	private Property createProperty(String name, String value) {
		if (name == null) return null;

		Property property = new Property();

		property.setName(name);
		property.setValue(value);
		property.setType(parsePropertyType(name));

		return property;
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
			return OperationResult.error(newDbError(OperationError.MESSAGE_UNKNOWN_ERROR));
		}

		OperationResult<Boolean> commitResult = db.commit();
		if (commitResult.isFailed()) {
			group.removeEntry(newEntry);
			return commitResult.takeError();
		}

		return commitResult.takeStatusWith(newEntry.getUuid());
	}

	private SimpleEntry createEntryFromNote(Note note) {
		SimpleEntry entry = SimpleEntry.createEntry(db.getKeepassDatabase());

		for (Property property : note.getProperties()) {
			entry.setProperty(property.getName(), property.getValue());
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
}
