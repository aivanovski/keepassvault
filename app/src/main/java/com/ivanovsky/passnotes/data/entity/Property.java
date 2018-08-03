package com.ivanovsky.passnotes.data.entity;

public class Property {

	private String name;
	private String value;
	private PropertyType type;

	public Property() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public PropertyType getType() {
		return type;
	}

	public void setType(PropertyType type) {
		this.type = type;
	}
}
