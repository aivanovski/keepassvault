package com.ivanovsky.passnotes.data.keepass.dao;

import com.ivanovsky.passnotes.data.keepass.KeepassDatabase;
import com.ivanovsky.passnotes.data.safedb.dao.NoteDao;
import com.ivanovsky.passnotes.data.safedb.model.Note;
import com.ivanovsky.passnotes.data.safedb.model.Property;

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
	public List<Note> getNotesByGroupUid(UUID groupUid) {
		List<Note> notes = new ArrayList<>();

		SimpleGroup rootGroup = db.getKeepassDatabase().getRootGroup();

		SimpleGroup group = findGroupByUidRecursively(rootGroup, groupUid);
		if (group != null) {
			notes.addAll(createNotesFromEntries(group.getEntries()));

			for (Note note : notes) {
				note.setGroupUid(groupUid);
			}
		}

		return notes;
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
}
