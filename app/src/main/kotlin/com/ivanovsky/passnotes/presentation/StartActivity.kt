package com.ivanovsky.passnotes.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ivanovsky.passnotes.presentation.main.MainActivity
import com.ivanovsky.passnotes.presentation.main.MainScreenArgs

class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(MainActivity.createStartIntent(this, MainScreenArgs(ApplicationLaunchMode.NORMAL)))
        finish()
    }
}