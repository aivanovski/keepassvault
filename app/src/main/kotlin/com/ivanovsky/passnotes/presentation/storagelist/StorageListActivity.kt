package com.ivanovsky.passnotes.presentation.storagelist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.initActionBar
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.requireArgument

class StorageListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.core_base_activity)

        val action = intent.extras?.getSerializable(EXTRA_REQUIRED_ACTION) as? Action
            ?: requireArgument(EXTRA_REQUIRED_ACTION)

        initActionBar(R.id.tool_bar)

        val fragment = StorageListFragment.newInstance(action)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    companion object {

        const val EXTRA_RESULT: String = "result"

        private const val EXTRA_REQUIRED_ACTION = "requiredAction"

        fun createStartIntent(context: Context, requiredAction: Action): Intent {
            return Intent(context, StorageListActivity::class.java).apply {
                putExtra(EXTRA_REQUIRED_ACTION, requiredAction)
            }
        }
    }
}