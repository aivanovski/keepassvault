package com.ivanovsky.passnotes.presentation.unlock

import android.app.Activity
import androidx.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Spinner

import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.repository.file.FSType
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.FragmentState
import com.ivanovsky.passnotes.presentation.debugmenu.DebugMenuActivity
import com.ivanovsky.passnotes.presentation.groups.GroupsActivity
import com.ivanovsky.passnotes.presentation.newdb.NewDatabaseActivity
import com.ivanovsky.passnotes.presentation.storagelist.Action
import com.ivanovsky.passnotes.presentation.storagelist.StorageListActivity

import java.util.ArrayList
import java.util.regex.Pattern

import com.ivanovsky.passnotes.util.FileUtils.getFileNameFromPath
import com.ivanovsky.passnotes.util.FileUtils.getFileNameWithoutExtensionFromPath
import com.ivanovsky.passnotes.util.InputMethodUtils.hideSoftInput

private const val REQUEST_CODE_PICK_FILE = 100

class UnlockFragment : BaseFragment(), UnlockContract.View {

	private var selectedFile: FileDescriptor? = null
	private var files: List<FileDescriptor>? = null

	private lateinit var fileAdapter: FileSpinnerAdapter
	private lateinit var presenter: UnlockContract.Presenter
	private lateinit var passwordRules: List<PasswordRule>
	private lateinit var fileSpinner: Spinner
	private lateinit var fab: FloatingActionButton
	private lateinit var passwordEditText: EditText

	companion object {

		fun newInstance(): UnlockFragment {
			return UnlockFragment()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		if (BuildConfig.DEBUG) {
			passwordRules = compileFileNamePatterns()
		}
	}

	private fun compileFileNamePatterns(): List<PasswordRule> {
		val rules = ArrayList<PasswordRule>()

		if (BuildConfig.DEBUG_FILE_NAME_PATTERNS != null && BuildConfig.DEBUG_PASSWORDS != null) {
			for (idx in BuildConfig.DEBUG_FILE_NAME_PATTERNS.indices) {
				val fileNamePattern = BuildConfig.DEBUG_FILE_NAME_PATTERNS[idx]

				val password = BuildConfig.DEBUG_PASSWORDS[idx]
				val pattern = Pattern.compile(fileNamePattern)

				rules.add(PasswordRule(pattern, password))
			}
		}

		return rules
	}

	override fun onResume() {
		super.onResume()
		presenter.start()
	}

	override fun onPause() {
		super.onPause()
		presenter.stop()
	}

	override fun onDestroy() {
		super.onDestroy()
		presenter.destroy()
	}

	override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
		val view = inflater.inflate(R.layout.unlock_fragment, container, false)

		fileSpinner = view.findViewById(R.id.file_spinner)
		fab = view.findViewById(R.id.fab)
		passwordEditText = view.findViewById(R.id.password)
		val unlockButton = view.findViewById<View>(R.id.unlock_button)

		fileAdapter = FileSpinnerAdapter(context!!)

		fileSpinner.adapter = fileAdapter

		fab.setOnClickListener { showNewDatabaseScreen() }
		unlockButton.setOnClickListener { onUnlockButtonClicked() }

		presenter.recentlyUsedFiles.observe(this,
				Observer { files -> setRecentlyUsedFiles(files!!) })
		presenter.selectedRecentlyUsedFile.observe(this,
				Observer { selectedFile -> setSelectedFile(selectedFile!!)})
		presenter.screenState.observe(this,
				Observer { state -> setScreenState(state) })
		presenter.showGroupsScreenAction.observe(this,
				Observer { showGroupsScreen() })
		presenter.showNewDatabaseScreenAction.observe(this,
				Observer { showNewDatabaseScreen() })
		presenter.hideKeyboardAction.observe(this,
				Observer { hideKeyboard() })
		presenter.showOpenFileScreenAction.observe(this,
				Observer { showOpenFileScreen() })
		presenter.showSettingsScreenAction.observe(this,
				Observer { showSettingScreen() })
		presenter.showAboutScreenAction.observe(this,
				Observer { showAboutScreen() })
		presenter.showDebugMenuScreenAction.observe(this,
				Observer { showDebugMenuScreen() })
		presenter.snackbarMessageAction.observe(this,
				Observer { message -> showSnackbar(message!!) })

		return view
	}

	private fun onUnlockButtonClicked() {
		val password = passwordEditText.text.toString().trim()

		val file = selectedFile
		if (file != null) {
			presenter.onUnlockButtonClicked(password, file)
		}
	}

	override fun getContentContainerId(): Int {
		return R.id.content
	}

	override fun onStateChanged(oldState: FragmentState?, newState: FragmentState) {
		when (newState) {
			FragmentState.EMPTY,
			FragmentState.DISPLAYING_DATA_WITH_ERROR_PANEL,
			FragmentState.DISPLAYING_DATA -> fab.show()

			FragmentState.LOADING, FragmentState.ERROR -> fab.hide()
		}
	}

	override fun setPresenter(presenter: UnlockContract.Presenter) {
		this.presenter = presenter
	}

	private fun createAdapterItems(files: List<FileDescriptor>): List<FileSpinnerAdapter.Item> {
		val items = ArrayList<FileSpinnerAdapter.Item>()

		for (file in files) {
			val path = file.path
			val filename = getFileNameFromPath(path)

			if (filename != null) {
				items.add(FileSpinnerAdapter.Item(filename, path, formatFsType(file.fsType)))
			}
		}

		return items
	}

	private fun formatFsType(fsType: FSType): String {
		return when (fsType) {
			FSType.DROPBOX -> "Dropbox"
			FSType.REGULAR_FS -> "Device"
		}
	}

	private fun setSelectedFile(file: FileDescriptor) {
		selectedFile = file
		selectFileInSpinner(file)

		if (BuildConfig.DEBUG) {
			val fileName = getFileNameWithoutExtensionFromPath(file.path)
			if (fileName != null) {
				for (rule in passwordRules) {
					if (rule.pattern.matcher(fileName).matches()) {
						passwordEditText.setText(rule.password)
						break
					}
				}
			}
		}
	}

	override fun setRecentlyUsedFiles(files: List<FileDescriptor>) {
		this.files = files

		fileSpinner.onItemSelectedListener = null

		fileAdapter.setItem(createAdapterItems(files))
		fileAdapter.notifyDataSetChanged()

		fileSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
			override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
				presenter.onFileSelectedByUser(files[position])
			}

			override fun onNothingSelected(parent: AdapterView<*>) {
			}
		}
	}

	override fun selectFileInSpinner(file: FileDescriptor) {
		val files = this.files
		if (files != null) {
			val position = files.indexOfFirst { f -> isFileEqualsByUidAndFsType(f, file) }
			if (position != -1) {
				fileSpinner.setSelection(position)
			}
		}
	}

	private fun isFileEqualsByUidAndFsType(lhs: FileDescriptor, rhs: FileDescriptor): Boolean {
		return lhs.uid == rhs.uid && lhs.fsType == rhs.fsType
	}

	override fun showGroupsScreen() {
		startActivity(GroupsActivity.createStartIntent(context!!))
	}

	override fun showNewDatabaseScreen() {
		startActivity(Intent(context, NewDatabaseActivity::class.java))
	}

	override fun hideKeyboard() {
		hideSoftInput(activity)
	}

	override fun showOpenFileScreen() {
		val intent = StorageListActivity.createStartIntent(context!!, Action.PICK_FILE)
		startActivityForResult(intent, REQUEST_CODE_PICK_FILE)
	}

	override fun showSettingScreen() {
		throw RuntimeException("Not implemented") //TODO: handle menu click
	}

	override fun showAboutScreen() {
		throw RuntimeException("Not implemented") //TODO: handle menu click
	}

	override fun showDebugMenuScreen() {
		val intent = DebugMenuActivity.createStartIntent(context!!)
		startActivity(intent)
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PICK_FILE) {
			val extras = data?.extras
			if (extras != null) {
				val file = extras.getParcelable<FileDescriptor>(StorageListActivity.EXTRA_RESULT)
				if (file != null) {
					presenter.onFilePicked(file)
				}
			}
		}
	}

	private class PasswordRule(val pattern: Pattern, val password: String)
}
