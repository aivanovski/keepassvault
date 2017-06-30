package com.ivanovsky.passnotes.ui.core;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.databinding.CoreBaseFragmentBinding;
import com.ivanovsky.passnotes.ui.core.widget.FragmentStateView;

public abstract class BaseFragment extends Fragment {

	private boolean isViewCreated;
	private CharSequence emptyText;
	private CharSequence errorText;
	private CharSequence errorPanelText;
	private CoreBaseFragmentBinding binding;
	private FragmentState state;

	protected abstract View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

	@Nullable
	@Override
	public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = DataBindingUtil.inflate(inflater, R.layout.core_base_fragment, container, false);

		View contentView = onCreateContentView(inflater, binding.contentContainer, savedInstanceState);
		if (contentView != null) {
			binding.contentContainer.addView(contentView);
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
				binding.contentContainer.setVisibility(View.GONE);
				binding.stateView.setState(FragmentStateView.State.LOADING);
				binding.stateView.setVisibility(View.VISIBLE);
				binding.errorPanelView.setVisibility(View.GONE);
				break;
			case EMPTY:
				binding.contentContainer.setVisibility(View.GONE);
				binding.stateView.setState(FragmentStateView.State.EMPTY);
				binding.stateView.setVisibility(View.VISIBLE);
				binding.errorPanelView.setVisibility(View.GONE);
				break;
			case DISPLAYING_DATA:
				binding.contentContainer.setVisibility(View.VISIBLE);
				binding.stateView.setVisibility(View.GONE);
				binding.errorPanelView.setVisibility(View.GONE);
				break;
			case ERROR:
				binding.contentContainer.setVisibility(View.GONE);
				binding.stateView.setState(FragmentStateView.State.ERROR);
				binding.stateView.setVisibility(View.VISIBLE);
				binding.errorPanelView.setVisibility(View.GONE);
				break;
			case DISPLAYING_DATA_WITH_ERROR_PANEL:
				binding.contentContainer.setVisibility(View.VISIBLE);
				binding.stateView.setVisibility(View.GONE);
				binding.errorPanelView.setVisibility(View.VISIBLE);
				break;
		}
	}

	protected void onStateChanged(FragmentState oldState, FragmentState newState) {
		//empty, should be overridden in derived class
	}

	protected boolean isViewCreated() {
		return isViewCreated;
	}

	public void setEmptyText(Integer textResId) {
		setEmptyText(textResId != null ? getResources().getString(textResId) : null);
	}

	public void setEmptyText(CharSequence emptyText) {
		this.emptyText = emptyText;
		if (isViewCreated) {
			binding.stateView.setEmptyText(emptyText);
		}
	}

	public void setEmptyState(Integer textResId) {
		setEmptyText(textResId);
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
}
