package com.ivanovsky.passnotes.presentation.main.navigation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.terrakok.cicerone.Router
import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.domain.entity.DatabaseStatus
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.ApplicationLaunchMode
import com.ivanovsky.passnotes.presentation.Screens.AboutScreen
import com.ivanovsky.passnotes.presentation.Screens.DebugMenuScreen
import com.ivanovsky.passnotes.presentation.Screens.MainSettingsScreen
import com.ivanovsky.passnotes.presentation.Screens.UnlockScreen
import com.ivanovsky.passnotes.presentation.core.BaseCellViewModel
import com.ivanovsky.passnotes.presentation.core.ViewModelTypes
import com.ivanovsky.passnotes.presentation.core.event.EventProvider
import com.ivanovsky.passnotes.presentation.core.event.EventProviderImpl
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.presentation.core.viewmodel.SingleTextWithIconCellViewModel
import com.ivanovsky.passnotes.presentation.main.navigation.cells.factory.NavigationMenuCellModelFactory
import com.ivanovsky.passnotes.presentation.main.navigation.cells.factory.NavigationMenuCellModelFactory.CellId
import com.ivanovsky.passnotes.presentation.main.navigation.cells.factory.NavigationMenuCellViewModelFactory
import com.ivanovsky.passnotes.presentation.main.navigation.cells.viewmodel.NavigationHeaderCellViewModel
import com.ivanovsky.passnotes.presentation.unlock.UnlockScreenArgs

class NavigationMenuViewModel(
    private val observerBus: ObserverBus,
    private val modelFactory: NavigationMenuCellModelFactory,
    private val viewModelFactory: NavigationMenuCellViewModelFactory,
    private val router: Router
) : ViewModel(),
    ObserverBus.DatabaseOpenObserver,
    ObserverBus.DatabaseCloseObserver {

    val cellViewTypes = ViewModelTypes()
        .add(NavigationHeaderCellViewModel::class, R.layout.cell_navigation_header)
        .add(SingleTextWithIconCellViewModel::class, R.layout.cell_navigation_item)

    val isNavigationMenuEnabled = MutableLiveData(false)
    val cellViewModels = MutableLiveData<List<BaseCellViewModel>>()
    val hideKeyboardEvent = SingleLiveEvent<Unit>()
    val hideMenuEvent = SingleLiveEvent<Unit>()

    private var isDbOpened = false
    private val eventProvider: EventProvider

    init {
        eventProvider = EventProviderImpl()
        subscribeToEvents()
        cellViewModels.value = buildCellViewModels()
    }

    override fun onDatabaseOpened(fsOptions: FSOptions, status: DatabaseStatus) {
        isDbOpened = true
        cellViewModels.value = buildCellViewModels()
    }

    override fun onDatabaseClosed() {
        isDbOpened = false
        cellViewModels.value = buildCellViewModels()
    }

    override fun onCleared() {
        super.onCleared()
        unsubscribeFromEvents()
    }

    fun setNavigationEnabled(isEnabled: Boolean) {
        isNavigationMenuEnabled.value = isEnabled
    }

    fun onMenuDragging() {
        hideKeyboardEvent.call(Unit)
    }

    private fun subscribeToEvents() {
        observerBus.register(this)
        eventProvider.subscribe(this) { event ->
            event.getInt(SingleTextWithIconCellViewModel.CLICK_EVENT)?.let { id ->
                onItemClicked(id)
            }
        }
    }

    private fun unsubscribeFromEvents() {
        observerBus.unregister(this)
        eventProvider.unSubscribe(this)
    }

    private fun onItemClicked(cellId: Int) {
        when (cellId) {
            CellId.SELECT_FILE -> {
                router.backTo(UnlockScreen(UnlockScreenArgs(ApplicationLaunchMode.NORMAL)))
            }
            CellId.LOCK -> {
                router.backTo(UnlockScreen(UnlockScreenArgs(ApplicationLaunchMode.NORMAL)))
            }
            CellId.SETTINGS -> {
                router.navigateTo(MainSettingsScreen())
            }
            CellId.ABOUT -> {
                router.navigateTo(AboutScreen())
            }
            CellId.DEBUG_MENU -> {
                router.navigateTo(DebugMenuScreen())
            }
        }
        hideMenuEvent.call()
    }

    private fun buildCellViewModels(): List<BaseCellViewModel> {
        val header = modelFactory.creteHeaderModel()
        val items = modelFactory.createMenuItemModels(
            isDatabaseOpened = isDbOpened,
            isDebugMenuVisible = BuildConfig.DEBUG
        )

        return viewModelFactory.createCellViewModels(header + items, eventProvider)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GlobalInjector.get<NavigationMenuViewModel>() as T
        }
    }
}