package com.ivanovsky.passnotes.presentation.storagelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.observe
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.repository.file.AuthType
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.databinding.StorageListFragmentBinding
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.extensions.requireArgument
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments
import org.koin.androidx.viewmodel.ext.android.viewModel

class StorageListFragment : BaseFragment() {

    private val viewModel: StorageListViewModel by viewModel()
    private val fileSystemResolver: FileSystemResolver by inject()
    private var requestedFSAuthority: FSAuthority? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupActionBar {
            title = getString(R.string.select_storage)
            setHomeAsUpIndicator(null)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onScreenStart()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return StorageListFragmentBinding.inflate(inflater)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }
            .root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                viewModel.navigateBack()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeToEvents()

        val requiredAction = arguments?.getSerializable(ARG_REQUIRED_ACTION) as? Action
            ?: requireArgument(ARG_REQUIRED_ACTION)

        viewModel.loadData(requiredAction)
    }

    private fun subscribeToEvents() {
        viewModel.showAuthActivityEvent.observe(viewLifecycleOwner) { fsAuthority ->
            showAuthActivity(fsAuthority)
        }
    }

    private fun showAuthActivity(fsAuthority: FSAuthority) {
        requestedFSAuthority = fsAuthority

        val authenticator = fileSystemResolver.resolveProvider(fsAuthority).authenticator ?: return
        if (authenticator.getAuthType() == AuthType.EXTERNAL) {
            authenticator.startAuthActivity(requireContext())
        }
    }

    companion object {

        private const val ARG_REQUIRED_ACTION = "requiredAction"

        fun newInstance(requiredAction: Action) = StorageListFragment().withArguments {
            putSerializable(ARG_REQUIRED_ACTION, requiredAction)
        }
    }
}