package com.ivanovsky.passnotes.presentation.unlock

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Spinner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.ScreenDisplayingMode
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.debugmenu.DebugMenuActivity
import com.ivanovsky.passnotes.presentation.groups.GroupsActivity
import com.ivanovsky.passnotes.presentation.newdb.NewDatabaseActivity
import com.ivanovsky.passnotes.presentation.storagelist.Action
import com.ivanovsky.passnotes.presentation.storagelist.StorageListActivity
import com.ivanovsky.passnotes.util.FileUtils
import java.util.*
import java.util.regex.Pattern

class UnlockFragment : BaseFragment(), UnlockContract.View {

    override var presenter: UnlockContract.Presenter? = null

    private lateinit var fileAdapter: FileSpinnerAdapter
    private lateinit var passwordRules: List<PasswordAutofillRule>
    private lateinit var fileSpinner: Spinner
    private lateinit var fab: FloatingActionButton
    private lateinit var passwordEditText: EditText

    private val itemsData = MutableLiveData<List<DropDownItem>>()
    private val selectedItemData = MutableLiveData<Int>()
    private val showGroupsScreenEvent = SingleLiveEvent<Unit>()
    private val showNewDatabaseScreenEvent = SingleLiveEvent<Unit>()
    private val showOpenFileScreenEvent = SingleLiveEvent<Unit>()
    private val showSettingsScreenEvent = SingleLiveEvent<Unit>()
    private val showAboutScreenEvent = SingleLiveEvent<Unit>()
    private val showDebugMenuScreenEvent = SingleLiveEvent<Unit>()

    private val spinnerSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            autofillPasswordIfNeed()
            presenter?.onRecentlyUsedItemSelected(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            passwordRules = compileDebugAutofillPatterns()
        }
    }

    private fun compileDebugAutofillPatterns(): List<PasswordAutofillRule> {
        val rules = ArrayList<PasswordAutofillRule>()

        if (BuildConfig.DEBUG_FILE_NAME_PATTERNS != null && BuildConfig.DEBUG_PASSWORDS != null) {
            for (idx in BuildConfig.DEBUG_FILE_NAME_PATTERNS.indices) {
                val fileNamePattern = BuildConfig.DEBUG_FILE_NAME_PATTERNS[idx]

                val password = BuildConfig.DEBUG_PASSWORDS[idx]
                val pattern = Pattern.compile(fileNamePattern)

                rules.add(PasswordAutofillRule(pattern, password))
            }
        }

        return rules
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
        val view = inflater.inflate(R.layout.unlock_fragment, container, false)

        fileSpinner = view.findViewById(R.id.file_spinner)
        fab = view.findViewById(R.id.fab)
        passwordEditText = view.findViewById(R.id.password)
        val unlockButton = view.findViewById<View>(R.id.unlock_button)

        fileAdapter = FileSpinnerAdapter(context!!)

        fileSpinner.adapter = fileAdapter

        fab.setOnClickListener { showNewDatabaseScreen() }
        unlockButton.setOnClickListener { onUnlockButtonClicked() }

        itemsData.observe(viewLifecycleOwner,
            Observer { items -> setRecentlyUsedItemsInternal(items) })
        selectedItemData.observe(viewLifecycleOwner,
            Observer { position -> setSelectedRecentlyUsedItemInternal(position) })
        showGroupsScreenEvent.observe(viewLifecycleOwner,
            Observer { showGroupsScreenInternal() })
        showNewDatabaseScreenEvent.observe(viewLifecycleOwner,
            Observer { showNewDatabaseScreenInternal() })
        showOpenFileScreenEvent.observe(viewLifecycleOwner,
            Observer { showOpenFileScreenInternal() })
        showSettingsScreenEvent.observe(viewLifecycleOwner,
            Observer { showSettingsScreenInternal() })
        showAboutScreenEvent.observe(viewLifecycleOwner,
            Observer { showAboutScreenInternal() })
        showDebugMenuScreenEvent.observe(viewLifecycleOwner,
            Observer { showDebugMenuScreenInternal() })

       return view
    }

    private fun onUnlockButtonClicked() {
        val password = passwordEditText.text.toString().trim()

        presenter?.onUnlockButtonClicked(password)
    }

    override fun getContentContainerId(): Int {
        return R.id.content
    }

    override fun onScreenStateChanged(screenState: ScreenState) {
        when (screenState.displayingMode) {
            ScreenDisplayingMode.EMPTY,
            ScreenDisplayingMode.DISPLAYING_DATA_WITH_ERROR_PANEL,
            ScreenDisplayingMode.DISPLAYING_DATA -> fab.show()

            ScreenDisplayingMode.LOADING, ScreenDisplayingMode.ERROR -> fab.hide()
        }
    }

    override fun setRecentlyUsedItems(items: List<DropDownItem>) {
        itemsData.value = items
    }

    private fun setRecentlyUsedItemsInternal(items: List<DropDownItem>) {
        fileSpinner.onItemSelectedListener = null

        fileAdapter.setItems(items)
        fileAdapter.notifyDataSetChanged()

        fileSpinner.onItemSelectedListener = spinnerSelectedListener
    }

    override fun setSelectedRecentlyUsedItem(position: Int) {
        selectedItemData.value = position
    }

    private fun setSelectedRecentlyUsedItemInternal(position: Int) {
        fileSpinner.onItemSelectedListener = null
        fileSpinner.setSelection(position)
        fileSpinner.onItemSelectedListener = spinnerSelectedListener

        autofillPasswordIfNeed()
    }

    private fun autofillPasswordIfNeed() {
        if (!BuildConfig.DEBUG) {
            return
        }

        val selectedPosition = fileSpinner.selectedItemPosition

        val filePath = itemsData.value?.get(selectedPosition)?.path ?: return

        val fileName = FileUtils.getFileNameWithoutExtensionFromPath(filePath) ?: return
        for (rule in passwordRules) {
            if (rule.pattern.matcher(fileName).matches()) {
                passwordEditText.setText(rule.password)
                break
            }
        }
    }

    override fun showGroupsScreen() {
        showGroupsScreenEvent.call()
    }

    private fun showGroupsScreenInternal() {
        startActivity(GroupsActivity.startForRootGroup(context!!))
    }

    override fun showNewDatabaseScreen() {
        showNewDatabaseScreenEvent.call()
    }

    private fun showNewDatabaseScreenInternal() {
        startActivity(Intent(context, NewDatabaseActivity::class.java))
    }

    override fun showOpenFileScreen() {
        showOpenFileScreenEvent.call()
    }

    private fun showOpenFileScreenInternal() {
        val intent = StorageListActivity.createStartIntent(context!!, Action.PICK_FILE)
        startActivityForResult(intent, REQUEST_CODE_PICK_FILE)
    }

    override fun showSettingScreen() {
        showSettingsScreenEvent.call()
    }

    private fun showSettingsScreenInternal() {
        throw RuntimeException("Not implemented") //TODO: handle menu click
    }

    override fun showAboutScreen() {
        showAboutScreenEvent.call()
    }

    private fun showAboutScreenInternal() {
        throw RuntimeException("Not implemented") //TODO: handle menu click
    }

    override fun showDebugMenuScreen() {
        showDebugMenuScreenEvent.call()
    }

    private fun showDebugMenuScreenInternal() {
        val intent = DebugMenuActivity.createStartIntent(context!!)
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PICK_FILE) {
            val extras = data?.extras
            if (extras != null) {
                val file = extras.getParcelable<FileDescriptor>(StorageListActivity.EXTRA_RESULT)
                if (file != null) {
                    presenter?.onFilePicked(file)
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PICK_FILE = 100
    }

    private class PasswordAutofillRule(val pattern: Pattern, val password: String)

    data class DropDownItem(val filename: String, val path: String, val storageType: String)
}
