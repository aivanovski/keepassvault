package com.ivanovsky.passnotes.data.repository.encdb;

import androidx.annotation.NonNull;

public interface EncryptedDatabaseKey {
    @NonNull
    byte[] getKey();
}
