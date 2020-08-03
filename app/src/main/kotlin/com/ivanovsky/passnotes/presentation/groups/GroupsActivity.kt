package com.ivanovsky.passnotes.presentation.groups

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.injection.DaggerInjector
import com.ivanovsky.passnotes.presentation.core.BaseActivity
import java.util.*

class GroupsActivity : BaseActivity() {

    private var groupUid: UUID? = null
    private var groupTitle: String? = null

    object ExtraKeys {
        const val GROUP_UID = "group_uid"
        const val GROUP_TITLE = "title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DaggerInjector.getInstance().appComponent.inject(this)

        setContentView(R.layout.core_base_activity)

        readExtraArgs()

        setSupportActionBar(findViewById(R.id.tool_bar))
        currentActionBar.title = groupTitle ?: getString(R.string.groups)
        currentActionBar.setDisplayHomeAsUpEnabled(true)

        val fragment = GroupsFragment()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        val presenter = GroupsPresenter(fragment, groupUid)
        fragment.presenter = presenter
    }

    private fun readExtraArgs() {
        val extras = intent.extras ?: return

        groupTitle = extras.getString(ExtraKeys.GROUP_TITLE)
        groupUid = extras.getSerializable(ExtraKeys.GROUP_UID) as? UUID
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

        fun startForRootGroup(context: Context): Intent {
            return Intent(context, GroupsActivity::class.java)
        }

        fun intentFroGroup(context: Context, group: Group): Intent {
            val intent = Intent(context, GroupsActivity::class.java)

            intent.putExtra(ExtraKeys.GROUP_UID, group.uid)
            intent.putExtra(ExtraKeys.GROUP_TITLE, group.title)

            return intent
        }
    }
}
