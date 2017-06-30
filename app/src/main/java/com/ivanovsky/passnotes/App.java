package com.ivanovsky.passnotes;

import android.app.Application;

import com.ivanovsky.passnotes.injection.AppComponent;
import com.ivanovsky.passnotes.injection.AppModule;
import com.ivanovsky.passnotes.injection.DaggerAppComponent;

public class App extends Application {

	private static AppComponent component;

	public static AppComponent getDaggerComponent() {
		return component;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		component = buildComponent();
	}

	private AppComponent buildComponent() {
		return DaggerAppComponent.builder()
				.appModule(new AppModule(this))
				.build();
	}
}
