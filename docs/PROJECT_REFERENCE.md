# 本地短信网关

一个运行在单台 Android 手机上的局域网短信网关。手机负责调用运营商 SIM 卡发送/接收短信，并在本机启动 HTTP 服务；同一局域网或已打通的 EasyTier、Tailscale 等组网设备可以通过 Web 控制台、HTTP API 或 MCP 访问它。

> 当前工程是一个可安装、可运行的 MVP。短信能力受 Android 版本、手机厂商系统、默认短信策略、运营商和 SIM 卡能力影响。请先在目标手机上验证发送、接收、送达回执以及锁屏后台运行。

## 功能概览

- Android 手机端首页：服务启动/停止、当前局域网地址、Wi‑Fi 状态、版本号和权限状态。
- 前台服务：应用切到后台、锁屏后继续维持 HTTP 服务；服务绑定 0.0.0.0，便于局域网和虚拟组网访问。
- 短信发送：支持国家区号、长短信分片、选择 SIM subscription、发送状态和送达状态回调。
- 短信接收：监听系统短信广播，并将收到的短信保存到本机记录。
- Web 控制台：概览、发送记录、收信记录、Webhook、API 文档、MCP Server、设置等页面。
- 记录详情：Web 端通过右侧抽屉查看发送/收信详情，手机端日志可以点行查看详情。
- Webhook：支持 sms.received、sms.sent、sms.delivered、sms.failed，带投递状态、重试和签名请求头。
- 内置测试 Webhook：默认地址为 http://127.0.0.1:<端口>/webhook/test，只返回成功，不处理业务数据。
- OpenAPI 风格 API 文档：Web 端可以查看接口、请求参数和示例，并直接发起请求。
- MCP Streamable HTTP：复用同一个端口和 API Token，通过 /mcp 提供短信网关工具。
- 数据清理：可以在 Web 端或手机端清理发送记录、收信记录和 Webhook 日志。
- GitHub Actions：推送版本 Tag 或发布 GitHub Release 时自动构建并上传 APK。

## 技术栈

| 项目 | 当前配置 |
| --- | --- |
| Android namespace | com.example.smsgateway |
| Application ID | com.example.smsgateway |
| minSdk | 26（Android 8.0） |
| compileSdk / targetSdk | 35 |
| Java / Kotlin JVM | 17 |
| Android Gradle Plugin | 8.6.1 |
| Kotlin Gradle Plugin | 2.0.21 |
| Gradle Wrapper | 8.7 |
| 默认服务端口 | 8080 |
| Web 服务绑定地址 | 0.0.0.0 |

## 项目结构

~~~text
.
├─ app/
│  └─ src/main/
│     ├─ assets/web/             # 内置 Web 控制台（HTML/CSS/JavaScript）
│     ├─ java/.../               # Android、HTTP API、MCP、短信和 Webhook 实现
│     ├─ res/                    # 手机端布局、图标、字符串和主题资源
│     └─ AndroidManifest.xml     # 权限、前台服务、短信广播接收器
├─ gradle/wrapper/               # Gradle Wrapper
├─ .github/workflows/            # GitHub Actions 构建流水线
├─ app/build.gradle.kts          # Android 模块和版本号
├─ build.gradle.kts              # 根项目插件版本
└─ settings.gradle.kts           # Gradle 仓库和模块配置
~~~

## 构建环境

本地构建需要：

1. Android SDK Platform 35 和对应 Build Tools。
2. JDK 17。
3. Git（如果需要提交或推送代码）。

Android Studio 可以直接打开项目并自动识别 Gradle Wrapper。命令行构建时，local.properties 只用于本机配置 Android SDK 路径，不要提交到 Git 仓库。

## 本地构建 APK

Windows PowerShell：

~~~powershell
.\gradlew.bat assembleDebug
~~~

macOS/Linux：

~~~bash
./gradlew assembleDebug
~~~

构建产物：

~~~text
app/build/outputs/apk/debug/app-debug.apk
~~~

assembleDebug 使用 Android Debug 签名，适合个人局域网部署和测试。若要发布到应用商店或作为正式生产包，应在 CI 中配置专用签名证书，并增加 release signing config；不要把 keystore、密码或 Token 提交到仓库。

## 安装到 Android 手机

手机和电脑可以通过 USB 或无线 ADB 连接。无线 ADB 示例：

~~~powershell
adb connect <手机局域网IP>:5555
adb -s <手机局域网IP>:5555 install -r app/build/outputs/apk/debug/app-debug.apk
~~~

首次启动后，请在手机系统设置中允许：

- 发送短信；
- 接收/读取短信；
- 读取电话状态和 SIM 信息；
- Android 13 及以上的通知权限；
- 忽略电池优化（如果系统提供此选项）。

小米等厂商系统还建议在“自启动、后台运行、电池策略、锁屏后运行”中允许本应用。仅勾选运行时权限不一定能保证厂商系统在锁屏后保活。

## 启动服务并访问 Web 控制台

1. 打开手机端应用，确认短信和 SIM 权限已授权。
2. 在首页点击“启动服务”。
3. 确认手机和电脑处于同一 Wi‑Fi，或电脑能够路由到手机的 EasyTier/Tailscale 地址。
4. 使用手机首页显示的局域网地址访问：

   ~~~text
   http://<手机IP>:8080/
   ~~~

5. 第一次访问 Web 控制台时，在登录页输入手机端显示的 API Token。

服务端监听所有网卡，但“手机显示的地址”仍取决于当前网络。切换 Wi‑Fi、关闭 Wi‑Fi 或切换 EasyTier 后，应以手机实时显示的地址为准；不要把 127.0.0.1 当作其他设备可访问的地址。

## HTTP API

除内置测试接收器外，/api/* 接口都需要 API Token。请求头格式：

~~~http
Authorization: Bearer <API_TOKEN>
~~~

### 接口清单

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | /api/v1/status | 服务状态、局域网地址、端口和计数 |
| GET | /api/v1/stats | 今日统计、24 小时收发趋势、近期活动 |
| GET | /api/v1/sims | 当前可用 SIM subscription 列表 |
| POST | /api/v1/sms/send | 提交短信发送任务 |
| GET | /api/v1/sms/sent?limit=100 | 查询发送记录 |
| DELETE | /api/v1/sms/sent | 清理全部发送记录 |
| GET | /api/v1/sms/received?limit=100 | 查询收信记录 |
| DELETE | /api/v1/sms/received | 清理全部收信记录 |
| GET | /api/v1/webhook | 查询 Webhook 配置（不返回 Token 明文） |
| PUT | /api/v1/webhook | 更新 Webhook 配置 |
| GET | /api/v1/webhook/logs?limit=100 | 查询 Webhook 投递日志 |
| DELETE | /api/v1/webhook/logs | 清理全部 Webhook 日志 |
| POST | /api/v1/webhook/test | 加入一次测试回调任务 |
| GET | /api/v1/settings | 查询服务设置 |
| PUT | /api/v1/settings | 更新服务设置，端口修改后需重启服务 |
| POST | /api/v1/settings/token/rotate | 生成新的 API Token |
| POST | /webhook/test | 内置测试接收器，无需 API Token |

### 发送短信

请求体中的 to 建议使用 E.164 格式，并带国家区号；中国大陆号码例如 +8613800138000。subscriptionId 用于选择指定 SIM，省略时由系统选择默认 SIM。

~~~bash
curl -X POST "http://<手机IP>:8080/api/v1/sms/send" \
  -H "Authorization: Bearer <API_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "to": "+8613800138000",
    "text": "这是一条测试短信",
    "subscriptionId": 1
  }'
~~~

成功提交返回 HTTP 202，记录初始状态通常为 queued 或 sending。之后可以通过发送记录观察 sent、delivered 或 failed。clientRequestId 是可选的幂等键，适合程序重试时避免重复提交；Web 控制台不展示该字段，但 API 和 MCP 仍支持它。

### 查询记录

~~~bash
curl "http://<手机IP>:8080/api/v1/sms/sent?limit=50" \
  -H "Authorization: Bearer <API_TOKEN>"

curl "http://<手机IP>:8080/api/v1/sms/received?limit=50" \
  -H "Authorization: Bearer <API_TOKEN>"
~~~

记录按最新优先返回，limit 范围为 1–500。当前版本的记录保存在手机本地应用存储中，不上传到第三方服务。

## Webhook

Webhook 配置使用 url、token、enabled、events、maxRetries、initialDelaySeconds 和 maxDelaySeconds。events 可包含：

~~~text
sms.received
sms.sent
sms.delivered
sms.failed
~~~

示例配置请求：

~~~bash
curl -X PUT "http://<手机IP>:8080/api/v1/webhook" \
  -H "Authorization: Bearer <API_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": true,
    "url": "http://<你的服务>/sms/webhook",
    "token": "<WEBHOOK_TOKEN>",
    "events": ["sms.received", "sms.sent", "sms.delivered", "sms.failed"],
    "maxRetries": 5,
    "initialDelaySeconds": 2,
    "maxDelaySeconds": 60
  }'
~~~

投递请求为 JSON，外层包含 event、eventId、occurredAt、deviceId 和 data。同时会发送以下请求头：

~~~text
X-SMS-Gateway-Event
X-SMS-Gateway-Id
X-SMS-Gateway-Timestamp
X-SMS-Gateway-Signature: sha256=<HMAC-SHA256(timestamp + ".<body>")>
X-SMS-Gateway-Token
~~~

目标地址返回 2xx 即视为成功；其他响应或网络异常会按配置重试，并记录在 Webhook 日志中。没有配置外部回调地址时，短信本身仍会正常保存，记录不会因为“未回调”而被判定为短信失败。

## MCP Server

手机端直接在同一个 HTTP 服务中提供 MCP Streamable HTTP 端点：

~~~text
POST http://<手机IP>:8080/mcp
~~~

MCP 请求同样使用 API Token：

~~~http
Authorization: Bearer <API_TOKEN>
~~~

Web 控制台的“MCP Server”页面会根据当前访问地址动态生成配置 JSON，并提供复制给 Agent 的配置和提示词。配置示例：

~~~json
{
  "mcpServers": {
    "local-sms-gateway": {
      "url": "http://<手机IP>:8080/mcp",
      "headers": {
        "Authorization": "Bearer <API_TOKEN>"
      }
    }
  }
}
~~~

当前提供的主要 MCP 工具包括：

- get_gateway_status、get_gateway_stats；
- list_sim_cards；
- send_sms；
- list_sent_messages、list_received_messages；
- get_webhook_config、update_webhook、test_webhook、list_webhook_logs；
- get_gateway_settings、update_gateway_settings；
- clear_sent_records、clear_received_records、clear_webhook_logs。

清理类工具要求显式传入 confirm=true。建议给 Agent 的指令中保留“发送短信前确认号码和内容、清理记录前确认”的约束。

## GitHub Actions 与 Release

工作流文件位于 .github/workflows/release-apk.yml，支持：

- 手动 workflow_dispatch：只构建并上传 APK 构建产物；
- 推送 v* Tag：构建 APK，并自动创建/更新 GitHub Release；
- 发布 GitHub Release：构建 APK，并把 APK 附加到对应 Release。

推荐发布流程：

~~~bash
git add .
git commit -m "chore: prepare release"
git push origin main
git tag v1.0.0
git push origin v1.0.0
~~~

构建完成后，APK 文件名类似：

~~~text
sms-gateway-1.0.0.apk
~~~

当前流水线产出的是可直接安装的 Debug 签名 APK，适合个人部署。若仓库未来用于公开发布，建议在 GitHub Actions Secrets 中配置 KEYSTORE_BASE64、KEYSTORE_PASSWORD、KEY_ALIAS 和 KEY_PASSWORD，再把流水线切换为正式签名的 assembleRelease。

## 安全说明

- API Token 和 Webhook Token 只保存在手机本地或浏览器本地配置中，不要写入源码、README、Issue 或日志。
- 当前服务默认使用 HTTP，不提供 TLS。不要把 8080 直接暴露到公网；优先使用家庭局域网、EasyTier、Tailscale 或其他可信 VPN 网络。
- 如果必须跨网络访问，请在网络层增加访问控制、防火墙或反向代理 HTTPS，并限制可访问设备。
- API Token 轮换后，旧 Token 会立即失效；MCP、脚本和 Web 控制台需要更新配置。
- 仓库忽略 local.properties、构建目录、APK 和本地 UI 截图，避免把本机环境和构建产物提交到版本库。

## 常见问题排查

### Web 页面打不开

确认手机端服务仍在运行、浏览器访问的是手机当前 Wi‑Fi/EasyTier 地址、电脑与手机网络互通，并检查手机系统是否限制后台网络。服务监听 0.0.0.0，但手机切换网络后旧 IP 会失效。

### 锁屏后无法访问

确认应用通知权限、前台服务权限、电池优化豁免、自启动和锁屏后台运行均已允许。小米系统还可能需要在应用详情中设置“无限制”电池策略。Wi‑Fi 休眠策略也可能影响局域网连接。

### 短信发送停留在发送中

系统发送广播可能延迟或被厂商系统拦截；应用会在超时后把仍未完成的任务标记为失败。送达状态依赖运营商、对方网络和手机是否支持送达报告，sent 只表示系统已接受发送任务，不等于对方已读或已收到。

### 收不到某些验证码短信

应用通过 Android 的 SMS_RECEIVED 广播接收运营商短信。网络短信、厂商短信应用策略、默认短信应用限制、双卡广播携带的 subscription 信息以及系统权限都可能影响结果。请检查短信/读取短信权限，并在目标 Android 版本和实际 SIM 上测试；应用不会按号码筛选，只要系统把短信广播交给应用即可记录。

### 双卡选择

发送 API 可以传入 subscriptionId 选择 SIM；省略时使用 Android 默认短信 SIM。接收短信由系统广播携带的 subscription 信息决定，若系统没有提供该信息，记录中可能无法区分具体 SIM。

## 开发与验证

修改 Web 资源后，建议执行：

~~~bash
node --check app/src/main/assets/web/app.js
~~~

修改 Android 代码后执行：

~~~bash
./gradlew assembleDebug
~~~

真机验证至少覆盖：服务启动/停止、Wi‑Fi 切换、锁屏后台、单卡/双卡发送、长短信、收信广播、送达报告、Webhook 重试、Token 认证、MCP initialize 和 tools/call。

## License

当前仓库未附带开源许可证。若要公开分发或接受外部贡献，请根据项目用途补充 LICENSE，并明确短信内容、电话号码和日志数据的隐私处理规则。
