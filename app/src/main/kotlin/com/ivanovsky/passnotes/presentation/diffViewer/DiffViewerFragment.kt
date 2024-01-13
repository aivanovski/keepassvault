package com.ivanovsky.passnotes.presentation.diffViewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.DiffViewerFragmentBinding
import com.ivanovsky.passnotes.presentation.core.DatabaseInteractionWatcher
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments

class DiffViewerFragment : Fragment() {

    private val args: DiffViewerScreenArgs by lazy {
        getMandatoryArgument(ARGUMENTS)
    }

    private val viewModel: DiffViewerViewModel by lazy {
        ViewModelProvider(
            this,
            DiffViewerViewModel.Factory(
                args = args
            )
        )[DiffViewerViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DiffViewerFragmentBinding.inflate(inflater, container, false)
            .also {
                it.lifecycleOwner = viewLifecycleOwner
                it.viewModel = viewModel
            }

        binding.recyclerView.itemAnimator = null

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (args.isHoldDatabaseInteraction) {
            viewLifecycleOwner.lifecycle.addObserver(DatabaseInteractionWatcher(this))
        }

        setupActionBar {
            title = getString(R.string.compare_files)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(null)
        }

        viewModel.start()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val action = MENU_ACTIONS[item.itemId] ?: throw IllegalArgumentException()
        action.invoke(viewModel)
        return true
    }

    companion object {

        private const val ARGUMENTS = "arguments"

        private val MENU_ACTIONS = mapOf<Int, (vm: DiffViewerViewModel) -> Unit>(
            android.R.id.home to { vm -> vm.navigateBack() },
        )

        fun newInstance(
            args: DiffViewerScreenArgs
        ): DiffViewerFragment = DiffViewerFragment()
            .withArguments {
                putParcelable(ARGUMENTS, args)
            }
    }
}