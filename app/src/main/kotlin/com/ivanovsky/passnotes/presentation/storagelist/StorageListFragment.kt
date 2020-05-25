package com.ivanovsky.passnotes.presentation.storagelist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.repository.file.FSType
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.domain.entity.StorageOption
import com.ivanovsky.passnotes.injection.Injector
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.adapter.SingleLineAdapter
import com.ivanovsky.passnotes.presentation.filepicker.FilePickerActivity
import javax.inject.Inject

class StorageListFragment : BaseFragment(), StorageListContract.View {

    @Inject
    lateinit var fileSystemResolver: FileSystemResolver

    override var presenter: StorageListContract.Presenter? = null
    private lateinit var adapter: SingleLineAdapter

    init {
        Injector.getInstance().appComponent.inject(this)
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
        val view = inflater.inflate(R.layout.storage_fragment, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)

        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        val dividerDecorator = DividerItemDecoration(context, layoutManager.orientation)

        adapter = SingleLineAdapter(context!!)

        recyclerView.layoutManager = layoutManager
        recyclerView.addItemDecoration(dividerDecorator)
        recyclerView.adapter = adapter

        presenter?.storageOptions?.observe(this,
            Observer { options -> setStorageOptions(options!!) })
        presenter?.showFilePickerScreenEvent?.observe(this,
            Observer { args ->
                showFilePickerScreen(
                    args!!.root,
                    args.action,
                    args.isBrowsingEnabled
                )
            })
        presenter?.fileSelectedEvent?.observe(this,
            Observer { file -> selectFileAndFinish(file!!) })
        presenter?.authActivityStartedEvent?.observe(this,
            Observer { fsType -> showAuthActivity(fsType!!) })

        return view
    }

    override fun setStorageOptions(options: List<StorageOption>) {
        adapter.setItems(createAdapterItems(options))
        adapter.notifyDataSetChanged()

        adapter.onItemClickListener = { pos -> presenter?.onStorageOptionClicked(options[pos]) }
    }

    override fun showFilePickerScreen(
        root: FileDescriptor,
        action: Action,
        isBrowsingEnabled: Boolean
    ) {
        val intent = FilePickerActivity.createStartIntent(
            context!!,
            convertActionToFilePickerMode(action),
            root,
            isBrowsingEnabled
        )

        startActivityForResult(intent, REQUEST_CODE_PICK_FILE)
    }

    private fun convertActionToFilePickerMode(action: Action): com.ivanovsky.passnotes.presentation.filepicker.Mode {
        return when (action) {
            Action.PICK_FILE -> com.ivanovsky.passnotes.presentation.filepicker.Mode.PICK_FILE
            Action.PICK_STORAGE -> com.ivanovsky.passnotes.presentation.filepicker.Mode.PICK_DIRECTORY
        }
    }

    private fun createAdapterItems(options: List<StorageOption>): List<SingleLineAdapter.Item> {
        return options.map { option -> SingleLineAdapter.Item(option.title) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PICK_FILE) {
            val extras = data?.extras ?: return

            val file =
                extras.getParcelable<FileDescriptor>(FilePickerActivity.EXTRA_RESULT) ?: return

            presenter?.onFilePicked(file)
        }
    }

    override fun selectFileAndFinish(file: FileDescriptor) {
        val data = Intent()

        data.putExtra(StorageListActivity.EXTRA_RESULT, file)

        activity!!.setResult(Activity.RESULT_OK, data)
        activity!!.finish()
    }

    override fun showAuthActivity(fsType: FSType) {
        val authenticator = fileSystemResolver.resolveProvider(fsType).authenticator
        authenticator?.startAuthActivity(context)
    }

    companion object {

        private const val REQUEST_CODE_PICK_FILE = 100

        fun newInstance(): StorageListFragment {
            return StorageListFragment()
        }
    }
}