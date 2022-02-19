package com.ivanovsky.passnotes.presentation.core

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ivanovsky.passnotes.presentation.main.navigation.NavigationMenuViewModel

abstract class BaseFragment : Fragment() {

    protected val navigationViewModel: NavigationMenuViewModel by lazy {
        ViewModelProvider(requireActivity(), NavigationMenuViewModel.FACTORY)
            .get(NavigationMenuViewModel::class.java)
    }

    override fun onStart() {
        super.onStart()
        navigationViewModel.setNavigationEnabled(false)
    }
}