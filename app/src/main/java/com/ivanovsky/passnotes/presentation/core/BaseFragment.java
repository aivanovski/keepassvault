package com.ivanovsky.passnotes.presentation.core;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.databinding.CoreBaseFragmentBinding;
import com.ivanovsky.passnotes.presentation.core.widget.FragmentStateView;

public abstract class BaseFragment extends Fragment {

	private boolean isViewCreated;
	private CharSequence emptyText;
	private CharSequence errorText;
	private CharSequence errorPanelText;
	private CoreBaseFragmentBinding binding;
	private FragmentState state;
	private View contentContainer;

	protected abstract View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

	@Nullable
	@Override
	public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = DataBindingUtil.inflate(inflater, R.layout.core_base_fragment, container, false);

		View contentView = onCreateContentView(inflater, binding.contentContainer, savedInstanceState);
		if (contentView != null) {
			binding.contentContainer.addView(contentView);
		}

		if (getContentContainerId() != -1) {
			//noinspection ConstantConditions
			contentContainer = contentView.findViewById(getContentContainerId());
		} else {
			contentContainer = binding.contentContainer;
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

		return binding.getRoot();
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
				binding.stateView.setState(FragmentStateView.State.LOADING);
				binding.stateView.setVisibility(View.VISIBLE);
				binding.errorPanelView.setVisibility(View.GONE);
				break;
			case EMPTY:
				contentContainer.setVisibility(View.GONE);
				binding.stateView.setState(FragmentStateView.State.EMPTY);
				binding.stateView.setVisibility(View.VISIBLE);
				binding.errorPanelView.setVisibility(View.GONE);
				break;
			case DISPLAYING_DATA:
				contentContainer.setVisibility(View.VISIBLE);
				binding.stateView.setVisibility(View.GONE);
				binding.errorPanelView.setVisibility(View.GONE);
				break;
			case ERROR:
				contentContainer.setVisibility(View.GONE);
				binding.stateView.setState(FragmentStateView.State.ERROR);
				binding.stateView.setVisibility(View.VISIBLE);
				binding.errorPanelView.setVisibility(View.GONE);
				break;
			case DISPLAYING_DATA_WITH_ERROR_PANEL:
				contentContainer.setVisibility(View.VISIBLE);
				binding.stateView.setVisibility(View.GONE);
				binding.errorPanelView.setVisibility(View.VISIBLE);
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
			binding.stateView.setEmptyText(emptyText);
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
			binding.stateView.setErrorText(errorText);
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
			binding.errorPanelView.setText(errorPanelText);
		}
	}

	public void showSnackbar(String message) {
		Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT)
				.show();
	}
}
