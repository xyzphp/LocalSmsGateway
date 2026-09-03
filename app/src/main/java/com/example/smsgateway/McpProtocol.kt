package com.example.smsgateway

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class McpHttpRequest(
    val method: String,
    val headers: Map<String, String>,
    val body: String
)

data class McpHttpResponse(
    val status: Int,
    val body: String = "",
    val contentType: String = "application/json; charset=utf-8",
    val headers: Map<String, String> = emptyMap()
)

/**
 * Small stateless MCP Streamable HTTP implementation for the existing gateway
 * socket. It intentionally reuses the gateway API token and port, so an MCP
 * client can connect to http://phone-ip:port/mcp without another service.
 */
object McpProtocol {
    const val PATH = "/mcp"

    private const val DEFAULT_PROTOCOL_VERSION = "2025-11-25"
    private val SUPPORTED_PROTOCOL_VERSIONS = setOf(
        "2025-03-26",
        "2025-06-18",
        "2025-11-25",
        "2026-07-28"
    )

    fun handle(
        context: Context,
        repository: GatewayRepository,
        sender: SmsSender,
        request: McpHttpRequest
    ): McpHttpResponse {
        if (request.method == "GET") {
            // This stateless implementation does not publish server-initiated
            // messages, so a GET SSE stream is not needed.
            return McpHttpResponse(
                status = 405,
                body = "",
                contentType = "text/plain; charset=utf-8",
                headers = mapOf("Allow" to "POST")
            )
        }
        if (request.method != "POST") {
            return McpHttpResponse(
                status = 405,
                body = "",
                contentType = "text/plain; charset=utf-8",
                headers = mapOf("Allow" to "POST")
            )
        }

        val protocolVersion = request.headers["mcp-protocol-version"]
        if (protocolVersion != null && protocolVersion !in SUPPORTED_PROTOCOL_VERSIONS) {
            return rpcError(
                id = null,
                code = -32600,
                message = "不支持的 MCP 协议版本：$protocolVersion",
                httpStatus = 400
            )
        }

        val rpc = try {
            JSONObject(request.body)
        } catch (_: Exception) {
            return rpcError(null, -32700, "请求体不是有效的 JSON", 400)
        }
        val method = rpc.optString("method").trim()
        if (method.isBlank()) return rpcError(null, -32600, "缺少 JSON-RPC method", 400)

        val id = if (rpc.has("id")) rpc.opt("id") else null
        if (method.startsWith("notifications/")) {
            return accepted()
        }

        return try {
            when (method) {
                "initialize" -> initialize(context, id, rpc.optJSONObject("params"))
                "ping" -> rpcResult(id, JSONObject())
                "tools/list" -> rpcResult(id, JSONObject().put("tools", toolDefinitions()))
                "resources/list" -> rpcResult(id, JSONObject().put("resources", JSONArray()))
                "prompts/list" -> rpcResult(id, JSONObject().put("prompts", JSONArray()))
                "tools/call" -> callTool(context, repository, sender, id, rpc.optJSONObject("params"))
                else -> rpcError(id, -32601, "不支持的 MCP 方法：$method")
            }
        } catch (error: IllegalArgumentException) {
            rpcError(id, -32602, error.message ?: "请求参数无效")
        } catch (error: Exception) {
            rpcError(id, -32603, error.message ?: "MCP Server 内部错误")
        }
    }

    private fun initialize(context: Context, id: Any?, params: JSONObject?): McpHttpResponse {
        val requestedVersion = params?.optString("protocolVersion").orEmpty()
        val selectedVersion = requestedVersion
            .takeIf { it in SUPPORTED_PROTOCOL_VERSIONS }
            ?: DEFAULT_PROTOCOL_VERSION
        val serverVersion = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "0.1.0"
        val result = JSONObject()
            .put("protocolVersion", selectedVersion)
            .put(
                "capabilities",
                JSONObject()
                    .put("tools", JSONObject().put("listChanged", false))
            )
            .put(
                "serverInfo",
                JSONObject()
                    .put("name", "local-sms-gateway")
                    .put("version", serverVersion)
            )
            .put("instructions", "这是运行在 Android 手机上的本地短信网关。发送短信前请确认收件人和短信内容。")
        return rpcResult(
            id,
            result,
            headers = mapOf("MCP-Protocol-Version" to selectedVersion)
        )
    }

    private fun toolDefinitions(): JSONArray = JSONArray().apply {
        put(tool("get_gateway_status", "读取短信网关在线状态、局域网地址和计数。", objectSchema()))
        put(tool("get_gateway_stats", "读取今日收发统计、24 小时趋势和近期活动。", objectSchema()))
        put(tool("list_sim_cards", "读取当前手机上的可用 SIM 卡。", objectSchema()))
        put(
            tool(
                "send_sms",
                "通过手机运营商短信能力发送短信。to 应包含国家区号，例如 +8613800138000。",
                objectSchema(
                    properties = JSONObject()
                        .put("to", stringProperty("收件人号码，建议使用 E.164 格式。"))
                        .put("text", stringProperty("短信正文。"))
                        .put("subscriptionId", integerProperty("可选的 SIM subscriptionId。"))
                        .put("clientRequestId", stringProperty("可选的客户端请求 ID。")),
                    required = JSONArray().put("to").put("text")
                )
            )
        )
        put(
            tool(
                "list_sent_messages",
                "读取发送短信记录。",
                objectSchema(
                    properties = JSONObject().put("limit", integerProperty("返回条数，范围 1-500，默认 100。", 100, 1, 500))
                )
            )
        )
        put(
            tool(
                "list_received_messages",
                "读取收到的短信记录。",
                objectSchema(
                    properties = JSONObject().put("limit", integerProperty("返回条数，范围 1-500，默认 100。", 100, 1, 500))
                )
            )
        )
        put(tool("get_webhook_config", "读取 Webhook 配置，不返回 Webhook Token 明文。", objectSchema()))
        put(
            tool(
                "update_webhook",
                "更新收信和短信状态 Webhook 配置。省略 token 时保持原值。",
                objectSchema(
                    properties = JSONObject()
                        .put("enabled", booleanProperty("是否启用 Webhook。"))
                        .put("url", stringProperty("Webhook HTTP/HTTPS 地址。"))
                        .put("token", stringProperty("Webhook 鉴权 Token。"))
                        .put("events", arrayStringProperty("订阅事件，例如 sms.received、sms.sent、sms.delivered、sms.failed。"))
                        .put("maxRetries", integerProperty("最大重试次数，范围 0-10。", 5, 0, 10))
                        .put("initialDelaySeconds", integerProperty("首次重试间隔秒数。", 2, 1, 300))
                        .put("maxDelaySeconds", integerProperty("最大重试间隔秒数。", 60, 1, 3600))
                )
            )
        )
        put(
            tool(
                "list_webhook_logs",
                "读取 Webhook 投递日志。",
                objectSchema(
                    properties = JSONObject().put("limit", integerProperty("返回条数，范围 1-500，默认 100。", 100, 1, 500))
                )
            )
        )
        put(tool("test_webhook", "加入一次 Webhook 测试回调任务。", objectSchema()))
        put(tool("get_gateway_settings", "读取短信网关设置。", objectSchema()))
        put(
            tool(
                "update_gateway_settings",
                "更新短信网关设置。修改端口后需要重启服务才会生效。",
                objectSchema(
                    properties = JSONObject()
                        .put("port", integerProperty("服务端口，范围 1024-65535。", 8080, 1024, 65535))
                        .put("autoStart", booleanProperty("是否随系统启动服务。"))
                        .put("defaultSubscriptionId", integerProperty("默认 SIM subscriptionId。"))
                        .put("sendRatePerMinute", integerProperty("每分钟发送上限。", 60, 1, 300))
                        .put("sentRetentionDays", integerProperty("发送记录保留天数。", 90, 1, 3650))
                        .put("receivedRetentionDays", integerProperty("收信记录保留天数。", 90, 1, 3650))
                )
            )
        )
        put(tool("clear_sent_records", "永久清理全部发送记录。必须显式传入 confirm=true。", confirmationSchema()))
        put(tool("clear_received_records", "永久清理全部收信记录。必须显式传入 confirm=true。", confirmationSchema()))
        put(tool("clear_webhook_logs", "永久清理全部 Webhook 日志。必须显式传入 confirm=true。", confirmationSchema()))
    }

    private fun callTool(
        context: Context,
        repository: GatewayRepository,
        sender: SmsSender,
        id: Any?,
        params: JSONObject?
    ): McpHttpResponse {
        val values = params ?: JSONObject()
        val name = values.optString("name").trim()
        val args = values.optJSONObject("arguments") ?: JSONObject()
        if (name.isBlank()) return rpcError(id, -32602, "缺少 tools/call 的 name")

        val result = when (name) {
            "get_gateway_status" -> toolJson(statusJson(context, repository))
            "get_gateway_stats" -> toolJson(statsJson(repository))
            "list_sim_cards" -> toolJson(
                JSONObject().put("data", JSONArray().apply {
                    SubscriptionUtils.activeSubscriptions(context).forEach { put(it.toJson()) }
                })
            )
            "send_sms" -> sendSmsTool(repository, sender, args)
            "list_sent_messages" -> toolJson(
                recordsJson(repository.listSent(limit(args)))
            )
            "list_received_messages" -> toolJson(
                recordsJson(repository.listReceived(limit(args)))
            )
            "get_webhook_config" -> toolJson(safeWebhookConfig(repository.webhookConfig()))
            "update_webhook" -> updateWebhookTool(repository, args)
            "list_webhook_logs" -> toolJson(recordsJson(repository.listWebhookLogs(limit(args))))
            "test_webhook" -> {
                WebhookDispatcher.enqueueTest(context)
                toolJson(JSONObject().put("message", "测试回调已加入队列"))
            }
            "get_gateway_settings" -> toolJson(repository.settings().toJson())
            "update_gateway_settings" -> {
                val settings = GatewaySettings.fromJson(args, repository.settings())
                repository.saveSettings(settings)
                toolJson(settings.toJson().put("restartRequired", true))
            }
            "clear_sent_records" -> clearTool(args, "发送记录") { repository.clearSentRecords() }
            "clear_received_records" -> clearTool(args, "收信记录") { repository.clearReceivedRecords() }
            "clear_webhook_logs" -> clearTool(args, "Webhook 日志") { repository.clearWebhookLogs() }
            else -> return rpcError(id, -32602, "未知工具：$name")
        }
        return rpcResult(id, result)
    }

    private fun sendSmsTool(
        repository: GatewayRepository,
        sender: SmsSender,
        args: JSONObject
    ): JSONObject {
        val to = requiredString(args, "to")
        val text = requiredString(args, "text")
        val subscriptionId = optionalInt(args, "subscriptionId")
        val clientRequestId = args.optString("clientRequestId").trim().takeIf { it.isNotBlank() }
        val record = sender.send(to, text, subscriptionId, clientRequestId)
        if (record.status == "failed" && record.errorCode == -1) {
            return toolFailure("尚未授予短信发送权限")
        }
        if (record.status == "failed" && record.errorCode == SmsSender.ERROR_RATE_LIMITED) {
            return toolFailure("发送过于频繁，请稍后重试")
        }
        return toolJson(record.toJson())
    }

    private fun updateWebhookTool(repository: GatewayRepository, args: JSONObject): JSONObject {
        val current = repository.webhookConfig()
        val url = if (args.has("url")) args.optString("url").trim() else current.url
        if (url.isNotBlank() && !url.startsWith("http://") && !url.startsWith("https://")) {
            return toolFailure("Webhook 地址必须以 http:// 或 https:// 开头")
        }
        val events = if (!args.has("events")) {
            current.events
        } else {
            val values = args.optJSONArray("events")
            val parsed = mutableSetOf<String>()
            if (values != null) {
                for (index in 0 until values.length()) {
                    values.optString(index).takeIf { it.isNotBlank() }?.let(parsed::add)
                }
            }
            parsed.ifEmpty { current.events }
        }
        val tokenValue = args.optString("token").trim()
        val config = WebhookConfig(
            enabled = if (args.has("enabled")) args.optBoolean("enabled") else current.enabled,
            url = url,
            token = tokenValue.ifBlank { current.token },
            events = events,
            maxRetries = args.optInt("maxRetries", current.maxRetries).coerceIn(0, 10),
            initialDelaySeconds = args.optInt("initialDelaySeconds", current.initialDelaySeconds).coerceIn(1, 300),
            maxDelaySeconds = args.optInt("maxDelaySeconds", current.maxDelaySeconds).coerceIn(1, 3600)
        )
        repository.saveWebhookConfig(config)
        return toolJson(safeWebhookConfig(config))
    }

    private fun clearTool(args: JSONObject, label: String, action: () -> Int): JSONObject {
        if (!args.optBoolean("confirm", false)) {
            return toolFailure("清理${label}是不可逆操作，请显式传入 confirm=true")
        }
        val count = action()
        return toolJson(JSONObject().put("deletedCount", count).put("message", "${label}已清理"))
    }

    private fun statusJson(context: Context, repository: GatewayRepository): JSONObject {
        val config = repository.webhookConfig()
        return JSONObject()
            .put("online", true)
            .put("deviceName", "Android SMS Gateway")
            .put("localAddress", NetworkUtils.baseUrl(context, repository.serverPort()))
            .put("wifiName", "当前 Wi-Fi")
            .put("port", repository.serverPort())
            .put("sentCount", repository.sentCount())
            .put("receivedCount", repository.receivedCount())
            .put("webhookEnabled", config.enabled && config.url.isNotBlank())
    }

    private fun statsJson(repository: GatewayRepository): JSONObject {
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val today = now.atZone(zone).toLocalDate()
        val sent = repository.listSent(500)
        val received = repository.listReceived(500)
        val webhookLogs = repository.listWebhookLogs(500)
        val todaySent = sent.filter { parseInstant(it.createdAt)?.atZone(zone)?.toLocalDate() == today }
        val todaySentFailed = todaySent.count { it.status == "failed" }
        val todaySentCompleted = todaySent.count { it.status == "sent" || it.status == "delivered" }
        val finalCount = todaySentCompleted + todaySentFailed
        val successRate = if (finalCount > 0) todaySentCompleted.toDouble() * 100.0 / finalCount else null

        val hourStart = now.truncatedTo(ChronoUnit.HOURS)
        val trendStart = hourStart.minus(23, ChronoUnit.HOURS)
        val trend = JSONArray()
        repeat(24) { index ->
            val start = trendStart.plus(index.toLong(), ChronoUnit.HOURS)
            val end = start.plus(1, ChronoUnit.HOURS)
            trend.put(
                JSONObject()
                    .put("label", start.atZone(zone).toString().replace(Regex(":\\d{2}Z$"), ":00"))
                    .put("sent", sent.count { timestampInRange(it.createdAt, start, end) })
                    .put("received", received.count { timestampInRange(it.receivedAt, start, end) })
            )
        }

        data class Activity(val timestamp: Instant, val value: JSONObject)
        val activities = mutableListOf<Activity>()
        sent.forEach { record ->
            parseInstant(record.sentAt ?: record.createdAt)?.let { timestamp ->
                activities += Activity(
                    timestamp,
                    JSONObject()
                        .put("timestamp", timestamp.toString())
                        .put("type", "发送短信")
                        .put("detail", "发送至 ${maskNumber(record.to)}")
                        .put("status", record.status)
                        .put("statusKind", "sent")
                )
            }
        }
        received.forEach { record ->
            parseInstant(record.receivedAt)?.let { timestamp ->
                activities += Activity(
                    timestamp,
                    JSONObject()
                        .put("timestamp", timestamp.toString())
                        .put("type", "接收短信")
                        .put("detail", "来自 ${maskNumber(record.from)}")
                        .put("status", "received")
                        .put("statusKind", "received")
                )
            }
        }
        webhookLogs.forEach { record ->
            parseInstant(record.occurredAt)?.let { timestamp ->
                activities += Activity(
                    timestamp,
                    JSONObject()
                        .put("timestamp", timestamp.toString())
                        .put("type", "Webhook")
                        .put("detail", "${webhookEventLabel(record.event)} · ${record.detail}")
                        .put("status", record.status)
                        .put("statusKind", "webhook")
                )
            }
        }
        val activityJson = JSONArray().apply {
            activities.sortedByDescending { it.timestamp }.take(12).forEach { put(it.value) }
        }
        return JSONObject()
            .put("date", today.toString())
            .put("todaySent", todaySent.size)
            .put("todayReceived", received.count { parseInstant(it.receivedAt)?.atZone(zone)?.toLocalDate() == today })
            .put("todaySentSuccessRate", successRate ?: JSONObject.NULL)
            .put("todaySentFailed", todaySentFailed)
            .put("trend", trend)
            .put("activities", activityJson)
    }

    private fun recordsJson(records: List<Any>): JSONObject = JSONObject().apply {
        put("data", JSONArray().apply {
            records.forEach { record ->
                when (record) {
                    is SentRecord -> put(record.toJson())
                    is ReceivedRecord -> put(record.toJson())
                    is WebhookLogRecord -> put(record.toJson())
                }
            }
        })
        put("count", records.size)
    }

    private fun safeWebhookConfig(config: WebhookConfig): JSONObject = config.toJson().apply {
        remove("token")
        put("tokenConfigured", config.token.isNotBlank())
    }

    private fun requiredString(args: JSONObject, key: String): String {
        val value = args.optString(key).trim()
        require(value.isNotBlank()) { "$key 不能为空" }
        return value
    }

    private fun optionalInt(args: JSONObject, key: String): Int? =
        if (!args.has(key) || args.isNull(key)) null else args.optInt(key)

    private fun limit(args: JSONObject): Int = args.optInt("limit", 100).coerceIn(1, 500)

    private fun parseInstant(value: String): Instant? = runCatching { Instant.parse(value) }.getOrNull()

    private fun timestampInRange(value: String, start: Instant, end: Instant): Boolean =
        parseInstant(value)?.let { !it.isBefore(start) && it.isBefore(end) } == true

    private fun maskNumber(value: String): String {
        val normalized = value.trim()
        return if (normalized.length > 7) "${normalized.take(3)}****${normalized.takeLast(4)}" else normalized
    }

    private fun webhookEventLabel(event: String): String = when (event) {
        "sms.received" -> "收信回调"
        "sms.sent" -> "发送回调"
        "sms.delivered" -> "送达回调"
        "sms.failed" -> "失败回调"
        "webhook.test" -> "测试回调"
        else -> "Webhook"
    }

    private fun tool(name: String, description: String, inputSchema: JSONObject): JSONObject = JSONObject()
        .put("name", name)
        .put("description", description)
        .put("inputSchema", inputSchema)

    private fun objectSchema(
        properties: JSONObject = JSONObject(),
        required: JSONArray = JSONArray()
    ): JSONObject = JSONObject()
        .put("type", "object")
        .put("properties", properties)
        .put("required", required)
        .put("additionalProperties", false)

    private fun confirmationSchema(): JSONObject = objectSchema(
        properties = JSONObject().put("confirm", booleanProperty("确认执行不可逆清理。")),
        required = JSONArray().put("confirm")
    )

    private fun stringProperty(description: String): JSONObject = JSONObject()
        .put("type", "string")
        .put("description", description)

    private fun integerProperty(
        description: String,
        default: Int? = null,
        minimum: Int? = null,
        maximum: Int? = null
    ): JSONObject = JSONObject().apply {
        put("type", "integer")
        put("description", description)
        if (default != null) put("default", default)
        if (minimum != null) put("minimum", minimum)
        if (maximum != null) put("maximum", maximum)
    }

    private fun booleanProperty(description: String): JSONObject = JSONObject()
        .put("type", "boolean")
        .put("description", description)

    private fun arrayStringProperty(description: String): JSONObject = JSONObject()
        .put("type", "array")
        .put("description", description)
        .put("items", JSONObject().put("type", "string"))

    private fun toolJson(value: JSONObject): JSONObject = toolText(value.toString())

    private fun toolText(text: String): JSONObject = JSONObject()
        .put("content", JSONArray().put(JSONObject().put("type", "text").put("text", text)))
        .put("isError", false)

    private fun toolFailure(message: String): JSONObject = JSONObject()
        .put("content", JSONArray().put(JSONObject().put("type", "text").put("text", message)))
        .put("isError", true)

    private fun rpcResult(
        id: Any?,
        result: JSONObject,
        headers: Map<String, String> = emptyMap()
    ): McpHttpResponse = McpHttpResponse(
        status = 200,
        body = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", id ?: JSONObject.NULL)
            .put("result", result)
            .toString(),
        headers = headers
    )

    private fun rpcError(
        id: Any?,
        code: Int,
        message: String,
        httpStatus: Int = 200
    ): McpHttpResponse = McpHttpResponse(
        status = httpStatus,
        body = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", id ?: JSONObject.NULL)
            .put("error", JSONObject().put("code", code).put("message", message))
            .toString()
    )

    private fun accepted(): McpHttpResponse = McpHttpResponse(
        status = 202,
        body = "",
        contentType = "text/plain; charset=utf-8"
    )
}
