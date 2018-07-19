package com.ivanovsky.passnotes.presentation.core;

public interface BaseView<T extends BasePresenter> {

	void setPresenter(T presenter);
	void setState(FragmentState state);
}
