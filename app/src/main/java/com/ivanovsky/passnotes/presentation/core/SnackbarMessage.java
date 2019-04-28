package com.ivanovsky.passnotes.presentation.core;

public class SnackbarMessage {

	private final boolean displayOkButton;
	private final String message;

	public SnackbarMessage(String message) {
		this.message = message;
		this.displayOkButton = false;
	}

	public SnackbarMessage(String message, boolean displayOkButton) {
		this.message = message;
		this.displayOkButton = displayOkButton;
	}

	public boolean isDisplayOkButton() {
		return displayOkButton;
	}

	public String getMessage() {
		return message;
	}
}
