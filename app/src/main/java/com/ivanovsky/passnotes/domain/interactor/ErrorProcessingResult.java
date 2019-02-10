package com.ivanovsky.passnotes.domain.interactor;

public class ErrorProcessingResult {

	private final String message;
	private final ErrorResolution resolution;

	ErrorProcessingResult(String message, ErrorResolution resolution) {
		this.message = message;
		this.resolution = resolution;
	}

	public String getMessage() {
		return message;
	}

	public ErrorResolution getResolution() {
		return resolution;
	}
}
