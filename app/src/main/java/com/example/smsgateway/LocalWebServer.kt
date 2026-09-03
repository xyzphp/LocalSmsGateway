package com.example.smsgateway

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class LocalWebServer(context: Context) {
    private val appContext = context.applicationContext
    private val repository = GatewayRepository(appContext)
    private val sender = SmsSender(appContext)
    private val clientExecutor = ThreadPoolExecutor(
        4, 8, 60L, TimeUnit.SECONDS,
        ArrayBlockingQueue(32),
        { runnable -> Thread(runnable, "sms-local-http-client").apply { isDaemon = true } },
        ThreadPoolExecutor.CallerRunsPolicy()
    )

    @Volatile
    private var running = false
    private var serverSocket: ServerSocket? = null
    private var acceptThread: Thread? = null

    fun start() {
        synchronized(this) {
            if (running) return
            val socket = ServerSocket()
            socket.reuseAddress = true
            // Bind the gateway on every local interface so LAN/VPN overlays such as EasyTier can reach it.
            socket.bind(InetSocketAddress("0.0.0.0", repository.serverPort()))
            serverSocket = socket
            running = true
            repository.setServiceRunning(true)
            acceptThread = thread(start = true, name = "sms-local-http-acceptor") {
                while (running) {
                    try {
                        val client = socket.accept()
                        clientExecutor.execute { handleClient(client) }
                    } catch (_: Exception) {
                        if (running) continue
                    }
                }
            }
        }
    }

    fun stop() {
        synchronized(this) {
            running = false
            sender.shutdown()
            repository.setServiceRunning(false)
            try {
                serverSocket?.close()
            } catch (_: Exception) {
                // Socket is already closed.
            }
            serverSocket = null
            acceptThread?.interrupt()
            acceptThread = null
        }
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            client.soTimeout = 10000
            val request = readRequest(client.getInputStream()) ?: return
            val path = request.target.substringBefore("?")

            if (request.method == "OPTIONS") {
                respond(client, 204, "No Content", "", "text/plain; charset=utf-8")
                return
            }

            // A loopback receiver used by the default Webhook configuration. It
            // deliberately acknowledges the request without parsing, persisting,
            // forwarding, or otherwise acting on the payload.
            if (path == WebhookConfig.BUILTIN_TEST_PATH) {
                if (request.method == "POST") {
                    respond(client, 204, "No Content", "", "text/plain; charset=utf-8")
                } else if (request.method == "GET") {
                    json(
                        client,
                        200,
                        JSONObject()
                            .put("ok", true)
                            .put("message", "内置测试 Webhook 已就绪")
                    )
                } else {
                    error(client, 405, "METHOD_NOT_ALLOWED", "仅支持 POST")
                }
                return
            }

            if (path == McpProtocol.PATH) {
                if (!repository.isValidTokenConstantTime(request.headers["authorization"]?.removePrefix("Bearer ")?.trim())) {
                    error(client, 401, "UNAUTHORIZED", "需要有效的 API Token")
                    return
                }
                val response = McpProtocol.handle(
                    context = appContext,
                    repository = repository,
                    sender = sender,
                    request = McpHttpRequest(
                        method = request.method,
                        headers = request.headers,
                        body = request.body
                    )
                )
                respond(
                    socket = client,
                    status = response.status,
                    statusText = reason(response.status),
                    body = response.body,
                    contentType = response.contentType,
                    additionalHeaders = response.headers
                )
                return
            }

            if (path.startsWith("/api/")) {
                if (!repository.isValidTokenConstantTime(request.headers["authorization"]?.removePrefix("Bearer ")?.trim())) {
                    error(client, 401, "UNAUTHORIZED", "需要有效的 API Token")
                    return
                }
                handleApi(client, request, path)
            } else {
                serveAsset(client, path)
            }
        }
    }

    private fun handleApi(socket: Socket, request: HttpRequest, path: String) {
        when {
            request.method == "GET" && path == "/api/v1/stats" -> {
                json(socket, 200, buildStats())
            }

            request.method == "GET" && path == "/api/v1/status" -> {
                val config = repository.webhookConfig()
                val body = JSONObject()
                    .put("online", true)
                    .put("deviceName", "Android SMS Gateway")
                    .put("localAddress", NetworkUtils.baseUrl(appContext, repository.serverPort()))
                    .put("wifiName", "当前 Wi-Fi")
                    .put("port", repository.serverPort())
                    .put("sentCount", repository.sentCount())
                    .put("receivedCount", repository.receivedCount())
                    .put("webhookEnabled", config.enabled && config.url.isNotBlank())
                json(socket, 200, body)
            }

            request.method == "POST" && path == "/api/v1/sms/send" -> {
                val body = parseJson(request.body) ?: run {
                    error(socket, 400, "INVALID_JSON", "请求体必须是 JSON")
                    return
                }
                val to = body.optString("to").trim()
                val text = body.optString("text").ifBlank { body.optString("message") }.trim()
                if (to.isBlank() || text.isBlank()) {
                    error(socket, 400, "INVALID_REQUEST", "to 和 text 不能为空")
                    return
                }
                val subscriptionId = if (body.has("subscriptionId") && !body.isNull("subscriptionId")) {
                    body.optInt("subscriptionId")
                } else {
                    null
                }
                val clientRequestId = body.optString("clientRequestId").takeIf { it.isNotBlank() }
                val record = sender.send(to, text, subscriptionId, clientRequestId)
                if (record.status == "failed" && record.errorCode == -1) {
                    error(socket, 403, "SMS_PERMISSION_REQUIRED", "尚未授予发送短信权限")
                    return
                }
                if (record.status == "failed" && record.errorCode == SmsSender.ERROR_RATE_LIMITED) {
                    error(socket, 429, "RATE_LIMITED", "发送过于频繁，请稍后重试")
                    return
                }
                json(
                    socket,
                    202,
                    JSONObject()
                        .put("code", 202)
                        .put("message", "已加入发送队列")
                        .put("data", record.toJson())
                )
            }

            request.method == "GET" && path == "/api/v1/sims" -> {
                val sims = JSONArray().apply {
                    SubscriptionUtils.activeSubscriptions(appContext).forEach { put(it.toJson()) }
                }
                json(socket, 200, JSONObject().put("data", sims))
            }

            request.method == "GET" && path == "/api/v1/sms/sent" -> {
                val limit = queryInt(request.target, "limit", 100)
                val records = JSONArray().apply { repository.listSent(limit).forEach { put(it.toJson()) } }
                json(socket, 200, JSONObject().put("data", records).put("count", records.length()))
            }

            request.method == "DELETE" && path == "/api/v1/sms/sent" -> {
                val deletedCount = repository.clearSentRecords()
                json(
                    socket,
                    200,
                    JSONObject()
                        .put("deletedCount", deletedCount)
                        .put("message", "发送记录已清理")
                )
            }

            request.method == "GET" && path == "/api/v1/sms/received" -> {
                val limit = queryInt(request.target, "limit", 100)
                val records = JSONArray().apply { repository.listReceived(limit).forEach { put(it.toJson()) } }
                json(socket, 200, JSONObject().put("data", records).put("count", records.length()))
            }

            request.method == "DELETE" && path == "/api/v1/sms/received" -> {
                val deletedCount = repository.clearReceivedRecords()
                json(
                    socket,
                    200,
                    JSONObject()
                        .put("deletedCount", deletedCount)
                        .put("message", "收信记录已清理")
                )
            }

            request.method == "GET" && path == "/api/v1/settings" -> {
                json(socket, 200, repository.settings().toJson())
            }

            request.method == "PUT" && path == "/api/v1/settings" -> {
                val body = parseJson(request.body) ?: run {
                    error(socket, 400, "INVALID_JSON", "请求体必须是 JSON")
                    return
                }
                val settings = GatewaySettings.fromJson(body, repository.settings())
                repository.saveSettings(settings)
                json(socket, 200, settings.toJson().put("restartRequired", true))
            }

            request.method == "POST" && path == "/api/v1/settings/token/rotate" -> {
                json(socket, 200, JSONObject().put("token", repository.rotateApiToken()))
            }

            request.method == "GET" && path == "/api/v1/webhook" -> {
                val config = repository.webhookConfig()
                val safe = config.toJson().apply {
                    remove("token")
                    put("tokenConfigured", config.token.isNotBlank())
                }
                json(socket, 200, safe)
            }

            request.method == "GET" && path == "/api/v1/webhook/logs" -> {
                val limit = queryInt(request.target, "limit", 100)
                val records = JSONArray().apply {
                    repository.listWebhookLogs(limit).forEach { put(it.toJson()) }
                }
                json(socket, 200, JSONObject().put("data", records).put("count", records.length()))
            }

            request.method == "DELETE" && path == "/api/v1/webhook/logs" -> {
                val deletedCount = repository.clearWebhookLogs()
                json(
                    socket,
                    200,
                    JSONObject()
                        .put("deletedCount", deletedCount)
                        .put("message", "Webhook 记录已清理")
                )
            }

            request.method == "PUT" && path == "/api/v1/webhook" -> {
                val body = parseJson(request.body) ?: run {
                    error(socket, 400, "INVALID_JSON", "请求体必须是 JSON")
                    return
                }
                val current = repository.webhookConfig()
                val url = body.optString("url", current.url).trim()
                if (url.isNotBlank() && !url.startsWith("http://") && !url.startsWith("https://")) {
                    error(socket, 400, "INVALID_WEBHOOK_URL", "Webhook 地址必须以 http:// 或 https:// 开头")
                    return
                }
                val events = mutableSetOf<String>()
                body.optJSONArray("events")?.let { values ->
                    for (index in 0 until values.length()) {
                        values.optString(index).takeIf { it.isNotBlank() }?.let(events::add)
                    }
                }
                val config = WebhookConfig(
                    enabled = body.optBoolean("enabled", current.enabled),
                    url = url,
                    token = body.optString("token")
                        .ifBlank { body.optString("secret") }
                        .ifBlank { current.token },
                    events = if (events.isEmpty()) current.events else events,
                    maxRetries = body.optInt("maxRetries", current.maxRetries).coerceIn(0, 10),
                    initialDelaySeconds = body.optInt("initialDelaySeconds", current.initialDelaySeconds).coerceIn(1, 300),
                    maxDelaySeconds = body.optInt("maxDelaySeconds", current.maxDelaySeconds).coerceIn(1, 3600)
                )
                repository.saveWebhookConfig(config)
                json(socket, 200, config.toJson().apply {
                    remove("token")
                    put("tokenConfigured", config.token.isNotBlank())
                })
            }

            request.method == "POST" && path == "/api/v1/webhook/test" -> {
                WebhookDispatcher.enqueueTest(appContext)
                json(socket, 202, JSONObject().put("message", "测试回调已加入队列"))
            }

            else -> error(socket, 404, "NOT_FOUND", "接口不存在")
        }
    }

    private fun serveAsset(socket: Socket, path: String) {
        val assetName = when (path) {
            "/", "/index.html" -> "index.html"
            "/styles.css" -> "styles.css"
            "/app.js" -> "app.js"
            else -> "index.html"
        }
        val contentType = when {
            assetName.endsWith(".css") -> "text/css; charset=utf-8"
            assetName.endsWith(".js") -> "application/javascript; charset=utf-8"
            else -> "text/html; charset=utf-8"
        }
        try {
            val content = appContext.assets.open("web/$assetName").use { it.readBytes() }
            respond(socket, 200, "OK", String(content, StandardCharsets.UTF_8), contentType)
        } catch (_: Exception) {
            error(socket, 404, "NOT_FOUND", "页面不存在")
        }
    }

    private fun readRequest(input: InputStream): HttpRequest? {
        val headerBytes = ByteArrayOutputStream()
        var previous3 = -1
        var previous2 = -1
        var previous1 = -1
        while (headerBytes.size() < MAX_HEADER_BYTES) {
            val current = input.read()
            if (current < 0) return null
            headerBytes.write(current)
            if (previous3 == '\r'.code && previous2 == '\n'.code && previous1 == '\r'.code && current == '\n'.code) {
                break
            }
            previous3 = previous2
            previous2 = previous1
            previous1 = current
        }

        val lines = headerBytes.toString(StandardCharsets.UTF_8.name()).split("\r\n")
        val requestLine = lines.firstOrNull()?.split(" ") ?: return null
        if (requestLine.size < 2) return null
        val headers = lines.drop(1)
            .filter { it.contains(":") }
            .associate { line ->
                val separator = line.indexOf(':')
                line.substring(0, separator).trim().lowercase() to line.substring(separator + 1).trim()
            }
        val isChunked = headers["transfer-encoding"]?.contains("chunked", ignoreCase = true) == true
        val bodyText = if (isChunked) readChunkedBody(input) else readFixedBody(input, headers["content-length"]?.toIntOrNull()?.coerceIn(0, MAX_BODY_BYTES) ?: 0)
        return HttpRequest(
            method = requestLine[0].uppercase(),
            target = requestLine[1],
            headers = headers,
            body = bodyText
        )
    }

    private fun readFixedBody(input: InputStream, contentLength: Int): String {
        if (contentLength <= 0) return ""
        val bodyBytes = ByteArray(contentLength)
        var offset = 0
        while (offset < contentLength) {
            val count = input.read(bodyBytes, offset, contentLength - offset)
            if (count < 0) break
            offset += count
        }
        return String(bodyBytes, 0, offset, StandardCharsets.UTF_8)
    }

    private fun readChunkedBody(input: InputStream): String {
        val out = ByteArrayOutputStream()
        var total = 0
        while (true) {
            val line = readLineStrict(input) ?: break
            val sizePart = line.substringBefore(";").trim()
            val chunkSize = sizePart.toIntOrNull(16) ?: break
            if (chunkSize == 0) { readLineStrict(input); break }
            if (chunkSize < 0 || total + chunkSize > MAX_BODY_BYTES) break
            var remaining = chunkSize
            val buf = ByteArray(8192)
            while (remaining > 0) {
                val want = minOf(buf.size, remaining)
                val count = input.read(buf, 0, want)
                if (count < 0) break
                out.write(buf, 0, count)
                remaining -= count
                total += count
            }
            readLineStrict(input)
            if (total >= MAX_BODY_BYTES) break
        }
        return out.toString(StandardCharsets.UTF_8.name())
    }

    private fun readLineStrict(input: InputStream): String? {
        val line = ByteArrayOutputStream()
        var prev = -1
        while (line.size() < 8192) {
            val cur = input.read()
            if (cur < 0) return if (line.size() == 0) null else line.toString(StandardCharsets.UTF_8.name())
            if (prev == 13 && cur == 10) {
                val bytes = line.toByteArray()
                return String(bytes, 0, bytes.size - 1, StandardCharsets.UTF_8)
            }
            line.write(cur)
            prev = cur
        }
        return null
    }

    private fun parseJson(body: String): JSONObject? = try {
        JSONObject(body)
    } catch (_: Exception) {
        null
    }

    private fun queryInt(target: String, name: String, default: Int): Int {
        val query = target.substringAfter('?', "")
        return query.split('&')
            .asSequence()
            .mapNotNull { item ->
                val pair = item.split('=', limit = 2)
                if (pair.size == 2 && URLDecoder.decode(pair[0], "UTF-8") == name) pair[1].toIntOrNull() else null
            }
            .firstOrNull()
            ?.coerceIn(1, 500)
            ?: default
    }

    private fun buildStats(): JSONObject {
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val today = now.atZone(zone).toLocalDate()
        val sent = repository.listSent(500)
        val received = repository.listReceived(500)
        val webhookLogs = repository.listWebhookLogs(500)
        val todaySentRecords = sent.filter { it.createdAt.toLocalDate(zone) == today }
        val todaySentFailed = todaySentRecords.count { it.status == "failed" }
        val todaySentCompleted = todaySentRecords.count { it.status == "sent" || it.status == "delivered" }
        val todaySentFinal = todaySentCompleted + todaySentFailed
        val todaySentSuccessRate = if (todaySentFinal > 0) {
            todaySentCompleted.toDouble() * 100.0 / todaySentFinal.toDouble()
        } else {
            null
        }
        val hourStart = now.atZone(zone).truncatedTo(ChronoUnit.HOURS).toInstant()
        val trendStart = hourStart.minusSeconds(23L * 60L * 60L)
        val hourFormatter = DateTimeFormatter.ofPattern("MM-dd HH:00")

        val trend = JSONArray()
        repeat(24) { index ->
            val bucketStart = trendStart.plusSeconds(index.toLong() * 60L * 60L)
            val bucketEnd = bucketStart.plusSeconds(60L * 60L)
            trend.put(
                JSONObject()
                    .put("label", bucketStart.atZone(zone).format(hourFormatter))
                    .put("sent", sent.count { it.createdAt.inRange(bucketStart, bucketEnd) })
                    .put("received", received.count { it.receivedAt.inRange(bucketStart, bucketEnd) })
            )
        }

        val sentById = sent.associateBy { it.messageId }
        val receivedById = received.associateBy { it.messageId }
        val activities = mutableListOf<StatsActivity>()
        sent.forEach { record ->
            val timestamp = record.sentAt ?: record.createdAt
            activities += StatsActivity(
                timestamp = timestamp,
                type = "发送短信",
                detail = "发送至 ${maskNumber(record.to)}",
                status = record.status,
                statusKind = "sent",
                messageId = record.messageId,
                relatedType = "sent"
            )
        }
        received.forEach { record ->
            activities += StatsActivity(
                timestamp = record.receivedAt,
                type = "接收短信",
                detail = "来自 ${maskNumber(record.from)}",
                status = "received",
                statusKind = "received",
                messageId = record.messageId,
                relatedType = "received"
            )
        }
        webhookLogs.forEach { record ->
            val relatedType = when (record.event) {
                "sms.received" -> "received"
                "sms.sent", "sms.delivered", "sms.failed" -> "sent"
                else -> null
            }
            val relatedDetail = when (relatedType) {
                "sent" -> record.messageId?.let { sentById[it]?.let { message -> "发送至 ${maskNumber(message.to)}" } }
                "received" -> record.messageId?.let { receivedById[it]?.let { message -> "来自 ${maskNumber(message.from)}" } }
                else -> null
            }
            activities += StatsActivity(
                timestamp = record.occurredAt,
                type = "Webhook",
                detail = "${webhookEventLabel(record.event)} · ${record.detail}",
                status = record.status,
                statusKind = "webhook",
                messageId = record.messageId,
                relatedType = relatedType,
                relatedDetail = relatedDetail
            )
        }

        val activityJson = JSONArray().apply {
            activities
                .sortedByDescending { it.timestamp.toInstantOrNull() ?: Instant.EPOCH }
                // Keep enough recent events for the web console to merge a message
                // with a Webhook delivery that happened a little later.
                .take(50)
                .forEach { put(it.toJson()) }
        }
        return JSONObject()
            .put("date", today.toString())
            .put("todaySent", todaySentRecords.size)
            .put("todayReceived", received.count { it.receivedAt.toLocalDate(zone) == today })
            .put("todaySentSuccessRate", todaySentSuccessRate ?: JSONObject.NULL)
            .put("todaySentFailed", todaySentFailed)
            .put("trend", trend)
            .put("activities", activityJson)
    }

    private fun maskNumber(value: String): String {
        val normalized = value.trim()
        return if (normalized.length > 7) {
            "${normalized.take(3)}****${normalized.takeLast(4)}"
        } else {
            normalized
        }
    }

    private fun webhookEventLabel(event: String): String = when (event) {
        "sms.received" -> "收信回调"
        "sms.sent" -> "发送回调"
        "sms.delivered" -> "送达回调"
        "sms.failed" -> "失败回调"
        "webhook.test" -> "测试回调"
        else -> "Webhook"
    }

    private data class StatsActivity(
        val timestamp: String,
        val type: String,
        val detail: String,
        val status: String,
        val statusKind: String,
        val messageId: String? = null,
        val relatedType: String? = null,
        val relatedDetail: String? = null
    ) {
        fun toJson(): JSONObject = JSONObject()
            .put("timestamp", timestamp)
            .put("type", type)
            .put("detail", detail)
            .put("status", status)
            .put("statusKind", statusKind)
            .put("messageId", messageId ?: JSONObject.NULL)
            .put("relatedType", relatedType ?: JSONObject.NULL)
            .put("relatedDetail", relatedDetail ?: JSONObject.NULL)
    }

    private fun String.toInstantOrNull(): Instant? =
        runCatching { Instant.parse(this) }.getOrNull()

    private fun String.inRange(start: Instant, end: Instant): Boolean =
        toInstantOrNull()?.let { !it.isBefore(start) && it.isBefore(end) } == true

    private fun String.toLocalDate(zone: ZoneId): LocalDate? =
        toInstantOrNull()?.atZone(zone)?.toLocalDate()

    private fun json(socket: Socket, status: Int, body: JSONObject) {
        respond(socket, status, reason(status), body.toString(), "application/json; charset=utf-8")
    }

    private fun error(socket: Socket, status: Int, code: String, message: String) {
        json(socket, status, JSONObject().put("code", code).put("message", message))
    }

    private fun respond(
        socket: Socket,
        status: Int,
        statusText: String,
        body: String,
        contentType: String,
        additionalHeaders: Map<String, String> = emptyMap()
    ) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val headers = buildString {
            append("HTTP/1.1 $status $statusText\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Connection: close\r\n")
            append("Cache-Control: no-store\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Access-Control-Allow-Headers: Authorization, Content-Type, Accept, MCP-Protocol-Version, MCP-Session-Id\r\n")
            append("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n")
            additionalHeaders.forEach { (name, value) -> append("$name: $value\r\n") }
            append("\r\n")
        }.toByteArray(StandardCharsets.UTF_8)
        socket.getOutputStream().use { output ->
            output.write(headers)
            output.write(bytes)
            output.flush()
        }
    }

    private fun reason(status: Int): String = when (status) {
        200 -> "OK"
        202 -> "Accepted"
        204 -> "No Content"
        400 -> "Bad Request"
        401 -> "Unauthorized"
        403 -> "Forbidden"
        404 -> "Not Found"
        429 -> "Too Many Requests"
        else -> "Error"
    }

    private data class HttpRequest(
        val method: String,
        val target: String,
        val headers: Map<String, String>,
        val body: String
    )

    companion object {
        private const val MAX_HEADER_BYTES = 64 * 1024
        private const val MAX_BODY_BYTES = 1024 * 1024
    }
}
