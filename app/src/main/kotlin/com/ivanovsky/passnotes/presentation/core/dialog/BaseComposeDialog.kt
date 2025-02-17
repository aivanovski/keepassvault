package com.ivanovsky.passnotes.presentation.core.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import com.ivanovsky.passnotes.databinding.CoreComposeDialogBinding
import com.ivanovsky.passnotes.presentation.core.compose.AppTheme
import com.ivanovsky.passnotes.presentation.core.compose.getComposeTheme

abstract class BaseComposeDialog<T : ViewModel> : DialogFragment() {

    abstract val viewModel: T

    @Composable
    abstract fun RenderDialog()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = CoreComposeDialogBinding.inflate(inflater, container, false)

        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val theme = requireContext().getComposeTheme()
                AppTheme(theme = theme) {
                    RenderDialog()
                }
            }
        }

        return binding.root
    }
}