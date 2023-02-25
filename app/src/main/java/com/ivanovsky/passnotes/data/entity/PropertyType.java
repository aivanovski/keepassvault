package com.ivanovsky.passnotes.data.entity;

import androidx.annotation.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum PropertyType {
    TITLE("Title"),
    PASSWORD("Password"),
    USER_NAME("UserName"),
    URL("URL"),
    NOTES("Notes");

    public static Set<PropertyType> DEFAULT_TYPES = createDefaultPropertiesSet();
    private final String propertyName;

    private static Set<PropertyType> createDefaultPropertiesSet() {
        return new HashSet<>(Arrays.asList(TITLE, PASSWORD, USER_NAME, URL, NOTES));
    }

    @Nullable
    public static PropertyType getByName(@Nullable String name) {
        if (name == null) {
            return null;
        }

        for (PropertyType type : DEFAULT_TYPES) {
            if (type.propertyName.equalsIgnoreCase(name)) {
                return type;
            }
        }

        return null;
    }

    PropertyType(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return propertyName;
    }
}
