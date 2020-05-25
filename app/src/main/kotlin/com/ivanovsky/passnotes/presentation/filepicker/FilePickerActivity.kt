package com.ivanovsky.passnotes.presentation.filepicker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.presentation.core.BaseActivity

class FilePickerActivity : BaseActivity() {

    private var isBrowsingEnabled: Boolean = false
    private lateinit var mode: Mode
    private lateinit var rootFile: FileDescriptor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.core_base_activity)

        mode = intent.extras.getSerializable(EXTRA_MODE) as Mode
        rootFile = intent.extras.getParcelable(EXTRA_ROOT_FILE)
        isBrowsingEnabled = intent.extras.getBoolean(EXTRA_IS_BROWSING_ENABLED)

        initCurrentActionBar(findViewById(R.id.tool_bar))
        currentActionBar.title = getString(R.string.select_directory)
        currentActionBar.setDisplayHomeAsUpEnabled(true)

        val fragment = FilePickerFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        val presenter = FilePickerPresenter(fragment, mode, rootFile, isBrowsingEnabled, this)
        fragment.presenter = presenter
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    companion object {

        const val EXTRA_RESULT = "result"
        private const val EXTRA_MODE = "mode"
        private const val EXTRA_ROOT_FILE = "rootFile"
        private const val EXTRA_IS_BROWSING_ENABLED = "isBrowsingEnabled"

        fun createStartIntent(
            context: Context, mode: Mode, rootFile: FileDescriptor,
            isBrowsingEnabled: Boolean
        ): Intent {
            val result = Intent(context, FilePickerActivity::class.java)

            result.putExtra(EXTRA_MODE, mode)
            result.putExtra(EXTRA_ROOT_FILE, rootFile)
            result.putExtra(EXTRA_IS_BROWSING_ENABLED, isBrowsingEnabled)

            return result
        }
    }
}