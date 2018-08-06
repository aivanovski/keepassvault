package com.ivanovsky.passnotes.presentation.core;

public class ScreenState {

	public static ScreenState loading() {
		ScreenState result = new ScreenState();

		result.state = FragmentState.LOADING;

		return result;
	}

	public static ScreenState empty(String message) {
		ScreenState result = new ScreenState();

		result.state = FragmentState.EMPTY;
		result.message = message;

		return result;
	}

	public static ScreenState error(String message) {
		ScreenState result = new ScreenState();

		result.state = FragmentState.ERROR;
		result.message = message;

		return result;
	}

	public static ScreenState data() {
		ScreenState result = new ScreenState();

		result.state = FragmentState.DISPLAYING_DATA;

		return result;
	}

	public static ScreenState dataWithError(String message) {
		ScreenState result = new ScreenState();

		result.state = FragmentState.DISPLAYING_DATA_WITH_ERROR_PANEL;
		result.message = message;

		return result;
	}

	private String message;
	private FragmentState state;

	private ScreenState() {
	}

	public String getMessage() {
		return message;
	}

	public FragmentState getState() {
		return state;
	}
}
