package com.ivanovsky.passnotes.presentation.autofill

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.graphics.BlendMode
import android.graphics.drawable.Icon
import android.service.autofill.InlinePresentation
import android.widget.RemoteViews
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import androidx.autofill.inline.v1.InlineSuggestionUi
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceProvider

class AutofillViewFactory(
    private val context: Context,
    private val resourceProvider: ResourceProvider
) {

    private val packageName = context.packageName

    @RequiresApi(api = 30)
    @SuppressLint("RestrictedApi")
    fun createUnlockInlineView(
        action: PendingIntent,
        spec: InlinePresentationSpec
    ): InlinePresentation {
        val slice = InlineSuggestionUi.newContentBuilder(action)
            .apply {
                setTitle(
                    resourceProvider.getString(
                        R.string.sign_in_with_app,
                        resourceProvider.getString(R.string.app_name)
                    )
                )
                setStartIcon(
                    Icon.createWithResource(context, R.mipmap.ic_launcher_round).apply {
                        setTintBlendMode(BlendMode.DST)
                    }
                )
            }
            .build()

        return InlinePresentation(
            slice.slice,
            spec,
            false
        )
    }

    fun createUnlockView(): RemoteViews {
        return RemoteViews(packageName, R.layout.autofill_login_item)
            .apply {
                setTextViewText(
                    R.id.title,
                    resourceProvider.getString(
                        R.string.sign_in_with_app,
                        resourceProvider.getString(R.string.app_name)
                    )
                )
            }
    }

    @RequiresApi(api = 30)
    @SuppressLint("RestrictedApi")
    fun createManualSelectionInlineVies(
        action: PendingIntent,
        spec: InlinePresentationSpec
    ): InlinePresentation {
        val slice = InlineSuggestionUi.newContentBuilder(action)
            .apply {
                setTitle(
                    resourceProvider.getString(R.string.select_entry_with_dots)
                )
                setStartIcon(
                    Icon.createWithResource(context, R.mipmap.ic_launcher_round).apply {
                        setTintBlendMode(BlendMode.DST)
                    }
                )
            }
            .build()

        return InlinePresentation(
            slice.slice,
            spec,
            false
        )
    }

    fun createManualSelectionView(): RemoteViews {
        return RemoteViews(packageName, R.layout.autofill_select_item)
    }

    fun createEntryView(title: String, description: String): RemoteViews {
        return RemoteViews(packageName, R.layout.autofill_entry_item)
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

    @RequiresApi(api = 30)
    @SuppressLint("RestrictedApi")
    fun createEntryInlineView(
        action: PendingIntent,
        title: String,
        description: String,
        spec: InlinePresentationSpec
    ): InlinePresentation {
        val slice = InlineSuggestionUi.newContentBuilder(action)
            .apply {
                setTitle(title)
                setContentDescription(description)
                setStartIcon(
                    Icon.createWithResource(context, R.mipmap.ic_launcher_round).apply {
                        setTintBlendMode(BlendMode.DST)
                    }
                )

            }
            .build()

        return InlinePresentation(
            slice.slice,
            spec,
            false
        )
    }
}