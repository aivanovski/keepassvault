package com.ivanovsky.passnotes.presentation.newdb

import android.os.Bundle
import android.view.MenuItem
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.BaseActivity

class NewDatabaseActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.core_base_activity)

        setSupportActionBar(findViewById(R.id.tool_bar))
        currentActionBar.title = getString(R.string.new_database)
        currentActionBar.setDisplayHomeAsUpEnabled(true)

        val fragment = NewDatabaseFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        val presenter = NewDatabasePresenter(fragment)
        fragment.presenter = presenter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            onBackPressed()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}