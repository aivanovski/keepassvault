package com.ivanovsky.passnotes.data.entity;

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

    private static Set<PropertyType> createDefaultPropertiesSet() {
        return new HashSet<>(Arrays.asList(TITLE, PASSWORD, USER_NAME, URL, NOTES));
    }

    private final String propertyName;

    PropertyType(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return propertyName;
    }
}
