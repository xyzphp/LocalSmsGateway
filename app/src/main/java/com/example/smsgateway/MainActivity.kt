package com.example.smsgateway

import android.Manifest
import android.app.AlertDialog
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var statusPage: View
    private lateinit var logsPage: View
    private lateinit var settingsPage: View
    private lateinit var serviceCard: View
    private lateinit var serviceStatusIcon: TextView
    private lateinit var serviceStatusTitle: TextView
    private lateinit var serviceStatusBadge: TextView
    private lateinit var serviceDividerTop: View
    private lateinit var serviceDividerBottom: View
    private lateinit var addressView: TextView
    private lateinit var serviceWifiIcon: ImageView
    private lateinit var serviceWifiView: TextView
    private lateinit var serviceActionRow: View
    private lateinit var serviceActionIcon: TextView
    private lateinit var serviceActionLabel: TextView
    private lateinit var appVersionView: TextView
    private lateinit var logList: LinearLayout
    private lateinit var logTabs: List<TextView>
    private lateinit var settingsPortInput: EditText
    private lateinit var settingsTokenInput: EditText
    private lateinit var settingsSendPermission: TextView
    private lateinit var settingsReceivePermission: TextView
    private lateinit var settingsSimPermission: TextView
    private lateinit var settingsBackgroundStatus: TextView
    private lateinit var settingsTokenSave: TextView
    private lateinit var settingsClearSent: View
    private lateinit var settingsClearReceived: View
    private lateinit var settingsClearWebhook: View
    private lateinit var navStatus: View
    private lateinit var navLogs: View
    private lateinit var navSettings: View
    private lateinit var navStatusIcon: ImageView
    private lateinit var navLogsIcon: ImageView
    private lateinit var navSettingsIcon: ImageView
    private lateinit var navStatusLabel: TextView
    private lateinit var navLogsLabel: TextView
    private lateinit var navSettingsLabel: TextView

    private val repository by lazy { GatewayRepository(this) }
    private val uiHandler = Handler(Looper.getMainLooper())
    private var currentPage = PAGE_STATUS
    private var logFilter = LOG_FILTER_ALL
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var backgroundSettingsPromptedThisSession = false

    private val logRefreshRunnable = object : Runnable {
        override fun run() {
            if (currentPage == PAGE_LOGS && !isFinishing) {
                renderLogs()
                uiHandler.postDelayed(this, LOG_REFRESH_INTERVAL_MILLIS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applySystemBarInsets()

        statusPage = findViewById(R.id.status_page)
        logsPage = findViewById(R.id.logs_page)
        settingsPage = findViewById(R.id.settings_page)
        serviceCard = findViewById(R.id.service_card)
        serviceStatusIcon = findViewById(R.id.service_status_icon)
        serviceStatusTitle = findViewById(R.id.service_status_title)
        serviceStatusBadge = findViewById(R.id.service_status_badge)
        serviceDividerTop = findViewById(R.id.service_divider_top)
        serviceDividerBottom = findViewById(R.id.service_divider_bottom)
        addressView = findViewById(R.id.local_address)
        serviceWifiIcon = findViewById(R.id.service_wifi_icon)
        serviceWifiView = findViewById(R.id.service_wifi)
        serviceActionRow = findViewById(R.id.service_action_row)
        serviceActionIcon = findViewById(R.id.service_action_icon)
        serviceActionLabel = findViewById(R.id.service_action_label)
        appVersionView = findViewById(R.id.app_version)
        logList = findViewById(R.id.mobile_log_list)
        logTabs = listOf(
            findViewById(R.id.log_tab_all),
            findViewById(R.id.log_tab_sent),
            findViewById(R.id.log_tab_received),
            findViewById(R.id.log_tab_webhook)
        )
        settingsPortInput = findViewById(R.id.mobile_settings_port)
        settingsTokenInput = findViewById(R.id.mobile_settings_token)
        settingsSendPermission = findViewById(R.id.mobile_settings_permission_send)
        settingsReceivePermission = findViewById(R.id.mobile_settings_permission_receive)
        settingsSimPermission = findViewById(R.id.mobile_settings_permission_sim)
        settingsBackgroundStatus = findViewById(R.id.mobile_settings_background_status)
        settingsTokenSave = findViewById(R.id.mobile_settings_token_save)
        settingsClearSent = findViewById(R.id.mobile_settings_clear_sent)
        settingsClearReceived = findViewById(R.id.mobile_settings_clear_received)
        settingsClearWebhook = findViewById(R.id.mobile_settings_clear_webhook)
        navStatus = findViewById(R.id.nav_status)
        navLogs = findViewById(R.id.nav_logs)
        navSettings = findViewById(R.id.nav_settings)
        navStatusIcon = findViewById(R.id.nav_status_icon)
        navLogsIcon = findViewById(R.id.nav_logs_icon)
        navSettingsIcon = findViewById(R.id.nav_settings_icon)
        navStatusLabel = findViewById(R.id.nav_status_label)
        navLogsLabel = findViewById(R.id.nav_logs_label)
        navSettingsLabel = findViewById(R.id.nav_settings_label)

        serviceActionRow.setOnClickListener {
            if (isGatewayRunning()) stopGateway() else startGateway()
        }
        findViewById<View>(R.id.mobile_settings_request_permissions).setOnClickListener {
            requestGatewayPermissions()
        }
        findViewById<View>(R.id.mobile_settings_background_row).setOnClickListener {
            openBackgroundRunSettings()
        }
        settingsClearSent.setOnClickListener {
            confirmClearRecords("发送记录") { repository.clearSentRecords() }
        }
        settingsClearReceived.setOnClickListener {
            confirmClearRecords("收信记录") { repository.clearReceivedRecords() }
        }
        settingsClearWebhook.setOnClickListener {
            confirmClearRecords("Webhook 记录") { repository.clearWebhookLogs() }
        }
        settingsTokenSave.setOnClickListener { saveTokenSetting() }
        settingsPortInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                savePortSetting(showToast = true)
                settingsPortInput.clearFocus()
                true
            } else {
                false
            }
        }
        settingsPortInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) savePortSetting(showToast = false)
        }

        navStatus.setOnClickListener { showPage(PAGE_STATUS) }
        navLogs.setOnClickListener { showPage(PAGE_LOGS) }
        navSettings.setOnClickListener { showPage(PAGE_SETTINGS) }
        logTabs.forEachIndexed { index, tab ->
            tab.setOnClickListener {
                logFilter = index
                updateLogTabs()
                renderLogs()
            }
        }

        showPage(PAGE_STATUS)
        refreshUi()
        if (hasSmsPermissions() && repository.isServiceRunning() && !SmsGatewayService.isActive) {
            uiHandler.post { startGateway(showToast = false) }
        }
    }

    private fun applySystemBarInsets() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        window.setDecorFitsSystemWindows(false)
        val root = findViewById<View>(R.id.root_container)
        val initialLeft = root.paddingLeft
        val initialRight = root.paddingRight
        val initialBottom = root.paddingBottom
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars())
            view.setPadding(initialLeft, bars.top, initialRight, initialBottom + bars.bottom)
            insets
        }
        root.requestApplyInsets()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
        scheduleLogRefresh()
        if (hasSmsPermissions() && repository.isServiceRunning() && !SmsGatewayService.isActive) {
            uiHandler.post { startGateway(showToast = false) }
        }
    }

    override fun onStart() {
        super.onStart()
        registerNetworkCallback()
    }

    override fun onStop() {
        unregisterNetworkCallback()
        super.onStop()
    }

    override fun onDestroy() {
        uiHandler.removeCallbacks(logRefreshRunnable)
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) refreshUi()
    }

    private fun showPage(page: Int) {
        if (currentPage == PAGE_SETTINGS && page != PAGE_SETTINGS) savePortSetting(showToast = false)
        currentPage = page
        statusPage.visibility = if (page == PAGE_STATUS) View.VISIBLE else View.GONE
        logsPage.visibility = if (page == PAGE_LOGS) View.VISIBLE else View.GONE
        settingsPage.visibility = if (page == PAGE_SETTINGS) View.VISIBLE else View.GONE

        val active = getColor(R.color.blue_600)
        val inactive = getColor(R.color.gray_500)
        navStatusLabel.setTextColor(if (page == PAGE_STATUS) active else inactive)
        navLogsLabel.setTextColor(if (page == PAGE_LOGS) active else inactive)
        navSettingsLabel.setTextColor(if (page == PAGE_SETTINGS) active else inactive)
        navStatusIcon.imageTintList = ColorStateList.valueOf(if (page == PAGE_STATUS) active else inactive)
        navLogsIcon.imageTintList = ColorStateList.valueOf(if (page == PAGE_LOGS) active else inactive)
        navSettingsIcon.imageTintList = ColorStateList.valueOf(if (page == PAGE_SETTINGS) active else inactive)
        navStatus.alpha = if (page == PAGE_STATUS) 1f else 0.78f
        navLogs.alpha = if (page == PAGE_LOGS) 1f else 0.78f
        navSettings.alpha = if (page == PAGE_SETTINGS) 1f else 0.78f
        scheduleLogRefresh()
    }

    private fun scheduleLogRefresh() {
        uiHandler.removeCallbacks(logRefreshRunnable)
        if (currentPage == PAGE_LOGS) {
            uiHandler.postDelayed(logRefreshRunnable, LOG_REFRESH_INTERVAL_MILLIS)
        }
    }

    private fun registerNetworkCallback() {
        if (networkCallback != null) return
        val connectivity = getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = refreshNetworkUi()
            override fun onLost(network: Network) = refreshNetworkUi()
        }
        try {
            connectivity.registerDefaultNetworkCallback(callback)
            networkCallback = callback
        } catch (_: Exception) {
            networkCallback = null
        }
    }

    private fun unregisterNetworkCallback() {
        val callback = networkCallback ?: return
        val connectivity = getSystemService(ConnectivityManager::class.java) ?: return
        try {
            connectivity.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
            // The system may already have removed the callback while the activity stopped.
        }
        networkCallback = null
    }

    private fun refreshNetworkUi() {
        uiHandler.post {
            if (!isFinishing) refreshUi()
        }
    }

    private fun startGateway(showToast: Boolean = true) {
        if (!hasSmsPermissions()) {
            requestGatewayPermissions()
            return
        }
        val serviceIntent = Intent(this, SmsGatewayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        if (showToast) Toast.makeText(this, "正在启动本地服务", Toast.LENGTH_SHORT).show()
        if (showToast && !backgroundSettingsPromptedThisSession && !isIgnoringBatteryOptimizations()) {
            backgroundSettingsPromptedThisSession = true
            uiHandler.postDelayed({
                if (!isFinishing) openBackgroundRunSettings()
            }, BACKGROUND_SETTINGS_PROMPT_DELAY_MILLIS)
        }
        uiHandler.postDelayed(::refreshUi, 500)
    }

    private fun stopGateway() {
        // Update the local UI state immediately; service onDestroy is asynchronous on Android.
        repository.setServiceRunning(false)
        repository.setAutoStart(false)
        stopService(Intent(this, SmsGatewayService::class.java))
        Toast.makeText(this, "服务已停止", Toast.LENGTH_SHORT).show()
        refreshUi()
        uiHandler.postDelayed(::refreshUi, 600)
    }

    private fun savePortSetting(showToast: Boolean): Boolean {
        val port = settingsPortInput.text.toString().trim().toIntOrNull()
        if (port == null || port !in 1024..65535) {
            settingsPortInput.error = "请输入 1024 - 65535"
            settingsPortInput.setText(repository.serverPort().toString())
            return false
        }

        settingsPortInput.error = null
        val previousPort = repository.serverPort()
        if (port == previousPort) return true

        val wasRunning = isGatewayRunning()
        repository.setServerPort(port)
        if (wasRunning) {
            stopService(Intent(this, SmsGatewayService::class.java))
            uiHandler.postDelayed({ startGateway(showToast = false) }, SERVICE_RESTART_DELAY_MILLIS)
            if (showToast) Toast.makeText(this, "端口已保存，服务正在重启", Toast.LENGTH_SHORT).show()
        } else if (showToast) {
            Toast.makeText(this, "端口已保存", Toast.LENGTH_SHORT).show()
        }
        refreshUi()
        return true
    }

    private fun saveTokenSetting() {
        val token = settingsTokenInput.text.toString().trim()
        if (token.length < MIN_TOKEN_LENGTH) {
            settingsTokenInput.error = "Token 至少需要 $MIN_TOKEN_LENGTH 个字符"
            return
        }
        if (!repository.setApiToken(token)) {
            settingsTokenInput.error = "Token 不能为空"
            return
        }
        settingsTokenInput.error = null
        settingsTokenInput.clearFocus()
        Toast.makeText(this, "API Token 已保存", Toast.LENGTH_SHORT).show()
        refreshUi()
    }

    private fun confirmClearRecords(label: String, clearAction: () -> Int) {
        AlertDialog.Builder(this)
            .setTitle("清理$label")
            .setMessage("将永久删除本机保存的全部$label，删除后无法恢复。")
            .setNegativeButton("取消", null)
            .setPositiveButton("清理") { _, _ ->
                val count = clearAction()
                val message = if (count > 0) {
                    "已清理 $count 条$label"
                } else {
                    "没有可清理的$label"
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                refreshUi()
                if (currentPage == PAGE_LOGS) renderLogs()
            }
            .show()
    }

    private fun requestGatewayPermissions() {
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.SEND_SMS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.RECEIVE_SMS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.READ_SMS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                checkSelfPermission(Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.READ_PHONE_NUMBERS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), REQUEST_PERMISSIONS)
        } else {
            Toast.makeText(this, "所需权限已授予", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasSmsPermissions(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            (checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED)

    private fun refreshUi() {
        val running = isGatewayRunning()
        val port = repository.serverPort()
        val wifiConnected = NetworkUtils.isWifiConnected(this)
        val address = if (wifiConnected) {
            NetworkUtils.baseUrl(this, port)
        } else {
            "暂无可用局域网地址"
        }
        val installedVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "--"

        appVersionView.text = "v$installedVersion"
        serviceCard.setBackgroundResource(
            if (running) R.drawable.bg_status_online else R.drawable.bg_status_offline
        )
        serviceStatusIcon.setBackgroundResource(
            if (running) R.drawable.bg_status_icon_online else R.drawable.bg_status_icon_offline
        )
        serviceStatusIcon.text = if (running) "✓" else ""
        serviceStatusIcon.setTextColor(getColor(if (running) R.color.white else R.color.gray_500))
        serviceStatusTitle.text = if (running) "服务运行中" else "服务未运行"
        serviceStatusTitle.setTextColor(getColor(if (running) R.color.green_600 else R.color.gray_700))
        serviceStatusBadge.text = if (running) "实时" else "已停止"
        serviceStatusBadge.setBackgroundResource(
            if (running) R.drawable.bg_badge_online else R.drawable.bg_badge_offline
        )
        serviceStatusBadge.setTextColor(getColor(if (running) R.color.green_600 else R.color.gray_700))
        val dividerColor = getColor(if (running) R.color.green_100 else R.color.gray_100)
        serviceDividerTop.setBackgroundColor(dividerColor)
        serviceDividerBottom.setBackgroundColor(dividerColor)
        addressView.text = address
        serviceWifiView.text = if (wifiConnected) "已连接 Wi-Fi" else "未连接 Wi-Fi"
        val wifiColor = getColor(if (wifiConnected) R.color.gray_700 else R.color.gray_500)
        serviceWifiView.setTextColor(wifiColor)
        serviceWifiIcon.imageTintList = ColorStateList.valueOf(wifiColor)
        serviceActionRow.setBackgroundResource(
            if (running) R.drawable.bg_action_stop else R.drawable.bg_action_start
        )
        serviceActionIcon.text = if (running) "■" else "▶"
        serviceActionIcon.setTextColor(getColor(if (running) R.color.red_600 else R.color.blue_600))
        serviceActionLabel.text = if (running) "停止服务" else "启动服务"
        serviceActionLabel.setTextColor(getColor(if (running) R.color.red_600 else R.color.blue_600))
        serviceActionRow.contentDescription = if (running) "停止服务" else "启动服务"

        if (!settingsPortInput.hasFocus()) setInputValue(settingsPortInput, port.toString())
        if (!settingsTokenInput.hasFocus()) setInputValue(settingsTokenInput, repository.apiToken())
        updatePermissionRows()
        updateBackgroundRunStatus()
        updateLogTabs()
        renderLogs()
    }

    private fun setInputValue(input: EditText, value: String) {
        if (input.text.toString() != value) {
            input.setText(value)
            input.setSelection(input.text.length)
        }
    }

    private fun isGatewayRunning(): Boolean =
        SmsGatewayService.isActive && repository.isServiceRunning()

    private fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val powerManager = getSystemService(PowerManager::class.java) ?: return false
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun updateBackgroundRunStatus() {
        if (!::settingsBackgroundStatus.isInitialized) return
        val allowed = isIgnoringBatteryOptimizations()
        settingsBackgroundStatus.text = if (allowed) {
            "电池策略已放行"
        } else {
            "未开启 · 点击去设置"
        }
        settingsBackgroundStatus.setTextColor(
            getColor(if (allowed) R.color.green_600 else R.color.orange_600)
        )
    }

    private fun openBackgroundRunSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (isIgnoringBatteryOptimizations()) {
            openAppDetailsSettings()
            return
        }

        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (_: Exception) {
                openAppDetailsSettings()
            }
        }
    }

    private fun openAppDetailsSettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "请在系统设置中将本应用电池策略改为“无限制”",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun updatePermissionRows() {
        updatePermissionStatus(
            settingsSendPermission,
            hasPermission(Manifest.permission.SEND_SMS)
        )
        updatePermissionStatus(
            settingsReceivePermission,
            hasPermission(Manifest.permission.RECEIVE_SMS) &&
                hasPermission(Manifest.permission.READ_SMS)
        )
        updatePermissionStatus(
            settingsSimPermission,
            hasPermission(Manifest.permission.READ_PHONE_STATE) &&
                hasPermission(Manifest.permission.READ_PHONE_NUMBERS)
        )
    }

    private fun updatePermissionStatus(view: TextView, granted: Boolean) {
        view.setBackgroundResource(
            if (granted) R.drawable.bg_status_icon_online else R.drawable.bg_status_icon_offline
        )
        view.text = if (granted) "✓" else "!"
        view.setTextColor(getColor(if (granted) R.color.white else R.color.gray_500))
    }

    private fun updateLogTabs() {
        logTabs.forEachIndexed { index, tab ->
            val selected = index == logFilter
            tab.setBackgroundResource(
                if (selected) R.drawable.bg_segment_active else android.R.color.transparent
            )
            tab.setTextColor(getColor(if (selected) R.color.white else R.color.gray_700))
            tab.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun renderLogs() {
        if (!::logList.isInitialized) return
        logList.removeAllViews()
        val items = buildLogItems()
        if (items.isEmpty()) {
            val empty = TextView(this).apply {
                background = getDrawable(R.drawable.bg_card)
                gravity = Gravity.CENTER
                minHeight = dp(108)
                setPadding(dp(16), dp(16), dp(16), dp(16))
                setTextColor(getColor(R.color.gray_500))
                textSize = 14f
                text = emptyLogText()
            }
            logList.addView(empty)
            return
        }

        items.forEachIndexed { index, item ->
            val row = createLogRow(item)
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            if (index > 0) params.topMargin = dp(8)
            logList.addView(row, params)
        }
    }

    private fun createLogRow(item: MobileLogItem): View {
        val row = LinearLayout(this).apply {
            background = getDrawable(R.drawable.bg_log_row)
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(13), dp(14), dp(13))
            setOnClickListener { showLogDetails(item) }
            contentDescription = "查看${item.detailTitle}详情"
        }

        val titleLine = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        val dot = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(10), dp(10))
            text = "●"
            setTextColor(getColor(item.tone.dotColor))
            textSize = 10f
        }
        val title = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(8)
            }
            setTextColor(getColor(R.color.navy_900))
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            text = item.title
        }
        val time = TextView(this).apply {
            setTextColor(getColor(R.color.gray_500))
            textSize = 11f
            text = formatLogTime(item.timestamp)
        }
        titleLine.addView(dot)
        titleLine.addView(title)
        titleLine.addView(time)

        val detailLine = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        val detail = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setTextColor(getColor(R.color.gray_700))
            maxLines = 2
            textSize = 13f
            text = item.detail
        }
        val status = TextView(this).apply {
            setBackgroundResource(item.tone.background)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setTextColor(getColor(item.tone.textColor))
            textSize = 11f
            text = item.status
        }
        val detailAction = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = dp(7) }
            background = getDrawable(R.drawable.bg_action_start)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setTextColor(getColor(R.color.blue_600))
            textSize = 11f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            text = "查看详情"
            setOnClickListener { showLogDetails(item) }
        }
        detailLine.addView(detail)
        detailLine.addView(status)
        detailLine.addView(detailAction)

        row.addView(titleLine)
        row.addView(detailLine)
        return row
    }

    private fun showLogDetails(item: MobileLogItem) {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(4), dp(20), dp(4))
        }
        val scroll = ScrollView(this).apply {
            addView(content)
        }
        val status = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
            setBackgroundResource(item.tone.background)
            setPadding(dp(10), dp(5), dp(10), dp(5))
            setTextColor(getColor(item.tone.textColor))
            textSize = 12f
            text = "${item.status}  ·  ${formatLogDateTime(item.timestamp)}"
        }
        content.addView(status)

        item.detailRows.forEachIndexed { index, (label, value) ->
            if (index > 0) {
                content.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(1)
                    ).apply {
                        topMargin = dp(10)
                        bottomMargin = dp(10)
                    }
                    setBackgroundColor(getColor(R.color.gray_100))
                })
            }
            addLogDetailRow(content, label, value)
        }

        item.messageText?.takeIf { it.isNotBlank() }?.let { message ->
            val messageLabel = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = if (item.detailRows.isEmpty()) dp(2) else dp(16)
                    bottomMargin = dp(7)
                }
                setTextColor(getColor(R.color.navy_900))
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                text = "短信内容"
            }
            val messageView = TextView(this).apply {
                background = GradientDrawable().apply {
                    setColor(getColor(R.color.gray_50))
                    cornerRadius = dp(12).toFloat()
                    setStroke(dp(1), getColor(R.color.gray_100))
                }
                setPadding(dp(13), dp(12), dp(13), dp(12))
                setTextColor(getColor(R.color.gray_700))
                textSize = 14f
                setLineSpacing(0f, 1.35f)
                setTextIsSelectable(true)
                text = message
            }
            content.addView(messageLabel)
            content.addView(messageView)
        }

        AlertDialog.Builder(this)
            .setTitle(item.detailTitle)
            .setView(scroll)
            .setPositiveButton("关闭", null)
            .show()
    }

    private fun addLogDetailRow(parent: LinearLayout, label: String, value: String) {
        val row = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.TOP
            orientation = LinearLayout.HORIZONTAL
        }
        val labelView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(86), ViewGroup.LayoutParams.WRAP_CONTENT)
            setTextColor(getColor(R.color.gray_500))
            textSize = 12f
            text = label
        }
        val valueView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setTextColor(getColor(R.color.navy_900))
            textSize = 13f
            setTextIsSelectable(true)
            text = value.ifBlank { "—" }
        }
        row.addView(labelView)
        row.addView(valueView)
        parent.addView(row)
    }

    private fun buildLogItems(): List<MobileLogItem> {
        val sent = repository.listSent(100)
        val received = repository.listReceived(100)
        val webhook = buildWebhookRecords(sent, received)
        val items = when (logFilter) {
            LOG_FILTER_SENT -> sent.map { sentLog(it) }
            LOG_FILTER_RECEIVED -> received.map { receivedLog(it) }
            LOG_FILTER_WEBHOOK -> webhook.map { webhookLog(it) }
            else -> sent.map { sentLog(it) } + received.map { receivedLog(it) } + webhook.map { webhookLog(it) }
        }
        return items.sortedByDescending { it.timestamp }.take(MAX_MOBILE_LOGS)
    }

    private fun buildWebhookRecords(
        sent: List<SentRecord>,
        received: List<ReceivedRecord>
    ): List<WebhookLogRecord> {
        val persisted = repository.listWebhookLogs(100)
        val clearedAt = repository.webhookLogsClearedAt()
        val known = persisted.map { "${it.event}:${it.messageId.orEmpty()}" }.toMutableSet()
        val config = repository.webhookConfig()
        val legacy = mutableListOf<WebhookLogRecord>()

        sent.forEach { record ->
            val event = when (record.status) {
                "delivered" -> "sms.delivered"
                "failed" -> "sms.failed"
                else -> "sms.sent"
            }
            val key = "$event:${record.messageId}"
            if (shouldShowLegacyWebhookRecord(record.sentAt ?: record.createdAt, clearedAt) && known.add(key)) {
                legacy += WebhookLogRecord(
                    eventId = "legacy_${record.messageId}",
                    event = event,
                    occurredAt = record.sentAt ?: record.createdAt,
                    messageId = record.messageId,
                    status = legacyWebhookStatus(config, event),
                    attemptCount = 0,
                    responseCode = null,
                    detail = "历史记录，无投递明细"
                )
            }
        }

        received.forEach { record ->
            val event = "sms.received"
            val key = "$event:${record.messageId}"
            if (shouldShowLegacyWebhookRecord(record.receivedAt, clearedAt) && known.add(key)) {
                legacy += WebhookLogRecord(
                    eventId = "legacy_${record.messageId}",
                    event = event,
                    occurredAt = record.receivedAt,
                    messageId = record.messageId,
                    status = legacyWebhookStatus(config, event, record.webhookStatus),
                    attemptCount = record.webhookRetryCount,
                    responseCode = null,
                    detail = "历史记录，无投递明细"
                )
            }
        }
        return persisted + legacy
    }

    private fun shouldShowLegacyWebhookRecord(timestamp: String, clearedAt: Long): Boolean {
        if (clearedAt <= 0L) return true
        val occurredAt = runCatching { Instant.parse(timestamp).toEpochMilli() }.getOrNull() ?: return false
        return occurredAt > clearedAt
    }

    private fun legacyWebhookStatus(
        config: WebhookConfig,
        event: String,
        existingStatus: String? = null
    ): String {
        if (config.url.isBlank()) return "not_configured"
        if (!config.enabled) return "disabled"
        if (event !in config.events) return "not_subscribed"
        return existingStatus?.takeIf { it in setOf("delivered", "failed", "pending") } ?: "pending"
    }

    private fun webhookLog(record: WebhookLogRecord): MobileLogItem {
        val message = record.messageId?.let { " · $it" }.orEmpty()
        return MobileLogItem(
            timestamp = record.occurredAt,
            title = "${webhookEventLabel(record.event)} · ${webhookStatusLabel(record.status)}",
            detail = "${record.detail}$message",
            status = webhookStatusLabel(record.status),
            tone = when (record.status) {
                "delivered" -> LogTone.SUCCESS
                "failed" -> LogTone.ERROR
                "pending" -> LogTone.PENDING
                "retrying" -> LogTone.WARNING
                "not_configured", "disabled", "not_subscribed" -> LogTone.NEUTRAL
                else -> LogTone.INFO
            },
            detailTitle = webhookEventLabel(record.event),
            detailRows = listOf(
                "事件" to record.event,
                "发生时间" to formatLogDateTime(record.occurredAt),
                "消息 ID" to (record.messageId ?: "—"),
                "投递次数" to record.attemptCount.toString(),
                "响应码" to (record.responseCode?.toString() ?: "—"),
                "投递结果" to record.detail.ifBlank { "—" }
            )
        )
    }

    private fun webhookEventLabel(event: String): String = when (event) {
        "sms.received" -> "收信回调"
        "sms.sent" -> "发送回调"
        "sms.delivered" -> "送达回调"
        "sms.failed" -> "失败回调"
        "webhook.test" -> "测试回调"
        else -> "Webhook"
    }

    private fun webhookStatusLabel(status: String): String = when (status) {
        "delivered" -> "回调成功"
        "failed" -> "回调失败"
        "not_configured" -> "未配置"
        "disabled" -> "已停用"
        "not_subscribed" -> "未订阅"
        "pending" -> "待回调"
        else -> status
    }

    private fun sentLog(record: SentRecord): MobileLogItem = MobileLogItem(
        timestamp = record.createdAt,
        title = "发送至 ${maskNumber(record.to)}",
        detail = "短信内容：${record.text}",
        status = sentStatusLabel(record.status),
        tone = when (record.status) {
            "failed" -> LogTone.ERROR
            "queued", "sending", "sent" -> LogTone.PENDING
            "delivered" -> LogTone.SUCCESS
            else -> LogTone.NEUTRAL
        },
        detailTitle = "发送短信",
        detailRows = buildList {
            add("收件号码" to record.to)
            add("创建时间" to formatLogDateTime(record.createdAt))
            add("发送时间" to (record.sentAt?.let(::formatLogDateTime) ?: "—"))
            add("送达时间" to (record.deliveredAt?.let(::formatLogDateTime) ?: "—"))
            add("短信状态" to sentStatusLabel(record.status))
            record.errorCode?.let { add("错误码" to it.toString()) }
            add("消息 ID" to record.messageId)
        },
        messageText = record.text
    )

    private fun receivedLog(record: ReceivedRecord): MobileLogItem = MobileLogItem(
        timestamp = record.receivedAt,
        title = "收到来自 ${maskNumber(record.from)}",
        detail = "短信内容：${record.text}",
        status = "已接收",
        tone = LogTone.SUCCESS,
        detailTitle = "收到短信",
        detailRows = listOf(
            "发件号码" to record.from,
            "接收时间" to formatLogDateTime(record.receivedAt),
            "Webhook 状态" to webhookStatusLabel(record.webhookStatus),
            "回调重试" to "${record.webhookRetryCount} 次",
            "消息 ID" to record.messageId
        ),
        messageText = record.text
    )

    private fun sentStatusLabel(status: String): String = when (status) {
        "queued" -> "已提交"
        "sending" -> "发送中"
        "sent" -> "已发送"
        "delivered" -> "已送达"
        "failed" -> "发送失败"
        else -> status
    }

    private fun maskNumber(value: String): String {
        val normalized = value.trim()
        return if (normalized.length > 7) {
            "${normalized.take(3)}****${normalized.takeLast(4)}"
        } else {
            normalized
        }
    }

    private fun formatLogTime(value: String): String = runCatching {
        Instant.parse(value)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MM-dd HH:mm:ss", Locale.getDefault()))
    }.getOrElse {
        value.replace('T', ' ').substringBefore('.')
    }

    private fun formatLogDateTime(value: String): String = runCatching {
        Instant.parse(value)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault()))
    }.getOrElse {
        value.replace('T', ' ').substringBefore('.')
    }

    private fun emptyLogText(): String = when (logFilter) {
        LOG_FILTER_SENT -> "暂无发送记录"
        LOG_FILTER_RECEIVED -> "暂无收信记录"
        LOG_FILTER_WEBHOOK -> "暂无 Webhook 记录"
        else -> "暂无日志记录"
    }

    private fun hasPermission(permission: String): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    private data class MobileLogItem(
        val timestamp: String,
        val title: String,
        val detail: String,
        val status: String,
        val tone: LogTone,
        val detailTitle: String,
        val detailRows: List<Pair<String, String>> = emptyList(),
        val messageText: String? = null
    )

    private enum class LogTone(
        val dotColor: Int,
        val textColor: Int,
        val background: Int
    ) {
        SUCCESS(R.color.green_600, R.color.green_600, R.drawable.bg_badge_online),
        ERROR(R.color.red_600, R.color.red_600, R.drawable.bg_action_stop),
        PENDING(R.color.blue_600, R.color.blue_600, R.drawable.bg_action_start),
        INFO(R.color.blue_600, R.color.blue_600, R.drawable.bg_action_start),
        WARNING(R.color.orange_600, R.color.orange_600, R.drawable.bg_badge_warning),
        NEUTRAL(R.color.gray_500, R.color.gray_700, R.drawable.bg_badge_offline)
    }

    companion object {
        private const val PAGE_STATUS = 0
        private const val PAGE_LOGS = 1
        private const val PAGE_SETTINGS = 2
        private const val LOG_FILTER_ALL = 0
        private const val LOG_FILTER_SENT = 1
        private const val LOG_FILTER_RECEIVED = 2
        private const val LOG_FILTER_WEBHOOK = 3
        private const val REQUEST_PERMISSIONS = 100
        private const val MIN_TOKEN_LENGTH = 8
        private const val MAX_MOBILE_LOGS = 100
        private const val LOG_REFRESH_INTERVAL_MILLIS = 2_000L
        private const val SERVICE_RESTART_DELAY_MILLIS = 500L
        private const val BACKGROUND_SETTINGS_PROMPT_DELAY_MILLIS = 700L
    }
}
