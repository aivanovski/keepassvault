package com.ivanovsky.passnotes.presentation.history

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.core.BaseScreenViewModel
import com.ivanovsky.passnotes.presentation.core.ScreenState
import com.ivanovsky.passnotes.presentation.core.ThemeProvider
import com.ivanovsky.passnotes.presentation.core.compose.themeFlow
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

class HistoryViewModel(
    private val interactor: HistoryInteractor,
    private val errorInteractor: ErrorInteractor,
    private val resourceProvider: ResourceProvider,
    private val themeProvider: ThemeProvider,
    private val router: Router,
    private val args: HistoryScreenArgs
) : BaseScreenViewModel() {

    val screenState = MutableLiveData<ScreenState>()
    val theme = themeFlow(themeProvider)

    fun start() {
        viewModelScope.launch {
            val getHistoryResult = interactor.getHistoryDiff(args.noteUid)
            if (getHistoryResult.isFailed) {
            }
        }
    }

    fun navigateBack() {
        router.exit()
    }

    class Factory(private val args: HistoryScreenArgs) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GlobalInjector.get<HistoryViewModel>(
                parametersOf(args)
            ) as T
        }
    }
}