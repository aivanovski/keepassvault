package com.ivanovsky.passnotes.presentation;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ivanovsky.passnotes.presentation.unlock.UnlockActivity;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(UnlockActivity.Companion.createStartIntent(this));
        finish();
    }
}
