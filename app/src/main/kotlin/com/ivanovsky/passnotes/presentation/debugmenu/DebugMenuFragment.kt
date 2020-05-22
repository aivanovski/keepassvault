package com.ivanovsky.passnotes.presentation.debugmenu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import androidx.lifecycle.Observer
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.repository.file.FSType
import com.ivanovsky.passnotes.presentation.core.BaseFragment

class DebugMenuFragment : BaseFragment(), DebugMenuContract.View {

	override lateinit var presenter: DebugMenuContract.Presenter

	private lateinit var fileSystemSpinner: Spinner
	private lateinit var filePathEditText: EditText
	private lateinit var passwordEditText: EditText
	private lateinit var readButton: View
	private lateinit var writeButton: View
	private lateinit var newButton: View
	private lateinit var openDbButton: View
	private lateinit var closeDbButton: View
	private lateinit var addEntryButton: View
	private lateinit var externalStorageCheckBox: CheckBox

	override fun onStart() {
		super.onStart()
		presenter.start()
	}

	override fun onDestroy() {
		super.onDestroy()
		presenter.destroy()
	}

	override fun onCreateContentView(inflater: LayoutInflater,
									 container: ViewGroup?,
									 savedInstanceState: Bundle?): View {
		val view = inflater.inflate(R.layout.debug_menu_layout, container, false)

		fileSystemSpinner = view.findViewById(R.id.file_system_spinner)
		filePathEditText = view.findViewById(R.id.file_path)
		passwordEditText = view.findViewById(R.id.password)
		readButton = view.findViewById(R.id.read_button)
		writeButton = view.findViewById(R.id.write_button)
		newButton = view.findViewById(R.id.new_button)
		openDbButton = view.findViewById(R.id.open_button)
		closeDbButton = view.findViewById(R.id.close_button)
		addEntryButton = view.findViewById(R.id.add_entry_button)
		externalStorageCheckBox = view.findViewById(R.id.external_storage_check_box)

		fileSystemSpinner.adapter = createSpinnerAdapter()

		presenter.writeButtonEnabled.observe(this,
				Observer { isEnabled -> setWriteButtonEnabled(isEnabled) })
		presenter.openDbButtonEnabled.observe(this,
				Observer { isEnabled -> setOpenDbButtonEnabled(isEnabled) })
		presenter.closeDbButtonEnabled.observe(this,
				Observer { isEnabled -> setCloseDbButtonEnabled(isEnabled) })
		presenter.addEntryButtonEnabled.observe(this,
				Observer { isEnabled -> setAddEntryButtonEnabled(isEnabled) })
		presenter.externalStorageCheckBoxChecked.observe(this,
				Observer { isChecked -> setExternalStorageCheckBoxChecked(isChecked) })

		readButton.setOnClickListener { onReadButtonClicked() }
		writeButton.setOnClickListener { onWriteButtonClicked() }
		newButton.setOnClickListener { onNewButtonClicked() }
		openDbButton.setOnClickListener { onOpenDbButtonClicked() }
		closeDbButton.setOnClickListener { onCloseDbButtonClicked() }
		addEntryButton.setOnClickListener { onAddEntryButtonClicked() }

		return view
	}

	private fun createSpinnerAdapter(): ArrayAdapter<String> {
		val items = arrayListOf("Device file system", "Dropbox")
		val adapter = ArrayAdapter(context!!, android.R.layout.simple_spinner_item, items)
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
		return adapter
	}

	override fun setWriteButtonEnabled(isEnabled: Boolean) {
		writeButton.isEnabled = isEnabled
	}

	override fun setOpenDbButtonEnabled(isEnabled: Boolean) {
		openDbButton.isEnabled = isEnabled
	}

	override fun setCloseDbButtonEnabled(isEnabled: Boolean) {
		closeDbButton.isEnabled = isEnabled
	}

	override fun setAddEntryButtonEnabled(isEnabled: Boolean) {
		addEntryButton.isEnabled = isEnabled
	}

	override fun setExternalStorageCheckBoxChecked(isChecked: Boolean) {
		externalStorageCheckBox.setOnCheckedChangeListener(null)

		externalStorageCheckBox.isChecked = isChecked

		externalStorageCheckBox.setOnCheckedChangeListener { _, checked ->
			onExternalStorageCheckedChanged(checked)
		}
	}

	private fun onReadButtonClicked() {
		presenter.onReadButtonClicked(getSelectedFile())
	}

	private fun onWriteButtonClicked() {
		presenter.onWriteButtonClicked()
	}

	private fun onNewButtonClicked() {
		presenter.onNewButtonClicked(getPassword(), getSelectedFile())
	}

	private fun onOpenDbButtonClicked() {
		presenter.onOpenDbButtonClicked(getPassword())
	}

	private fun onCloseDbButtonClicked() {
		presenter.onCloseDbButtonClicked()
	}

	private fun onAddEntryButtonClicked() {
		presenter.onAddEntryButtonClicked()
	}

	private fun getSelectedFile(): FileDescriptor {
		val fsType = getSelectedFileSystem()
		val path = getPath()

		val file = FileDescriptor()

		file.fsType = fsType
		file.path = path
		file.isDirectory = false
		file.isRoot = false
		file.uid = null

		return file
	}

	private fun getSelectedFileSystem(): FSType {
		return if (fileSystemSpinner.selectedItemPosition == 0) {
			FSType.REGULAR_FS
		} else {
			FSType.DROPBOX
		}
	}

	private fun getPath(): String {
		return filePathEditText.text.toString()
	}

	private fun getPassword(): String {
		return passwordEditText.text.toString()
	}

	private fun onExternalStorageCheckedChanged(isChecked: Boolean) {
		presenter.onExternalStorageCheckedChanged(isChecked)
	}

	companion object {
		fun newInstance(): DebugMenuFragment {
			return DebugMenuFragment()
		}
	}
}