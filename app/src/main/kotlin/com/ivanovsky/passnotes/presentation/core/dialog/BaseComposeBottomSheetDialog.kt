package com.ivanovsky.passnotes.presentation.core.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.databinding.ComposeViewBinding
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.presentation.core.ThemeProvider
import com.ivanovsky.passnotes.presentation.core.compose.AppTheme
import com.ivanovsky.passnotes.presentation.core.compose.getComposeTheme

abstract class BaseComposeBottomSheetDialog<T : ViewModel> : BottomSheetDialogFragment() {

    abstract val viewModel: T

    @Composable
    abstract fun RenderDialog()

    // private var onClickListener: ((index: Int) -> Unit)? = null

    override fun getTheme(): Int = R.style.BottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // if (savedInstanceState != null) {
        //     dismiss()
        // }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = ComposeViewBinding.inflate(inflater, container, false)

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

    // private fun handleEvent(event: BottomSheetUiEvent) {
    //     when (event) {
    //         is BottomSheetUiEvent.OnClick -> {
    //             onClickListener?.invoke(event.index)
    //             dismiss()
    //         }
    //     }
    // }

    companion object {

        // val TAG: String = BottomSheetMenuFragment::class.java.simpleName

        private const val MENU = "menu"

        // fun newInstance(
        //     menu: BottomSheetMenu,
        //     onClick: (index: Int) -> Unit
        // ): BottomSheetMenuFragment {
        //     val bundle = Bundle()
        //         .apply {
        //             putParcelable(MENU, menu)
        //         }
        //
        //     return BottomSheetMenuFragment()
        //         .apply {
        //             arguments = bundle
        //             onClickListener = onClick
        //         }
        // }
    }
}