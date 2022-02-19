package com.ivanovsky.passnotes.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.domain.interactor.main.MainInteractor
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode.AUTOFILL_SELECTION
import com.ivanovsky.passnotes.presentation.Screens.GroupsScreen
import com.ivanovsky.passnotes.presentation.Screens.SearchScreen
import com.ivanovsky.passnotes.presentation.Screens.UnlockScreen
import com.ivanovsky.passnotes.presentation.groups.GroupsScreenArgs
import com.ivanovsky.passnotes.presentation.search.SearchScreenArgs
import com.ivanovsky.passnotes.presentation.unlock.UnlockScreenArgs
import org.koin.core.parameter.parametersOf

class MainViewModel(
    private val interactor: MainInteractor,
    private val router: Router,
    private val args: MainScreenArgs
) : ViewModel() {

    fun navigateToRootScreen() {
        if (args.appMode == AUTOFILL_SELECTION && interactor.isDatabaseOpened()) {
            val chain = arrayOf(
                UnlockScreen(
                    UnlockScreenArgs(
                        appMode = args.appMode,
                        autofillStructure = args.autofillStructure
                    )
                ),
                GroupsScreen(
                    GroupsScreenArgs(
                        appMode = args.appMode,
                        groupUid = null,
                        isCloseDatabaseOnExit = true
                    )
                ),
                SearchScreen(
                    SearchScreenArgs(
                        appMode = args.appMode,
                        autofillStructure = args.autofillStructure
                    )
                )
            )
            router.newRootChain(*chain)
        } else {
            router.newRootScreen(
                UnlockScreen(
                    UnlockScreenArgs(
                        appMode = args.appMode,
                        autofillStructure = args.autofillStructure
                    )
                )
            )
        }
    }

    class Factory(private val args: MainScreenArgs) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return GlobalInjector.get<MainViewModel>(
                parametersOf(args)
            ) as T
        }
    }
}