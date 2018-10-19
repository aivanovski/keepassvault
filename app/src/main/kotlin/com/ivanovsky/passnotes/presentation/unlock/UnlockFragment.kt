package com.ivanovsky.passnotes.presentation.unlock

import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Spinner

import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.UsedFile
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.FragmentState
import com.ivanovsky.passnotes.presentation.groups.GroupsActivity
import com.ivanovsky.passnotes.presentation.newdb.NewDatabaseActivity

import java.io.File
import java.util.ArrayList
import java.util.regex.Pattern

import com.ivanovsky.passnotes.util.FileUtils.getFileNameFromPath
import com.ivanovsky.passnotes.util.FileUtils.getFileNameWithoutExtensionFromPath
import com.ivanovsky.passnotes.util.InputMethodUtils.hideSoftInput

class UnlockFragment : BaseFragment(), UnlockContract.View {

	private lateinit var selectedUsedFile: UsedFile
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

	override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
		val view = inflater.inflate(R.layout.unlock_fragment, container, false)

		fileSpinner = view.findViewById(R.id.file_spinner)
		fab = view.findViewById(R.id.fab)
		passwordEditText = view.findViewById(R.id.password)
		val unlockButton = view.findViewById<View>(R.id.unlock_button)

		fileAdapter = FileSpinnerAdapter(context)

		fileSpinner.adapter = fileAdapter

		fab.setOnClickListener { showNewDatabaseScreen() }
		unlockButton.setOnClickListener { onUnlockButtonClicked() }

		presenter.recentlyUsedFiles.observe(this,
				Observer { files -> setRecentlyUsedFiles(files!!) })
		presenter.screenState.observe(this,
				Observer { state -> setScreenState(state) })
		presenter.showGroupsScreenAction.observe(this,
				Observer { showGroupsScreen() })
		presenter.showNewDatabaseScreenAction.observe(this,
				Observer { showNewDatabaseScreen() })
		presenter.hideKeyboardAction.observe(this,
				Observer { hideKeyboard() })

		return view
	}

	private fun onUnlockButtonClicked() {
		val password = passwordEditText.text.toString().trim { it <= ' ' }

		val dbFile = File(selectedUsedFile.filePath)
		presenter.onUnlockButtonClicked(password, dbFile)
	}

	override fun getContentContainerId(): Int {
		return R.id.content
	}

	override fun onStateChanged(oldState: FragmentState?, newState: FragmentState) {
		when (newState) {
			FragmentState.EMPTY, FragmentState.DISPLAYING_DATA_WITH_ERROR_PANEL, FragmentState.DISPLAYING_DATA -> fab.visibility = View.VISIBLE

			FragmentState.LOADING, FragmentState.ERROR -> fab.visibility = View.GONE
		}
	}

	override fun setPresenter(presenter: UnlockContract.Presenter) {
		this.presenter = presenter
	}

	private fun createAdapterItems(files: List<UsedFile>): List<FileSpinnerAdapter.Item> {
		val items = ArrayList<FileSpinnerAdapter.Item>()

		for (file in files) {
			val path = file.filePath
			val filename = getFileNameFromPath(path)

			if (filename != null) {
				items.add(FileSpinnerAdapter.Item(filename, path))
			}
		}

		return items
	}

	private fun onFileSelected(file: UsedFile) {
		selectedUsedFile = file

		if (BuildConfig.DEBUG) {
			val fileName = getFileNameWithoutExtensionFromPath(file.filePath)
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

	override fun setRecentlyUsedFiles(files: List<UsedFile>) {
		selectedUsedFile = files[0]

		fileAdapter.setItem(createAdapterItems(files))
		fileAdapter.notifyDataSetChanged()

		fileSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
			override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
				onFileSelected(files[position])
			}

			override fun onNothingSelected(parent: AdapterView<*>) {}
		}
	}

	override fun showGroupsScreen() {
		startActivity(GroupsActivity.createStartIntent(context))
	}

	override fun showNewDatabaseScreen() {
		startActivity(Intent(context, NewDatabaseActivity::class.java))
	}

	override fun hideKeyboard() {
		hideSoftInput(activity)
	}

	private class PasswordRule(val pattern: Pattern, val password: String)
}
