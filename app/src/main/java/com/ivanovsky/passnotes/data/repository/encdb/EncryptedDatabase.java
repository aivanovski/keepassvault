package com.ivanovsky.passnotes.data.repository.encdb;

import androidx.annotation.NonNull;

import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.repository.GroupRepository;
import com.ivanovsky.passnotes.data.repository.NoteRepository;
import com.ivanovsky.passnotes.data.repository.TemplateRepository;

public interface EncryptedDatabase {

    @NonNull
    Object getLock();

    @NonNull
    GroupRepository getGroupRepository();

    @NonNull
    NoteRepository getNoteRepository();

    @NonNull
    TemplateRepository getTemplateRepository();

    @NonNull
    OperationResult<Boolean> commit();
}
