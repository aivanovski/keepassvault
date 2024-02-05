package com.ivanovsky.passnotes.presentation.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.CoreComposeFragmentBinding
import com.ivanovsky.passnotes.presentation.about.AboutScreen
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.compose.AppTheme
import com.ivanovsky.passnotes.presentation.core.compose.getComposeTheme
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments

class HistoryFragment : BaseFragment() {

    private val viewModel: HistoryViewModel by lazy {
        ViewModelProvider(
            this,
            HistoryViewModel.Factory(
                args = getMandatoryArgument(ARGUMENTS)
            )
        )
            .get(HistoryViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = CoreComposeFragmentBinding.inflate(inflater, container, false)

        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val theme by viewModel.theme.collectAsState(initial = context.getComposeTheme())
                val state by viewModel.screenState.observeAsState(initial = ScreenState.loading())

                AppTheme(theme = theme) {

                }
            }
        }

        return binding.root
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

        setupActionBar {
            title = getString(R.string.history)
            setHomeAsUpIndicator(null)
            setDisplayHomeAsUpEnabled(true)
        }

        viewModel.start()
    }

    companion object {

        private const val ARGUMENTS = "argumetns"

        fun newInstance(args: HistoryScreenArgs): HistoryFragment {
            return HistoryFragment()
                .withArguments {
                    putParcelable(ARGUMENTS, args)
                }
        }
    }
}