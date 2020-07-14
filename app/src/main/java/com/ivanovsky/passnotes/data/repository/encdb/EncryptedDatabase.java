package com.ivanovsky.passnotes.data.repository.encdb;

import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.GroupRepository;
import com.ivanovsky.passnotes.data.repository.NoteRepository;
import com.ivanovsky.passnotes.data.repository.TemplateRepository;

public interface EncryptedDatabase {

	GroupRepository getGroupRepository();
	NoteRepository getNoteRepository();
	TemplateRepository getTemplateRepository();
	OperationResult<Boolean> commit();
}
