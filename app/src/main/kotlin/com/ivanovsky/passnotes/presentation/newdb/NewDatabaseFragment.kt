package com.ivanovsky.passnotes.presentation.newdb

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.Observer
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.validation.*
import com.ivanovsky.passnotes.presentation.groups.GroupsActivity
import com.ivanovsky.passnotes.presentation.storagelist.Action
import com.ivanovsky.passnotes.presentation.storagelist.StorageListActivity
import com.ivanovsky.passnotes.util.InputMethodUtils.hideSoftInput
import java.util.regex.Pattern

class NewDatabaseFragment : BaseFragment(), NewDatabaseContract.View {

    override var presenter: NewDatabaseContract.Presenter? = null
    private lateinit var menu: Menu
    private lateinit var storageLayout: View
    private lateinit var storageTypeTextView: TextView
    private lateinit var storagePathTextView: TextView
    private lateinit var filenameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmationEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onStart() {
        super.onStart()
        presenter?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter?.destroy()
    }

    override fun onCreateContentView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.new_database_fragment, container, false)

        storageLayout = view.findViewById(R.id.storage_layout)
        storageTypeTextView = view.findViewById(R.id.storage_type)
        storagePathTextView = view.findViewById(R.id.storage_path)
        filenameEditText = view.findViewById(R.id.filename)
        passwordEditText = view.findViewById(R.id.password)
        confirmationEditText = view.findViewById(R.id.password_confirmation)

        storageLayout.setOnClickListener { presenter?.selectStorage() }

        presenter?.storageTypeAndPath?.observe(this,
            Observer { typeAndPath ->
                setStorageTypeAndPath(
                    typeAndPath!!.first,
                    typeAndPath.second
                )
            })
        presenter?.doneButtonVisibility?.observe(this,
            Observer { isVisible -> setDoneButtonVisibility(isVisible!!) })
        presenter?.showGroupsScreenEvent?.observe(this,
            Observer { showGroupsScreen() })
        presenter?.showStorageScreenEvent?.observe(this,
            Observer { showStorageScreen() })

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        this.menu = menu

        inflater.inflate(R.menu.base_done, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.menu_done) {
            onDoneMenuClicked()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun onDoneMenuClicked() {
        val validator = Validator.Builder()
            .validation(
                NotEmptyValidation.Builder()
                    .withTarget(filenameEditText)
                    .withTarget(passwordEditText)
                    .withTarget(confirmationEditText)
                    .withErrorMessage(R.string.empty_field)
                    .withPriority(BaseValidation.PRIORITY_MAX)
                    .abortOnError(true)
                    .build()
            )
            .validation(
                PatternValidation.Builder()
                    .withPattern(FILE_NAME_PATTERN)
                    .withTarget(filenameEditText)
                    .withErrorMessage(R.string.field_contains_illegal_character)
                    .build()
            )
            .validation(
                PatternValidation.Builder()
                    .withPattern(PASSWORD_PATTERN)
                    .withTarget(passwordEditText)
                    .withErrorMessage(R.string.field_contains_illegal_character)
                    .abortOnError(true)
                    .build()
            )
            .validation(
                IdenticalContentValidation.Builder()
                    .withFirstTarget(passwordEditText)
                    .withSecondTarget(confirmationEditText)
                    .withErrorMessage(R.string.this_field_should_match_password)
                    .build()
            )
            .build()
        if (validator.validateAll()) {
            val filename = filenameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            presenter?.createNewDatabaseFile(filename, password)
        }
    }

    override fun setStorageTypeAndPath(type: String, path: String) {
        storageTypeTextView.text = type
        storagePathTextView.text = path
    }

    override fun setDoneButtonVisibility(isVisible: Boolean) {
        val item = menu.findItem(R.id.menu_done)

        item?.isVisible = isVisible
    }

    override fun showGroupsScreen() {
        activity!!.finish()

        startActivity(GroupsActivity.startForRootGroup(context!!))
    }

    override fun showStorageScreen() {
        startActivityForResult(
            StorageListActivity.createStartIntent(context!!, Action.PICK_STORAGE),
            REQUEST_CODE_PICK_STORAGE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val extras = data?.extras
        if (resultCode == Activity.RESULT_OK &&
            requestCode == REQUEST_CODE_PICK_STORAGE &&
            extras != null
        ) {
            val file = extras.getParcelable<FileDescriptor>(StorageListActivity.EXTRA_RESULT)
            if (file != null) {
                presenter?.onStorageSelected(file)
            }
        }
    }

    companion object {

        private const val REQUEST_CODE_PICK_STORAGE = 100

        private val FILE_NAME_PATTERN = Pattern.compile("[\\w]{1,50}")
        private val PASSWORD_PATTERN = Pattern.compile("[\\w@#$!%^&+=]{4,20}")

        fun newInstance(): NewDatabaseFragment {
            return NewDatabaseFragment()
        }
    }
}