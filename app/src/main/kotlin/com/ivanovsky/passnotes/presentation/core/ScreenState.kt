package com.ivanovsky.passnotes.presentation.core

import androidx.compose.runtime.Immutable
import com.ivanovsky.passnotes.data.entity.OperationError

@Immutable
data class ScreenState private constructor(
    val type: ScreenStateType,
    val emptyText: String? = null,
    val error: OperationError? = null,
    val errorButtonText: String? = null
) {

    val isDisplayingData: Boolean
        get() = (type == ScreenStateType.DATA || type == ScreenStateType.DATA_WITH_ERROR)

    val isDisplayingErrorPanel: Boolean
        get() = (type == ScreenStateType.DATA_WITH_ERROR)

    val isDisplayingLoading: Boolean
        get() = (type == ScreenStateType.LOADING)

    val isDisplayingEmptyState: Boolean
        get() = (type == ScreenStateType.EMPTY)

    val isNotInitialized: Boolean
        get() = (type == ScreenStateType.NOT_INITIALIZED)

    val isDisplayingError: Boolean
        get() = (type == ScreenStateType.ERROR)

    companion object {

        fun data(): ScreenState =
            ScreenState(ScreenStateType.DATA)

        fun loading(): ScreenState =
            ScreenState(ScreenStateType.LOADING)

        fun empty(emptyText: String?): ScreenState =
            ScreenState(
                ScreenStateType.EMPTY,
                emptyText = emptyText
            )

        fun notInitialized(): ScreenState =
            ScreenState(ScreenStateType.NOT_INITIALIZED)

        fun error(error: OperationError): ScreenState =
            ScreenState(
                type = ScreenStateType.ERROR,
                emptyText = null,
                error = error
            )

        fun dataWithError(error: OperationError): ScreenState =
            ScreenState(
                type = ScreenStateType.DATA_WITH_ERROR,
                emptyText = null,
                error = error
            )

        fun dataWithError(error: OperationError, errorButtonText: String? = null): ScreenState =
            ScreenState(
                type = ScreenStateType.DATA_WITH_ERROR,
                emptyText = null,
                error = error,
                errorButtonText = errorButtonText
            )
    }
}