package com.example.smsgateway

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import java.util.concurrent.Executors

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        RECEIVER_EXECUTOR.execute {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages.isNullOrEmpty()) {
                    Log.w(TAG, "SMS_RECEIVED broadcast did not contain message parts")
                    return@execute
                }

                val grouped = messages.groupBy { it.originatingAddress ?: "unknown" }
                val repository = GatewayRepository(appContext)
                val subscriptionId = SubscriptionUtils.incomingSubscriptionId(appContext, intent)
                grouped.forEach { (from, parts) ->
                    val text = parts
                        .sortedBy { it.timestampMillis }
                        .joinToString(separator = "") { it.messageBody ?: "" }
                    val record = repository.addReceivedIfNew(from, text, subscriptionId) ?: return@forEach
                    WebhookDispatcher.enqueue(
                        context = appContext,
                        event = "sms.received",
                        data = record.toJson()
                    )
                    Log.i(TAG, "Incoming SMS recorded")
                }
            } catch (error: Exception) {
                Log.e(TAG, "Failed to process incoming SMS", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SmsReceiver"
        private val RECEIVER_EXECUTOR = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "sms-receiver").apply { isDaemon = true }
        }
    }
}
