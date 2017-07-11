package com.ivanovsky.passnotes.injection;

import com.ivanovsky.passnotes.ui.StartActivity;

import javax.inject.Singleton;

import dagger.Component;

@Component(modules = { AppModule.class })
@Singleton
public interface AppComponent {

	void inject(StartActivity startActivity);
}
