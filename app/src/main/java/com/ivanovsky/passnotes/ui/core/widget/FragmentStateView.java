package com.ivanovsky.passnotes.ui.core.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.ivanovsky.passnotes.R;

public class FragmentStateView extends FrameLayout {

	private State state;
	private View progressView;
	private TextView emptyTextView;
	private TextView errorTextView;
	private ViewGroup errorTextLayout;

	public enum State {
		LOADING,
		EMPTY,
		ERROR
	}

	public FragmentStateView(Context context) {
		super(context);
		init();
	}

	public FragmentStateView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		LayoutInflater.from(getContext()).inflate(R.layout.core_fragment_state_view, this, true);

		progressView = findViewById(R.id.progress_view);
		emptyTextView = (TextView) findViewById(R.id.empty_text);
		errorTextView = (TextView) findViewById(R.id.error_text);
		errorTextLayout = (ViewGroup) findViewById(R.id.error_layout);

		setState(state != null ? state : State.LOADING);
	}

	public void setState(State state) {
		if (this.state != state) {
			this.state = state;
			switch (state) {
				case LOADING:
					progressView.setVisibility(VISIBLE);
					emptyTextView.setVisibility(GONE);
					errorTextLayout.setVisibility(GONE);
					break;
				case EMPTY:
					progressView.setVisibility(GONE);
					emptyTextView.setVisibility(VISIBLE);
					errorTextLayout.setVisibility(GONE);
					break;
				case ERROR:
					progressView.setVisibility(GONE);
					emptyTextView.setVisibility(GONE);
					errorTextLayout.setVisibility(VISIBLE);
					break;
			}
		}
	}

	public void setErrorText(int resId) {
		errorTextView.setText(getResources().getString(resId));
	}

	public void setErrorText(CharSequence text) {
		errorTextView.setText(text);
	}

	public void setEmptyText(int resId) {
		emptyTextView.setText(getResources().getString(resId));
	}

	public void setEmptyText(CharSequence text) {
		emptyTextView.setText(text);
	}
}
