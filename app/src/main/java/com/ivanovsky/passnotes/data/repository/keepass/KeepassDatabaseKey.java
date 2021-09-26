package com.ivanovsky.passnotes.data.repository.keepass;

import androidx.annotation.NonNull;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabaseKey;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class KeepassDatabaseKey implements EncryptedDatabaseKey {

    private final String password;

    public KeepassDatabaseKey(String password) {
        this.password = password;
    }

    @NonNull
    @Override
    public byte[] getKey() {
        return password.getBytes();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        KeepassDatabaseKey that = (KeepassDatabaseKey) o;

        return new EqualsBuilder()
                .append(password, that.password)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(password)
                .toHashCode();
    }
}
