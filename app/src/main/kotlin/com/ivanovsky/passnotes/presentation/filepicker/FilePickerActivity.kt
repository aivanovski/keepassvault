package com.ivanovsky.passnotes.presentation.filepicker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.BaseActivity
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.initActionBar
import com.ivanovsky.passnotes.presentation.core_mvvm.extensions.requireExtraValue
import com.ivanovsky.passnotes.presentation.filepicker.model.FilePickerArgs

class FilePickerActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.core_base_activity)

        val args = intent.extras?.getParcelable(EXTRA_ARGS) as? FilePickerArgs
            ?: requireExtraValue(EXTRA_ARGS)

        initActionBar(R.id.tool_bar)

        val fragment = FilePickerFragment.newInstance(args)
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

        const val EXTRA_RESULT = "result"

        private const val EXTRA_ARGS = "args"

        fun createStartIntent(
            context: Context,
            args: FilePickerArgs
        ): Intent {
            return Intent(context, FilePickerActivity::class.java).apply {
                putExtra(EXTRA_ARGS, args)
            }
        }
    }
}