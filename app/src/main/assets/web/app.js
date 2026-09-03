const NAV_ITEMS = [
    { id: "overview", label: "概览", icon: "overview" },
    { id: "sent", label: "发送记录", icon: "sent" },
    { id: "received", label: "收信记录", icon: "received" },
    { id: "webhook", label: "Webhook", icon: "webhook" },
    { id: "mcp", label: "MCP Server", icon: "mcp" },
    { id: "docs", label: "API 文档", icon: "docs" },
    { id: "settings", label: "设置", icon: "settings" }
];

const APP_VERSION = "0.1.72";

function uiIcon(name, className = "") {
    const icons = {
        overview: '<rect x="4" y="4" width="6" height="6" rx="1.2"/><rect x="14" y="4" width="6" height="6" rx="1.2"/><rect x="4" y="14" width="6" height="6" rx="1.2"/><rect x="14" y="14" width="6" height="6" rx="1.2"/>',
        sent: '<path d="M4 11.5 20 4l-4.5 16-4.1-6.1L4 11.5Z"/><path d="m11.4 13.9 6.1-6.1"/>',
        received: '<rect x="4" y="5" width="16" height="14" rx="2"/><path d="m5 7 7 5.5L19 7"/>',
        webhook: '<circle cx="6" cy="7" r="2.2"/><circle cx="18" cy="7" r="2.2"/><circle cx="12" cy="17" r="2.2"/><path d="m8 7h3.5M14 7H16M7.7 8.7l2.8 5.6M16.3 8.7l-2.8 5.6"/>',
        mcp: '<g fill="none" stroke="currentColor" stroke-width="12" stroke-linecap="round"><path d="M18 84.8528L85.8822 16.9706C95.2548 7.59798 110.451 7.59798 119.823 16.9706C129.196 26.3431 129.196 41.5391 119.823 50.9117L68.5581 102.177"/><path d="M69.2652 101.47L119.823 50.9117C129.196 41.5391 144.392 41.5391 153.765 50.9117L154.118 51.2652C163.491 60.6378 163.491 75.8338 154.118 85.2063L92.7248 146.6C89.6006 149.724 89.6006 154.789 92.7248 157.913L105.331 170.52"/><path d="M102.853 33.9411L52.6482 84.1457C43.2756 93.5183 43.2756 108.714 52.6482 118.087C62.0208 127.459 77.2167 127.459 86.5893 118.087L136.794 67.8822"/></g>',
        docs: '<path d="M6 3.5h8l4 4v13H6z"/><path d="M14 3.5v4h4M9 12h6M9 16h6"/>',
        settings: '<circle cx="12" cy="12" r="3.2"/><path d="M12 2.8v2M12 19.2v2M2.8 12h2M19.2 12h2M5.5 5.5l1.4 1.4M17.1 17.1l1.4 1.4M18.5 5.5l-1.4 1.4M6.9 17.1l-1.4 1.4"/>',
        sun: '<circle cx="12" cy="12" r="3.5"/><path d="M12 2.7v2M12 19.3v2M2.7 12h2M19.3 12h2M5.4 5.4l1.4 1.4M17.2 17.2l1.4 1.4M18.6 5.4l-1.4 1.4M6.8 17.2l-1.4 1.4"/>',
        moon: '<path d="M20 15.2A8.5 8.5 0 0 1 8.8 4a8.5 8.5 0 1 0 11.2 11.2Z"/>',
        chevronLeft: '<path d="m14.5 5-7 7 7 7"/>',
        chevronRight: '<path d="m9.5 5 7 7-7 7"/>',
        sms: '<path d="M13 3h22c5.523 0 10 4.477 10 10v22c0 5.523-4.477 10-10 10H13C7.477 45 3 40.523 3 35V13C3 7.477 7.477 3 13 3Z" fill="#1769E0" stroke="none"/><path d="M12 12h24c2.761 0 5 2.239 5 5v11c0 2.761-2.239 5-5 5H25l-7 6v-6h-6c-2.761 0-5-2.239-5-5V17c0-2.761 2.239-5 5-5Z" fill="white" stroke="none"/><path d="M14 20h20v3H14ZM14 26h13v3H14Z" fill="#1769E0" stroke="none"/><path d="M34 35a5 5 0 1 0 10 0a5 5 0 1 0-10 0Z" fill="#1F9D55" stroke="none"/><path d="M36 35l2 2 4-4 1.5 1.5-5.5 5.5-3.5-3.5Z" fill="white" stroke="none"/>'
    };
    const viewBox = name === "mcp" ? "0 0 180 180" : name === "sms" ? "0 0 48 48" : "0 0 24 24";
    return `<svg class="ui-icon ${className}" viewBox="${viewBox}" aria-hidden="true" focusable="false">${icons[name] || icons.overview}</svg>`;
}

const ENDPOINTS = [
    { method: "GET", operation: "get", path: "/api/v1/status", desc: "服务状态与设备信息" },
    { method: "GET", operation: "get", path: "/api/v1/stats", desc: "真实统计、趋势与近期活动" },
    { method: "GET", operation: "get", path: "/api/v1/sims", desc: "可用 SIM 卡订阅" },
    { method: "POST", operation: "post", path: "/api/v1/sms/send", desc: "发送短信" },
    { method: "GET", operation: "get", path: "/api/v1/sms/sent", desc: "已发送短信记录" },
    { method: "DELETE", operation: "delete", path: "/api/v1/sms/sent", desc: "清理发送记录" },
    { method: "GET", operation: "get", path: "/api/v1/sms/received", desc: "收到的短信记录" },
    { method: "DELETE", operation: "delete", path: "/api/v1/sms/received", desc: "清理收信记录" },
    { method: "GET", operation: "get", path: "/api/v1/webhook", desc: "Webhook 配置查询" },
    { method: "PUT", operation: "put", path: "/api/v1/webhook", desc: "更新 Webhook 配置" },
    { method: "GET", operation: "get", path: "/api/v1/webhook/logs", desc: "Webhook 投递日志" },
    { method: "DELETE", operation: "delete", path: "/api/v1/webhook/logs", desc: "清理 Webhook 日志" },
    { method: "POST", operation: "post", path: "/api/v1/webhook/test", desc: "测试 Webhook 回调" },
    { method: "POST", operation: "post", path: "/webhook/test", desc: "内置测试接收器（仅确认收到）" },
    { method: "GET", operation: "get", path: "/api/v1/settings", desc: "服务设置查询" },
    { method: "PUT", operation: "put", path: "/api/v1/settings", desc: "更新服务设置" },
    { method: "POST", operation: "post", path: "/api/v1/settings/token/rotate", desc: "重新生成 API Token" }
];

function openapiJsonResponse(description, schema) {
    return { description, content: { "application/json": { schema } } };
}

const OPENAPI_SPEC = {
    openapi: "3.1.0",
    info: {
        title: "本地短信网关 API",
        version: APP_VERSION,
        description: "单台 Android 手机在同一局域网内提供的短信发送、收信、状态和 WebHook API。"
    },
    servers: [{
        url: "http://{phoneIp}:8080",
        description: "手机局域网地址",
        variables: { phoneIp: { default: "192.168.0.17", description: "手机当前局域网 IPv4 地址" } }
    }],
    security: [{ bearerAuth: [] }],
    paths: {
        "/api/v1/status": {
            get: {
                operationId: "getStatus",
                summary: "读取服务状态",
                responses: { "200": openapiJsonResponse("服务状态", { "$ref": "#/components/schemas/Status" }) }
            }
        },
        "/api/v1/stats": {
            get: {
                operationId: "getStats",
                summary: "读取真实统计与活动",
                responses: { "200": openapiJsonResponse("统计数据", { "$ref": "#/components/schemas/Stats" }) }
            }
        },
        "/api/v1/sims": {
            get: {
                operationId: "listSims",
                summary: "读取可用 SIM 卡",
                responses: { "200": openapiJsonResponse("SIM 卡列表", { type: "object", properties: { data: { type: "array", items: { "$ref": "#/components/schemas/Sim" } } } }) }
            }
        },
        "/api/v1/sms/send": {
            post: {
                operationId: "sendSms",
                summary: "发送短信",
                requestBody: { required: true, content: { "application/json": { schema: { "$ref": "#/components/schemas/SendSmsRequest" } } } },
                responses: {
                    "202": openapiJsonResponse("已加入发送队列", { "$ref": "#/components/schemas/AcceptedSend" }),
                    "400": openapiJsonResponse("参数错误", { "$ref": "#/components/schemas/Error" }),
                    "403": openapiJsonResponse("短信发送权限未授予", { "$ref": "#/components/schemas/Error" })
                }
            }
        },
        "/api/v1/sms/sent": {
            get: {
                operationId: "listSentSms",
                summary: "查询发送记录",
                parameters: [{ name: "limit", in: "query", schema: { type: "integer", minimum: 1, maximum: 500, default: 100 } }],
                responses: { "200": openapiJsonResponse("发送记录列表", { type: "object", properties: { data: { type: "array", items: { "$ref": "#/components/schemas/SentRecord" } }, count: { type: "integer" } } }) }
            },
            delete: {
                operationId: "clearSentSms",
                summary: "清理全部发送记录",
                responses: { "200": openapiJsonResponse("清理结果", { "$ref": "#/components/schemas/ClearRecordsResponse" }) }
            }
        },
        "/api/v1/sms/received": {
            get: {
                operationId: "listReceivedSms",
                summary: "查询收信记录",
                parameters: [{ name: "limit", in: "query", schema: { type: "integer", minimum: 1, maximum: 500, default: 100 } }],
                responses: { "200": openapiJsonResponse("收信记录列表", { type: "object", properties: { data: { type: "array", items: { "$ref": "#/components/schemas/ReceivedRecord" } }, count: { type: "integer" } } }) }
            },
            delete: {
                operationId: "clearReceivedSms",
                summary: "清理全部收信记录",
                responses: { "200": openapiJsonResponse("清理结果", { "$ref": "#/components/schemas/ClearRecordsResponse" }) }
            }
        },
        "/api/v1/webhook": {
            get: {
                operationId: "getWebhook",
                summary: "读取 Webhook 配置",
                responses: { "200": openapiJsonResponse("Webhook 配置", { "$ref": "#/components/schemas/WebhookConfigResponse" }) }
            },
            put: {
                operationId: "updateWebhook",
                summary: "更新 Webhook 配置",
                requestBody: { required: true, content: { "application/json": { schema: { "$ref": "#/components/schemas/WebhookUpdate" } } } },
                responses: { "200": openapiJsonResponse("更新后的配置", { "$ref": "#/components/schemas/WebhookConfigResponse" }) }
            }
        },
        "/api/v1/webhook/logs": {
            get: {
                operationId: "listWebhookLogs",
                summary: "查询 Webhook 投递日志",
                parameters: [{ name: "limit", in: "query", schema: { type: "integer", minimum: 1, maximum: 500, default: 100 } }],
                responses: { "200": openapiJsonResponse("投递日志列表", { type: "object", properties: { data: { type: "array", items: { "$ref": "#/components/schemas/WebhookLog" } }, count: { type: "integer" } } }) }
            },
            delete: {
                operationId: "clearWebhookLogs",
                summary: "清理全部 Webhook 日志",
                responses: { "200": openapiJsonResponse("清理结果", { "$ref": "#/components/schemas/ClearRecordsResponse" }) }
            }
        },
        "/api/v1/webhook/test": {
            post: {
                operationId: "testWebhook",
                summary: "发送 Webhook 测试事件",
                responses: { "202": openapiJsonResponse("测试事件已加入队列", { type: "object", properties: { message: { type: "string" } } }) }
            }
        },
        "/webhook/test": {
            post: {
                operationId: "receiveBuiltinTestWebhook",
                summary: "接收内置测试 Webhook",
                security: [],
                requestBody: { required: false, content: { "application/json": { schema: { type: "object", additionalProperties: true } } } },
                responses: { "204": { description: "已接收；不执行任何业务逻辑" } }
            }
        },
        "/api/v1/settings": {
            get: {
                operationId: "getSettings",
                summary: "读取服务设置",
                responses: { "200": openapiJsonResponse("服务设置", { "$ref": "#/components/schemas/Settings" }) }
            },
            put: {
                operationId: "updateSettings",
                summary: "更新服务设置",
                requestBody: { required: true, content: { "application/json": { schema: { "$ref": "#/components/schemas/Settings" } } } },
                responses: { "200": openapiJsonResponse("更新后的设置", { "$ref": "#/components/schemas/Settings" }) }
            }
        },
        "/api/v1/settings/token/rotate": {
            post: {
                operationId: "rotateApiToken",
                summary: "重新生成 API Token",
                responses: { "200": openapiJsonResponse("新的 API Token", { type: "object", required: ["token"], properties: { token: { type: "string" } } }) }
            }
        }
    },
    components: {
        securitySchemes: {
            bearerAuth: { type: "http", scheme: "bearer", bearerFormat: "API Token" }
        },
        schemas: {
            Error: { type: "object", required: ["code", "message"], properties: { code: { type: "string" }, message: { type: "string" } } },
            ClearRecordsResponse: { type: "object", required: ["deletedCount"], properties: { deletedCount: { type: "integer", minimum: 0 }, message: { type: "string" } } },
            Status: { type: "object", properties: { online: { type: "boolean" }, deviceName: { type: "string" }, localAddress: { type: "string" }, wifiName: { type: "string" }, port: { type: "integer" }, sentCount: { type: "integer" }, receivedCount: { type: "integer" }, webhookEnabled: { type: "boolean" } } },
            Stats: { type: "object", properties: { date: { type: "string", format: "date" }, todaySent: { type: "integer" }, todayReceived: { type: "integer" }, todaySentSuccessRate: { type: ["number", "null"], minimum: 0, maximum: 100 }, todaySentFailed: { type: "integer" }, trend: { type: "array", items: { type: "object", properties: { label: { type: "string" }, sent: { type: "integer" }, received: { type: "integer" } } } }, activities: { type: "array", items: { type: "object", properties: { timestamp: { type: "string", format: "date-time" }, type: { type: "string" }, detail: { type: "string" }, status: { type: "string" }, statusKind: { type: "string" }, messageId: { type: ["string", "null"] }, relatedType: { type: ["string", "null"], enum: ["sent", "received", null] }, relatedDetail: { type: ["string", "null"] } } } } } },
            Sim: { type: "object", properties: { subscriptionId: { type: ["integer", "null"] }, simSlot: { type: "integer" }, label: { type: "string" }, number: { type: ["string", "null"] } } },
            SendSmsRequest: { type: "object", required: ["to", "text"], properties: { to: { type: "string" }, text: { type: "string" }, subscriptionId: { type: ["integer", "null"] }, clientRequestId: { type: ["string", "null"] } } },
            SentRecord: { type: "object", properties: { messageId: { type: "string" }, to: { type: "string" }, text: { type: "string" }, createdAt: { type: "string", format: "date-time" }, subscriptionId: { type: ["integer", "null"] }, clientRequestId: { type: ["string", "null"] }, status: { type: "string", enum: ["queued", "sending", "sent", "delivered", "failed"] }, sentAt: { type: ["string", "null"], format: "date-time" }, deliveredAt: { type: ["string", "null"], format: "date-time" }, errorCode: { type: ["integer", "null"] } } },
            ReceivedRecord: { type: "object", properties: { messageId: { type: "string" }, from: { type: "string" }, text: { type: "string" }, receivedAt: { type: "string", format: "date-time" }, subscriptionId: { type: ["integer", "null"] }, webhookStatus: { type: "string", enum: ["pending", "delivered", "failed", "not_configured", "disabled", "not_subscribed"] }, webhookRetryCount: { type: "integer" } } },
            WebhookLog: { type: "object", properties: { eventId: { type: "string" }, event: { type: "string" }, occurredAt: { type: "string", format: "date-time" }, messageId: { type: ["string", "null"] }, status: { type: "string", enum: ["delivered", "failed", "not_configured", "disabled", "not_subscribed"] }, attemptCount: { type: "integer" }, responseCode: { type: ["integer", "null"] }, detail: { type: "string" } } },
            WebhookUpdate: { type: "object", properties: { enabled: { type: "boolean" }, url: { type: "string", format: "uri" }, token: { type: "string", writeOnly: true }, events: { type: "array", items: { type: "string", enum: ["sms.received", "sms.sent", "sms.delivered", "sms.failed"] } }, maxRetries: { type: "integer", minimum: 0, maximum: 10 }, initialDelaySeconds: { type: "integer", minimum: 1 }, maxDelaySeconds: { type: "integer", minimum: 1 } } },
            WebhookConfigResponse: { type: "object", properties: { enabled: { type: "boolean" }, url: { type: "string" }, tokenConfigured: { type: "boolean" }, events: { type: "array", items: { type: "string" } }, maxRetries: { type: "integer" }, initialDelaySeconds: { type: "integer" }, maxDelaySeconds: { type: "integer" } } },
            AcceptedSend: { type: "object", properties: { code: { type: "integer", example: 202 }, message: { type: "string" }, data: { "$ref": "#/components/schemas/SentRecord" } } },
            Settings: { type: "object", properties: { port: { type: "integer", minimum: 1024, maximum: 65535 }, autoStart: { type: "boolean" }, defaultSubscriptionId: { type: "integer" }, sendRatePerMinute: { type: "integer" }, sentRetentionDays: { type: "integer" }, receivedRetentionDays: { type: "integer" } } }
        }
    }
};

const DEMO_STATUS = {
    online: false,
    localAddress: "",
    wifiName: "未连接",
    port: 8080,
    sentCount: 0,
    receivedCount: 0,
    webhookEnabled: false
};

const DEMO_STATS = {
    date: "",
    todaySent: 0,
    todayReceived: 0,
    todaySentSuccessRate: null,
    todaySentFailed: 0,
    trend: Array.from({ length: 24 }, (_, index) => ({ label: `${String(index).padStart(2, "0")}:00`, sent: 0, received: 0 })),
    activities: []
};

const DEFAULT_WEBHOOK = {
    enabled: true,
    url: "http://127.0.0.1:8080/webhook/test",
    tokenConfigured: false,
    events: ["sms.received", "sms.sent", "sms.delivered", "sms.failed"],
    maxRetries: 5,
    initialDelaySeconds: 2,
    maxDelaySeconds: 60
};

const DEFAULT_MCP_CONFIG = {
    name: "local-sms-gateway",
    createSkill: false
};

function readStoredToken() {
    try {
        return window.localStorage.getItem("sms-gateway-token") || "";
    } catch (_) {
        return "";
    }
}

function writeStoredToken(value) {
    try {
        window.localStorage.setItem("sms-gateway-token", value);
        return true;
    } catch (_) {
        return false;
    }
}

function readStoredPreference(key, fallback = "") {
    try {
        const value = window.localStorage.getItem(key);
        return value == null ? fallback : value;
    } catch (_) {
        return fallback;
    }
}

function writeStoredPreference(key, value) {
    try {
        window.localStorage.setItem(key, value);
        return true;
    } catch (_) {
        return false;
    }
}

const state = {
    route: getRoute(),
    token: readStoredToken(),
    sidebarCollapsed: readStoredPreference("sms-gateway-sidebar-collapsed", "0") === "1",
    theme: readStoredPreference("sms-gateway-theme", "light") === "dark" ? "dark" : "light",
    status: { ...DEMO_STATUS },
    stats: { ...DEMO_STATS },
    sent: [],
    received: [],
    webhook: { ...DEFAULT_WEBHOOK },
    webhookLogs: [],
    mcpConfig: {
        name: readStoredPreference("sms-gateway-mcp-name", DEFAULT_MCP_CONFIG.name),
        createSkill: readStoredPreference("sms-gateway-mcp-create-skill", "0") === "1"
    },
    settings: { port: 8080, autoStart: false, defaultSubscriptionId: 1, sendRatePerMinute: 60, sentRetentionDays: 90, receivedRetentionDays: 90 },
    sims: [{ subscriptionId: null, simSlot: 1, label: "默认 SIM", number: null }],
    selectedEndpoint: "GET /api/v1/status",
    sentFilters: { query: "", from: "", to: "", status: "all", page: 1 },
    receivedFilters: { query: "", from: "", to: "", status: "all", page: 1 },
    selectedSentId: "",
    selectedReceivedId: "",
    recordDrawer: { kind: "", id: "" },
    docsDrafts: {},
    docsResponse: null,
    docsRequesting: false,
    demo: true,
    loadedRoute: null,
    loadingRoute: null,
    loadGeneration: 0,
    modalOpen: false,
    sendModalOpen: false,
    tokenGeneration: 0,
    authError: ""
};

function getRoute() {
    const route = window.location.hash.replace(/^#/, "");
    return NAV_ITEMS.some((item) => item.id === route) ? route : "overview";
}

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, (char) => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    }[char]));
}

function formatTime(value) {
    if (!value) return "—";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return escapeHtml(value);
    const pad = (part) => String(part).padStart(2, "0");
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function shortText(value, length = 25) {
    const text = String(value ?? "");
    return escapeHtml(text.length > length ? `${text.slice(0, length)}…` : text);
}

function statusChip(status, kind = "sent") {
    const map = kind === "received"
        ? {
            delivered: ["已回调", "green"],
            retrying: ["重试中", "orange"],
            failed: ["回调失败", "red"],
            pending: ["待回调", "blue"],
            not_configured: ["未配置", "gray"],
            disabled: ["已停用", "gray"],
            not_subscribed: ["未订阅", "gray"]
        }
        : kind === "smsReceived"
            ? { received: ["已接收", "green"] }
        : {
            queued: ["已提交", "blue"],
            sending: ["发送中", "blue"],
            sent: ["已发送", "blue"],
            delivered: ["已送达", "green"],
            failed: ["发送失败", "red"]
        };
    const [label, color] = map[status] || [status || "未知", "blue"];
    return `<span class="chip ${color}">${label}</span>`;
}

function webhookAvailability(event) {
    const webhook = state.webhook || {};
    if (!String(webhook.url || "").trim()) return "not_configured";
    if (webhook.enabled === false) return "disabled";
    if (!(webhook.events || []).includes(event)) return "not_subscribed";
    return "";
}

function receivedWebhookStatus(row) {
    const unavailable = webhookAvailability("sms.received");
    if (unavailable) return unavailable;
    return row.webhookStatus || "pending";
}

function sentWebhookEvent(row) {
    return row?.status === "delivered"
        ? "sms.delivered"
        : row?.status === "failed"
            ? "sms.failed"
            : "sms.sent";
}

function sentWebhookLog(row) {
    const messageId = String(row?.messageId || "");
    const event = sentWebhookEvent(row);
    return (state.webhookLogs || []).find((log) =>
        String(log?.messageId || "") === messageId && log?.event === event
    ) || null;
}

function sentWebhookStatus(row) {
    if (row?.webhookStatus) return row.webhookStatus;
    const unavailable = webhookAvailability(sentWebhookEvent(row));
    if (unavailable) return unavailable;
    return sentWebhookLog(row)?.status || "pending";
}

function receivedSmsStatusChip() {
    return statusChip("received", "smsReceived");
}

function webhookEventLabel(event) {
    return {
        "sms.received": "收信通知",
        "sms.sent": "发送通知",
        "sms.delivered": "送达通知",
        "sms.failed": "失败通知",
        "webhook.test": "测试回调"
    }[event] || event || "Webhook";
}

function webhookStatusLabel(status) {
    return {
        delivered: "已回调",
        failed: "回调失败",
        not_configured: "未配置",
        disabled: "已停用",
        not_subscribed: "未订阅",
        pending: "待回调",
        retrying: "重试中"
    }[status] || status || "未知";
}

function webhookAttemptSummary(row, status) {
    if (["not_configured", "disabled", "not_subscribed"].includes(status)) {
        return status === "not_configured" ? "未配置回调地址，无需投递" : webhookStatusLabel(status);
    }
    const attempts = row.attemptCount ?? Math.max(1, (row.webhookRetryCount || 0) + 1);
    return `尝试 ${attempts} 次`;
}

function methodClass(method) {
    return method.includes("POST") ? "post" : method.includes("PUT") ? "put" : method.includes("DELETE") ? "delete" : "";
}

function pageHead(title, subtitle = "", actions = "") {
    return `<div class="page-head"><div><h1>${title}</h1>${subtitle ? `<p>${subtitle}</p>` : ""}</div><div class="page-actions">${actions}</div></div>`;
}

function shell(content) {
    const nav = NAV_ITEMS.map((item) => `
        <a class="nav-link ${state.route === item.id ? "active" : ""}" href="#${item.id}">
            <span class="nav-icon">${uiIcon(item.icon)}</span><span>${item.label}</span>
        </a>`).join("");
    const darkMode = state.theme === "dark";
    const themeLabel = darkMode ? "浅色模式" : "深色模式";
    const themeIcon = darkMode ? uiIcon("sun") : uiIcon("moon");
    const collapseLabel = state.sidebarCollapsed ? "展开" : "收起";
    const collapseIcon = state.sidebarCollapsed ? uiIcon("chevronRight") : uiIcon("chevronLeft");
    return `
        <div class="app-shell ${state.sidebarCollapsed ? "sidebar-collapsed" : ""} ${darkMode ? "theme-dark" : ""}">
            <aside class="sidebar">
                <div class="brand"><div class="brand-main"><span class="brand-mark">${uiIcon("sms")}</span><div class="brand-copy"><span class="brand-name">本地短信网关</span><span class="brand-version">v${APP_VERSION}</span></div></div></div>
                <nav class="nav">${nav}</nav>
                <div class="sidebar-footer">
                    <div class="sidebar-controls">
                        <button type="button" class="sidebar-control" data-action="toggle-theme" title="${themeLabel}"><span class="sidebar-control-icon">${themeIcon}</span><span class="sidebar-control-label">${themeLabel}</span></button>
                        <button type="button" class="sidebar-control collapse-button" data-action="toggle-sidebar" title="${collapseLabel}"><span class="sidebar-control-icon">${collapseIcon}</span><span class="sidebar-control-label">${collapseLabel}</span></button>
                    </div>
                </div>
            </aside>
            <main class="main">
                <header class="topbar">
                    <div class="topbar-actions">
                        <button type="button" class="primary-button topbar-send-button" data-action="open-send"><span class="button-icon">${uiIcon("sent")}</span><span>发送短信</span></button>
                        <button type="button" class="token-button" data-action="open-token">${state.token ? "Token 已设置" : "设置 API Token"}</button>
                        <button type="button" class="ghost-button logout-button" data-action="logout">退出登录</button>
                    </div>
                </header>
                <section class="page">${content}</section>
            </main>
        </div>
        <div id="token-modal" class="modal-backdrop" ${state.modalOpen ? "" : "hidden"}>
            <div class="modal">
                <h2>连接本地短信网关</h2>
                <p>请输入手机端显示的 API Token。Token 只保存在当前浏览器，用于访问局域网 API。</p>
                <form id="token-form">
                    <div class="field"><label for="token-input">API Token</label><input id="token-input" type="password" autocomplete="off" value="${escapeHtml(state.token)}" placeholder="粘贴手机端的 Token" />${state.authError ? `<span class="field-error">${escapeHtml(state.authError)}</span>` : ""}</div>
                    <div class="modal-actions"><button type="button" class="ghost-button" data-action="close-token">取消</button><button id="token-save" class="primary-button" type="button" data-action="save-token">保存并连接</button></div>
                </form>
            </div>
        </div>
        <div id="send-modal" class="modal-backdrop" ${state.sendModalOpen ? "" : "hidden"}>
            <div class="modal send-modal">
                <div class="modal-header"><div><span class="modal-eyebrow">LOCAL SMS</span><h2>发送短信</h2><p>通过当前手机 SIM 卡发送短信</p></div><button type="button" class="modal-close" data-action="close-send" aria-label="关闭">×</button></div>
                <form id="send-modal-form" class="form-stack">
                    <div class="field"><label for="modal-send-sim">SIM 卡</label><select id="modal-send-sim" data-send-sim>${simOptionsHtml()}</select></div>
                    <div class="field"><label for="modal-send-to">收件人号码 <span class="required">*</span></label><div class="phone-input"><select id="modal-send-country" data-send-country aria-label="国家或地区区号">${countryCodeOptionsHtml()}</select><input id="modal-send-to" data-send-to required inputmode="tel" autocomplete="tel-national" placeholder="例如：13800138000" /></div><span class="hint" data-send-preview>将自动添加 +86</span></div>
                    <div class="field"><label for="modal-send-text">短信内容 <span class="required">*</span></label><textarea id="modal-send-text" data-send-text required maxlength="1000" placeholder="请输入短信内容"></textarea><span class="hint"><span data-send-count>0</span> / 1000 字符</span></div>
                    <div class="modal-actions"><button type="button" class="ghost-button" data-action="close-send">取消</button><button class="primary-button" type="submit">➤ 发送短信</button></div>
                </form>
            </div>
        </div>`;
}

function authShell() {
    return `
        <main class="auth-page">
            <section class="auth-card" aria-labelledby="auth-title">
                <div class="auth-brand">
                    <span class="auth-brand-mark">${uiIcon("sms")}</span>
                    <div class="auth-brand-copy">
                        <span class="auth-eyebrow">LOCAL SMS GATEWAY</span>
                        <h1>本地短信网关</h1>
                        <span class="auth-version">v${APP_VERSION}</span>
                    </div>
                </div>
                <div class="auth-heading">
                    <h2 id="auth-title">安全连接</h2>
                    <p>请输入手机端显示的 API Token，验证后进入控制台。</p>
                </div>
                <form id="token-form" class="auth-form">
                    <div class="field">
                        <label for="token-input">API Token</label>
                        <input id="token-input" type="password" autocomplete="off" spellcheck="false" value="${escapeHtml(state.token)}" placeholder="粘贴手机端的 API Token" autofocus />
                        ${state.authError ? `<span class="field-error" role="alert">${escapeHtml(state.authError)}</span>` : ""}
                    </div>
                    <button id="token-save" class="primary-button auth-submit" type="submit" data-action="save-token">进入控制台</button>
                </form>
                <p class="auth-privacy">Token 仅保存在当前浏览器，用于访问本机局域网 API。</p>
            </section>
        </main>`;
}

function render() {
    const pages = { overview: pageOverview, sent: pageSent, received: pageReceived, webhook: pageWebhook, mcp: pageMcp, docs: pageDocs, settings: pageSettings };
    document.documentElement.dataset.theme = state.theme;
    document.querySelector("#app").innerHTML = state.token ? shell(pages[state.route]()) : authShell();
    bindGlobalActions();
    if (state.token) bindPageActions();
}

function startRouteLoad(route = state.route) {
    if (!state.token) return;
    if (state.loadedRoute === route || state.loadingRoute === route) return;
    state.loadingRoute = route;
    loadRoute(route);
}

function pageOverview() {
    const stats = state.stats || DEMO_STATS;
    const todaySent = Number(stats.todaySent) || 0;
    const todayReceived = Number(stats.todayReceived) || 0;
    const todaySentFailed = Math.max(0, Number(stats.todaySentFailed) || 0);
    const successRateNumber = stats.todaySentSuccessRate == null ? Number.NaN : Number(stats.todaySentSuccessRate);
    const successRate = Number.isFinite(successRateNumber) ? `${Math.round(Math.max(0, Math.min(100, successRateNumber)))}%` : "—";
    const successRateFoot = Number.isFinite(successRateNumber) ? "按已完成发送统计" : "暂无已完成发送";
    const trend = Array.isArray(stats.trend) ? stats.trend : [];
    const maxValue = Math.max(1, ...trend.flatMap((item) => [Number(item.sent) || 0, Number(item.received) || 0]));
    const midValue = maxValue > 1 ? Math.ceil(maxValue / 2) : "";
    const sentTotal = trend.reduce((total, item) => total + Math.max(0, Number(item.sent) || 0), 0);
    const receivedTotal = trend.reduce((total, item) => total + Math.max(0, Number(item.received) || 0), 0);
    const bars = trend.map((item, index) => {
        const sentValue = Math.max(0, Number(item.sent) || 0);
        const receivedValue = Math.max(0, Number(item.received) || 0);
        const sentHeight = Math.round((sentValue / maxValue) * 100);
        const receivedHeight = Math.round((receivedValue / maxValue) * 100);
        const label = index % 4 === 0 || index === trend.length - 1 ? String(item.label || "").slice(-5) : "";
        const title = `${item.label || ""} · 发送 ${sentValue} · 收信 ${receivedValue}`;
        return `<div class="bar-group" title="${escapeHtml(title)}"><div class="bar ${sentValue > 0 ? "active" : "zero"}" style="height:${sentHeight}%"></div><div class="bar receive ${receivedValue > 0 ? "active" : "zero"}" style="height:${receivedHeight}%"></div><label>${escapeHtml(label)}</label></div>`;
    }).join("");
    const activities = mergeActivities(stats.activities || []);
    const activity = activities.slice(0, 8).map(renderActivityRow).join("");
    const dateLabel = stats.date ? `统计日期 ${escapeHtml(stats.date)}` : "暂无统计日期";
    return `${pageHead("概览", "查看本地短信网关的真实收发数据")}
        <div class="stat-grid compact overview-stats">
            <div class="stat-card"><span class="stat-icon">➤</span><div class="stat-label">今日发送</div><div class="stat-value">${todaySent}</div><div class="stat-foot">${dateLabel}</div></div>
            <div class="stat-card"><span class="stat-icon">⇩</span><div class="stat-label">今日收信</div><div class="stat-value">${todayReceived}</div><div class="stat-foot">${dateLabel}</div></div>
            <div class="stat-card"><span class="stat-icon">✓</span><div class="stat-label">发送成功率</div><div class="stat-value">${successRate}</div><div class="stat-foot">${successRateFoot}</div></div>
            <div class="stat-card"><span class="stat-icon">!</span><div class="stat-label">今日发送失败</div><div class="stat-value">${todaySentFailed}</div><div class="stat-foot warning">${dateLabel}</div></div>
        </div>
        <div class="overview-stack">
            <section class="panel trend-panel overview-trend"><div class="trend-head"><div><h2 class="panel-title">24 小时收发趋势</h2><span class="trend-caption">${dateLabel} · 鼠标悬停查看小时数据</span></div><div class="trend-summary"><div class="trend-total sent"><i></i><strong>${sentTotal}</strong><span>发送</span></div><div class="trend-total received"><i></i><strong>${receivedTotal}</strong><span>收信</span></div></div></div><div class="trend-chart"><div class="chart-y"><span>${maxValue}</span><span>${midValue}</span><span>0</span></div><div class="chart-area">${bars || `<div class="empty-state">暂无趋势数据</div>`}</div></div></section>
            <section class="panel activity-panel"><div class="activity-head"><div><h2 class="panel-title">最近活动</h2><span class="trend-caption">同一条短信的收发与 Webhook 状态会合并显示</span></div><span class="activity-count">${Math.min(8, activities.length)} 条</span></div><div class="activity-table"><div class="activity-row header"><span>时间</span><span>类型</span><span>详情</span><span>状态</span></div>${activity || `<div class="empty-state">暂无活动记录</div>`}</div><div class="activity-footer"><a class="activity-link" href="#sent">发送记录 →</a><a class="activity-link" href="#received">收信记录 →</a><a class="activity-link" href="#webhook">Webhook 日志 →</a></div></section>
        </div>`;
}

function activityStatusChip(item) {
    if (item.statusKind === "received") return `<span class="chip green">已接收</span>`;
    if (item.statusKind === "webhook") return statusChip(item.status, "received");
    return statusChip(item.status);
}

function mergeActivities(items) {
    const groups = new Map();
    const independent = [];
    (Array.isArray(items) ? items : []).forEach((item, index) => {
        const messageId = String(item?.messageId || "").trim();
        if (!messageId) {
            independent.push({ ...item, activityIndex: index });
            return;
        }
        if (!groups.has(messageId)) groups.set(messageId, { message: null, webhooks: [], activityIndex: index });
        const group = groups.get(messageId);
        if (item.statusKind === "webhook") group.webhooks.push(item);
        else if (!group.message) group.message = item;
    });

    const grouped = [];
    [...groups.values()].forEach((group) => {
        if (!group.message) {
            group.webhooks.forEach((webhook) => grouped.push({ ...webhook, activityIndex: group.activityIndex }));
            return;
        }
        if (!group.webhooks.length) {
            grouped.push({ ...group.message, activityIndex: group.activityIndex });
            return;
        }
        const allItems = [group.message, ...group.webhooks];
        const latest = allItems.reduce((current, item) => {
            const currentTime = Date.parse(current?.timestamp || "");
            const itemTime = Date.parse(item?.timestamp || "");
            return Number.isNaN(currentTime) || (!Number.isNaN(itemTime) && itemTime > currentTime) ? item : current;
        });
        grouped.push({
            ...group.message,
            timestamp: latest.timestamp || group.message.timestamp,
            mergedWebhookItems: group.webhooks,
            activityIndex: group.activityIndex
        });
    });

    return [...grouped, ...independent].sort((left, right) => {
        const leftTime = Date.parse(left?.timestamp || "");
        const rightTime = Date.parse(right?.timestamp || "");
        if (!Number.isNaN(leftTime) && !Number.isNaN(rightTime) && leftTime !== rightTime) return rightTime - leftTime;
        return (left.activityIndex || 0) - (right.activityIndex || 0);
    });
}

function webhookAggregateStatus(items) {
    const statuses = items.map((item) => item.status || "pending");
    if (statuses.includes("failed")) return "failed";
    if (statuses.includes("pending") || statuses.includes("retrying")) return "pending";
    if (statuses.includes("delivered")) return "delivered";
    return statuses[0] || "pending";
}

function activityStatusMarkup(item) {
    const webhooks = Array.isArray(item.mergedWebhookItems) ? item.mergedWebhookItems : [];
    if (!webhooks.length) return activityStatusChip(item);
    return `<span class="activity-statuses">${activityStatusChip(item)}${statusChip(webhookAggregateStatus(webhooks), "received")}</span>`;
}

function activityRoute(item) {
    if (item.relatedType === "sent") return "sent";
    if (item.relatedType === "received") return "received";
    return "";
}

function activityRelation(item) {
    const messageId = String(item.messageId || "").trim();
    if (!messageId || item.statusKind !== "webhook") return "";
    const relationLabel = item.relatedType === "received" ? "关联收信" : item.relatedType === "sent" ? "关联发送" : "关联消息";
    const relationDetail = String(item.relatedDetail || "").trim();
    const shortId = messageId.length > 18 ? `${messageId.slice(0, 9)}…${messageId.slice(-6)}` : messageId;
    const visible = relationDetail ? `${relationLabel} · ${relationDetail}` : `${relationLabel} · ${shortId}`;
    return `<span class="activity-relation" title="消息 ID：${escapeHtml(messageId)}">${escapeHtml(visible)}</span>`;
}

function renderActivityRow(item) {
    const messageId = String(item.messageId || "").trim();
    const route = activityRoute(item);
    const clickable = Boolean(messageId && route);
    const attrs = clickable ? ` data-action="open-activity" data-activity-route="${route}" data-activity-id="${escapeHtml(messageId)}"` : "";
    const webhooks = Array.isArray(item.mergedWebhookItems) ? item.mergedWebhookItems : [];
    const type = item.type || "短信";
    const webhookDetail = webhooks.length
        ? `<span class="activity-related-line"><strong>Webhook</strong> · ${webhooks.map((webhook) => escapeHtml(webhook.detail || "Webhook 投递")).join(" / ")}</span>`
        : activityRelation(item);
    const title = webhooks.length
        ? `已合并 ${webhooks.length} 条 Webhook 活动 · 消息 ID：${messageId}`
        : messageId ? `消息 ID：${messageId}` : "";
    return `<div class="activity-row${clickable ? " activity-linked" : ""}"${attrs}${title ? ` title="${escapeHtml(title)}"` : ""}><span>${formatTime(item.timestamp)}</span><span>${escapeHtml(type)}</span><span class="activity-detail"><span class="activity-detail-main">${escapeHtml(item.detail || "")}</span>${webhookDetail}</span>${activityStatusMarkup(item)}</div>`;
}

function simOptionsHtml() {
    return (state.sims || []).map((sim) => `<option value="${sim.subscriptionId ?? ""}">${escapeHtml(sim.label || `SIM ${sim.simSlot}`)}${sim.number ? `（${escapeHtml(sim.number)}）` : ""}</option>`).join("") || `<option value="">默认 SIM</option>`;
}

const COUNTRY_CODES = [
    { code: "+86", label: "中国大陆" },
    { code: "+852", label: "中国香港" },
    { code: "+853", label: "中国澳门" },
    { code: "+886", label: "中国台湾" },
    { code: "+1", label: "美国 / 加拿大" },
    { code: "+44", label: "英国" },
    { code: "+81", label: "日本" },
    { code: "+82", label: "韩国" },
    { code: "+65", label: "新加坡" },
    { code: "+60", label: "马来西亚" },
    { code: "+61", label: "澳大利亚" }
];

function countryCodeOptionsHtml() {
    return COUNTRY_CODES.map((item) => `<option value="${item.code}">${item.code} ${item.label}</option>`).join("");
}

function composeRecipient(countryCode, value) {
    const raw = String(value ?? "").trim();
    if (!raw) return "";
    const source = raw.replace(/[()\s-]/g, "");
    const digits = source.replace(/\D/g, "");
    if (!digits) return "";
    const code = String(countryCode || "+86").replace(/[^\d+]/g, "") || "+86";
    const codeDigits = code.replace(/\D/g, "");
    if (source.startsWith("+")) return `+${digits}`;
    if (digits.startsWith("00")) return `+${digits.slice(2)}`;
    if (digits.length > 10 && digits.startsWith(codeDigits)) return `+${digits}`;
    return `${code}${digits.replace(/^0+(?=\d)/, "")}`;
}

function updateRecipientPreview(form) {
    if (!form) return;
    const countryCode = form.querySelector("[data-send-country]")?.value || "+86";
    const input = form.querySelector("[data-send-to]");
    const preview = form.querySelector("[data-send-preview]");
    if (!preview) return;
    const recipient = composeRecipient(countryCode, input?.value || "");
    preview.textContent = recipient ? `实际发送至 ${recipient}` : `将自动添加 ${countryCode}`;
}

function simNumberLabel(subscriptionId) {
    const targetId = subscriptionId == null ? null : Number(subscriptionId);
    const sims = state.sims || [];
    let sim = sims.find((item) => {
        const itemId = item.subscriptionId == null ? null : Number(item.subscriptionId);
        return itemId === targetId;
    });
    if (!sim && targetId == null && sims.length === 1) sim = sims[0];
    if (sim?.number) return escapeHtml(sim.number);
    if (sim?.label) return `${escapeHtml(sim.label)} · 号码未提供`;
    return targetId == null ? "默认 SIM · 号码未提供" : `SIM ${targetId} · 号码未提供`;
}

function pageSend() {
    return `${pageHead("发送短信", "通过当前手机 SIM 卡发送短信")}
        <div class="form-layout send-layout">
            <section class="panel"><h2 class="panel-title">新建短信</h2><form id="send-form" class="form-stack">
                <div class="field"><label for="send-sim">SIM 卡</label><select id="send-sim" data-send-sim>${simOptionsHtml()}</select><span class="hint">双卡设备会显示真实 subscriptionId；默认 SIM 不指定订阅</span></div>
                <div class="field"><label for="send-to">收件人号码 <span class="required">*</span></label><div class="phone-input"><select id="send-country" data-send-country aria-label="国家或地区区号">${countryCodeOptionsHtml()}</select><input id="send-to" data-send-to required inputmode="tel" autocomplete="tel-national" placeholder="例如：13800138000" /></div><span class="hint" data-send-preview>将自动添加 +86</span></div>
                <div class="field"><label for="send-text">短信内容 <span class="required">*</span></label><textarea id="send-text" data-send-text required maxlength="1000" placeholder="请输入短信内容"></textarea><span class="hint"><span id="text-count" data-send-count>0</span> / 1000 字符 · 长短信会按短信分片发送</span></div>
                <div class="form-actions"><button class="primary-button" type="submit">➤ 发送短信</button><button class="ghost-button" type="reset">清空</button></div>
            </form></section>
        </div>`;
}

const RECORD_PAGE_SIZE = 10;

function localDateKey(value) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "";
    return [date.getFullYear(), String(date.getMonth() + 1).padStart(2, "0"), String(date.getDate()).padStart(2, "0")].join("-");
}

function filterRows(rows, filters, kind) {
    const query = String(filters.query || "").trim().toLowerCase();
    return rows.filter((row) => {
        const dateValue = kind === "sent" ? row.createdAt : row.receivedAt;
        const searchable = kind === "sent"
            ? [row.messageId, row.to, row.text]
            : [row.messageId, row.from, row.text];
        if (query && !searchable.some((value) => String(value || "").toLowerCase().includes(query))) return false;
        const date = localDateKey(dateValue);
        if (filters.from && (!date || date < filters.from)) return false;
        if (filters.to && (!date || date > filters.to)) return false;
        if (filters.status && filters.status !== "all") {
            const status = kind === "sent" ? row.status : receivedWebhookStatus(row);
            if (status !== filters.status) return false;
        }
        return true;
    });
}

function renderPagination(total, page, listName) {
    const pageCount = Math.max(1, Math.ceil(total / RECORD_PAGE_SIZE));
    const current = Math.min(Math.max(1, page), pageCount);
    const pages = [];
    const start = Math.max(1, Math.min(current - 2, pageCount - 4));
    const end = Math.min(pageCount, start + 4);
    for (let index = start; index <= end; index += 1) {
        pages.push(`<button type="button" class="${index === current ? "active" : ""}" data-action="change-page" data-list="${listName}" data-page="${index}">${index}</button>`);
    }
    return `<div class="pagination"><span>共 ${total} 条</span><div class="page-buttons"><button type="button" data-action="change-page" data-list="${listName}" data-page="${current - 1}" ${current <= 1 ? "disabled" : ""}>‹</button>${pages.join("")}<button type="button" data-action="change-page" data-list="${listName}" data-page="${current + 1}" ${current >= pageCount ? "disabled" : ""}>›</button></div><span>${RECORD_PAGE_SIZE} 条/页</span></div>`;
}

function recordSimOptions(rows, selected) {
    const ids = [...new Set(rows.map((row) => String(row.subscriptionId || 1)))].sort((a, b) => Number(a) - Number(b));
    return [`<option value="all">全部 SIM</option>`, ...ids.map((id) => `<option value="${id}" ${selected === id ? "selected" : ""}>SIM ${id}</option>`)].join("");
}

function pageSentLegacy() {
    const filters = state.sentFilters;
    const filtered = filterRows(state.sent, filters, "sent");
    const pageCount = Math.max(1, Math.ceil(filtered.length / RECORD_PAGE_SIZE));
    const page = Math.min(Math.max(1, filters.page), pageCount);
    const rows = filtered.slice((page - 1) * RECORD_PAGE_SIZE, page * RECORD_PAGE_SIZE);
    return `${pageHead("发送记录", "查询发送请求、发送结果和送达回执")}
        <section class="panel"><form id="sent-filter-form" class="toolbar"><input id="sent-query" class="search" value="${escapeHtml(filters.query)}" placeholder="⌕  搜索消息 ID、接收号码或内容" /><input id="sent-from" type="date" value="${filters.from}" /><input id="sent-to" type="date" value="${filters.to}" /><select id="sent-status"><option value="all" ${filters.status === "all" ? "selected" : ""}>全部状态</option><option value="queued" ${filters.status === "queued" ? "selected" : ""}>已提交</option><option value="sending" ${filters.status === "sending" ? "selected" : ""}>发送中</option><option value="sent" ${filters.status === "sent" ? "selected" : ""}>已发送</option><option value="delivered" ${filters.status === "delivered" ? "selected" : ""}>已送达</option><option value="failed" ${filters.status === "failed" ? "selected" : ""}>发送失败</option></select><select id="sent-sim">${recordSimOptions(state.sent, filters.sim)}</select><button class="primary-button" type="submit">查询</button><button class="ghost-button" type="button" data-action="reset-sent-filter">重置</button></form>
        <div class="table-wrap"><table class="data-table"><thead><tr><th>消息 ID</th><th>接收号码</th><th>内容预览</th><th>时间</th><th>SIM</th><th>状态</th></tr></thead><tbody>${rows.length ? rows.map((row) => `<tr><td class="id-text">${escapeHtml(row.messageId)}</td><td>${escapeHtml(row.to)}</td><td class="table-content">${shortText(row.text)}</td><td>${formatTime(row.createdAt)}</td><td>${simNumberLabel(row.subscriptionId)}</td><td>${statusChip(row.status)}</td></tr>`).join("") : `<tr><td colspan="6"><div class="empty-state"><strong>${state.sent.length ? "没有符合条件的发送记录" : "暂无发送记录"}</strong>${state.sent.length ? "请调整筛选条件后重试。" : "发送短信后记录会显示在这里。"}</div></td></tr>`}</tbody></table></div>
        ${renderPagination(filtered.length, page, "sent")}</section>`;
}

function pageReceivedLegacy() {
    const filters = state.receivedFilters;
    const filtered = filterRows(state.received, filters, "received");
    const pageCount = Math.max(1, Math.ceil(filtered.length / RECORD_PAGE_SIZE));
    const page = Math.min(Math.max(1, filters.page), pageCount);
    const rows = filtered.slice((page - 1) * RECORD_PAGE_SIZE, page * RECORD_PAGE_SIZE);
    const selected = rows.find((row) => row.messageId === state.selectedReceivedId) || rows[0];
    const selectedWebhookStatus = selected ? receivedWebhookStatus(selected) : "";
    return `${pageHead("收信记录", "查看收到的短信及 Webhook 回调状态；未配置地址时不计为失败")}
        <section class="panel"><form id="received-filter-form" class="toolbar"><input id="received-query" class="search" value="${escapeHtml(filters.query)}" placeholder="⌕  搜索消息 ID、发件号码或内容" /><input id="received-from" type="date" value="${filters.from}" /><input id="received-to" type="date" value="${filters.to}" /><select id="received-status"><option value="all" ${filters.status === "all" ? "selected" : ""}>全部 Webhook 状态</option><option value="delivered" ${filters.status === "delivered" ? "selected" : ""}>已回调</option><option value="retrying" ${filters.status === "retrying" ? "selected" : ""}>重试中</option><option value="failed" ${filters.status === "failed" ? "selected" : ""}>回调失败</option><option value="not_configured" ${filters.status === "not_configured" ? "selected" : ""}>未配置</option><option value="disabled" ${filters.status === "disabled" ? "selected" : ""}>已停用</option><option value="not_subscribed" ${filters.status === "not_subscribed" ? "selected" : ""}>未订阅</option></select><select id="received-sim">${recordSimOptions(state.received, filters.sim)}</select><button class="primary-button" type="submit">查询</button><button class="ghost-button" type="button" data-action="reset-received-filter">重置</button></form>
        <div class="table-wrap"><table class="data-table"><thead><tr><th>消息 ID</th><th>发件号码</th><th>内容预览</th><th>接收时间</th><th>SIM</th><th>Webhook 状态</th></tr></thead><tbody>${rows.length ? rows.map((row) => `<tr class="${selected && row.messageId === selected.messageId ? "selected" : ""}" data-action="select-received" data-id="${escapeHtml(row.messageId)}"><td class="id-text">${escapeHtml(row.messageId)}</td><td>${escapeHtml(row.from)}</td><td class="table-content">${shortText(row.text)}</td><td>${formatTime(row.receivedAt)}</td><td>${simNumberLabel(row.subscriptionId)}</td><td>${statusChip(receivedWebhookStatus(row), "received")}</td></tr>`).join("") : `<tr><td colspan="6"><div class="empty-state"><strong>${state.received.length ? "没有符合条件的收信记录" : "暂无收信记录"}</strong>${state.received.length ? "请调整筛选条件后重试。" : "收到短信后记录会显示在这里。"}</div></td></tr>`}</tbody></table></div>
        ${selected ? `<div class="record-detail"><div class="record-detail-head"><div><span class="detail-eyebrow">RECEIVED MESSAGE</span><h3>消息详情</h3></div><div class="detail-status">${statusChip(selectedWebhookStatus, "received")}<span>${escapeHtml(webhookAttemptSummary(selected, selectedWebhookStatus))}</span></div></div><div class="detail-summary"><div class="detail-summary-item detail-summary-primary"><span class="detail-avatar">✉</span><div><span class="detail-label">发件号码</span><strong>${escapeHtml(selected.from)}</strong></div></div><div class="detail-summary-item"><span class="detail-label">接收时间</span><strong>${formatTime(selected.receivedAt)}</strong></div><div class="detail-summary-item"><span class="detail-label">SIM 卡</span><strong>SIM ${selected.subscriptionId || 1}</strong></div></div><div class="detail-block"><div class="detail-block-head"><span>短信内容</span><span class="detail-count">${String(selected.text || "").length} 字</span></div><div class="detail-message-text">${escapeHtml(selected.text)}</div></div><div class="detail-webhook"><div class="detail-block-head"><span>Webhook 回调</span>${statusChip(selectedWebhookStatus, "received")}</div><p>${escapeHtml(webhookAttemptSummary(selected, selectedWebhookStatus))}</p></div><div class="detail-message-id"><span>消息 ID</span><code>${escapeHtml(selected.messageId)}</code></div></div>` : ""}
        ${renderPagination(filtered.length, page, "received")}</section>`;
}

function recordDetailPlaceholder(title, message) {
    return `<div class="record-detail record-detail-empty"><span class="record-detail-empty-icon">⌁</span><strong>${escapeHtml(title)}</strong><p>${escapeHtml(message)}</p></div>`;
}

function renderSentDetail(selected) {
    if (!selected) return recordDetailPlaceholder("暂无发送详情", "发送短信后，选择左侧记录查看完整信息。");
    const webhookStatus = sentWebhookStatus(selected);
    const statusDescription = {
        queued: "已加入发送队列，等待短信系统处理",
        sending: "短信正在发送中",
        sent: "已发送，等待送达报告",
        delivered: "已收到运营商送达回执",
        failed: `发送失败${selected.errorCode == null ? "" : ` · 错误码 ${selected.errorCode}`}`
    }[selected.status] || "等待系统更新状态";
    return `<div class="record-detail sent-detail"><div class="record-detail-head"><div><span class="detail-eyebrow">SENT MESSAGE</span><h3>发送详情</h3></div><div class="detail-head-actions"><div class="detail-status"><div class="detail-status-stack"><div class="detail-status-line"><span>短信状态</span>${statusChip(selected.status)}</div><div class="detail-status-line"><span>Webhook 状态</span>${statusChip(webhookStatus, "received")}</div><span class="detail-status-note">${escapeHtml(statusDescription)}</span></div></div><button type="button" class="detail-close" data-action="close-record-drawer" aria-label="关闭详情">×</button></div></div><div class="detail-summary"><div class="detail-summary-item detail-summary-primary"><span class="detail-avatar">${uiIcon("sent")}</span><div><span class="detail-label">收件号码</span><strong>${escapeHtml(selected.to)}</strong></div></div><div class="detail-summary-item"><span class="detail-label">创建时间</span><strong>${formatTime(selected.createdAt)}</strong></div></div><div class="detail-block"><div class="detail-block-head"><span>短信内容</span><span class="detail-count">${String(selected.text || "").length} 字</span></div><div class="detail-message-text">${escapeHtml(selected.text)}</div></div><div class="detail-status-grid"><div class="detail-status-item"><span class="detail-label">发送时间</span><strong>${formatTime(selected.sentAt)}</strong></div><div class="detail-status-item"><span class="detail-label">送达时间</span><strong>${formatTime(selected.deliveredAt)}</strong></div><div class="detail-status-item"><span class="detail-label">短信状态</span><strong>${statusChip(selected.status)}</strong></div><div class="detail-status-item"><span class="detail-label">Webhook 状态</span><strong>${statusChip(webhookStatus, "received")}</strong></div></div><div class="detail-message-id"><span>消息 ID</span><code>${escapeHtml(selected.messageId)}</code></div></div>`;
}

function renderReceivedDetail(selected) {
    if (!selected) return recordDetailPlaceholder("暂无收信详情", "收到短信后，选择左侧记录查看完整信息。");
    const webhookStatus = receivedWebhookStatus(selected);
    return `<div class="record-detail received-detail"><div class="record-detail-head"><div><span class="detail-eyebrow">RECEIVED MESSAGE</span><h3>收信详情</h3></div><div class="detail-head-actions"><div class="detail-status"><div class="detail-status-stack"><div class="detail-status-line"><span>收信状态</span>${receivedSmsStatusChip()}</div><div class="detail-status-line"><span>Webhook 状态</span>${statusChip(webhookStatus, "received")}</div><span class="detail-status-note">${escapeHtml(webhookAttemptSummary(selected, webhookStatus))}</span></div></div><button type="button" class="detail-close" data-action="close-record-drawer" aria-label="关闭详情">×</button></div></div><div class="detail-summary"><div class="detail-summary-item detail-summary-primary"><span class="detail-avatar">${uiIcon("received")}</span><div><span class="detail-label">发件号码</span><strong>${escapeHtml(selected.from)}</strong></div></div><div class="detail-summary-item"><span class="detail-label">接收时间</span><strong>${formatTime(selected.receivedAt)}</strong></div></div><div class="detail-block"><div class="detail-block-head"><span>短信内容</span><span class="detail-count">${String(selected.text || "").length} 字</span></div><div class="detail-message-text">${escapeHtml(selected.text)}</div></div><div class="detail-webhook"><div class="detail-block-head"><span>Webhook 回调</span>${statusChip(webhookStatus, "received")}</div><p>${escapeHtml(webhookAttemptSummary(selected, webhookStatus))}</p></div><div class="detail-message-id"><span>消息 ID</span><code>${escapeHtml(selected.messageId)}</code></div></div>`;
}

function recordDrawerMarkup(kind, selected) {
    if (!selected) return "";
    const title = kind === "sent" ? "发送消息详情" : "收信消息详情";
    const detail = kind === "sent" ? renderSentDetail(selected) : renderReceivedDetail(selected);
    return `<div class="record-drawer-layer"><button type="button" class="record-drawer-backdrop" data-action="close-record-drawer" aria-label="关闭详情"></button><aside class="record-drawer" role="dialog" aria-modal="true" aria-label="${title}">${detail}</aside></div>`;
}

function pageSent() {
    const filters = state.sentFilters;
    const filtered = filterRows(state.sent, filters, "sent");
    const pageCount = Math.max(1, Math.ceil(filtered.length / RECORD_PAGE_SIZE));
    const page = Math.min(Math.max(1, filters.page), pageCount);
    const rows = filtered.slice((page - 1) * RECORD_PAGE_SIZE, page * RECORD_PAGE_SIZE);
    const selected = state.recordDrawer.kind === "sent"
        ? state.sent.find((row) => row.messageId === state.recordDrawer.id)
        : null;
    const selectedId = selected?.messageId || "";
    return `${pageHead("发送记录", "查询发送结果、送达回执和 Webhook 投递状态")}
        <section class="panel records-panel"><form id="sent-filter-form" class="toolbar"><input id="sent-query" class="search" value="${escapeHtml(filters.query)}" placeholder="⌕  搜索消息 ID、接收号码或内容" /><input id="sent-from" type="date" value="${filters.from}" /><input id="sent-to" type="date" value="${filters.to}" /><select id="sent-status"><option value="all" ${filters.status === "all" ? "selected" : ""}>全部状态</option><option value="queued" ${filters.status === "queued" ? "selected" : ""}>已提交</option><option value="sending" ${filters.status === "sending" ? "selected" : ""}>发送中</option><option value="sent" ${filters.status === "sent" ? "selected" : ""}>已发送</option><option value="delivered" ${filters.status === "delivered" ? "selected" : ""}>已送达</option><option value="failed" ${filters.status === "failed" ? "selected" : ""}>发送失败</option></select><button class="primary-button" type="submit">查询</button><button class="ghost-button" type="button" data-action="reset-sent-filter">重置</button></form><div class="records-table-column"><div class="records-column-head"><strong>发送列表</strong><span>${filtered.length} 条匹配记录</span></div><div class="table-wrap"><table class="data-table"><thead><tr><th>消息 ID</th><th>接收号码</th><th>内容预览</th><th>时间</th><th>短信状态</th><th>Webhook 状态</th><th>操作</th></tr></thead><tbody>${rows.length ? rows.map((row) => `<tr class="${row.messageId === selectedId ? "selected" : ""}" data-action="select-sent" data-id="${escapeHtml(row.messageId)}"><td class="id-text">${escapeHtml(row.messageId)}</td><td>${escapeHtml(row.to)}</td><td class="table-content">${shortText(row.text)}</td><td>${formatTime(row.createdAt)}</td><td>${statusChip(row.status)}</td><td>${statusChip(sentWebhookStatus(row), "received")}</td><td class="record-action-cell"><button type="button" class="table-action" data-action="open-record-drawer" data-kind="sent" data-id="${escapeHtml(row.messageId)}">查看详情</button></td></tr>`).join("") : `<tr><td colspan="7"><div class="empty-state"><strong>${state.sent.length ? "没有符合条件的发送记录" : "暂无发送记录"}</strong>${state.sent.length ? "请调整筛选条件后重试。" : "发送短信后记录会显示在这里。"}</div></td></tr>`}</tbody></table></div>${renderPagination(filtered.length, page, "sent")}</div></section>
        ${recordDrawerMarkup("sent", selected)}`;
}

function pageReceived() {
    const filters = state.receivedFilters;
    const filtered = filterRows(state.received, filters, "received");
    const pageCount = Math.max(1, Math.ceil(filtered.length / RECORD_PAGE_SIZE));
    const page = Math.min(Math.max(1, filters.page), pageCount);
    const rows = filtered.slice((page - 1) * RECORD_PAGE_SIZE, page * RECORD_PAGE_SIZE);
    const selected = state.recordDrawer.kind === "received"
        ? state.received.find((row) => row.messageId === state.recordDrawer.id)
        : null;
    const selectedId = selected?.messageId || "";
    return `${pageHead("收信记录", "查看收信状态及 Webhook 回调状态；未配置地址时不计为失败")}
        <section class="panel records-panel"><form id="received-filter-form" class="toolbar"><input id="received-query" class="search" value="${escapeHtml(filters.query)}" placeholder="⌕  搜索消息 ID、发件号码或内容" /><input id="received-from" type="date" value="${filters.from}" /><input id="received-to" type="date" value="${filters.to}" /><select id="received-status"><option value="all" ${filters.status === "all" ? "selected" : ""}>全部 Webhook 状态</option><option value="delivered" ${filters.status === "delivered" ? "selected" : ""}>已回调</option><option value="retrying" ${filters.status === "retrying" ? "selected" : ""}>重试中</option><option value="failed" ${filters.status === "failed" ? "selected" : ""}>回调失败</option><option value="not_configured" ${filters.status === "not_configured" ? "selected" : ""}>未配置</option><option value="disabled" ${filters.status === "disabled" ? "selected" : ""}>已停用</option><option value="not_subscribed" ${filters.status === "not_subscribed" ? "selected" : ""}>未订阅</option></select><button class="primary-button" type="submit">查询</button><button class="ghost-button" type="button" data-action="reset-received-filter">重置</button></form><div class="records-table-column"><div class="records-column-head"><strong>收信列表</strong><span>${filtered.length} 条匹配记录</span></div><div class="table-wrap"><table class="data-table"><thead><tr><th>消息 ID</th><th>发件号码</th><th>内容预览</th><th>接收时间</th><th>收信状态</th><th>Webhook 状态</th><th>操作</th></tr></thead><tbody>${rows.length ? rows.map((row) => `<tr class="${row.messageId === selectedId ? "selected" : ""}" data-action="select-received" data-id="${escapeHtml(row.messageId)}"><td class="id-text">${escapeHtml(row.messageId)}</td><td>${escapeHtml(row.from)}</td><td class="table-content">${shortText(row.text)}</td><td>${formatTime(row.receivedAt)}</td><td>${receivedSmsStatusChip()}</td><td>${statusChip(receivedWebhookStatus(row), "received")}</td><td class="record-action-cell"><button type="button" class="table-action" data-action="open-record-drawer" data-kind="received" data-id="${escapeHtml(row.messageId)}">查看详情</button></td></tr>`).join("") : `<tr><td colspan="7"><div class="empty-state"><strong>${state.received.length ? "没有符合条件的收信记录" : "暂无收信记录"}</strong>${state.received.length ? "请调整筛选条件后重试。" : "收到短信后记录会显示在这里。"}</div></td></tr>`}</tbody></table></div>${renderPagination(filtered.length, page, "received")}</div></section>
        ${recordDrawerMarkup("received", selected)}`;
}

function defaultMcpEndpoint() {
    const address = String(window.location?.origin || "").replace(/\/+$/, "");
    return `${address || "http://127.0.0.1:8080"}/mcp`;
}

function mcpEndpoint() {
    return defaultMcpEndpoint();
}

function mcpDraftFromForm() {
    const form = document.querySelector("#mcp-config-form");
    if (!form) return { ...state.mcpConfig };
    return {
        name: document.querySelector("#mcp-name")?.value.trim() || DEFAULT_MCP_CONFIG.name,
        createSkill: Boolean(document.querySelector("#mcp-create-skill")?.checked)
    };
}

function mcpClientConfig(config = state.mcpConfig) {
    const name = String(config?.name || DEFAULT_MCP_CONFIG.name).trim() || DEFAULT_MCP_CONFIG.name;
    const server = { url: mcpEndpoint() };
    if (state.token) {
        server.headers = { Authorization: `Bearer ${state.token}` };
    }
    return { mcpServers: { [name]: server } };
}

function mcpConfigJson(config = state.mcpConfig) {
    return JSON.stringify(mcpClientConfig(config), null, 2);
}

function mcpAgentPrompt(config = state.mcpConfig) {
    const name = String(config?.name || DEFAULT_MCP_CONFIG.name).trim() || DEFAULT_MCP_CONFIG.name;
    const skillInstruction = config?.createSkill
        ? `5. 另外创建一个名为 ${name} 的 Skill（使用当前客户端支持的 Skill 文件或目录格式），用于说明如何通过这个 MCP Server 完成本地短信网关的安全调用。Skill 中至少写明：连接地址、Authorization 配置、可用工具范围，以及发送短信前需要获得用户明确确认。创建完成后只检查配置文件是否有效，不要发送短信、读取或清理真实记录。`
        : "5. 本次不需要创建 Skill，只完成远程 MCP Server 配置和连通性验证。";
    return `请帮我把下面的远程 MCP Server 配置到你的 MCP 客户端中。

要求：
1. Server 名称使用 ${name}。
2. 使用 Streamable HTTP，连接地址和 Authorization 按下面 JSON 原样配置。
3. 配置完成后只执行 initialize 和 tools/list 进行连通性验证，不要发送短信、修改 Webhook 或清理记录。
4. 如果当前客户端的配置字段名称略有不同，请转换成等价的远程 MCP 配置，但不要修改 URL、端口或 Token。
${skillInstruction}

mcpServers 配置：
${mcpConfigJson(config)}`;
}

function updateMcpPreview() {
    const preview = document.querySelector("#mcp-config-json");
    if (preview) preview.textContent = mcpConfigJson(mcpDraftFromForm());
    const prompt = document.querySelector("#mcp-agent-prompt");
    if (prompt) prompt.textContent = mcpAgentPrompt(mcpDraftFromForm());
}

function pageMcp() {
    const config = state.mcpConfig || DEFAULT_MCP_CONFIG;
    const endpoint = mcpEndpoint();
    const tokenHint = state.token ? "当前浏览器已保存的 API Token 会自动写入配置" : "请先在顶栏设置 API Token，复制的配置才可直接调用";
    return `${pageHead("MCP Server", "让 AI 客户端通过 MCP 调用本地短信网关")}
        <div class="mcp-page">
            <section class="panel mcp-help-panel"><div class="mcp-help-icon" aria-hidden="true">${uiIcon("mcp")}</div><div class="mcp-help-content"><div class="mcp-help-top"><div><span class="mcp-eyebrow">QUICK START</span><h2 class="panel-title">使用方式</h2></div><span class="mcp-transport-badge">Streamable HTTP</span></div><p>复制下方配置，粘贴到支持远程 MCP 的客户端中。连接后即可调用发送短信、读取收信记录、读取 SIM、查询状态和 Webhook 等工具。</p><div class="mcp-steps"><div class="mcp-step"><span class="mcp-step-index">1</span><div><strong>复制配置</strong><small>复制 JSON 或给 Agent 的提示词</small></div></div><div class="mcp-step"><span class="mcp-step-index">2</span><div><strong>粘贴到客户端</strong><small>保存到 MCP 客户端配置中</small></div></div><div class="mcp-step"><span class="mcp-step-index">3</span><div><strong>验证连接</strong><small>仅执行 initialize 和 tools/list</small></div></div></div><div class="mcp-endpoint-line"><span>当前端点</span><code>${escapeHtml(endpoint)}</code><button class="ghost-button" type="button" data-copy="${escapeHtml(endpoint)}">复制地址</button></div></div></section>
            <div class="mcp-layout">
                <section class="panel mcp-config-panel"><div class="mcp-panel-heading"><div><span class="mcp-eyebrow">SERVER SETUP</span><h2 class="panel-title">连接配置</h2></div></div><p class="mcp-intro">MCP 与现有短信网关共用 ${escapeHtml(String(state.status?.port || 8080))} 端口，端点固定为 <code>/mcp</code>，无需再启动一个端口。</p><form id="mcp-config-form" class="form-stack">
                    <div class="field"><label for="mcp-name">Server 名称</label><input id="mcp-name" value="${escapeHtml(config.name || DEFAULT_MCP_CONFIG.name)}" placeholder="local-sms-gateway" /><span class="hint">会成为 mcpServers 配置中的键名</span></div>
                    <label class="mcp-check-row mcp-skill-option" for="mcp-create-skill"><input id="mcp-create-skill" type="checkbox" ${config.createSkill ? "checked" : ""} /><span><strong>同时创建为 Skill</strong><small>开启后，给 Agent 的配置提示词会包含 Skill 的创建要求；MCP JSON 本身不会增加额外字段。</small></span></label>
                    <div class="mcp-fixed-list"><div class="mcp-fixed-row"><span><strong>MCP Server 地址</strong><small>自动使用当前手机局域网地址和现有服务端口</small></span><code>${escapeHtml(endpoint)}</code></div><div class="mcp-fixed-row"><span><strong>Authorization</strong><small>${tokenHint}</small></span><span class="mcp-token-state ${state.token ? "ready" : "missing"}">${state.token ? "自动附加 Token" : "尚未设置 Token"}</span></div></div>
                    <div class="form-actions"><button class="primary-button" type="submit">保存配置</button><button class="ghost-button" type="button" data-action="test-mcp">测试连接</button></div>
                </form></section>
                <section class="panel mcp-json-panel"><div class="mcp-json-head"><div><span class="mcp-eyebrow">CLIENT CONFIGURATION</span><h2 class="panel-title">mcpServers 配置</h2></div><button class="ghost-button" type="button" data-action="copy-mcp-config">复制 JSON</button></div><pre id="mcp-config-json" class="mcp-json">${escapeHtml(mcpConfigJson(config))}</pre><div class="mcp-security-note"><span class="mcp-note-icon">!</span><span>配置会自动包含当前 API Token，请只粘贴到可信的 MCP 客户端。</span></div></section>
            </div>
            <section class="panel mcp-prompt-panel"><div class="mcp-json-head"><div><span class="mcp-eyebrow">AGENT INSTRUCTION</span><h2 class="panel-title">给 Agent 的配置提示词</h2></div><button class="ghost-button" type="button" data-action="copy-mcp-prompt">复制提示词</button></div><pre id="mcp-agent-prompt" class="mcp-agent-prompt">${escapeHtml(mcpAgentPrompt(config))}</pre><div class="mcp-security-note"><span class="mcp-note-icon">!</span><span>提示词中包含 API Token。发送给 Agent 前，请确认当前对话和客户端配置环境可信。</span></div></section>
        </div>`;
}

function pageWebhook() {
    const w = state.webhook || DEFAULT_WEBHOOK;
    const has = (event) => (w.events || []).includes(event) ? "checked" : "";
    const isBuiltin = String(w.url || "").includes("127.0.0.1") && String(w.url || "").endsWith("/webhook/test");
    return `${pageHead("Webhook", "配置收信和短信发送状态的通知回调")}
        <div class="webhook-layout"><section class="panel"><h2 class="panel-title">Webhook 配置</h2><form id="webhook-form" class="form-stack">
            <div class="field"><label for="webhook-url">Webhook URL <span class="required">*</span></label><input id="webhook-url" value="${escapeHtml(w.url || "")}" placeholder="https://example.com/api/sms/webhook" /><span class="hint">${isBuiltin ? "APK 内置测试接收器：只返回成功，不处理请求内容；电脑调用请使用手机局域网地址 + /webhook/test。" : "可以填写外部 HTTPS 地址，也可以填写局域网服务器地址"}</span></div>
            <div class="field"><label for="webhook-token">Token</label><input id="webhook-token" type="password" placeholder="${(w.tokenConfigured ?? w.secretConfigured) ? "已配置，留空保持不变" : "用于 WebHook 鉴权"}" /><span class="hint">请求会携带 X-SMS-Gateway-Token，同时保留签名校验头</span></div>
            <div><label style="display:block;color:var(--navy);font-weight:650;margin-bottom:8px">事件订阅</label><div class="switch-list">
                ${webhookSwitch("sms.received", "收信通知", "收到短信后回调", has("sms.received"))}
                ${webhookSwitch("sms.sent", "发送提交通知", "短信提交给系统后回调", has("sms.sent"))}
                ${webhookSwitch("sms.delivered", "送达通知", "收到运营商送达回执后回调", has("sms.delivered"))}
                ${webhookSwitch("sms.failed", "发送失败通知", "发送失败或系统拒绝后回调", has("sms.failed"))}
            </div></div>
            <div class="retry-grid"><div class="field"><label for="max-retries">最大重试次数</label><input id="max-retries" type="number" min="0" max="10" value="${w.maxRetries ?? 5}" /></div><div class="field"><label for="initial-delay">初始间隔（秒）</label><input id="initial-delay" type="number" min="1" value="${w.initialDelaySeconds ?? 2}" /></div><div class="field"><label for="max-delay">最大间隔（秒）</label><input id="max-delay" type="number" min="1" value="${w.maxDelaySeconds ?? 60}" /></div></div>
            <div class="form-actions"><button class="primary-button" type="submit">保存设置</button><button class="ghost-button" type="button" data-action="test-webhook">▷ 测试回调</button></div>
        </form></section>
        <section class="panel"><h2 class="panel-title">最近回调投递</h2><div class="delivery-list">${renderWebhookDeliveries()}</div></section></div>`;
}

function renderWebhookDeliveries() {
    const logs = state.webhookLogs || [];
    if (!logs.length) {
        return `<div class="empty-state"><strong>暂无 Webhook 投递</strong>收到短信或产生发送状态后，实际投递结果会显示在这里。</div>`;
    }
    return logs.slice(0, 20).map((log) => {
        const status = log.status || "pending";
        const rowClass = status === "failed" ? "failed" : ["not_configured", "disabled", "not_subscribed"].includes(status) ? "warning" : "";
        const response = log.responseCode == null ? webhookStatusLabel(status) : `HTTP ${log.responseCode}`;
        const attempts = log.attemptCount ? ` · 尝试 ${log.attemptCount} 次` : "";
        const detail = log.detail && log.detail !== response ? ` · ${escapeHtml(log.detail)}` : "";
        return `<div class="delivery-row ${rowClass}"><em>${escapeHtml(response)}</em><strong>${escapeHtml(webhookEventLabel(log.event))}</strong><span>${escapeHtml(log.eventId || "")} · ${formatTime(log.occurredAt)}${attempts}${detail}</span></div>`;
    }).join("");
}

function webhookSwitch(event, title, description, checked) {
    return `<div class="switch-row"><div class="switch-copy"><strong>${title}</strong><span>${description}</span></div><label class="switch"><input type="checkbox" data-webhook-event="${event}" ${checked} /><span class="slider"></span></label></div>`;
}

function pageDocsLegacy() {
    const endpoint = ENDPOINTS.find((item) => `${item.method} ${item.path}` === state.selectedEndpoint) || ENDPOINTS[0];
    const operation = OPENAPI_SPEC.paths[endpoint.path]?.[endpoint.operation] || {};
    const baseUrl = state.status?.localAddress || "http://手机IP:8080";
    const requestExamples = {
        "/api/v1/sms/send": { to: "13800138000", text: "你好，这是一次测试短信。", subscriptionId: 1 },
        "/api/v1/webhook": { enabled: true, url: "http://192.168.0.10:9000/webhook", token: "callback_token", events: ["sms.received", "sms.sent", "sms.delivered", "sms.failed"], maxRetries: 5, initialDelaySeconds: 2, maxDelaySeconds: 60 },
        "/api/v1/settings": { port: 8080, autoStart: false, defaultSubscriptionId: 1, sendRatePerMinute: 60, sentRetentionDays: 90, receivedRetentionDays: 90 }
    };
    const example = requestExamples[endpoint.path];
    const request = operation.requestBody ? JSON.stringify(example || {}, null, 2) : "此操作不需要请求体。";
    const curlLines = [`curl -X ${endpoint.method} '${baseUrl}${endpoint.path}'`, "  -H 'Authorization: Bearer access_token'"];
    if (example) {
        curlLines.push("  -H 'Content-Type: application/json'", `  -d '${JSON.stringify(example)}'`);
    }
    const curl = curlLines.join(" \\\n");
    const responses = Object.entries(operation.responses || {}).map(([code, response]) => `<tr><td><span class="chip ${code.startsWith("2") ? "green" : code === "401" ? "orange" : "red"}">${code}</span></td><td>${escapeHtml(response.description || "")}</td><td><code>${escapeHtml(response.content?.["application/json"]?.schema?.$ref?.split("/").pop() || response.content?.["application/json"]?.schema?.type || "—")}</code></td></tr>`).join("");
    const openapiJson = JSON.stringify(OPENAPI_SPEC, null, 2);
    return `${pageHead("API 文档", "OpenAPI 3.1 规范 · 所有接口均使用 Bearer API Token 鉴权", `<button class="ghost-button" data-action="copy-openapi">复制 OpenAPI JSON</button>`)}
        <div class="docs-layout"><aside class="endpoint-list"><div class="muted" style="padding:0 4px 4px">API 端点</div>${ENDPOINTS.map((item) => { const key = `${item.method} ${item.path}`; return `<button class="endpoint-button ${key === state.selectedEndpoint ? "selected" : ""}" data-endpoint="${escapeHtml(key)}"><span class="method ${methodClass(item.method)}">${item.method}</span><span class="endpoint-path">${item.path}</span><span class="endpoint-desc">${item.desc}</span></button>`; }).join("")}</aside>
        <section class="panel docs-main"><div class="docs-title"><span class="method ${methodClass(endpoint.method)}">${endpoint.method}</span><h2>${endpoint.path}</h2></div><p class="muted">${escapeHtml(operation.summary || endpoint.desc)}。规范版本：${OPENAPI_SPEC.openapi}。</p><div class="docs-meta"><div class="meta-box"><label>基础 URL</label><code>${escapeHtml(baseUrl)}</code></div><div class="meta-box"><label>认证方式</label><code>Authorization: Bearer access_token</code></div><div class="meta-box"><label>OpenAPI</label><code>3.1.0</code></div></div><div class="docs-code-grid"><div><h3>请求体（JSON）</h3><div class="code-card"><pre>${escapeHtml(request)}</pre></div></div><div><h3>响应定义</h3><table class="error-table"><thead><tr><th>状态码</th><th>说明</th><th>Schema</th></tr></thead><tbody>${responses || `<tr><td colspan="3">暂无响应定义</td></tr>`}</tbody></table></div></div><div style="margin-top:18px"><div class="code-tabs"><div class="tab-buttons"><button class="active">cURL</button><button type="button" data-action="copy-openapi">OpenAPI JSON</button></div><button class="primary-button" data-copy="${escapeHtml(curl)}">复制 cURL</button></div><div class="code-card"><pre>${escapeHtml(curl)}</pre></div></div><div style="margin-top:18px"><h3>OpenAPI 文档（JSON）</h3><div class="code-card docs-spec-code"><pre>${escapeHtml(openapiJson)}</pre></div></div></section></div>`;
}

function docsDraftFor(endpoint, operation, example) {
    const key = `${endpoint.method} ${endpoint.path}`;
    if (!state.docsDrafts[key]) {
        const params = {};
        (operation.parameters || []).forEach((parameter) => {
            params[parameter.name] = parameter.schema?.default ?? "";
        });
        state.docsDrafts[key] = {
            body: operation.requestBody ? JSON.stringify(example || {}, null, 2) : "",
            params
        };
    }
    state.docsDrafts[key].params ||= {};
    return state.docsDrafts[key];
}

function docsQueryString(operation, draft) {
    return (operation.parameters || []).map((parameter) => {
        const value = String(draft.params?.[parameter.name] ?? "").trim();
        return value ? `${encodeURIComponent(parameter.name)}=${encodeURIComponent(value)}` : "";
    }).filter(Boolean).join("&");
}

function prettyDocsBody(raw) {
    if (!raw) return "（空响应）";
    try {
        return JSON.stringify(JSON.parse(raw), null, 2);
    } catch (_) {
        return raw;
    }
}

function renderDocsResponse() {
    if (state.docsRequesting) {
        return `<div class="docs-response-empty"><span class="docs-response-spinner">↻</span><strong>请求发送中…</strong><p>正在等待手机网关返回响应。</p></div>`;
    }
    const result = state.docsResponse;
    if (!result) {
        return `<div class="docs-response-empty"><span class="docs-response-placeholder">⌁</span><strong>发送请求后查看响应</strong><p>响应状态、响应头和 JSON 内容会显示在这里。</p></div>`;
    }
    if (result.error) {
        return `<div class="docs-response-empty error"><span class="docs-response-placeholder">!</span><strong>请求失败</strong><p>${escapeHtml(result.error)}</p></div>`;
    }
    const statusClass = result.ok ? "success" : "error";
    const statusText = `${result.status ?? "ERROR"} ${result.statusText || ""}`.trim();
    const headers = result.headers || [];
    return `<div class="docs-response-summary"><div><span class="docs-http-status ${statusClass}">${escapeHtml(statusText)}</span><span class="docs-response-duration">${result.durationMs} ms</span></div><span class="docs-response-count">${headers.length} 个响应头</span></div><pre class="docs-response-code">${escapeHtml(result.body || "（空响应）")}</pre><details class="docs-response-headers"><summary>查看响应头</summary><div class="table-wrap"><table class="data-table"><thead><tr><th>名称</th><th>值</th></tr></thead><tbody>${headers.length ? headers.map((header) => `<tr><td class="id-text">${escapeHtml(header.name)}</td><td>${escapeHtml(header.value)}</td></tr>`).join("") : `<tr><td colspan="2">暂无响应头</td></tr>`}</tbody></table></div></details>`;
}

function pageDocs() {
    const endpoint = ENDPOINTS.find((item) => `${item.method} ${item.path}` === state.selectedEndpoint) || ENDPOINTS[0];
    const operation = OPENAPI_SPEC.paths[endpoint.path]?.[endpoint.operation] || {};
    const requestExamples = {
        "/api/v1/sms/send": { to: "13800138000", text: "你好，这是一次测试短信。", subscriptionId: 1 },
        "/api/v1/webhook": { enabled: true, url: "http://192.168.0.10:9000/webhook", token: "callback_token", events: ["sms.received", "sms.sent", "sms.delivered", "sms.failed"], maxRetries: 5, initialDelaySeconds: 2, maxDelaySeconds: 60 },
        "/api/v1/settings": { port: 8080, autoStart: false, defaultSubscriptionId: 1, sendRatePerMinute: 60, sentRetentionDays: 90, receivedRetentionDays: 90 }
    };
    const example = requestExamples[endpoint.path];
    const draft = docsDraftFor(endpoint, operation, example);
    const query = docsQueryString(operation, draft);
    const requestPath = `${endpoint.path}${query ? `?${query}` : ""}`;
    const baseUrl = String(state.status?.localAddress || window.location.origin || "http://手机IP:8080").replace(/\/$/, "");
    const displayUrl = `${baseUrl}${requestPath}`;
    const parameters = operation.parameters || [];
    const parameterMarkup = parameters.length ? `<div class="docs-parameters"><div class="docs-subsection-head"><strong>Query 参数</strong><span>可选</span></div>${parameters.map((parameter) => `<div class="docs-param-row"><label for="docs-param-${escapeHtml(parameter.name)}"><code>${escapeHtml(parameter.name)}</code><small>${escapeHtml(parameter.description || "URL 查询参数")}</small></label><input id="docs-param-${escapeHtml(parameter.name)}" data-docs-param="${escapeHtml(parameter.name)}" value="${escapeHtml(draft.params?.[parameter.name] ?? "")}" placeholder="${escapeHtml(String(parameter.schema?.default ?? ""))}" /></div>`).join("")}</div>` : "";
    const bodyMarkup = operation.requestBody ? `<div class="docs-body-editor"><div class="docs-subsection-head"><strong>Body</strong><span>application/json</span></div><textarea id="docs-request-body" spellcheck="false" placeholder="请输入 JSON 请求体">${escapeHtml(draft.body)}</textarea></div>` : `<div class="docs-no-body">此接口不需要请求体</div>`;
    const curlLines = [`curl -X ${endpoint.method} '${displayUrl}'`, "  -H 'Authorization: Bearer access_token'"];
    if (operation.requestBody) {
        curlLines.push("  -H 'Content-Type: application/json'", `  -d '${draft.body}'`);
    }
    const curl = curlLines.join(" \\\n");
    const responses = Object.entries(operation.responses || {}).map(([code, response]) => `<tr><td><span class="chip ${code.startsWith("2") ? "green" : code === "401" ? "orange" : "red"}">${code}</span></td><td>${escapeHtml(response.description || "")}</td><td><code>${escapeHtml(response.content?.["application/json"]?.schema?.$ref?.split("/").pop() || response.content?.["application/json"]?.schema?.type || "—")}</code></td></tr>`).join("");
    const openapiJson = JSON.stringify(OPENAPI_SPEC, null, 2);
    return `${pageHead("API 文档", "OpenAPI 3.1 规范 · 支持在页面内直接调试接口", `<button type="button" class="ghost-button" data-action="copy-openapi">复制 OpenAPI JSON</button>`)}
        <div class="docs-layout docs-workbench"><aside class="endpoint-list docs-sidebar"><div class="endpoint-list-head"><span class="docs-collection-kicker">LOCAL SMS</span><strong>API Collection</strong><small>Bearer Token · ${ENDPOINTS.length} 个接口</small></div><div class="endpoint-group-label">ENDPOINTS</div>${ENDPOINTS.map((item) => { const key = `${item.method} ${item.path}`; return `<button type="button" class="endpoint-button ${key === state.selectedEndpoint ? "selected" : ""}" data-endpoint="${escapeHtml(key)}"><span><span class="method ${methodClass(item.method)}">${item.method}</span><span class="endpoint-path">${item.path}</span></span><span class="endpoint-desc">${item.desc}</span></button>`; }).join("")}</aside>
        <section class="docs-main"><section class="panel docs-request-panel"><div class="docs-title"><span class="method ${methodClass(endpoint.method)}">${endpoint.method}</span><h2>${endpoint.path}</h2></div><p class="muted">${escapeHtml(operation.summary || endpoint.desc)}。规范版本：${OPENAPI_SPEC.openapi}。</p><form id="docs-request-form"><div class="docs-urlbar"><span class="docs-request-method ${methodClass(endpoint.method)}">${endpoint.method}</span><input aria-label="请求地址" readonly value="${escapeHtml(displayUrl)}" /><button class="primary-button docs-send-button" type="submit" ${state.docsRequesting ? "disabled" : ""}>${state.docsRequesting ? "请求中…" : "发送请求"}</button></div><div class="docs-auth-note"><span class="status-dot"></span>${state.token ? "Bearer Token 已配置，请求会自动附加 Authorization" : "尚未配置 Token，请先点击右上角设置 API Token"}</div>${parameterMarkup}${bodyMarkup}</form></section><section class="panel docs-response-panel"><div class="docs-section-head"><div><h2 class="panel-title">Response</h2><span class="trend-caption">本次请求的实际响应</span></div>${state.docsResponse?.status ? `<span class="docs-response-mini-status ${state.docsResponse.ok ? "success" : "error"}">${state.docsResponse.status}</span>` : ""}</div>${renderDocsResponse()}</section><section class="panel docs-reference-panel"><div class="docs-section-head"><div><h2 class="panel-title">接口说明</h2><span class="trend-caption">响应定义与可复制的 cURL 命令</span></div><span class="docs-openapi-badge">OpenAPI 3.1</span></div><div class="docs-code-grid"><div><h3>响应定义</h3><table class="error-table"><thead><tr><th>状态码</th><th>说明</th><th>Schema</th></tr></thead><tbody>${responses || `<tr><td colspan="3">暂无响应定义</td></tr>`}</tbody></table></div><div><div class="code-tabs"><h3>cURL</h3><button type="button" class="ghost-button" data-copy="${escapeHtml(curl)}">复制</button></div><div class="code-card"><pre>${escapeHtml(curl)}</pre></div></div></div><details class="docs-openapi-details"><summary>查看完整 OpenAPI JSON</summary><div class="code-card docs-spec-code"><pre>${escapeHtml(openapiJson)}</pre></div></details></section></section></div>`;
}

async function executeDocsRequest(event) {
    event.preventDefault();
    const endpoint = ENDPOINTS.find((item) => `${item.method} ${item.path}` === state.selectedEndpoint) || ENDPOINTS[0];
    const operation = OPENAPI_SPEC.paths[endpoint.path]?.[endpoint.operation] || {};
    const key = `${endpoint.method} ${endpoint.path}`;
    const draft = state.docsDrafts[key] || { body: "", params: {} };
    draft.params = {};
    document.querySelectorAll("[data-docs-param]").forEach((input) => {
        draft.params[input.dataset.docsParam] = input.value;
    });
    draft.body = document.querySelector("#docs-request-body")?.value || "";
    state.docsDrafts[key] = draft;
    let parsedBody;
    if (operation.requestBody && draft.body.trim()) {
        try {
            parsedBody = JSON.parse(draft.body);
        } catch (_) {
            toast("请求体不是有效的 JSON");
            return;
        }
    }
    const query = docsQueryString(operation, draft);
    const requestPath = `${endpoint.path}${query ? `?${query}` : ""}`;
    const requestToken = state.token;
    const requestTokenGeneration = state.tokenGeneration;
    const headers = {};
    if (requestToken) headers.Authorization = `Bearer ${requestToken}`;
    if (parsedBody !== undefined) headers["Content-Type"] = "application/json";
    const options = { method: endpoint.method, headers };
    if (parsedBody !== undefined) options.body = JSON.stringify(parsedBody);
    state.docsRequesting = true;
    state.docsResponse = null;
    render();
    const startedAt = performance.now();
    try {
        const response = await fetch(requestPath, options);
        const raw = await response.text();
        state.docsResponse = {
            ok: response.ok,
            status: response.status,
            statusText: response.statusText,
            durationMs: Math.round(performance.now() - startedAt),
            headers: Array.from(response.headers.entries()).map(([name, value]) => ({ name, value })),
            body: prettyDocsBody(raw)
        };
        if (response.status === 401 &&
            requestToken &&
            requestToken === state.token &&
            requestTokenGeneration === state.tokenGeneration) {
            showAuthPage();
        }
    } catch (error) {
        state.docsResponse = { error: error.message || "网络请求失败" };
        toast(error.message || "网络请求失败");
    } finally {
        state.docsRequesting = false;
        render();
    }
}

function pageSettings() {
    const settings = state.settings || { port: 8080, autoStart: false, defaultSubscriptionId: 1, sendRatePerMinute: 60, sentRetentionDays: 90, receivedRetentionDays: 90 };
    return `${pageHead("设置", "管理短信发送和本地数据保留策略")}
        <div class="settings-grid"><section class="settings-card"><h2><span class="settings-icon">▣</span>短信与 SIM</h2><div class="settings-row"><div><label>默认 SIM 卡</label><small>用于页面发送短信的默认订阅</small></div><select id="settings-sim"><option value="1" ${settings.defaultSubscriptionId === 1 ? "selected" : ""}>SIM 1</option><option value="2" ${settings.defaultSubscriptionId === 2 ? "selected" : ""}>SIM 2</option></select></div><div class="settings-row"><div><label>发送限速</label><small>每分钟最多发送短信条数</small></div><input id="settings-rate" type="number" min="1" max="300" value="${settings.sendRatePerMinute || 60}" /></div></section>
        <section class="settings-card"><h2><span class="settings-icon">◉</span>数据管理</h2><div class="settings-row"><div><label>发送记录保留</label><small>自动清理超过保留天数的记录</small></div><select id="settings-sent-retention"><option value="90" ${settings.sentRetentionDays === 90 ? "selected" : ""}>90 天</option><option value="30" ${settings.sentRetentionDays === 30 ? "selected" : ""}>30 天</option><option value="365" ${settings.sentRetentionDays === 365 ? "selected" : ""}>365 天</option></select></div><div class="settings-row"><div><label>收信记录保留</label><small>自动清理超过保留天数的记录</small></div><select id="settings-received-retention"><option value="90" ${settings.receivedRetentionDays === 90 ? "selected" : ""}>90 天</option><option value="30" ${settings.receivedRetentionDays === 30 ? "selected" : ""}>30 天</option><option value="365" ${settings.receivedRetentionDays === 365 ? "selected" : ""}>365 天</option></select></div><div class="settings-row settings-danger-row"><div><label>清理发送记录</label><small>永久删除本机保存的全部发送记录</small></div><button class="danger-button" data-action="clear-sent-records" type="button">清理</button></div><div class="settings-row settings-danger-row"><div><label>清理收信记录</label><small>永久删除本机保存的全部收信记录</small></div><button class="danger-button" data-action="clear-received-records" type="button">清理</button></div><div class="settings-row settings-danger-row"><div><label>清理 Webhook 记录</label><small>永久删除本机保存的全部 Webhook 日志</small></div><button class="danger-button" data-action="clear-webhook-records" type="button">清理</button></div></section><div class="settings-actions"><button class="ghost-button" data-action="reset-settings" type="button">恢复默认</button><button class="primary-button" data-action="save-settings" type="button">保存设置</button></div></div>`;
}

function clearSessionState() {
    state.token = "";
    state.tokenGeneration += 1;
    state.loadGeneration += 1;
    state.modalOpen = false;
    state.sendModalOpen = false;
    state.loadedRoute = null;
    state.loadingRoute = null;
    state.status = { ...DEMO_STATUS };
    state.stats = { ...DEMO_STATS };
    state.sent = [];
    state.received = [];
    state.recordDrawer = { kind: "", id: "" };
    state.webhook = { ...DEFAULT_WEBHOOK };
    state.webhookLogs = [];
    state.sims = [{ subscriptionId: null, simSlot: 1, label: "默认 SIM", number: null }];
    state.docsResponse = null;
    state.docsRequesting = false;
    writeStoredToken("");
}

function showAuthPage(message = "Token 无效或未授权，请重新输入手机端 Token。") {
    clearSessionState();
    state.authError = message;
    render();
    toast("API Token 无效，请重新输入");
}

function logout(event) {
    event?.preventDefault();
    clearSessionState();
    state.authError = "";
    render();
    toast("已退出登录，Token 已清除");
}

async function loadRoute(route) {
    const generation = ++state.loadGeneration;
    try {
        if (route === "overview") {
            const [status, stats, sims] = await Promise.all([
                api("/api/v1/status"),
                api("/api/v1/stats"),
                api("/api/v1/sims")
            ]);
            state.status = status;
            state.stats = stats;
            state.sims = sims.data || state.sims;
        } else if (route === "sent") {
            const [sent, webhook, logs, sims] = await Promise.all([
                api("/api/v1/sms/sent?limit=500"),
                api("/api/v1/webhook"),
                api("/api/v1/webhook/logs?limit=500"),
                api("/api/v1/sims")
            ]);
            state.sent = sent.data || [];
            state.webhook = webhook;
            state.webhookLogs = logs.data || [];
            state.sims = sims.data || state.sims;
            state.sentFilters.page = 1;
            state.selectedSentId = state.sent.some((row) => row.messageId === state.selectedSentId)
                ? state.selectedSentId
                : "";
            if (state.recordDrawer.kind === "sent" && !state.sent.some((row) => row.messageId === state.recordDrawer.id)) {
                state.recordDrawer = { kind: "", id: "" };
            }
        } else if (route === "received") {
            const [received, webhook, sims] = await Promise.all([
                api("/api/v1/sms/received?limit=500"),
                api("/api/v1/webhook"),
                api("/api/v1/sims")
            ]);
            state.received = received.data || [];
            state.webhook = webhook;
            state.sims = sims.data || state.sims;
            state.receivedFilters.page = 1;
            state.selectedReceivedId = state.received.some((row) => row.messageId === state.selectedReceivedId)
                ? state.selectedReceivedId
                : "";
            if (state.recordDrawer.kind === "received" && !state.received.some((row) => row.messageId === state.recordDrawer.id)) {
                state.recordDrawer = { kind: "", id: "" };
            }
        } else if (route === "webhook") {
            const [webhook, logs] = await Promise.all([
                api("/api/v1/webhook"),
                api("/api/v1/webhook/logs?limit=100")
            ]);
            state.webhook = webhook;
            state.webhookLogs = logs.data || [];
        } else if (route === "docs") {
            state.status = await api("/api/v1/status");
        } else if (route === "mcp") {
            state.status = await api("/api/v1/status");
        } else if (route === "settings") {
            state.settings = await api("/api/v1/settings");
        }
        state.demo = false;
        if (state.route === route && state.loadGeneration === generation) {
            state.loadedRoute = route;
            state.modalOpen = false;
            render();
        }
    } catch (error) {
        if (state.route === route && state.loadGeneration === generation) {
            state.demo = true;
            state.loadedRoute = route;
            render();
        }
    } finally {
        if (state.loadingRoute === route && state.loadGeneration === generation) state.loadingRoute = null;
    }
}

async function api(path, options = {}) {
    const requestToken = state.token;
    const requestTokenGeneration = state.tokenGeneration;
    const headers = { ...(options.headers || {}) };
    if (requestToken) headers.Authorization = `Bearer ${requestToken}`;
    if (options.body && !headers["Content-Type"]) headers["Content-Type"] = "application/json";
    const response = await fetch(path, { ...options, headers });
    const raw = await response.text();
    let data = {};
    try { data = raw ? JSON.parse(raw) : {}; } catch (_) { data = { raw }; }
    if (response.status === 401 &&
        requestToken &&
        requestToken === state.token &&
        requestTokenGeneration === state.tokenGeneration) {
        showAuthPage();
    }
    if (response.ok &&
        requestToken &&
        requestToken === state.token &&
        requestTokenGeneration === state.tokenGeneration) {
        state.authError = "";
        state.modalOpen = false;
    }
    if (!response.ok) throw Object.assign(new Error(data.message || "请求失败"), { response, data });
    return data;
}

function bindGlobalActions() {
    window.onhashchange = () => {
        const nextRoute = getRoute();
        if (state.recordDrawer.kind && state.recordDrawer.kind !== nextRoute) {
            state.recordDrawer = { kind: "", id: "" };
        }
        state.route = nextRoute;
        state.sendModalOpen = false;
        state.loadedRoute = null;
        render();
        startRouteLoad(state.route);
    };
    window.onkeydown = (event) => {
        if (event.key === "Escape" && state.recordDrawer.kind) closeRecordDrawer(event);
    };
    document.querySelector('[data-action="open-token"]')?.addEventListener("click", () => {
        state.modalOpen = true;
        state.authError = "";
        render();
        document.querySelector("#token-input")?.focus();
    });
    document.querySelector('[data-action="close-token"]')?.addEventListener("click", () => {
        state.modalOpen = false;
        state.authError = "";
        render();
    });
    document.querySelector('[data-action="toggle-theme"]')?.addEventListener("click", () => {
        state.theme = state.theme === "dark" ? "light" : "dark";
        writeStoredPreference("sms-gateway-theme", state.theme);
        render();
    });
    document.querySelector('[data-action="toggle-sidebar"]')?.addEventListener("click", () => {
        state.sidebarCollapsed = !state.sidebarCollapsed;
        writeStoredPreference("sms-gateway-sidebar-collapsed", state.sidebarCollapsed ? "1" : "0");
        render();
    });
    document.querySelector("#token-form")?.addEventListener("submit", saveToken);
    const tokenSaveButton = document.querySelector('[data-action="save-token"]');
    if (tokenSaveButton && tokenSaveButton.type !== "submit") tokenSaveButton.addEventListener("click", saveToken);
    document.querySelector('[data-action="logout"]')?.addEventListener("click", logout);
    document.querySelector('[data-action="open-send"]')?.addEventListener("click", () => {
        state.sendModalOpen = true;
        state.modalOpen = false;
        render();
        document.querySelector("#modal-send-to")?.focus();
        refreshSimsForSending();
    });
    document.querySelectorAll('[data-action="close-send"]').forEach((button) => button.addEventListener("click", closeSendModal));
    document.querySelector("#send-modal")?.addEventListener("click", (event) => {
        if (event.target === event.currentTarget) closeSendModal();
    });
}

async function refreshSimsForSending() {
    try {
        const result = await api("/api/v1/sims");
        if (!Array.isArray(result.data) || result.data.length === 0) return;
        state.sims = result.data;
        const select = document.querySelector("#modal-send-sim");
        if (!select || !state.sendModalOpen) return;
        const selectedValue = select.value;
        select.innerHTML = simOptionsHtml();
        if ([...select.options].some((option) => option.value === selectedValue)) {
            select.value = selectedValue;
        }
    } catch (error) {
        if (state.sendModalOpen && error.response?.status !== 401) {
            toast(error.message || "读取 SIM 卡失败");
        }
    }
}

function saveToken(event) {
    event.preventDefault();
    const input = document.querySelector("#token-input");
    const token = input ? input.value.trim() : "";
    if (!token) {
        toast("请输入手机端显示的 API Token");
        input?.focus();
        return;
    }
    state.token = token;
    state.tokenGeneration += 1;
    state.authError = "";
    const persisted = writeStoredToken(state.token);
    state.modalOpen = false;
    state.demo = false;
    state.loadedRoute = null;
    state.loadingRoute = null;
    toast(persisted ? "Token 已保存，正在连接手机服务" : "Token 已保存到本页，正在连接手机服务");
    render();
    startRouteLoad(state.route);
}

function closeSendModal(event) {
    event?.preventDefault();
    state.sendModalOpen = false;
    render();
}

function openRecordDrawer(kind, messageId, event) {
    event?.preventDefault();
    event?.stopPropagation();
    if ((kind !== "sent" && kind !== "received") || !messageId) return;
    state.recordDrawer = { kind, id: messageId };
    if (kind === "sent") state.selectedSentId = messageId;
    if (kind === "received") state.selectedReceivedId = messageId;
    render();
}

function closeRecordDrawer(event) {
    event?.preventDefault();
    event?.stopPropagation();
    state.recordDrawer = { kind: "", id: "" };
    render();
}

function bindPageActions() {
    document.querySelectorAll("[data-send-to]").forEach((input) => {
        const updatePreview = () => updateRecipientPreview(input.form);
        input.addEventListener("input", updatePreview);
        input.form?.querySelector("[data-send-country]")?.addEventListener("change", updatePreview);
        updatePreview();
    });
    document.querySelectorAll("[data-send-text]").forEach((input) => {
        const count = input.form?.querySelector("[data-send-count]");
        const updateCount = () => {
            if (count) count.textContent = input.value.length;
        };
        input.addEventListener("input", updateCount);
        input.form?.addEventListener("reset", () => window.setTimeout(updateCount, 0));
        updateCount();
    });
    document.querySelector("#send-form")?.addEventListener("submit", sendSms);
    document.querySelector("#send-modal-form")?.addEventListener("submit", sendSms);
    document.querySelector("#sent-filter-form")?.addEventListener("submit", (event) => {
        event.preventDefault();
        state.sentFilters = {
            query: document.querySelector("#sent-query").value.trim(),
            from: document.querySelector("#sent-from").value,
            to: document.querySelector("#sent-to").value,
            status: document.querySelector("#sent-status").value,
            page: 1
        };
        render();
    });
    document.querySelector("#received-filter-form")?.addEventListener("submit", (event) => {
        event.preventDefault();
        state.receivedFilters = {
            query: document.querySelector("#received-query").value.trim(),
            from: document.querySelector("#received-from").value,
            to: document.querySelector("#received-to").value,
            status: document.querySelector("#received-status").value,
            page: 1
        };
        render();
    });
    document.querySelector('[data-action="reset-sent-filter"]')?.addEventListener("click", () => {
        state.sentFilters = { query: "", from: "", to: "", status: "all", page: 1 };
        render();
    });
    document.querySelector('[data-action="reset-received-filter"]')?.addEventListener("click", () => {
        state.receivedFilters = { query: "", from: "", to: "", status: "all", page: 1 };
        render();
    });
    document.querySelectorAll('[data-action="change-page"]').forEach((button) => button.addEventListener("click", () => {
        const list = button.dataset.list;
        const page = Number(button.dataset.page);
        if (button.disabled || !Number.isFinite(page) || page < 1) return;
        if (list === "sent") state.sentFilters.page = page;
        if (list === "received") state.receivedFilters.page = page;
        render();
    }));
    document.querySelectorAll('[data-action="select-received"]').forEach((row) => row.addEventListener("click", (event) => {
        openRecordDrawer("received", row.dataset.id || "", event);
    }));
    document.querySelectorAll('[data-action="select-sent"]').forEach((row) => row.addEventListener("click", (event) => {
        openRecordDrawer("sent", row.dataset.id || "", event);
    }));
    document.querySelectorAll('[data-action="open-record-drawer"]').forEach((button) => button.addEventListener("click", (event) => {
        openRecordDrawer(button.dataset.kind || "", button.dataset.id || "", event);
    }));
    document.querySelectorAll('[data-action="close-record-drawer"]').forEach((button) => button.addEventListener("click", closeRecordDrawer));
    document.querySelectorAll('[data-action="open-activity"]').forEach((row) => row.addEventListener("click", () => {
        const route = row.dataset.activityRoute;
        const messageId = row.dataset.activityId || "";
        if (route === "sent") state.selectedSentId = messageId;
        if (route === "received") state.selectedReceivedId = messageId;
        if (route !== "sent" && route !== "received") return;
        state.recordDrawer = { kind: route, id: messageId };
        window.location.hash = `#${route}`;
    }));
    document.querySelector("#webhook-form")?.addEventListener("submit", saveWebhook);
    document.querySelector("#mcp-config-form")?.addEventListener("submit", saveMcpConfig);
    document.querySelector("#mcp-config-form")?.addEventListener("input", updateMcpPreview);
    document.querySelector('[data-action="copy-mcp-config"]')?.addEventListener("click", () => copyText(mcpConfigJson(mcpDraftFromForm())));
    document.querySelector('[data-action="copy-mcp-prompt"]')?.addEventListener("click", () => copyText(mcpAgentPrompt(mcpDraftFromForm())));
    document.querySelector('[data-action="test-mcp"]')?.addEventListener("click", testMcpConnection);
    document.querySelector("#docs-request-form")?.addEventListener("submit", executeDocsRequest);
    document.querySelector("#docs-request-body")?.addEventListener("input", (event) => {
        const draft = state.docsDrafts[state.selectedEndpoint];
        if (draft) draft.body = event.target.value;
    });
    document.querySelectorAll("[data-docs-param]").forEach((input) => input.addEventListener("input", () => {
        const draft = state.docsDrafts[state.selectedEndpoint];
        if (draft) draft.params[input.dataset.docsParam] = input.value;
    }));
    document.querySelectorAll("[data-endpoint]").forEach((button) => button.addEventListener("click", () => {
        state.selectedEndpoint = button.dataset.endpoint;
        state.docsResponse = null;
        state.docsRequesting = false;
        render();
    }));
    document.querySelectorAll("[data-copy]").forEach((button) => button.addEventListener("click", () => copyText(button.dataset.copy)));
    document.querySelectorAll('[data-action="copy-openapi"]').forEach((button) => button.addEventListener("click", () => copyText(JSON.stringify(OPENAPI_SPEC, null, 2))));
    document.querySelector('[data-action="test-webhook"]')?.addEventListener("click", testWebhook);
    document.querySelector('[data-action="rotate-token"]')?.addEventListener("click", rotateToken);
    document.querySelector('[data-action="save-settings"]')?.addEventListener("click", saveSettings);
    document.querySelector('[data-action="reset-settings"]')?.addEventListener("click", () => toast("已恢复本页默认值（尚未提交）"));
    document.querySelector('[data-action="clear-sent-records"]')?.addEventListener("click", () => clearRecords("sent"));
    document.querySelector('[data-action="clear-received-records"]')?.addEventListener("click", () => clearRecords("received"));
    document.querySelector('[data-action="clear-webhook-records"]')?.addEventListener("click", () => clearRecords("webhook"));
}

async function sendSms(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const countryCode = form.querySelector("[data-send-country]")?.value || "+86";
    const to = composeRecipient(countryCode, form.querySelector("[data-send-to]")?.value || "");
    const text = form.querySelector("[data-send-text]")?.value.trim() || "";
    const simValue = form.querySelector("[data-send-sim]")?.value || "";
    const subscriptionId = simValue ? Number(simValue) : null;
    if (!to || !text) return toast("请填写手机号和短信内容");
    const isModal = form.id === "send-modal-form";
    const submitButton = form.querySelector('button[type="submit"]');
    const originalButtonText = submitButton?.textContent || "";
    if (submitButton) {
        submitButton.disabled = true;
        submitButton.textContent = "发送中…";
    }
    try {
        const result = await api("/api/v1/sms/send", { method: "POST", body: JSON.stringify({ to, text, subscriptionId }) });
        state.demo = false;
        toast(`已加入发送队列：${result.data?.messageId || ""}`);
        if (isModal) {
            state.sendModalOpen = false;
            state.loadedRoute = null;
            state.loadingRoute = null;
            render();
            startRouteLoad(state.route);
        } else {
            window.location.hash = "sent";
        }
    } catch (error) {
        toast(error.message || "发送失败");
    } finally {
        if (submitButton) {
            submitButton.disabled = false;
            submitButton.textContent = originalButtonText;
        }
    }
}

async function saveWebhook(event) {
    event.preventDefault();
    const events = [...document.querySelectorAll("[data-webhook-event]:checked")].map((input) => input.dataset.webhookEvent);
    const payload = {
        enabled: true,
        url: document.querySelector("#webhook-url").value.trim(),
        token: document.querySelector("#webhook-token").value,
        events,
        maxRetries: Number(document.querySelector("#max-retries").value),
        initialDelaySeconds: Number(document.querySelector("#initial-delay").value),
        maxDelaySeconds: Number(document.querySelector("#max-delay").value)
    };
    try {
        state.webhook = await api("/api/v1/webhook", { method: "PUT", body: JSON.stringify(payload) });
        state.demo = false;
        toast("Webhook 设置已保存");
        render();
    } catch (error) {
        toast(error.message || "保存失败");
    }
}

function saveMcpConfig(event) {
    event.preventDefault();
    const draft = mcpDraftFromForm();
    state.mcpConfig = {
        name: String(draft.name || DEFAULT_MCP_CONFIG.name).trim() || DEFAULT_MCP_CONFIG.name,
        createSkill: Boolean(draft.createSkill)
    };
    writeStoredPreference("sms-gateway-mcp-name", state.mcpConfig.name);
    writeStoredPreference("sms-gateway-mcp-create-skill", state.mcpConfig.createSkill ? "1" : "0");
    toast("MCP Server 配置已保存");
    render();
}

async function testMcpConnection() {
    const endpoint = mcpEndpoint();
    if (!/^https?:\/\//i.test(endpoint)) {
        toast("MCP Server 地址必须以 http:// 或 https:// 开头");
        return;
    }
    if (!state.token) {
        toast("请先设置 API Token 后再测试");
        return;
    }
    const headers = {
        Accept: "application/json, text/event-stream",
        "Content-Type": "application/json"
    };
    if (state.token) headers.Authorization = `Bearer ${state.token}`;
    try {
        const response = await fetch(endpoint, {
            method: "POST",
            headers,
            body: JSON.stringify({
                jsonrpc: "2.0",
                id: "mcp-web-test",
                method: "initialize",
                params: {
                    protocolVersion: "2025-11-25",
                    capabilities: {},
                    clientInfo: { name: "sms-gateway-web", version: APP_VERSION }
                }
            })
        });
        const raw = await response.text();
        let data = {};
        try { data = raw ? JSON.parse(raw) : {}; } catch (_) { data = {}; }
        if (!response.ok) throw new Error(data.error?.message || data.message || `HTTP ${response.status}`);
        const serverInfo = data.result?.serverInfo;
        toast(serverInfo ? `MCP 连接成功：${serverInfo.name || "Server"}` : "MCP Server 已响应");
    } catch (error) {
        toast(`MCP 连接失败：${error.message || "请求失败"}`);
    }
}

async function testWebhook() {
    try {
        await api("/api/v1/webhook/test", { method: "POST" });
        toast("测试回调已加入队列");
    } catch (error) {
        toast(error.message || "测试回调失败");
    }
}

async function rotateToken() {
    if (!window.confirm("重新生成 Token 后，已保存的客户端将无法继续访问，确定继续吗？")) return;
    try {
        const result = await api("/api/v1/settings/token/rotate", { method: "POST" });
        state.token = result.token || "";
        state.tokenGeneration += 1;
        state.authError = "";
        writeStoredToken(state.token);
        toast("Token 已重新生成并更新到当前浏览器");
        render();
    } catch (error) {
        toast(error.message || "Token 重置失败");
    }
}

async function saveSettings() {
    const payload = {
        defaultSubscriptionId: Number(document.querySelector("#settings-sim").value),
        sendRatePerMinute: Number(document.querySelector("#settings-rate").value),
        sentRetentionDays: Number(document.querySelector("#settings-sent-retention").value),
        receivedRetentionDays: Number(document.querySelector("#settings-received-retention").value)
    };
    try {
        state.settings = await api("/api/v1/settings", { method: "PUT", body: JSON.stringify(payload) });
        state.demo = false;
        toast("设置已保存");
        render();
    } catch (error) {
        toast(error.message || "设置保存失败");
    }
}

async function clearRecords(kind) {
    const configs = {
        sent: { label: "发送记录", path: "/api/v1/sms/sent" },
        received: { label: "收信记录", path: "/api/v1/sms/received" },
        webhook: { label: "Webhook 记录", path: "/api/v1/webhook/logs" }
    };
    const config = configs[kind];
    if (!config) return;
    if (!window.confirm(`将永久删除本机保存的全部${config.label}，删除后无法恢复。确定继续吗？`)) return;

    try {
        const result = await api(config.path, { method: "DELETE" });
        const count = Number(result.deletedCount) || 0;
        if (kind === "sent") {
            state.sent = [];
            state.selectedSentId = "";
            state.sentFilters.page = 1;
        } else if (kind === "received") {
            state.received = [];
            state.selectedReceivedId = "";
            state.receivedFilters.page = 1;
        } else {
            state.webhookLogs = [];
        }
        state.demo = false;
        state.loadedRoute = null;
        state.loadingRoute = null;
        render();
        startRouteLoad(state.route);
        toast(count > 0 ? `已清理 ${count} 条${config.label}` : `没有可清理的${config.label}`);
    } catch (error) {
        toast(error.message || `清理${config.label}失败`);
    }
}

function copyText(value) {
    const text = String(value ?? "");
    const legacyCopy = () => {
        const textarea = document.createElement("textarea");
        textarea.value = text;
        textarea.setAttribute("readonly", "");
        textarea.style.position = "fixed";
        textarea.style.top = "-1000px";
        textarea.style.left = "-1000px";
        textarea.style.opacity = "0";
        document.body.appendChild(textarea);
        textarea.focus();
        textarea.select();
        textarea.setSelectionRange(0, textarea.value.length);
        let copied = false;
        try {
            copied = document.execCommand("copy");
        } catch (_) {
            copied = false;
        }
        textarea.remove();
        toast(copied ? "已复制到剪贴板" : "复制失败，请手动复制");
    };
    if (!navigator.clipboard || !window.isSecureContext) {
        legacyCopy();
        return;
    }
    navigator.clipboard.writeText(text)
        .then(() => toast("已复制到剪贴板"))
        .catch(legacyCopy);
}

function toast(message) {
    document.querySelector(".toast")?.remove();
    const element = document.createElement("div");
    element.className = "toast";
    element.textContent = message;
    document.body.appendChild(element);
    window.setTimeout(() => element.remove(), 3000);
}

window.addEventListener("DOMContentLoaded", () => {
    render();
    startRouteLoad(state.route);
});
