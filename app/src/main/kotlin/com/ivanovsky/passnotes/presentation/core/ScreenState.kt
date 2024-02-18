package com.ivanovsky.passnotes.presentation.core

data class ScreenState private constructor(
    val type: ScreenStateType,
    val emptyText: String? = null,
    val errorText: String? = null,
    val errorButtonText: String? = null
) {

    val isDisplayingData: Boolean
        get() = type == ScreenStateType.DATA ||
            type == ScreenStateType.DATA_WITH_ERROR

    val isDisplayingLoading: Boolean
        get() = type == ScreenStateType.LOADING

    val isDisplayingEmptyState: Boolean
        get() = type == ScreenStateType.EMPTY

    val isNotInitialized: Boolean
        get() = type == ScreenStateType.NOT_INITIALIZED

    val isDisplayingError: Boolean
        get() = type == ScreenStateType.ERROR

    companion object {

        fun data(): ScreenState {
            return ScreenState(ScreenStateType.DATA)
        }

        fun loading(): ScreenState {
            return ScreenState(ScreenStateType.LOADING)
        }

        fun empty(emptyText: String?): ScreenState {
            return ScreenState(
                ScreenStateType.EMPTY,
                emptyText = emptyText
            )
        }

        fun notInitialized(): ScreenState {
            return ScreenState(ScreenStateType.NOT_INITIALIZED)
        }

        fun error(errorText: String?): ScreenState {
            return ScreenState(
                ScreenStateType.ERROR,
                emptyText = null,
                errorText = errorText
            )
        }

        fun dataWithError(errorText: String?, errorButtonText: String? = null): ScreenState {
            return ScreenState(
                ScreenStateType.DATA_WITH_ERROR,
                emptyText = null,
                errorText = errorText,
                errorButtonText = errorButtonText
            )
        }
    }
}