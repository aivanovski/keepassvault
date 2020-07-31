package com.ivanovsky.passnotes.presentation.filepicker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.domain.PermissionHelper
import com.ivanovsky.passnotes.injection.DaggerInjector
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.core.BaseListFragment
import com.ivanovsky.passnotes.presentation.core.livedata.SingleLiveEvent

class FilePickerFragment : BaseListFragment<List<FileDescriptor>>(), FilePickerContract.View {

    private val permissionHelper: PermissionHelper by inject()

    override var presenter: FilePickerContract.Presenter? = null
    private lateinit var adapter: FilePickerAdapter

    private val items = MutableLiveData<List<FilePickerAdapter.Item>>()
    private val doneButtonVisibility = MutableLiveData<Boolean>()
    private val requestPermissionEvent = SingleLiveEvent<String>()
    private val selectFileAndFinishEvent = SingleLiveEvent<FileDescriptor>()

    private var menu: Menu? = null

    init {
        DaggerInjector.getInstance().appComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        items.observe(viewLifecycleOwner,
            Observer { items -> setItemsInternal(items) })
        doneButtonVisibility.observe(viewLifecycleOwner,
            Observer { isVisible -> setDoneButtonVisibilityInternal(isVisible) })
        requestPermissionEvent.observe(viewLifecycleOwner,
            Observer { permission -> requestPermissionInternal(permission) })
        selectFileAndFinishEvent.observe(viewLifecycleOwner,
            Observer { file -> selectFileAndFinishInternal(file) })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        this.menu = menu

        inflater.inflate(R.menu.base_done, menu)
    }

    override fun setDoneButtonVisibility(isVisible: Boolean) {
        doneButtonVisibility.value = isVisible
    }

    private fun setDoneButtonVisibilityInternal(isVisible: Boolean) {
        val menu = this.menu ?: return

        val item = menu.findItem(R.id.menu_done) //TODO: wtf with !!
        item.isVisible = isVisible
    }

    override fun selectFileAndFinish(file: FileDescriptor) {
        selectFileAndFinishEvent.call(file)
    }

    private fun selectFileAndFinishInternal(file: FileDescriptor) {
        val activity = requireActivity()

        val data = Intent()

        data.putExtra(FilePickerActivity.EXTRA_RESULT, file)

        activity.setResult(Activity.RESULT_OK, data)
        activity.finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.menu_done) {
            presenter?.onDoneButtonClicked()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        presenter?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter?.destroy()
    }

    override fun onCreateConfig(): Config {
        return Config(
            layout = Layout.VERTICAL_LIST,
            isFloatingActionButtonEnabled = false,
            isSwipeRefreshEnabled = false,
            isDividerEnabled = true
        )
    }

    override fun onCreateListAdapter(): FilePickerAdapter {
        adapter = FilePickerAdapter(context!!)
        return adapter
    }

    override fun setItems(items: List<FilePickerAdapter.Item>) {
        this.items.value = items
    }

    private fun setItemsInternal(items: List<FilePickerAdapter.Item>) {
        adapter.setItems(items)
        adapter.notifyDataSetChanged()
        adapter.onItemClickListener = { position -> presenter?.onItemClicked(position) }
    }

    override fun requestPermission(permission: String) {
        requestPermissionEvent.call(permission)
    }

    private fun requestPermissionInternal(permission: String) {
        permissionHelper.requestPermission(this, permission, REQUEST_CODE_PERMISSION)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSION) {
            presenter?.onPermissionResult(permissionHelper.isAllGranted(grantResults))
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSION = 100

        fun newInstance(): FilePickerFragment {
            return FilePickerFragment()
        }
    }
}