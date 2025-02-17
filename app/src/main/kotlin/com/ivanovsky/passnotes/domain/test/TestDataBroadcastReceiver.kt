package com.ivanovsky.passnotes.domain.test

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.ivanovsky.passnotes.R
import com.ivanovsky.passnotes.domain.ResourceProvider
import com.ivanovsky.passnotes.extensions.formatReadableMessage
import com.ivanovsky.passnotes.extensions.getOrThrow
import com.ivanovsky.passnotes.injection.GlobalInjector.get
import timber.log.Timber

class TestDataBroadcastReceiver : BroadcastReceiver() {

    private val parser = TestDataParser()
    private val resourceProvider: ResourceProvider by lazy { get() }
    private val interactor: TestDataInteractor by lazy { get() }

    override fun onReceive(context: Context, intent: Intent) {
        val extras = intent.extras ?: Bundle.EMPTY

        val parseCommandResult = parser.parse(extras)
        if (parseCommandResult.isFailed) {
            val message = parseCommandResult.error.formatReadableMessage(resourceProvider)
            Timber.e(message)
            showToast(context, message)
            return
        }

        val command = parseCommandResult.getOrThrow()
        val processCommandResult = interactor.process(command)
        if (processCommandResult.isFailed) {
            val message = processCommandResult.error.formatReadableMessage(resourceProvider)
            Timber.e(message)
            showToast(context, message)
            return
        }

        showToast(context, context.getString(R.string.test_data_receiver_success_message))
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG)
            .show()
    }
}