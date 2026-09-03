package com.example.smsgateway

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object WebhookDispatcher {
    private val intake = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "sms-webhook-intake").apply { isDaemon = true }
    }
    private val ioPool = Executors.newFixedThreadPool(2) { runnable ->
        Thread(runnable, "sms-webhook-io").apply { isDaemon = true }
    }
    private val retryScheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "sms-webhook-retry").apply { isDaemon = true }
    }

    fun enqueue(context: Context, event: String, data: JSONObject) {
        val appContext = context.applicationContext
        val snapshot = data.toString()
        intake.execute {
            val repository = GatewayRepository(appContext)
            val config = repository.webhookConfig()
            val eventId = newEventId()
            val timestamp = Instant.now().toString()
            when {
                config.url.isBlank() -> {
                    recordSkipped(repository, eventId, timestamp, event, JSONObject(snapshot), "not_configured", "未配置回调地址")
                    return@execute
                }
                !config.enabled -> {
                    recordSkipped(repository, eventId, timestamp, event, JSONObject(snapshot), "disabled", "Webhook 已停用")
                    return@execute
                }
                event !in config.events -> {
                    recordSkipped(repository, eventId, timestamp, event, JSONObject(snapshot), "not_subscribed", "未订阅此事件")
                    return@execute
                }
            }
            startDelivery(appContext, config, event, JSONObject(snapshot), eventId, timestamp, attempt = 1)
        }
    }

    fun enqueueTest(context: Context) {
        val appContext = context.applicationContext
        intake.execute {
            val repository = GatewayRepository(appContext)
            val config = repository.webhookConfig()
            val event = "webhook.test"
            val data = JSONObject().put("message", "本地短信网关测试回调")
            val eventId = newEventId()
            val timestamp = Instant.now().toString()
            if (config.url.isBlank()) {
                recordSkipped(repository, eventId, timestamp, event, data, "not_configured", "未配置回调地址")
                return@execute
            }
            if (!config.enabled) {
                recordSkipped(repository, eventId, timestamp, event, data, "disabled", "Webhook 已停用")
                return@execute
            }
            startDelivery(appContext, config, event, data, eventId, timestamp, attempt = 1)
        }
    }

    private fun recordSkipped(repository: GatewayRepository, eventId: String, timestamp: String, event: String, data: JSONObject, status: String, detail: String) {
        val messageId = data.optString("messageId").takeIf { it.isNotBlank() }
        updateReceivedStatus(repository, messageId, status, 0)
        repository.addWebhookLog(WebhookLogRecord(eventId, event, timestamp, messageId, status, 0, null, detail))
    }

    private fun startDelivery(appContext: Context, config: WebhookConfig, event: String, data: JSONObject, eventId: String, timestamp: String, attempt: Int) {
        ioPool.execute {
            val payload = JSONObject().put("event", event).put("eventId", eventId).put("occurredAt", timestamp).put("deviceId", "android-local-gateway").put("data", data)
            val body = payload.toString()
            val responseCode = post(config, event, eventId, timestamp, body)
            val delivered = responseCode in 200..299
            val maxAttempts = (config.maxRetries + 1).coerceIn(1, 11)
            if (!delivered && attempt < maxAttempts) {
                val delaySeconds = retryDelaySeconds(config, attempt)
                retryScheduler.schedule({ startDelivery(appContext, config, event, data, eventId, timestamp, attempt + 1) }, delaySeconds, TimeUnit.SECONDS)
                return@execute
            }
            val repository = GatewayRepository(appContext)
            val messageId = data.optString("messageId")
            if (messageId.startsWith("rcv_")) {
                updateReceivedStatus(repository, messageId, if (delivered) "delivered" else "failed", (attempt - 1).coerceAtLeast(0))
            }
            repository.addWebhookLog(WebhookLogRecord(eventId, event, timestamp, messageId.takeIf { it.isNotBlank() }, if (delivered) "delivered" else "failed", attempt, responseCode.takeIf { it >= 0 }, if (delivered) "HTTP ${responseCode}" else "HTTP ${responseCode} · 共尝试 ${attempt} 次"))
        }
    }

    private fun retryDelaySeconds(config: WebhookConfig, attempt: Int): Long {
        var delay = config.initialDelaySeconds.toDouble()
        repeat(attempt - 1) { delay *= 2.0 }
        return delay.toLong().coerceIn(1L, config.maxDelaySeconds.toLong().coerceAtLeast(1L))
    }

    private fun updateReceivedStatus(repository: GatewayRepository, messageId: String?, status: String, retryCount: Int) {
        if (messageId?.startsWith("rcv_") == true) repository.updateReceivedWebhookStatus(messageId, status, retryCount)
    }

    private fun newEventId(): String = "evt_${UUID.randomUUID().toString().replace("-", "").take(16)}"

    private fun post(config: WebhookConfig, event: String, eventId: String, timestamp: String, body: String): Int {
        val connection = try {
            (URL(config.url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("X-SMS-Gateway-Event", event)
                setRequestProperty("X-SMS-Gateway-Id", eventId)
                setRequestProperty("X-SMS-Gateway-Timestamp", timestamp)
                setRequestProperty("X-SMS-Gateway-Signature", "sha256=${hmacSha256(config.token, "$timestamp.$body")}")
                setRequestProperty("X-SMS-Gateway-Token", config.token)
            }
        } catch (_: Exception) { return -1 }
        return try {
            connection.outputStream.use { output -> output.write(body.toByteArray(StandardCharsets.UTF_8)) }
            connection.responseCode
        } catch (_: Exception) { -1 } finally { connection.disconnect() }
    }

    private fun hmacSha256(secret: String, value: String): String {
        if (secret.isBlank()) return "unsigned"
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
            mac.doFinal(value.toByteArray(StandardCharsets.UTF_8)).joinToString("") { byte -> "%02x".format(byte) }
        } catch (_: Exception) {
            MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8)).joinToString("") { byte -> "%02x".format(byte) }
        }
    }
}
