package com.ivanovsky.passnotes.data.entity;

public enum PropertyType {

	TITLE("Title"),
	PASSWORD("Password"),
	USER_NAME("UserName"),
	URL("URL"),
	NOTES("Notes");

	private final String propertyName;

	PropertyType(String propertyName) {
		this.propertyName = propertyName;
	}

	public String getPropertyName() {
		return propertyName;
	}
}
