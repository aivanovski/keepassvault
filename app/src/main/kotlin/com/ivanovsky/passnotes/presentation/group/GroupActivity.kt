package com.ivanovsky.passnotes.presentation.group

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.presentation.core.BaseActivity
import java.util.*

class GroupActivity : BaseActivity() {

    private var parentGroupUid: UUID? = null

    object ExtraKeys {
        const val PARENT_GROUP_UID = "parentGroupUid"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.core_base_activity)

        readExtraArgs()

        setSupportActionBar(findViewById(R.id.tool_bar))
        currentActionBar.title = getString(R.string.new_group)
        currentActionBar.setDisplayHomeAsUpEnabled(true)

        val fragment = GroupFragment.newInstance()
        val presenter = GroupPresenter(fragment, parentGroupUid)
        fragment.presenter = presenter

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun readExtraArgs() {
        val extras = intent.extras ?: return

        parentGroupUid = extras.getSerializable(ExtraKeys.PARENT_GROUP_UID) as? UUID
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

        fun createGroupInRoot(context: Context): Intent {
            return Intent(context, GroupActivity::class.java)
        }

        fun createChildGroup(context: Context, parentGroupUid: UUID?): Intent {
            val intent = Intent(context, GroupActivity::class.java)
            intent.putExtra(ExtraKeys.PARENT_GROUP_UID, parentGroupUid)
            return intent
        }
    }
}