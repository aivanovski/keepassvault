package com.ivanovsky.passnotes.presentation.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.CoreComposeFragmentBinding
import com.ivanovsky.passnotes.presentation.core.BaseFragment
import com.ivanovsky.passnotes.presentation.core.compose.AppTheme
import com.ivanovsky.passnotes.presentation.core.compose.getComposeTheme
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import org.koin.androidx.viewmodel.ext.android.viewModel

class AboutFragment : BaseFragment() {

    private val viewModel: AboutViewModel by viewModel()

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                viewModel.onBackClicked()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
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

                AppTheme(theme = theme) {
                    AboutScreen(
                        version = viewModel.appVersion,
                        buildType = viewModel.appBuildType
                    )
                }
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupActionBar {
            title = getString(R.string.about)
            setHomeAsUpIndicator(null)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    companion object {

        fun newInstance() = AboutFragment()
    }
}