package com.ivanovsky.passnotes.data.repository.keepass.keepass_java;

import com.ivanovsky.passnotes.domain.entity.DatabaseStatus;

public interface OnStatusChangeListener {
    void onDatabaseStatusChanged(DatabaseStatus status);
}
