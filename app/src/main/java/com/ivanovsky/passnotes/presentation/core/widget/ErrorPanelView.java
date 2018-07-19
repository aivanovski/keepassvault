package com.ivanovsky.passnotes.presentation.core.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.ivanovsky.passnotes.R;

public class ErrorPanelView extends FrameLayout {

	private TextView errorTextView;

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

		errorTextView = (TextView) findViewById(R.id.error_panel_text);
	}

	public void setText(CharSequence text) {
		errorTextView.setText(text);
	}

	public void setText(int resId) {
		errorTextView.setText(resId);
	}
}

