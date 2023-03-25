package com.ivanovsky.passnotes.presentation.core

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ivanovsky.passnotes.presentation.main.navigation.NavigationMenuViewModel

abstract class BaseFragment : Fragment() {

    protected val navigationViewModel: NavigationMenuViewModel by lazy {
        ViewModelProvider(requireActivity(), NavigationMenuViewModel.Factory())
            .get(NavigationMenuViewModel::class.java)
    }

    override fun onStart() {
        super.onStart()
        navigationViewModel.setNavigationEnabled(false)
    }

    /**
     * Called when user clicks the back button.
     *
     * @return `true` to prevent back click from being propagated further, or `false` to indicate
     * that back click should be handled by [android.app.Activity]
     */
    open fun onBackPressed(): Boolean {
        return false
    }
}