package com.ivanovsky.passnotes.presentation.core.dialog.propertyAction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.data.entity.Property
import com.ivanovsky.passnotes.data.entity.PropertyType
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.injection.GlobalInjector
import com.ivanovsky.passnotes.presentation.core.dialog.propertyAction.PropertyAction.CopyText
import com.ivanovsky.passnotes.presentation.core.dialog.propertyAction.PropertyAction.OpenUrl
import com.ivanovsky.passnotes.presentation.core.event.SingleLiveEvent
import com.ivanovsky.passnotes.util.StringUtils
import com.ivanovsky.passnotes.util.UrlUtils
import com.ivanovsky.passnotes.util.substituteAll
import org.koin.core.parameter.parametersOf

class PropertyActionDialogViewModel(
    private val resourceProvider: ResourceProvider,
    args: PropertyActionDialogArgs
) : ViewModel() {

    val actions = buildOptions(args.property)
    val openUrlEvent = SingleLiveEvent<String>()

    fun processAction(action: PropertyAction): Boolean {
        return when (action) {
            is OpenUrl -> {
                openUrlEvent.call(action.url)
                true
            }

            else -> false
        }
    }

    private fun buildOptions(property: Property): List<PropertyAction> {
        val options = mutableListOf<PropertyAction>()

        if (!property.name.isNullOrBlank()) {
            options.add(
                CopyText(
                    title = resourceProvider.getString(
                        R.string.copy_with_str,
                        "'${property.name}'"
                    ),
                    text = property.name,
                    isProtected = false
                )
            )
        }

        if (!property.value.isNullOrBlank()) {
            val name = if (property.isProtected) {
                property.value.substituteAll(StringUtils.STAR)
            } else {
                property.value
            }

            options.add(
                CopyText(
                    title = resourceProvider.getString(R.string.copy_with_str, "'$name'"),
                    text = property.value,
                    isProtected = property.isProtected
                )
            )

            if (property.type == PropertyType.URL) {
                val url = UrlUtils.parseUrl(property.value)
                if (url?.isValid() == true) {
                    options.add(
                        OpenUrl(
                            title = resourceProvider.getString(
                                R.string.open_with_str,
                                property.value
                            ),
                            url = url.formatToString()
                        )
                    )
                }
            }
        }

        return options
    }

    class Factory(
        private val args: PropertyActionDialogArgs
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GlobalInjector.get<PropertyActionDialogViewModel>(
                parametersOf(args)
            ) as T
        }
    }
}