package com.ivanovsky.passnotes.presentation.storagelist

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.os.Environment
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
import com.ivanovsky.passnotes.presentation.filepicker.Mode
import java.io.File

class StorageListFragment : BaseFragment(), StorageListContract.View {

	private lateinit var presenter: StorageListContract.Presenter
	private lateinit var adapter: SingleLineAdapter

	companion object {

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
				Observer { options -> showStorageOptions(options!!)})

		presenter.showFilePickerScreenAction.observe(this,
				Observer { option -> showFilePickerScreen(option!!) })

		return view
	}

	private fun showStorageOptions(options: List<StorageOption>) {
		adapter.setItems(createAdapterItems(options))
		adapter.notifyDataSetChanged()

		adapter.onItemClickListener = { pos -> presenter.selectStorage(options[pos])}
	}

	private fun showFilePickerScreen(option: StorageOption) {
		if (option.type == FSType.REGULAR_FS) {
			val mode = Mode.PICK_DIRECTORY
			val rootFile = FileDescriptor.fromRegularFile(createExternalStorageDir())//TODO: fix path creating

			startActivityForResult(FilePickerActivity.createStartIntent(context, mode, rootFile), 1)
		} else if (option.type == FSType.DROPBOX) {
			throw RuntimeException("Not implemented")//TODO: implement
		}
	}

	private fun createAdapterItems(options: List<StorageOption>): List<SingleLineAdapter.Item> {
		return options.map { option -> SingleLineAdapter.Item(option.name) }
	}

	private fun createExternalStorageDir(): File {
		return Environment.getExternalStorageDirectory()
	}
}