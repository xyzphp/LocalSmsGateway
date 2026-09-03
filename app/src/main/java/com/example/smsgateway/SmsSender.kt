package com.example.smsgateway

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SmsSender(private val context: Context) {
    private val repository = GatewayRepository(context)
    private val timeoutExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "sms-send-timeout").apply { isDaemon = true }
    }

    fun send(
        to: String,
        text: String,
        subscriptionId: Int?,
        clientRequestId: String?
    ): SentRecord {
        val normalizedClientId = clientRequestId?.trim().orEmpty()
        if (normalizedClientId.isNotBlank()) {
            repository.findSentByClientRequestId(normalizedClientId)?.let { return it }
        }
        if (!repository.tryAcquireSendSlot(repository.settings().sendRatePerMinute)) {
            // Rate-limited: persist without clientRequestId so a later retry with the same ID can still send.
            val limited = repository.addSent(to, text, subscriptionId, null)
            return repository.updateSentStatus(limited.messageId, "failed", ERROR_RATE_LIMITED) ?: limited.copy(status = "failed", errorCode = ERROR_RATE_LIMITED)
        }
        repository.pruneByRetention()
        // Atomic single-check: tryAddSent creates the record or returns null when a concurrent
        // retry with the same clientRequestId already won. Null means return existing, never resend.
        val created = repository.tryAddSent(to, text, subscriptionId, clientRequestId)
        val record = if (created != null) {
            created
        } else {
            return repository.findSentByClientRequestId(normalizedClientId)
                ?: repository.addSent(to, text, subscriptionId, clientRequestId)
        }
        try {
            val manager = if (subscriptionId != null && subscriptionId > 0) {
                SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            } else {
                SmsManager.getDefault()
            }
            val parts = manager.divideMessage(text)
            val totalParts = parts.size.coerceAtLeast(1)
            repository.updateSentPartCount(record.messageId, totalParts)
            if (parts.size == 1) {
                manager.sendTextMessage(
                    to,
                    null,
                    text,
                    statusIntent(record.messageId, SmsStatusReceiver.KIND_SENT, 0),
                    statusIntent(record.messageId, SmsStatusReceiver.KIND_DELIVERY, 1)
                )
            } else {
                val sentIntents = ArrayList<PendingIntent>(parts.size)
                val deliveryIntents = ArrayList<PendingIntent>(parts.size)
                parts.indices.forEach { index ->
                    sentIntents += statusIntent(record.messageId, SmsStatusReceiver.KIND_SENT, index)
                    deliveryIntents += statusIntent(record.messageId, SmsStatusReceiver.KIND_DELIVERY, index + 1000)
                }
                manager.sendMultipartTextMessage(to, null, parts, sentIntents, deliveryIntents)
            }
            repository.updateSentStatus(record.messageId, "sending") ?: record.copy(status = "sending")
            timeoutExecutor.schedule({
                val timedOut = repository.failSendingIfPending(record.messageId)
                if (timedOut != null) {
                    WebhookDispatcher.enqueue(context, "sms.failed", timedOut.toJson())
                }
            }, GatewayRepository.SEND_PENDING_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        } catch (error: SecurityException) {
            val failed = repository.updateSentStatus(record.messageId, "failed", -1)
                ?: record.copy(status = "failed", errorCode = -1)
            WebhookDispatcher.enqueue(context, "sms.failed", failed.toJson())
            return failed
        } catch (_: Exception) {
            val failed = repository.updateSentStatus(record.messageId, "failed", -2)
                ?: record.copy(status = "failed", errorCode = -2)
            WebhookDispatcher.enqueue(context, "sms.failed", failed.toJson())
            return failed
        }
        return repository.listSent(500).firstOrNull { it.messageId == record.messageId }
            ?: record.copy(status = "sending")
    }

    fun shutdown() {
        timeoutExecutor.shutdownNow()
    }

    private fun statusIntent(messageId: String, kind: String, offset: Int): PendingIntent {
        val intent = Intent(context, SmsStatusReceiver::class.java).apply {
            action = SmsStatusReceiver.ACTION_STATUS
            putExtra(SmsStatusReceiver.EXTRA_MESSAGE_ID, messageId)
            putExtra(SmsStatusReceiver.EXTRA_KIND, kind)
            putExtra(SmsStatusReceiver.EXTRA_PART_INDEX, offset)
        }
        // Unique request codes per part so multipart PendingIntents never collide.
        val requestCode = REQUEST_CODE_GEN.getAndIncrement()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ERROR_RATE_LIMITED = -4
        private val REQUEST_CODE_GEN = AtomicInteger(1 shl 20)
    }
}
