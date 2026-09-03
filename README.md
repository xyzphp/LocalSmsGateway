# 本地短信网关

本地短信网关是一款运行在 Android 手机上的短信网关应用。它使用手机的 SIM 卡发送和接收短信，并在手机上提供局域网 HTTP 服务，方便电脑、脚本、自动化工具和 MCP Agent 调用。

![本地短信网关项目总览](docs/assets/local-sms-gateway-overview.png)

## 项目特点

- 单手机、局域网优先，默认服务端口为 8080。
- 支持短信发送、收信记录、发送状态和送达状态。
- 支持选择 SIM 卡发送长短信。
- 提供 Web 控制台，可查看统计、记录、详情和 Webhook 日志。
- 提供需要 Token 认证的 HTTP API。
- 提供 Webhook，用于接收收信、发送和状态变化通知。
- 提供 MCP Streamable HTTP 服务，地址为 /mcp。
- Android 前台服务支持后台运行和锁屏后维持服务。
- 支持清理发送记录、收信记录和 Webhook 记录。

![短信网关数据流与集成方式](docs/assets/local-sms-gateway-flow.png)

## 使用方式

1. 在 Android 手机上安装 APK 并打开应用。
2. 授予短信、电话状态、SIM 信息和通知权限。
3. 在首页启动服务。
4. 在浏览器访问手机显示的地址：

   ~~~text
   http://<手机IP>:8080/
   ~~~

5. 首次进入 Web 控制台时，输入手机端显示的 API Token。

手机和访问设备需要处于同一局域网，或通过 EasyTier、Tailscale 等方式实现网络互通。小米等系统还需要允许应用自启动、后台运行和忽略电池优化。

## 构建和安装

项目使用 Gradle 8.7、Android Gradle Plugin 8.6.1、Kotlin 2.0.21 和 JDK 17。

Windows：

~~~powershell
.\gradlew.bat assembleDebug
~~~

macOS/Linux：

~~~bash
./gradlew assembleDebug
~~~

APK 位于：

~~~text
app/build/outputs/apk/debug/app-debug.apk
~~~

使用 ADB 安装：

~~~bash
adb connect <手机IP>:5555
adb -s <手机IP>:5555 install -r app-debug.apk
~~~

## API 和 MCP

HTTP API 使用以下请求头认证：

~~~http
Authorization: Bearer <API_TOKEN>
~~~

主要接口包括：

- GET /api/v1/status：服务状态；
- GET /api/v1/stats：统计和近期活动；
- GET /api/v1/sims：SIM 卡列表；
- POST /api/v1/sms/send：发送短信；
- GET /api/v1/sms/sent：发送记录；
- GET /api/v1/sms/received：收信记录；
- GET/PUT /api/v1/webhook：Webhook 配置；
- GET /api/v1/webhook/logs：Webhook 日志。

MCP 客户端连接地址：

~~~text
http://<手机IP>:8080/mcp
~~~

完整接口、Webhook、MCP 工具和排错说明请查看 [项目参考文档](docs/PROJECT_REFERENCE.md)，Web 控制台也提供可直接请求的 API 文档页面。

## 自动构建

GitHub Actions 配置位于 [.github/workflows/release-apk.yml](.github/workflows/release-apk.yml)：

- 手动运行可生成 APK 构建产物；
- 推送 v* Tag 会自动构建并创建或更新 Release；
- 发布 GitHub Release 会自动构建并附加 APK。

例如：

~~~bash
git tag v0.1.72
git push origin v0.1.72
~~~

当前流水线生成的是可安装的 Debug 签名 APK，适合个人部署和测试。正式公开发布时应配置独立的 Release 签名证书。

## 安全提示

API Token 和 Webhook Token 不要提交到仓库。当前服务默认使用 HTTP，建议只在可信局域网或 VPN 组网中使用，不要直接暴露到公网。

## 项目状态

这是一个持续完善中的个人项目。短信接收广播、运营商送达回执、双卡识别和厂商后台策略需要在实际手机、SIM 卡和 Android 版本上验证。
