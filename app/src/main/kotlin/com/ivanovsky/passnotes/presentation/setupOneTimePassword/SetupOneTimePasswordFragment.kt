package com.ivanovsky.passnotes.presentation.setupOneTimePassword

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.CoreComposeFragmentBinding
import com.ivanovsky.passnotes.presentation.core.DatabaseInteractionWatcher
import com.ivanovsky.passnotes.presentation.core.FragmentWithDoneButton
import com.ivanovsky.passnotes.presentation.core.compose.AppTheme
import com.ivanovsky.passnotes.presentation.core.compose.getComposeTheme
import com.ivanovsky.passnotes.presentation.core.extensions.getMandatoryArgument
import com.ivanovsky.passnotes.presentation.core.extensions.setupActionBar
import com.ivanovsky.passnotes.presentation.core.extensions.withArguments

class SetupOneTimePasswordFragment : FragmentWithDoneButton() {

    val viewModel: SetupOneTimePasswordViewModel by lazy {
        ViewModelProvider(
            this,
            SetupOneTimePasswordViewModel.Factory(
                args = getMandatoryArgument(ARGUMENTS)
            )
        )[SetupOneTimePasswordViewModel::class.java]
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
                    SetupOneTimePasswordScreen(viewModel)
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

            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycle.addObserver(DatabaseInteractionWatcher(this))

        setupActionBar {
            title = getString(R.string.add_one_time_password)
            setHomeAsUpIndicator(null)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onDoneMenuClicked() {
        viewModel.onDoneClicked()
    }

    companion object {
        val TAG: String = SetupOneTimePasswordFragment::class.java.simpleName

        private const val ARGUMENTS = "arguments"

        fun newInstance(args: SetupOneTimePasswordArgs): SetupOneTimePasswordFragment =
            SetupOneTimePasswordFragment()
                .withArguments {
                    putParcelable(ARGUMENTS, args)
                }
    }
}