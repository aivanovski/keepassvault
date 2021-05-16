package com.ivanovsky.passnotes.presentation.groups

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Group
import com.ivanovsky.passnotes.presentation.core.extensions.initActionBar

class GroupsActivity : AppCompatActivity() {

    object ExtraKeys {
        const val GROUP = "group"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.core_base_activity)

        initActionBar(R.id.tool_bar)

        val group = intent?.extras?.getParcelable(ExtraKeys.GROUP) as? Group
        val fragment = GroupsFragment.newInstance(group)

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

        fun startForRootGroup(context: Context): Intent {
            return Intent(context, GroupsActivity::class.java)
        }

        fun intentFroGroup(context: Context, group: Group): Intent {
            val intent = Intent(context, GroupsActivity::class.java)

            intent.putExtra(ExtraKeys.GROUP, group)

            return intent
        }
    }
}
