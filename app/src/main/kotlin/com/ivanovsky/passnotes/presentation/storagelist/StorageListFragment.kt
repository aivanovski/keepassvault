package com.ivanovsky.passnotes.presentation.storagelist

import android.app.Activity
import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.repository.file.FSType
import com.ivanovsky.passnotes.domain.entity.StorageOption
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.adapter.SingleLineAdapter
import com.ivanovsky.passnotes.presentation.filepicker.FilePickerActivity

class StorageListFragment : BaseFragment(), StorageListContract.View {

	private lateinit var presenter: StorageListContract.Presenter
	private lateinit var adapter: SingleLineAdapter

	companion object {

		private const val REQUEST_CODE_PICK_FILE = 100

		fun newInstance(): StorageListFragment {
			return StorageListFragment()
		}
	}

	override fun setPresenter(presenter: StorageListContract.Presenter) {
		this.presenter = presenter
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
		val view = inflater.inflate(R.layout.storage_fragment, container, false)

		val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)

		val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
		val dividerDecorator = DividerItemDecoration(context, layoutManager.orientation)

		adapter = SingleLineAdapter(context)

		recyclerView.layoutManager = layoutManager
		recyclerView.addItemDecoration(dividerDecorator)
		recyclerView.adapter = adapter

		presenter.screenState.observe(this,
				Observer { screenState -> setScreenState(screenState)})
		presenter.storageOptions.observe(this,
				Observer { options -> setStorageOptions(options!!)})
		presenter.showFilePickerScreenAction.observe(this,
				Observer { fileAndMode -> showFilePickerScreen(fileAndMode!!.first, fileAndMode.second) })
		presenter.fileSelectedAction.observe(this,
				Observer { file -> selectFileAndFinish(file!!) })

		return view
	}

	override fun setStorageOptions(options: List<StorageOption>) {
		adapter.setItems(createAdapterItems(options))
		adapter.notifyDataSetChanged()

		adapter.onItemClickListener = { pos -> presenter.onStorageOptionClicked(options[pos])}
	}

	override fun showFilePickerScreen(root: FileDescriptor, mode: Mode) {
		if (root.fsType == FSType.REGULAR_FS) {
			val intent = FilePickerActivity.createStartIntent(context,
					convertModeForFilePicker(mode),
					root)

			startActivityForResult(intent, REQUEST_CODE_PICK_FILE)

		} else if (root.fsType == FSType.DROPBOX) {
			throw RuntimeException("Not implemented")//TODO: implement
		}
	}

	private fun convertModeForFilePicker(mode: Mode): com.ivanovsky.passnotes.presentation.filepicker.Mode {
		return when (mode) {
			Mode.PICK_FILE -> com.ivanovsky.passnotes.presentation.filepicker.Mode.PICK_FILE
			Mode.PICK_DIRECTORY -> com.ivanovsky.passnotes.presentation.filepicker.Mode.PICK_DIRECTORY
		}
	}

	private fun createAdapterItems(options: List<StorageOption>): List<SingleLineAdapter.Item> {
		return options.map { option -> SingleLineAdapter.Item(option.title) }
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PICK_FILE) {
			val extras = data?.extras
			if (extras != null) {
				val file = extras.getParcelable<FileDescriptor>(FilePickerActivity.EXTRA_RESULT)
				presenter.onFilePicked(file)
			}
		}
	}

	override fun selectFileAndFinish(file: FileDescriptor) {
		val data = Intent()

		data.putExtra(StorageListActivity.EXTRA_RESULT, file)

		activity.setResult(Activity.RESULT_OK, data)
		activity.finish()
	}
}