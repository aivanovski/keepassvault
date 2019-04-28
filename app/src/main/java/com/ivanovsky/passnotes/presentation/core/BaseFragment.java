package com.ivanovsky.passnotes.presentation.core;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.presentation.core.widget.ErrorPanelView;
import com.ivanovsky.passnotes.presentation.core.widget.FragmentStateView;

public abstract class BaseFragment extends Fragment {

	private boolean isViewCreated;
	private CharSequence emptyText;
	private CharSequence errorText;
	private CharSequence errorPanelText;
	private FragmentState state;
	private ViewGroup contentContainer;
	private FragmentStateView stateView;
	private ErrorPanelView errorPanelView;
	private ViewGroup rootLayout;

	protected abstract View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

	@Nullable
	@Override
	public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.core_base_fragment, container, false);

		contentContainer = view.findViewById(R.id.content_container);
		stateView = view.findViewById(R.id.state_view);
		errorPanelView = view.findViewById(R.id.error_panel_view);
		rootLayout = view.findViewById(R.id.root_layout);

		View contentView = onCreateContentView(inflater, contentContainer, savedInstanceState);
		if (contentView != null) {
			contentContainer.addView(contentView);
		}

		if (getContentContainerId() != -1) {
			//noinspection ConstantConditions
			contentContainer = contentView.findViewById(getContentContainerId());
		}

		isViewCreated = true;

		if (state != null) {
			applyStateToViews(state);
			onStateChanged(null, state);
		} else {
			setState(FragmentState.LOADING);
		}

		if (emptyText != null) {
			setEmptyText(emptyText);
		}

		if (errorText != null) {
			setErrorText(errorText);
		}

		if (errorPanelText != null) {
			setErrorPanelText(errorPanelText);
		}

		return view;
	}

	protected int getContentContainerId() {
		//determines view that will be shown/hidden if fragment state will be changed, should be overridden in derived class
		return -1;
	}

	public final FragmentState getState() {
		return state;
	}

	public final void setState(FragmentState state) {
		FragmentState oldState = this.state;
		this.state = state;
		if (isViewCreated) {
			applyStateToViews(state);

			if (oldState != state) {
				onStateChanged(oldState, state);
			}
		}
	}

	private void applyStateToViews(FragmentState state) {
		switch (state) {
			case LOADING:
				contentContainer.setVisibility(View.GONE);
				stateView.setState(FragmentStateView.State.LOADING);
				stateView.setVisibility(View.VISIBLE);
				errorPanelView.setVisibility(View.GONE);
				break;
			case EMPTY:
				contentContainer.setVisibility(View.GONE);
				stateView.setState(FragmentStateView.State.EMPTY);
				stateView.setVisibility(View.VISIBLE);
				errorPanelView.setVisibility(View.GONE);
				break;
			case DISPLAYING_DATA:
				contentContainer.setVisibility(View.VISIBLE);
				stateView.setVisibility(View.GONE);
				errorPanelView.setVisibility(View.GONE);
				break;
			case ERROR:
				contentContainer.setVisibility(View.GONE);
				stateView.setState(FragmentStateView.State.ERROR);
				stateView.setVisibility(View.VISIBLE);
				errorPanelView.setVisibility(View.GONE);
				break;
			case DISPLAYING_DATA_WITH_ERROR_PANEL:
				contentContainer.setVisibility(View.VISIBLE);
				stateView.setVisibility(View.GONE);
				errorPanelView.setVisibility(View.VISIBLE);
				errorPanelView.setState(ErrorPanelView.State.MESSAGE);
				break;
			case DISPLAYING_DATA_WITH_RETRY_BUTTON:
				contentContainer.setVisibility(View.VISIBLE);
				stateView.setVisibility(View.GONE);
				errorPanelView.setVisibility(View.VISIBLE);
				errorPanelView.setState(ErrorPanelView.State.MESSAGE_WITH_RETRY);
				break;
		}
	}

	protected void onStateChanged(FragmentState oldState, FragmentState newState) {
		//empty, should be overridden in derived class
	}

	protected ActionBar getActionBar() {
		ActionBar result = null;

		AppCompatActivity activity = (AppCompatActivity) getActivity();
		if (activity != null) {
			result = activity.getSupportActionBar();
		}

		return result;
	}

	protected boolean isViewCreated() {
		return isViewCreated;
	}

	public void setEmptyText(CharSequence emptyText) {
		this.emptyText = emptyText;
		if (isViewCreated) {
			stateView.setEmptyText(emptyText);
		}
	}

	public void setEmptyTextAndState(CharSequence emptyText) {
		setEmptyText(emptyText);
		setState(FragmentState.EMPTY);
	}

	public void setErrorText(Integer textResId) {
		Context context = getActivity();
		setErrorText(context.getResources().getString(textResId));
	}

	public void setErrorTextAndState(int textResId) {
		setErrorText(textResId);
		setState(FragmentState.ERROR);
	}

	public void setErrorText(CharSequence errorText) {
		this.errorText = errorText;
		if (isViewCreated) {
			stateView.setErrorText(errorText);
		}
	}

	public void setErrorPanelTextAndState(int textResId) {
		setErrorPanelText(textResId);
		setState(FragmentState.DISPLAYING_DATA_WITH_ERROR_PANEL);
	}

	public void setErrorPanelTextAndState(CharSequence errorPanelText) {
		setErrorPanelText(errorPanelText);
		setState(FragmentState.DISPLAYING_DATA_WITH_ERROR_PANEL);
	}

	public void setErrorPanelText(int resId) {
		Context context = getActivity();
		if (context != null) {
			setErrorPanelText(context.getResources().getString(resId));
		}
	}

	public void setErrorPanelText(CharSequence errorPanelText) {
		this.errorPanelText = errorPanelText;
		if (isViewCreated) {
			errorPanelView.setText(errorPanelText);
		}
	}

	public void setScreenState(ScreenState screenState) {
		FragmentState state = screenState.getState();

		switch (state) {
			case EMPTY:
				setEmptyText(screenState.getMessage());
				break;
			case ERROR:
				setErrorText(screenState.getMessage());
				break;
			case DISPLAYING_DATA_WITH_ERROR_PANEL:
				setErrorPanelText(screenState.getMessage());
				break;
		}

		setState(state);
	}

	public void showSnackbar(String message) {
		Snackbar.make(rootLayout, message, Snackbar.LENGTH_SHORT)
				.show();
	}

	public void showSnackbar(SnackbarMessage message) {
		Snackbar snackbar;

		if (message.isDisplayOkButton()) {
			snackbar = Snackbar.make(rootLayout, message.getMessage(), Snackbar.LENGTH_INDEFINITE);
			snackbar.setAction(R.string.ok, view -> snackbar.dismiss());
		} else {
			snackbar = Snackbar.make(rootLayout, message.getMessage(), Snackbar.LENGTH_SHORT);
		}

		snackbar.show();
	}

	public void showToast(String message) {
		Toast.makeText(getContext(), message, Toast.LENGTH_SHORT)
				.show();
	}
}
