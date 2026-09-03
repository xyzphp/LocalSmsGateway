package com.example.smsgateway

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Fallback for devices that do not deliver SMS_RECEIVED to a non-default SMS app.
 * It only scans the recent inbox window and relies on the repository's short
 * duplicate window so a broadcast and a provider row do not create two logs.
 */
object SmsInboxSync {
    private const val TAG = "SmsInboxSync"
    private const val LOOKBACK_MILLIS = 15 * 60 * 1000L
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "sms-inbox-sync").apply { isDaemon = true }
    }
    private val running = AtomicBoolean(false)

    fun syncAsync(context: Context) {
        if (!running.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        executor.execute {
            try {
                sync(appContext)
            } finally {
                running.set(false)
            }
        }
    }

    private fun sync(context: Context) {
        if (context.checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val since = System.currentTimeMillis() - LOOKBACK_MILLIS
        val records = mutableListOf<ReceivedRecord>()
        val projection = arrayOf("_id", "address", "body", "date", "sub_id")
        try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                "date >= ?",
                arrayOf(since.toString()),
                "date ASC"
            )?.use { cursor ->
                val addressIndex = cursor.getColumnIndex("address")
                val bodyIndex = cursor.getColumnIndex("body")
                val subscriptionIndex = cursor.getColumnIndex("sub_id")
                if (addressIndex < 0 || bodyIndex < 0) return@use

                while (cursor.moveToNext()) {
                    val from = cursor.getString(addressIndex).orEmpty().ifBlank { "unknown" }
                    val body = cursor.getString(bodyIndex).orEmpty()
                    if (body.isBlank()) continue
                    val subscriptionId = if (subscriptionIndex >= 0 && !cursor.isNull(subscriptionIndex)) {
                        cursor.getInt(subscriptionIndex).takeIf { it > 0 }
                    } else {
                        null
                    }
                    GatewayRepository(context).addReceivedIfNew(from, body, subscriptionId)?.let(records::add)
                }
            }
        } catch (error: Exception) {
            Log.e(TAG, "Failed to sync SMS inbox", error)
            return
        }

        records.forEach { record ->
            WebhookDispatcher.enqueue(
                context = context,
                event = "sms.received",
                data = record.toJson()
            )
        }
        if (records.isNotEmpty()) Log.i(TAG, "Imported ${records.size} incoming SMS record(s)")
    }
}
