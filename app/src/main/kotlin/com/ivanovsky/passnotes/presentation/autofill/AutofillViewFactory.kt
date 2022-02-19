package com.ivanovsky.passnotes.presentation.autofill

import android.content.Context
import android.widget.RemoteViews
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceProvider

class AutofillViewFactory(
    context: Context,
    private val resourceProvider: ResourceProvider
) {

    private val packageName = context.packageName

    fun createUnlockView(): RemoteViews =
        RemoteViews(packageName, R.layout.autofill_login_item)
            .apply {
                setTextViewText(
                    R.id.title,
                    resourceProvider.getString(
                        R.string.sign_in_with_app,
                        resourceProvider.getString(R.string.app_name)
                    )
                )
            }

    fun createSelectionView(): RemoteViews =
        RemoteViews(packageName, R.layout.autofill_select_item)

    fun createEntryView(title: String, description: String): RemoteViews =
        RemoteViews(packageName, R.layout.autofill_entry_item)
            .apply {
                setTextViewText(
                    R.id.title,
                    title
                )
                setTextViewText(
                    R.id.description,
                    description
                )
            }
}