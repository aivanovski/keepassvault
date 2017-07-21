package com.ivanovsky.passnotes.ui.core;

public interface BaseView<T extends BasePresenter> {

	void setPresenter(T presenter);
	void setState(FragmentState state);
}
