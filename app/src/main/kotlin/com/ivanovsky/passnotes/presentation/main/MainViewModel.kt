package com.ivanovsky.passnotes.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ivanovsky.passnotes.domain.interactor.main.MainInteractor
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode.AUTOFILL_SELECTION
import com.ivanovsky.passnotes.presentation.NewScreens
import com.ivanovsky.passnotes.presentation.core.navigation.Router
import com.ivanovsky.passnotes.presentation.groups.GroupsScreenArgs
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
                NewScreens.UnlockScreen(
                    UnlockScreenArgs(
                        appMode = args.appMode,
                        autofillStructure = args.autofillStructure
                    )
                ),
                NewScreens.GroupsScreen(
                    GroupsScreenArgs(
                        appMode = args.appMode,
                        groupUid = null,
                        isCloseDatabaseOnExit = false,
                        isSearchModeEnabled = true,
                        autofillStructure = args.autofillStructure
                    )
                )
            )
            router.setRoot(*chain)
        } else {
            router.setRoot(
                NewScreens.UnlockScreen(
                    UnlockScreenArgs(
                        appMode = args.appMode,
                        autofillStructure = args.autofillStructure,
                        note = args.note
                    )
                )
            )
        }
    }

    class Factory(private val args: MainScreenArgs) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GlobalInjector.get<MainViewModel>(
                parametersOf(args)
            ) as T
        }
    }
}