package com.ivanovsky.passnotes.presentation.debugmenu

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.BaseActivity
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.initActionBar

class DebugMenuActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.core_base_activity)

        initActionBar(R.id.tool_bar)
        setSupportActionBar(findViewById(R.id.tool_bar))

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, DebugMenuFragment.newInstance())
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            onBackPressed()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    companion object {

        fun createStartIntent(context: Context): Intent {
            return Intent(context, DebugMenuActivity::class.java)
        }
    }
}