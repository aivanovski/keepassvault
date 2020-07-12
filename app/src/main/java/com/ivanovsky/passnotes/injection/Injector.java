package com.ivanovsky.passnotes.injection;

import com.ivanovsky.passnotes.App;

public class Injector {

	private static volatile Injector instance;

	private volatile AppComponent appComponent;

	public static Injector getInstance() {
		if (instance == null) {
			synchronized (Injector.class) {
				if (instance == null) {
					instance = new Injector();
				}
			}
		}
		return instance;
	}

	private Injector() {
		appComponent = buildComponent();
	}

	private AppComponent buildComponent() {
		return DaggerAppComponent.builder()
				.appModule(new AppModule(App.getInstance()))
				.build();
	}

	public AppComponent getAppComponent() {
		return appComponent;
	}
}
