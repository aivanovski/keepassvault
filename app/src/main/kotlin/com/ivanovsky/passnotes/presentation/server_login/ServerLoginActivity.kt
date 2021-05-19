package com.ivanovsky.passnotes.presentation.server_login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.extensions.initActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.requireExtraValue

class ServerLoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.core_base_activity)
        initActionBar(R.id.tool_bar)

        val args = intent.extras?.getParcelable<ServerLoginArgs>(EXTRA_ARGS)
            ?: requireExtraValue(EXTRA_ARGS)

        val fragment = ServerLoginFragment.newInstance(args)
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
            args: ServerLoginArgs
        ): Intent {
            return Intent(context, ServerLoginActivity::class.java).apply {
                putExtra(EXTRA_ARGS, args)
            }
        }
    }
}