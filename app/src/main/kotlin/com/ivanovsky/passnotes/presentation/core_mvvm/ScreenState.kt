package com.ivanovsky.passnotes.presentation.core_mvvm

data class ScreenState(
    val screenDisplayingType: ScreenDisplayingType,
    val emptyText: String? = null,
    val errorText: String? = null
) {

    val isDisplayingData: Boolean
        get() = screenDisplayingType == ScreenDisplayingType.DATA

    val isDisplayingLoading: Boolean
        get() = screenDisplayingType == ScreenDisplayingType.LOADING

    val isDisplayingEmptyState: Boolean
        get() = screenDisplayingType == ScreenDisplayingType.EMPTY

    val isNotInitialized: Boolean
        get() = screenDisplayingType == ScreenDisplayingType.NOT_INITIALIZED

    companion object {

        fun data(): ScreenState {
            return ScreenState(ScreenDisplayingType.DATA)
        }

        fun loading(): ScreenState {
            return ScreenState(ScreenDisplayingType.LOADING)
        }

        fun empty(emptyText: String?): ScreenState {
            return ScreenState(
                ScreenDisplayingType.EMPTY,
                emptyText = emptyText
            )
        }

        fun notInitialized(): ScreenState {
            return ScreenState(ScreenDisplayingType.NOT_INITIALIZED)
        }

        fun error(errorText: String?): ScreenState {
            return ScreenState(
                ScreenDisplayingType.ERROR,
                emptyText = null,
                errorText = errorText
            )
        }

        fun dataWithError(errorText: String?): ScreenState {
            return ScreenState(
                ScreenDisplayingType.DATA_WITH_ERROR,
                emptyText = null,
                errorText = errorText
            )
        }
    }

}