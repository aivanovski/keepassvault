package com.ivanovsky.passnotes.data.repository;

import com.ivanovsky.passnotes.data.entity.Template;

import java.util.List;

import javax.annotation.Nullable;

public interface TemplateRepository {

    @Nullable
    List<Template> getTemplates();
}
