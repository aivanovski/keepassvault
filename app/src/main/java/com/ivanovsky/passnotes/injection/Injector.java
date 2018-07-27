package com.ivanovsky.passnotes.injection;

import com.ivanovsky.passnotes.App;
import com.ivanovsky.passnotes.data.repository.encdb.EncryptedDatabase;
import com.ivanovsky.passnotes.injection.encdb.EncryptedDatabaseComponent;
import com.ivanovsky.passnotes.injection.encdb.EncryptedDatabaseModule;

public class Injector {

	private static volatile Injector instance;

	private volatile AppComponent appComponent;
	private volatile EncryptedDatabaseComponent dbComponent;

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

	public void createEncryptedDatabaseComponent(EncryptedDatabase database) {
		dbComponent = appComponent.plus(new EncryptedDatabaseModule(App.getInstance(), database));
	}

	public void releaseEncryptedDatabaseComponent() {
		dbComponent = null;
	}

	public EncryptedDatabaseComponent getEncryptedDatabaseComponent() {
		return dbComponent;
	}
}
