package com.ivanovsky.passnotes.presentation.core

import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceFragmentCompat
import com.ivanovsky.passnotes.presentation.main.navigation.NavigationMenuViewModel

abstract class BasePreferenceFragment : PreferenceFragmentCompat() {

    protected val navigationViewModel: NavigationMenuViewModel by lazy {
        ViewModelProvider(requireActivity(), NavigationMenuViewModel.Factory())
            .get(NavigationMenuViewModel::class.java)
    }

    override fun onStart() {
        super.onStart()
        navigationViewModel.setNavigationEnabled(false)
    }
}