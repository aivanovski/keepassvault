package com.ivanovsky.passnotes.presentation.core

enum class ScreenDisplayingMode {
    NOT_INITIALIZED,
    LOADING,
    EMPTY,
    DISPLAYING_DATA,
    DISPLAYING_DATA_WITH_ERROR_PANEL,
    DISPLAYING_DATA_WITH_RETRY_BUTTON,
    ERROR
}