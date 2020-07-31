package com.ivanovsky.passnotes.presentation.core

data class ScreenState private constructor(
        val displayingMode: ScreenDisplayingMode,
        val message: String? = null
) {

    val isDisplayingData: Boolean
        get() = displayingMode == ScreenDisplayingMode.DISPLAYING_DATA

    val isNotInitialized: Boolean
        get() = displayingMode == ScreenDisplayingMode.NOT_INITIALIZED

    companion object {
        fun loading(): ScreenState {
            return ScreenState(ScreenDisplayingMode.LOADING)
        }

        fun empty(message: String): ScreenState {
            return ScreenState(ScreenDisplayingMode.EMPTY, message)
        }

        fun error(message: String): ScreenState {
            return ScreenState(ScreenDisplayingMode.ERROR, message)
        }

        fun data(): ScreenState {
            return ScreenState(ScreenDisplayingMode.DISPLAYING_DATA)
        }

        fun dataWithError(message: String): ScreenState {
            return ScreenState(ScreenDisplayingMode.DISPLAYING_DATA_WITH_ERROR_PANEL, message)
        }

        fun notInitialized(): ScreenState {
            return ScreenState(ScreenDisplayingMode.NOT_INITIALIZED)
        }
    }
}