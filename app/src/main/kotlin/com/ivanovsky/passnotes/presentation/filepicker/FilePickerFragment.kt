package com.ivanovsky.passnotes.presentation.filepicker

import android.app.Activity
import androidx.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.view.*
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.domain.PermissionHelper
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.BaseListFragment
import javax.inject.Inject

class FilePickerFragment : BaseListFragment<List<FileDescriptor>>(), FilePickerContract.View {

	@Inject
	lateinit var permissionHelper: PermissionHelper

	override lateinit var presenter: FilePickerContract.Presenter
	private lateinit var adapter: FilePickerAdapter

	private var menu: Menu? = null

	init {
		Injector.getInstance().appComponent.inject(this)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setHasOptionsMenu(true)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		presenter.items.observe(viewLifecycleOwner,
			Observer { files -> setItems(files) })
		presenter.requestPermissionEvent.observe(viewLifecycleOwner,
			Observer { permission -> requestPermission(permission) })
		presenter.doneButtonVisibility.observe(viewLifecycleOwner,
			Observer { isVisible -> setDoneButtonVisibility(isVisible) })
		presenter.fileSelectedEvent.observe(viewLifecycleOwner,
			Observer { file -> selectFileAndFinish(file) })
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		this.menu = menu

		inflater.inflate(R.menu.base_done, menu)
	}

	override fun setDoneButtonVisibility(isVisible: Boolean) {
		if (menu != null) {
			val item = menu!!.findItem(R.id.menu_done) //TODO: wtf with !!
			item.isVisible = isVisible
		}
	}

	override fun selectFileAndFinish(file: FileDescriptor) {
		val data = Intent()

		data.putExtra(FilePickerActivity.EXTRA_RESULT, file)

		activity!!.setResult(Activity.RESULT_OK, data)
		activity!!.finish()
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == R.id.menu_done) {
			presenter.onDoneButtonClicked()
			return true
		} else {
			return super.onOptionsItemSelected(item)
		}
	}

	override fun onStart() {
		super.onStart()
		presenter.start()
	}

	override fun onDestroy() {
		super.onDestroy()
		presenter.destroy()
	}

	override fun onCreateConfig(): Config {
		return Config(layout = Layout.VERTICAL_LIST,
				isFloatingActionButtonEnabled = false,
				isSwipeRefreshEnabled = false,
				isDividerEnabled = true)
	}

	override fun onCreateListAdapter(): FilePickerAdapter {
		adapter = FilePickerAdapter(context!!)
		return adapter
	}

	override fun setItems(items: List<FilePickerAdapter.Item>) {
		adapter.setItems(items)
		adapter.notifyDataSetChanged()

		adapter.onItemClickListener = { position -> presenter.onItemClicked(position)}
	}

	override fun requestPermission(permission: String) {
		permissionHelper.requestPermission(this, permission, REQUEST_CODE_PERMISSION)
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)

		if (requestCode == REQUEST_CODE_PERMISSION) {
			presenter.onPermissionResult(permissionHelper.isAllGranted(grantResults))
		}
	}

	companion object {
		private const val REQUEST_CODE_PERMISSION = 100

		fun newInstance(): FilePickerFragment {
			return FilePickerFragment()
		}
	}
}