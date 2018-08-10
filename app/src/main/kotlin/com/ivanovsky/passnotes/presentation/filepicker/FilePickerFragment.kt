package com.ivanovsky.passnotes.presentation.filepicker

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.domain.PermissionHelper
import com.ivanovsky.passnotes.domain.entity.FileListAndParent
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.BaseListFragment
import javax.inject.Inject

class FilePickerFragment : BaseListFragment<List<FileDescriptor>>(), FilePickerContract.View {

	@Inject
	lateinit var permissionHelper: PermissionHelper

	private lateinit var presenter: FilePickerContract.Presenter
	private lateinit var adapter: FilePickerAdapter

	private var menu: Menu? = null

	companion object {

		private const val REQUEST_CODE_PERMISSION = 100

		fun newInstance(): FilePickerFragment {
			return FilePickerFragment()
		}
	}

	init {
		Injector.getInstance().appComponent.inject(this)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		presenter.items.observe(this,
				Observer { files -> updateItems(files!!) })
		presenter.screenState.observe(this,
				Observer { state -> setScreenState(state) })
		presenter.requestPermissionAction.observe(this,
				Observer { permission -> requestPermission(permission!!) })
		presenter.doneButtonVisibility.observe(this,
				Observer { isVisible -> updateDoneButtonVisibility(isVisible!!)})

		setHasOptionsMenu(true)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		this.menu = menu

		inflater.inflate(R.menu.base_done, menu)
	}

	private fun updateDoneButtonVisibility(isVisible: Boolean) {
		if (menu != null) {
			val item = menu!!.findItem(R.id.menu_done) //TODO: wtf with !!
			item.isVisible = isVisible
		}
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == R.id.menu_done) {

			return true
		} else {
			return super.onOptionsItemSelected(item)
		}
	}

	override fun setPresenter(presenter: FilePickerContract.Presenter) {
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

	override fun onCreateConfig(): Config {
		return Config(layout = Layout.VERTICAL_LIST,
				isFloatingActionButtonEnabled = false,
				isSwipeRefreshEnabled = false,
				isDividerEnabled = true)
	}

	override fun onCreateListAdapter(): RecyclerView.Adapter<out RecyclerView.ViewHolder> {
		adapter = FilePickerAdapter(context)
		return adapter
	}

	private fun updateItems(filesAndParent: FileListAndParent) {
		val files = filesAndParent.files.toMutableList()

		if (filesAndParent.parent != null) {
			files.add(0, filesAndParent.parent)
		}

		adapter.setItems(createAdapterItems(files, filesAndParent.parent))
		adapter.notifyDataSetChanged()

		adapter.onItemClickListener = { position -> presenter.onFileSelected(files[position])}
	}

	private fun createAdapterItems(files: List<FileDescriptor>, parent: FileDescriptor?): List<FilePickerAdapter.Item> {
		val items = mutableListOf<FilePickerAdapter.Item>()

		for (file in files) {
			val iconResId = getIconResId(file.isDirectory)
			val title = if (file == parent) ".." else file.name + "/"
			val description = "25 Jun 2016"

			items.add(FilePickerAdapter.Item(iconResId, title, description, false))
		}

		return items
	}

	@DrawableRes
	private fun getIconResId(isDirectory: Boolean): Int {
		return if (isDirectory) R.drawable.ic_folder_white_24dp else R.drawable.ic_file_white_24dp
	}

	private fun requestPermission(permission: String) {
		permissionHelper.requestPermission(this, permission, REQUEST_CODE_PERMISSION)
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)

		if (requestCode == REQUEST_CODE_PERMISSION) {
			presenter.onPermissionResult(permissionHelper.isAllGranted(grantResults))
		}
	}
}