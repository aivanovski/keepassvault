package com.ivanovsky.passnotes.ui.notes

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import com.ivanovsky.passnotes.App
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.safedb.model.Group
import com.ivanovsky.passnotes.databinding.CoreBaseActivityBinding
import com.ivanovsky.passnotes.ui.core.BaseActivity

class NotesActivity : BaseActivity() {

    private val EXTRA_GROUP_UID = "groupUid"
    private val EXTRA_GROUP_TITLE = "groupTitle"

    companion object {
        fun createStartIntent(context: Context, group: Group): Intent {
            val result =  Intent(context, NotesActivity::class.java)


            return result
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.getDaggerComponent().inject(this)

        var binding = DataBindingUtil.setContentView<CoreBaseActivityBinding>(this, R.layout.core_base_activity)

        setSupportActionBar(binding.toolBar)
        currentActionBar.title = ""
    }
}