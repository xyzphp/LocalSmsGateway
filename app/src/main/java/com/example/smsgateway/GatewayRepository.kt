package com.example.smsgateway

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

data class SentRecord(
    val messageId: String,
    val to: String,
    val text: String,
    val createdAt: String,
    val subscriptionId: Int?,
    val clientRequestId: String?,
    val status: String,
    val sentAt: String?,
    val deliveredAt: String?,
    val errorCode: Int?,
    val partCount: Int = 1,
    val sentParts: Int = 0,
    val deliveredParts: Int = 0,
    val failedParts: Int = 0
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("messageId", messageId)
        put("to", to)
        put("text", text)
        put("createdAt", createdAt)
        putNullable("subscriptionId", subscriptionId)
        putNullable("clientRequestId", clientRequestId)
        put("status", status)
        putNullable("sentAt", sentAt)
        putNullable("deliveredAt", deliveredAt)
        putNullable("errorCode", errorCode)
        put("partCount", partCount.coerceAtLeast(1))
        put("sentParts", sentParts.coerceAtLeast(0))
        put("deliveredParts", deliveredParts.coerceAtLeast(0))
        put("failedParts", failedParts.coerceAtLeast(0))
    }

    companion object {
        fun fromJson(value: JSONObject): SentRecord = SentRecord(
            messageId = value.optString("messageId"),
            to = value.optString("to"),
            text = value.optString("text"),
            createdAt = value.optString("createdAt"),
            subscriptionId = value.optionalInt("subscriptionId"),
            clientRequestId = value.optionalString("clientRequestId"),
            status = value.optString("status", "queued"),
            sentAt = value.optionalString("sentAt"),
            deliveredAt = value.optionalString("deliveredAt"),
            errorCode = value.optionalInt("errorCode"),
            partCount = value.optInt("partCount", 1).coerceAtLeast(1),
            sentParts = value.optInt("sentParts", 0).coerceAtLeast(0),
            deliveredParts = value.optInt("deliveredParts", 0).coerceAtLeast(0),
            failedParts = value.optInt("failedParts", 0).coerceAtLeast(0)
        )
    }
}

data class PartAggregationResult(
    val record: SentRecord,
    val statusChanged: Boolean,
    val emitEvent: String?
)

data class ReceivedRecord(
    val messageId: String,
    val from: String,
    val text: String,
    val receivedAt: String,
    val subscriptionId: Int?,
    val webhookStatus: String,
    val webhookRetryCount: Int
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("messageId", messageId)
        put("from", from)
        put("text", text)
        put("receivedAt", receivedAt)
        putNullable("subscriptionId", subscriptionId)
        put("webhookStatus", webhookStatus)
        put("webhookRetryCount", webhookRetryCount)
    }

    companion object {
        fun fromJson(value: JSONObject): ReceivedRecord = ReceivedRecord(
            messageId = value.optString("messageId"),
            from = value.optString("from"),
            text = value.optString("text"),
            receivedAt = value.optString("receivedAt"),
            subscriptionId = value.optionalInt("subscriptionId"),
            webhookStatus = value.optString("webhookStatus", "pending"),
            webhookRetryCount = value.optInt("webhookRetryCount", 0)
        )
    }
}

data class WebhookLogRecord(
    val eventId: String,
    val event: String,
    val occurredAt: String,
    val messageId: String?,
    val status: String,
    val attemptCount: Int,
    val responseCode: Int?,
    val detail: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("eventId", eventId)
        put("event", event)
        put("occurredAt", occurredAt)
        putNullable("messageId", messageId)
        put("status", status)
        put("attemptCount", attemptCount)
        putNullable("responseCode", responseCode)
        put("detail", detail)
    }

    companion object {
        fun fromJson(value: JSONObject): WebhookLogRecord = WebhookLogRecord(
            eventId = value.optString("eventId"),
            event = value.optString("event"),
            occurredAt = value.optString("occurredAt"),
            messageId = value.optionalString("messageId"),
            status = value.optString("status", "pending"),
            attemptCount = value.optInt("attemptCount", 0),
            responseCode = value.optionalInt("responseCode"),
            detail = value.optString("detail")
        )
    }
}

data class WebhookConfig(
    val enabled: Boolean,
    val url: String,
    val token: String,
    val events: Set<String>,
    val maxRetries: Int,
    val initialDelaySeconds: Int,
    val maxDelaySeconds: Int
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("enabled", enabled)
        put("url", url)
        put("token", token)
        put("events", JSONArray().apply { events.forEach { event -> put(event) } })
        put("maxRetries", maxRetries)
        put("initialDelaySeconds", initialDelaySeconds)
        put("maxDelaySeconds", maxDelaySeconds)
    }

    companion object {
        val DEFAULT_EVENTS = setOf(
            "sms.received",
            "sms.sent",
            "sms.delivered",
            "sms.failed"
        )

        fun default(port: Int = 8080): WebhookConfig = WebhookConfig(
            enabled = true,
            url = "http://127.0.0.1:$port$BUILTIN_TEST_PATH",
            token = "",
            events = DEFAULT_EVENTS,
            maxRetries = 5,
            initialDelaySeconds = 2,
            maxDelaySeconds = 60
        )

        fun fromJson(value: JSONObject): WebhookConfig {
            val events = mutableSetOf<String>()
            value.optJSONArray("events")?.let { array ->
                for (index in 0 until array.length()) {
                    array.optString(index).takeIf { it.isNotBlank() }?.let(events::add)
                }
            }
            return WebhookConfig(
                enabled = value.optBoolean("enabled", false),
                url = value.optString("url"),
                token = value.optString("token").ifBlank { value.optString("secret") },
                events = if (events.isEmpty()) DEFAULT_EVENTS else events,
                maxRetries = value.optInt("maxRetries", 5).coerceIn(0, 10),
                initialDelaySeconds = value.optInt("initialDelaySeconds", 2).coerceIn(1, 300),
                maxDelaySeconds = value.optInt("maxDelaySeconds", 60).coerceIn(1, 3600)
            )
        }

        const val BUILTIN_TEST_PATH = "/webhook/test"
    }
}

class GatewayRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(
        "sms_gateway_store",
        Context.MODE_PRIVATE
    )
    private val lock: Any get() = GLOBAL_LOCK

    fun apiToken(): String = synchronized(lock) {
        preferences.getString(KEY_API_TOKEN, null) ?: UUID.randomUUID().toString().also {
            preferences.edit().putString(KEY_API_TOKEN, it).apply()
        }
    }

    fun rotateApiToken(): String = synchronized(lock) {
        UUID.randomUUID().toString().also {
            preferences.edit().putString(KEY_API_TOKEN, it).apply()
        }
    }

    fun setApiToken(token: String): Boolean = synchronized(lock) {
        val normalized = token.trim()
        if (normalized.isBlank()) return@synchronized false
        preferences.edit().putString(KEY_API_TOKEN, normalized).apply()
        true
    }

    fun isValidToken(token: String?): Boolean = !token.isNullOrBlank() && token == apiToken()

    fun serverPort(): Int = preferences.getInt(KEY_PORT, DEFAULT_PORT).coerceIn(1024, 65535)

    fun setServerPort(port: Int) {
        val normalizedPort = port.coerceIn(1024, 65535)
        val previousPort = serverPort()
        preferences.edit().putInt(KEY_PORT, normalizedPort).apply()

        // Keep the built-in receiver valid when the user changes the gateway port.
        val previousBuiltinUrl = "http://127.0.0.1:$previousPort${WebhookConfig.BUILTIN_TEST_PATH}"
        val rawWebhook = preferences.getString(KEY_WEBHOOK, null) ?: return
        val currentWebhook = runCatching { WebhookConfig.fromJson(JSONObject(rawWebhook)) }.getOrNull() ?: return
        if (currentWebhook.url == previousBuiltinUrl) {
            saveWebhookConfig(
                currentWebhook.copy(
                    url = "http://127.0.0.1:$normalizedPort${WebhookConfig.BUILTIN_TEST_PATH}"
                )
            )
        }
    }

    fun isServiceRunning(): Boolean = preferences.getBoolean(KEY_RUNNING, false)

    fun setServiceRunning(running: Boolean) {
        preferences.edit().putBoolean(KEY_RUNNING, running).apply()
    }

    fun setAutoStart(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_START, enabled).apply()
    }

    fun findSentByClientRequestId(clientRequestId: String): SentRecord? = synchronized(lock) {
        val id = clientRequestId.trim()
        if (id.isBlank()) return@synchronized null
        val records = readArray(KEY_SENT)
        for (index in 0 until records.length()) {
            val value = records.optJSONObject(index) ?: continue
            if (value.optString("clientRequestId") == id) {
                if (value.optString("status") == "failed" && value.optInt("errorCode", 0) == ERROR_RATE_LIMITED) continue
                return@synchronized SentRecord.fromJson(value)
            }
        }
        null
    }

    fun addSent(
        to: String,
        text: String,
        subscriptionId: Int?,
        clientRequestId: String?,
        partCount: Int = 1
    ): SentRecord = synchronized(lock) {
        // Idempotency: same clientRequestId must not create a duplicate SMS send.
        val normalizedClientId = clientRequestId?.trim().orEmpty()
        if (normalizedClientId.isNotBlank()) {
            val records = readArray(KEY_SENT)
            for (index in 0 until records.length()) {
                val value = records.optJSONObject(index) ?: continue
                if (value.optString("clientRequestId") == normalizedClientId) {
                    if (value.optString("status") == "failed" && value.optInt("errorCode", 0) == ERROR_RATE_LIMITED) continue
                    return@synchronized SentRecord.fromJson(value)
                }
            }
        }
        val record = SentRecord(
            messageId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}",
            to = to,
            text = text,
            createdAt = nowIso(),
            subscriptionId = subscriptionId,
            clientRequestId = clientRequestId,
            status = "queued",
            sentAt = null,
            deliveredAt = null,
            errorCode = null,
            partCount = partCount.coerceAtLeast(1)
        )
        prepend(KEY_SENT, record.toJson())
        pruneByRetentionLocked()
        record
    }

    fun tryAddSent(
        to: String,
        text: String,
        subscriptionId: Int?,
        clientRequestId: String?,
        partCount: Int = 1
    ): SentRecord? = synchronized(lock) {
        val normalizedClientId = clientRequestId?.trim().orEmpty()
        if (normalizedClientId.isNotBlank()) {
            val records = readArray(KEY_SENT)
            for (index in 0 until records.length()) {
                val value = records.optJSONObject(index) ?: continue
                if (value.optString("clientRequestId") == normalizedClientId) {
                    if (value.optString("status") == "failed" && value.optInt("errorCode", 0) == ERROR_RATE_LIMITED) continue
                    return@synchronized null
                }
            }
        }
        val record = SentRecord(
            messageId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}",
            to = to,
            text = text,
            createdAt = nowIso(),
            subscriptionId = subscriptionId,
            clientRequestId = clientRequestId,
            status = "queued",
            sentAt = null,
            deliveredAt = null,
            errorCode = null,
            partCount = partCount.coerceAtLeast(1)
        )
        prepend(KEY_SENT, record.toJson())
        pruneByRetentionLocked()
        record
    }

    fun updateSentPartCount(messageId: String, partCount: Int): SentRecord? = synchronized(lock) {
        val records = readArray(KEY_SENT)
        var updated: SentRecord? = null
        for (index in 0 until records.length()) {
            val value = records.optJSONObject(index) ?: continue
            if (value.optString("messageId") != messageId) continue
            value.put("partCount", partCount.coerceAtLeast(1))
            updated = SentRecord.fromJson(value)
            break
        }
        if (updated != null) saveArray(KEY_SENT, records)
        updated
    }

    // Aggregates multipart sent/delivery callbacks. Only the transition that
    // completes the whole message returns statusChanged=true, so callers emit
    // exactly one sms.sent / sms.delivered / sms.failed webhook per message.
    fun updateSentPartResult(
        messageId: String,
        kind: String,
        success: Boolean,
        errorCode: Int? = null
    ): PartAggregationResult? = synchronized(lock) {
        val records = readArray(KEY_SENT)
        for (index in 0 until records.length()) {
            val value = records.optJSONObject(index) ?: continue
            if (value.optString("messageId") != messageId) continue
            val total = value.optInt("partCount", 1).coerceAtLeast(1)
            var sentParts = value.optInt("sentParts", 0).coerceAtLeast(0)
            var deliveredParts = value.optInt("deliveredParts", 0).coerceAtLeast(0)
            var failedParts = value.optInt("failedParts", 0).coerceAtLeast(0)
            val previousStatus = value.optString("status", "queued")
            if (previousStatus == "failed" || previousStatus == "delivered") {
                return@synchronized PartAggregationResult(SentRecord.fromJson(value), false, null)
            }
            if (!success) {
                failedParts = (failedParts + 1).coerceAtMost(total)
            } else if (kind == "delivery") {
                deliveredParts = (deliveredParts + 1).coerceAtMost(total)
                sentParts = maxOf(sentParts, deliveredParts)
            } else {
                sentParts = (sentParts + 1).coerceAtMost(total)
            }
            value.put("sentParts", sentParts)
            value.put("deliveredParts", deliveredParts)
            value.put("failedParts", failedParts)
            var nextStatus = previousStatus
            var emit: String? = null
            if (failedParts > 0) {
                nextStatus = "failed"
                value.put("errorCode", errorCode ?: value.opt("errorCode") ?: -2)
                emit = "sms.failed"
            } else if (deliveredParts >= total) {
                nextStatus = "delivered"
                value.put("deliveredAt", nowIso())
                if (value.isNull("sentAt")) value.put("sentAt", nowIso())
                value.put("errorCode", JSONObject.NULL)
                emit = "sms.delivered"
            } else if (sentParts >= total) {
                nextStatus = "sent"
                value.put("sentAt", nowIso())
                value.put("errorCode", JSONObject.NULL)
                emit = "sms.sent"
            } else {
                nextStatus = "sending"
            }
            val changed = nextStatus != previousStatus
            value.put("status", nextStatus)
            val updated = SentRecord.fromJson(value)
            saveArray(KEY_SENT, records)
            return@synchronized PartAggregationResult(updated, changed, if (changed) emit else null)
        }
        null
    }

    fun updateSentStatus(
        messageId: String,
        status: String,
        errorCode: Int? = null
    ): SentRecord? = synchronized(lock) {
        val records = readArray(KEY_SENT)
        var updated: SentRecord? = null
        for (index in 0 until records.length()) {
            val value = records.optJSONObject(index) ?: continue
            if (value.optString("messageId") != messageId) continue
            val previous = value.optString("status")
            if (previous == "failed" || previous == "delivered") {
                return@synchronized SentRecord.fromJson(value)
            }
            value.put("status", status)
            if (status == "sent") value.put("sentAt", nowIso())
            if (status == "delivered") value.put("deliveredAt", nowIso())
            if (errorCode == null) value.put("errorCode", JSONObject.NULL) else value.put("errorCode", errorCode)
            updated = SentRecord.fromJson(value)
            break
        }
        if (updated != null) saveArray(KEY_SENT, records)
        updated
    }

    fun failSendingIfPending(messageId: String): SentRecord? = synchronized(lock) {
        val records = readArray(KEY_SENT)
        for (index in 0 until records.length()) {
            val value = records.optJSONObject(index) ?: continue
            if (value.optString("messageId") != messageId) continue
            if (value.optString("status") != "sending" && value.optString("status") != "queued") return@synchronized null
            value.put("status", "failed")
            value.put("errorCode", ERROR_SEND_TIMEOUT)
            val updated = SentRecord.fromJson(value)
            saveArray(KEY_SENT, records)
            return@synchronized updated
        }
        null
    }

    fun reconcileStaleSending(
        timeoutMillis: Long = SEND_PENDING_TIMEOUT_MILLIS
    ): List<SentRecord> = synchronized(lock) {
        val records = readArray(KEY_SENT)
        val now = Instant.now().toEpochMilli()
        val updatedRecords = mutableListOf<SentRecord>()
        var changed = false
        for (index in 0 until records.length()) {
            val value = records.optJSONObject(index) ?: continue
            val status = value.optString("status")
            if (status != "sending" && status != "queued") continue
            val createdAt = runCatching { Instant.parse(value.optString("createdAt")).toEpochMilli() }.getOrNull()
                ?: continue
            if (now - createdAt < timeoutMillis) continue
            value.put("status", "failed")
            value.put("errorCode", ERROR_SEND_TIMEOUT)
            updatedRecords += SentRecord.fromJson(value)
            changed = true
        }
        if (changed) saveArray(KEY_SENT, records)
        updatedRecords
    }

    fun listSent(limit: Int = 100): List<SentRecord> = synchronized(lock) {
        val records = readArray(KEY_SENT)
        val fallbackSubscriptionId = SubscriptionUtils.singleActiveSubscriptionId(appContext)
        var changed = false
        val result = (0 until minOf(records.length(), limit.coerceIn(1, 500))).mapNotNull { index ->
            records.optJSONObject(index)?.let { value ->
                if (value.optionalInt("subscriptionId")?.takeIf { it > 0 } == null && fallbackSubscriptionId != null) {
                    value.put("subscriptionId", fallbackSubscriptionId)
                    changed = true
                }
                SentRecord.fromJson(value)
            }
        }
        if (changed) saveArray(KEY_SENT, records)
        result
    }

    fun clearSentRecords(): Int = clearRecords(KEY_SENT)

    fun addReceived(from: String, text: String, subscriptionId: Int?): ReceivedRecord = synchronized(lock) {
        val resolvedSubscriptionId = subscriptionId ?: SubscriptionUtils.singleActiveSubscriptionId(appContext)
        val record = ReceivedRecord(
            messageId = "rcv_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}",
            from = from,
            text = text,
            receivedAt = nowIso(),
            subscriptionId = resolvedSubscriptionId,
            webhookStatus = "pending",
            webhookRetryCount = 0
        )
        prepend(KEY_RECEIVED, record.toJson())
        record
    }

    fun addReceivedIfNew(
        from: String,
        text: String,
        subscriptionId: Int?
    ): ReceivedRecord? = synchronized(lock) {
        val normalizedFrom = from.trim().ifBlank { "unknown" }
        val normalizedText = text
        val now = System.currentTimeMillis()
        val resolvedSubscriptionId = subscriptionId ?: SubscriptionUtils.singleActiveSubscriptionId(appContext)
        val records = readArray(KEY_RECEIVED)
        val duplicateIndex = (0 until records.length()).firstOrNull { index ->
            val value = records.optJSONObject(index) ?: return@firstOrNull false
            if (value.optString("from") != normalizedFrom ||
                value.optString("text") != normalizedText
            ) return@firstOrNull false
            val receivedAt = runCatching {
                Instant.parse(value.optString("receivedAt")).toEpochMilli()
            }.getOrNull() ?: return@firstOrNull false
            now - receivedAt in 0..RECEIVED_DEDUPE_WINDOW_MILLIS
        }
        if (duplicateIndex != null) {
            val existing = records.optJSONObject(duplicateIndex)
            if (existing != null &&
                existing.optionalInt("subscriptionId")?.takeIf { it > 0 } == null &&
                resolvedSubscriptionId != null
            ) {
                existing.put("subscriptionId", resolvedSubscriptionId)
                saveArray(KEY_RECEIVED, records)
            }
            return@synchronized null
        }

        val record = ReceivedRecord(
            messageId = "rcv_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}",
            from = normalizedFrom,
            text = normalizedText,
            receivedAt = nowIso(),
            subscriptionId = resolvedSubscriptionId,
            webhookStatus = "pending",
            webhookRetryCount = 0
        )
        prepend(KEY_RECEIVED, record.toJson())
        record
    }

    fun updateReceivedWebhookStatus(messageId: String, status: String, retryCount: Int? = null) = synchronized(lock) {
        val records = readArray(KEY_RECEIVED)
        for (index in 0 until records.length()) {
            val value = records.optJSONObject(index) ?: continue
            if (value.optString("messageId") != messageId) continue
            value.put("webhookStatus", status)
            if (retryCount != null) value.put("webhookRetryCount", retryCount)
            break
        }
        saveArray(KEY_RECEIVED, records)
    }

    fun listReceived(limit: Int = 100): List<ReceivedRecord> = synchronized(lock) {
        val records = readArray(KEY_RECEIVED)
        val fallbackSubscriptionId = SubscriptionUtils.singleActiveSubscriptionId(appContext)
        var changed = false
        val result = (0 until minOf(records.length(), limit.coerceIn(1, 500))).mapNotNull { index ->
            records.optJSONObject(index)?.let { value ->
                if (value.optionalInt("subscriptionId")?.takeIf { it > 0 } == null && fallbackSubscriptionId != null) {
                    value.put("subscriptionId", fallbackSubscriptionId)
                    changed = true
                }
                ReceivedRecord.fromJson(value)
            }
        }
        if (changed) saveArray(KEY_RECEIVED, records)
        result
    }

    fun clearReceivedRecords(): Int = clearRecords(KEY_RECEIVED)

    fun addWebhookLog(record: WebhookLogRecord) = synchronized(lock) {
        prepend(KEY_WEBHOOK_LOGS, record.toJson())
    }

    fun listWebhookLogs(limit: Int = 100): List<WebhookLogRecord> = synchronized(lock) {
        val records = readArray(KEY_WEBHOOK_LOGS)
        (0 until minOf(records.length(), limit.coerceIn(1, 500))).mapNotNull { index ->
            records.optJSONObject(index)?.let(WebhookLogRecord::fromJson)
        }
    }

    fun clearWebhookLogs(): Int = synchronized(lock) {
        val count = readArray(KEY_WEBHOOK_LOGS).length()
        preferences.edit()
            .putString(KEY_WEBHOOK_LOGS, JSONArray().toString())
            .putLong(KEY_WEBHOOK_LOGS_CLEARED_AT, System.currentTimeMillis())
            .commit()
        count
    }

    fun webhookLogsClearedAt(): Long = synchronized(lock) {
        preferences.getLong(KEY_WEBHOOK_LOGS_CLEARED_AT, 0L)
    }

    fun sentCount(): Int = synchronized(lock) { readArray(KEY_SENT).length() }

    fun receivedCount(): Int = synchronized(lock) { readArray(KEY_RECEIVED).length() }

    fun webhookConfig(): WebhookConfig {
        val raw = preferences.getString(KEY_WEBHOOK, null) ?: return defaultWebhook()
        val config = try {
            WebhookConfig.fromJson(JSONObject(raw))
        } catch (_: Exception) {
            defaultWebhook()
        }
        // Replace only the old placeholder that shipped with the initial build.
        // A user-configured external URL must never be overwritten.
        if (config.url == LEGACY_DEMO_WEBHOOK_URL && config.token == LEGACY_DEMO_WEBHOOK_TOKEN) {
            return defaultWebhook().also(::saveWebhookConfig)
        }
        return config
    }

    fun saveWebhookConfig(config: WebhookConfig) {
        preferences.edit().putString(KEY_WEBHOOK, config.toJson().toString()).apply()
    }

    fun settings(): GatewaySettings = GatewaySettings(
        port = serverPort(),
        autoStart = preferences.getBoolean(KEY_AUTO_START, false),
        defaultSubscriptionId = preferences.getInt(KEY_DEFAULT_SUBSCRIPTION, 1),
        sendRatePerMinute = preferences.getInt(KEY_SEND_RATE, 60),
        sentRetentionDays = preferences.getInt(KEY_SENT_RETENTION, 90),
        receivedRetentionDays = preferences.getInt(KEY_RECEIVED_RETENTION, 90)
    )

    fun saveSettings(settings: GatewaySettings) {
        setServerPort(settings.port)
        preferences.edit()
            .putBoolean(KEY_AUTO_START, settings.autoStart)
            .putInt(KEY_DEFAULT_SUBSCRIPTION, settings.defaultSubscriptionId.coerceAtLeast(1))
            .putInt(KEY_SEND_RATE, settings.sendRatePerMinute.coerceIn(1, 300))
            .putInt(KEY_SENT_RETENTION, settings.sentRetentionDays.coerceIn(1, 3650))
            .putInt(KEY_RECEIVED_RETENTION, settings.receivedRetentionDays.coerceIn(1, 3650))
            .apply()
    }

    fun tryAcquireSendSlot(ratePerMinute: Int): Boolean = synchronized(lock) {
        val limit = ratePerMinute.coerceIn(1, 300)
        val now = System.currentTimeMillis()
        val windowStart = now - 60_000L
        val raw = preferences.getString(KEY_SEND_TIMESTAMPS, "[]") ?: "[]"
        val arr = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
        val kept = JSONArray()
        for (i in 0 until arr.length()) {
            val ts = arr.optLong(i, 0L)
            if (ts >= windowStart) kept.put(ts)
        }
        if (kept.length() >= limit) {
            preferences.edit().putString(KEY_SEND_TIMESTAMPS, kept.toString()).apply()
            return@synchronized false
        }
        kept.put(now)
        preferences.edit().putString(KEY_SEND_TIMESTAMPS, kept.toString()).apply()
        true
    }

    fun pruneByRetention(): Int = synchronized(lock) { pruneByRetentionLocked() }

    private fun pruneByRetentionLocked(): Int {
        val sentDays = preferences.getInt(KEY_SENT_RETENTION, 90).coerceIn(1, 3650)
        val receivedDays = preferences.getInt(KEY_RECEIVED_RETENTION, 90).coerceIn(1, 3650)
        var removed = 0
        removed += pruneArrayByAge(KEY_SENT, "createdAt", sentDays)
        removed += pruneArrayByAge(KEY_RECEIVED, "receivedAt", receivedDays)
        return removed
    }

    private fun pruneArrayByAge(key: String, timeField: String, retentionDays: Int): Int {
        val raw = readArray(key)
        if (raw.length() == 0) return 0
        val cutoff = Instant.now().minusSeconds(retentionDays.toLong() * 24L * 60L * 60L)
        val kept = JSONArray()
        var removed = 0
        for (i in 0 until raw.length()) {
            val obj = raw.optJSONObject(i) ?: continue
            val ts = runCatching { Instant.parse(obj.optString(timeField)) }.getOrNull()
            if (ts == null || ts.isBefore(cutoff)) { removed += 1; continue }
            kept.put(obj)
        }
        if (removed > 0) saveArray(key, kept)
        return removed
    }

    fun isValidTokenConstantTime(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        val expected = apiToken()
        return try {
            java.security.MessageDigest.isEqual(token.toByteArray(Charsets.UTF_8), expected.toByteArray(Charsets.UTF_8))
        } catch (_: Exception) { false }
    }


    private fun defaultWebhook(): WebhookConfig = WebhookConfig.default(serverPort())

    private fun prepend(key: String, value: JSONObject) {
        val old = readArray(key)
        val next = JSONArray().put(value)
        for (index in 0 until minOf(old.length(), MAX_RECORDS - 1)) {
            next.put(old.get(index))
        }
        saveArray(key, next)
    }

    private fun clearRecords(key: String): Int = synchronized(lock) {
        val count = readArray(key).length()
        saveArray(key, JSONArray())
        count
    }

    private fun readArray(key: String): JSONArray {
        val raw = preferences.getString(key, "[]") ?: "[]"
        return try {
            JSONArray(raw)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun saveArray(key: String, value: JSONArray) {
        // Keep SMS records durable before a short-lived broadcast process is released.
        preferences.edit().putString(key, value.toString()).commit()
    }

    companion object {
        const val DEFAULT_PORT = 8080
        const val SEND_PENDING_TIMEOUT_MILLIS = 30_000L
        const val ERROR_SEND_TIMEOUT = -3
        const val ERROR_RATE_LIMITED = -4
        private const val MAX_RECORDS = 500
        private const val RECEIVED_DEDUPE_WINDOW_MILLIS = 10 * 60 * 1000L
        private const val KEY_API_TOKEN = "api_token"
        private const val KEY_PORT = "server_port"
        private const val KEY_RUNNING = "service_running"
        private const val KEY_SENT = "sent_records"
        private const val KEY_RECEIVED = "received_records"
        private const val KEY_WEBHOOK_LOGS = "webhook_logs"
        private const val KEY_WEBHOOK_LOGS_CLEARED_AT = "webhook_logs_cleared_at"
        private const val KEY_WEBHOOK = "webhook_config"
        private const val KEY_AUTO_START = "auto_start"
        private const val KEY_DEFAULT_SUBSCRIPTION = "default_subscription_id"
        private const val KEY_SEND_RATE = "send_rate_per_minute"
        private const val KEY_SENT_RETENTION = "sent_retention_days"
        private const val KEY_RECEIVED_RETENTION = "received_retention_days"
        private const val LEGACY_DEMO_WEBHOOK_URL = "http://192.168.0.10:9000/webhook"
        private const val LEGACY_DEMO_WEBHOOK_TOKEN = "callback_token"
        private const val KEY_SEND_TIMESTAMPS = "send_timestamps"
        private val GLOBAL_LOCK = Any()
    }
}

data class GatewaySettings(
    val port: Int,
    val autoStart: Boolean,
    val defaultSubscriptionId: Int,
    val sendRatePerMinute: Int,
    val sentRetentionDays: Int,
    val receivedRetentionDays: Int
) {
    fun toJson(): JSONObject = JSONObject()
        .put("port", port)
        .put("autoStart", autoStart)
        .put("defaultSubscriptionId", defaultSubscriptionId)
        .put("sendRatePerMinute", sendRatePerMinute)
        .put("sentRetentionDays", sentRetentionDays)
        .put("receivedRetentionDays", receivedRetentionDays)

    companion object {
        fun fromJson(value: JSONObject, fallback: GatewaySettings): GatewaySettings = GatewaySettings(
            port = value.optInt("port", fallback.port).coerceIn(1024, 65535),
            autoStart = value.optBoolean("autoStart", fallback.autoStart),
            defaultSubscriptionId = value.optInt("defaultSubscriptionId", fallback.defaultSubscriptionId).coerceAtLeast(1),
            sendRatePerMinute = value.optInt("sendRatePerMinute", fallback.sendRatePerMinute).coerceIn(1, 300),
            sentRetentionDays = value.optInt("sentRetentionDays", fallback.sentRetentionDays).coerceIn(1, 3650),
            receivedRetentionDays = value.optInt("receivedRetentionDays", fallback.receivedRetentionDays).coerceIn(1, 3650)
        )
    }
}

private fun JSONObject.putNullable(key: String, value: Any?) {
    put(key, value ?: JSONObject.NULL)
}

private fun JSONObject.optionalString(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

private fun JSONObject.optionalInt(key: String): Int? =
    if (isNull(key) || !has(key)) null else optInt(key)

private fun nowIso(): String = java.time.Instant.now().toString()
