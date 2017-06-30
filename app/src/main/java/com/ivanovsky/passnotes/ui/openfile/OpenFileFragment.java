package com.ivanovsky.passnotes.ui.openfile;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.databinding.OpenFileFragmentBinding;
import com.ivanovsky.passnotes.ui.core.BaseFragment;

public class OpenFileFragment extends BaseFragment {

	public static OpenFileFragment newInstance() {
		return new OpenFileFragment();
	}

	@Override
	protected View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		OpenFileFragmentBinding binding = DataBindingUtil.inflate(inflater, R.layout.open_file_fragment, container, false);
		return binding.getRoot();
	}
}
