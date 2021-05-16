package com.ivanovsky.passnotes.injection;

import javax.inject.Singleton;

import dagger.Component;

@Component(modules = { AppModule.class })
@Singleton
public interface AppComponent {

    // TODO: remove
}
