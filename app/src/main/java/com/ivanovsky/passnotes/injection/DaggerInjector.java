package com.ivanovsky.passnotes.injection;

import com.ivanovsky.passnotes.App;

public class DaggerInjector {

	private static volatile DaggerInjector instance;

	private volatile AppComponent appComponent;

	public static DaggerInjector getInstance() {
		if (instance == null) {
			synchronized (DaggerInjector.class) {
				if (instance == null) {
					instance = new DaggerInjector();
				}
			}
		}
		return instance;
	}

	private DaggerInjector() {
		appComponent = buildComponent();
	}

	private AppComponent buildComponent() {
		return DaggerAppComponent.builder()
				.appModule(new AppModule(App.getAppInstance(), App.getSharedModule()))
				.build();
	}

	public AppComponent getAppComponent() {
		return appComponent;
	}
}
