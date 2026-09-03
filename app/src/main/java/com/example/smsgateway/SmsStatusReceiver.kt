package com.example.smsgateway

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SmsStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_STATUS) return
        val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID) ?: return
        val kind = intent.getStringExtra(EXTRA_KIND) ?: KIND_SENT
        val repository = GatewayRepository(context)
        val success = resultCode == Activity.RESULT_OK
        val aggregated = repository.updateSentPartResult(
            messageId = messageId,
            kind = kind,
            success = success,
            errorCode = if (success) null else resultCode
        ) ?: return
        val event = aggregated.emitEvent ?: return
        if (!aggregated.statusChanged) return
        WebhookDispatcher.enqueue(
            context = context,
            event = event,
            data = aggregated.record.toJson()
        )
    }

    companion object {
        const val ACTION_STATUS = "com.example.smsgateway.action.SMS_STATUS"
        const val EXTRA_MESSAGE_ID = "message_id"
        const val EXTRA_KIND = "kind"
        const val EXTRA_PART_INDEX = "part_index"
        const val KIND_SENT = "sent"
        const val KIND_DELIVERY = "delivery"
    }
}
