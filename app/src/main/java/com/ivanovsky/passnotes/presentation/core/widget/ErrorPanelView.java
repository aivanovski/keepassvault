package com.ivanovsky.passnotes.presentation.core.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.ivanovsky.passnotes.R;

public class ErrorPanelView extends FrameLayout {

	private View retryButton;
	private TextView errorTextView;

	public enum State {
		MESSAGE,
		MESSAGE_WITH_RETRY
	}

	public ErrorPanelView(Context context) {
		super(context);
		init();
	}

	public ErrorPanelView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		setBackgroundResource(R.color.material_error_panel_background);

		LayoutInflater.from(getContext()).inflate(R.layout.core_error_panel_view, this, true);

		errorTextView = findViewById(R.id.error_panel_text);
		retryButton = findViewById(R.id.retry_button);
	}

	public void setText(CharSequence text) {
		errorTextView.setText(text);
	}

	public void setText(int resId) {
		errorTextView.setText(resId);
	}

	public void setState(State state) {
		if (state == State.MESSAGE) {
			retryButton.setVisibility(View.GONE);
		} else if (state == State.MESSAGE_WITH_RETRY) {
			retryButton.setVisibility(View.VISIBLE);
		}
	}
}

