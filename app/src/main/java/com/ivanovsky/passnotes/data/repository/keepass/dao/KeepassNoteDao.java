package com.ivanovsky.passnotes.data.repository.keepass.dao;

import com.ivanovsky.passnotes.data.entity.OperationError;
import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabase;
import com.ivanovsky.passnotes.data.repository.encdb.dao.NoteDao;
import com.ivanovsky.passnotes.data.entity.Note;
import com.ivanovsky.passnotes.data.entity.Property;

import org.linguafranca.pwdb.kdbx.simple.SimpleEntry;
import org.linguafranca.pwdb.kdbx.simple.SimpleGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KeepassNoteDao implements NoteDao {

	private final KeepassDatabase db;

	public KeepassNoteDao(KeepassDatabase db) {
		this.db = db;
	}

	@Override
	public OperationResult<List<Note>> getNotesByGroupUid(UUID groupUid) {
		List<Note> notes = new ArrayList<>();

		SimpleGroup rootGroup = db.getKeepassDatabase().getRootGroup();

		SimpleGroup group = findGroupByUidRecursively(rootGroup, groupUid);
		if (group != null) {
			notes.addAll(createNotesFromEntries(group.getEntries()));

			for (Note note : notes) {
				note.setGroupUid(groupUid);
			}
		}

		return OperationResult.success(notes);
	}

	private SimpleGroup findGroupByUidRecursively(SimpleGroup group, UUID uid) {
		if (group == null) return null;

		SimpleGroup result = null;

		if (uid != null) {
			if (uid.equals(group.getUuid())) {
				result = group;

			} else {
				List<SimpleGroup> childGroups = group.getGroups();
				if (childGroups != null) {

					for (SimpleGroup childGroup : childGroups) {
						SimpleGroup matchedGroup = findGroupByUidRecursively(childGroup, uid);
						if (matchedGroup != null) {
							result = matchedGroup;
							break;
						}
					}
				}
			}
		}

		return result;
	}

	private List<Note> createNotesFromEntries(List<SimpleEntry> entries) {
		List<Note> notes = new ArrayList<>();

		if (entries != null) {
			for (SimpleEntry entry : entries) {
				Note note = new Note();

				note.setUid(entry.getUuid());
				note.setTitle(entry.getTitle());

				List<Property> properties = new ArrayList<>();
				List<String> propertyNames = entry.getPropertyNames();
				if (propertyNames != null) {
					for (String propertyName : propertyNames) {
						String propertyValue = entry.getProperty(propertyName);

						properties.add(new Property(propertyName, propertyValue));
					}
				}

				note.setProperties(properties);

				notes.add(note);
			}
		}

		return notes;
	}

	private Note createNoteFromEntry(SimpleEntry entry) {
		Note note = new Note();

		note.setUid(entry.getUuid());
		note.setTitle(entry.getTitle());

		List<Property> properties = new ArrayList<>();
		List<String> propertyNames = entry.getPropertyNames();
		if (propertyNames != null) {
			for (String propertyName : propertyNames) {
				String propertyValue = entry.getProperty(propertyName);

				properties.add(new Property(propertyName, propertyValue));
			}
		}

		note.setProperties(properties);

		return note;
	}

	@Override
	public OperationResult<UUID> insert(Note note) {
		OperationResult<UUID> result = new OperationResult<>();

		SimpleGroup rootGroup = db.getKeepassDatabase().getRootGroup();

		SimpleGroup group = findGroupByUidRecursively(rootGroup, note.getGroupUid());
		if (group != null) {
			SimpleEntry newEntry = group.addEntry(createEntryFromNote(note));

			if (newEntry != null && newEntry.getUuid() != null) {
				if (db.commit()) {
					result.setResult(newEntry.getUuid());
				} else {
					result.setError(OperationError.newDbError(OperationError.MESSAGE_FAILTE_TO_COMMIT));
				}

			} else {
				result.setError(OperationError.newDbError(OperationError.MESSAGE_UNKNOWN_ERROR));
			}

		} else {
			result.setError(OperationError.newDbError(OperationError.MESSAGE_FAILED_TO_FIND_GROUP));
		}

		return result;
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
	public OperationResult<Note> getNoteById(UUID noteUid) {
		OperationResult<Note> result = new OperationResult<>();

		SimpleGroup rootGroup = db.getKeepassDatabase().getRootGroup();

		if (noteUid != null) {
			List<? extends SimpleEntry> entries = rootGroup.findEntries(
					entry -> entry.getUuid().equals(noteUid),
					true);
			if (entries.size() != 0) {
				result.setResult(createNotesFromEntries());
			}
		}


		return result;
	}

	private SimpleEntry findEntryByUid(SimpleGroup group, UUID noteUid) {
		SimpleEntry result = null;

		List<SimpleEntry> matchedEnatries = group.findEntries()

		return result;
	}
}
