package com.ivanovsky.passnotes.presentation.filepicker

import android.app.Activity
import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.domain.PermissionHelper
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
				Observer { files -> setItems(files!!) })
		presenter.screenState.observe(this,
				Observer { state -> setScreenState(state) })
		presenter.requestPermissionAction.observe(this,
				Observer { permission -> requestPermission(permission!!) })
		presenter.doneButtonVisibility.observe(this,
				Observer { isVisible -> setDoneButtonVisibility(isVisible!!) })
		presenter.fileSelectedAction.observe(this,
				Observer { file -> selectFileAndFinish(file!!) })
		presenter.snackbarMessageAction.observe(this,
				Observer { message -> showSnackbar(message!!) })

		setHasOptionsMenu(true)
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

		activity.setResult(Activity.RESULT_OK, data)
		activity.finish()
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == R.id.menu_done) {
			presenter.onDoneButtonClicked()
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
}