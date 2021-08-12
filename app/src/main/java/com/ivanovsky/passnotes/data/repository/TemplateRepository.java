package com.ivanovsky.passnotes.data.repository;

import androidx.annotation.NonNull;

import com.ivanovsky.passnotes.data.entity.OperationResult;
import com.ivanovsky.passnotes.data.entity.Template;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

public interface TemplateRepository {

    @Nullable
    UUID getTemplateGroupUid();

    @Nullable
    List<Template> getTemplates();

    @NonNull
    OperationResult<Boolean> addTemplates(@NonNull List<Template> templates);
}
