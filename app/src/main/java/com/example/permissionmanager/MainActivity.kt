package com.example.permissionmanager

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.permissionmanager.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "权限管理"

        // 提前在后台预热"全部应用申请了哪些权限"的缓存，等用户点进麦克风/
        // 相机等分类时大概率已经扫描完了，不用现点现扫，减少等待感。
        AppPermissionCache.warmUp(this)

        setupRecyclerView()
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menu.add(0, 1, 0, "诊断：查找系统权限页面组件名")
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == 1) {
            showDiagnosticDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * 【诊断】遍历几个可能装了系统权限管理页面的包，把类名里带
     * Permission / Privacy / Access 关键字的 Activity 全部列出来，
     * 方便直接在真机上找到当前 ColorOS 版本实际可用的组件名，
     * 不需要电脑 / adb。
     */
    private fun showDiagnosticDialog() {
        val candidatePackages = listOf(
            "com.coloros.safecenter",
            "com.android.settings",
            "com.android.permissioncontroller",
            "com.oplus.securitypermission"
        )
        val keywords = listOf("permission", "privacy", "access")

        val sb = StringBuilder()
        for (pkg in candidatePackages) {
            sb.append("== $pkg ==\n")
            try {
                val info = packageManager.getPackageInfo(
                    pkg,
                    android.content.pm.PackageManager.GET_ACTIVITIES
                )
                val activities = info.activities
                if (activities == null || activities.isEmpty()) {
                    sb.append("(未取到 activity 列表，可能需要系统签名权限)\n\n")
                    continue
                }
                val matched = activities
                    .map { it.name }
                    .filter { name -> keywords.any { name.lowercase().contains(it) } }
                if (matched.isEmpty()) {
                    sb.append("(没有匹配到含 permission/privacy/access 的 Activity)\n\n")
                } else {
                    matched.sorted().forEach { sb.append(it).append("\n") }
                    sb.append("\n")
                }
            } catch (e: Exception) {
                sb.append("读取失败: ${e.javaClass.simpleName} - ${e.message}\n\n")
            }
        }

        showDebugTextDialog("候选权限页面组件", sb.toString())
    }

    private fun setupRecyclerView() {
        val permissionList = listOf(
            PermissionItem("特殊应用权限（总览）", "跳到系统的\"特殊应用权限\"汇总页，包含悬浮窗、后台弹出、使用情况访问等全部分类（不同厂商实现不同，找不到会自动退回本应用详情页）",
                R.drawable.ic_grid, R.color.tint_black, PermissionType.SPECIAL_ACCESS_OVERVIEW),
            PermissionItem("无障碍", "允许应用使用无障碍服务，控制设备",
                R.drawable.ic_accessibility, R.color.tint_purple, PermissionType.ACCESSIBILITY),
            PermissionItem("悬浮窗", "允许应用在其他应用上层显示，可能影响其他应用",
                R.drawable.ic_overlay, R.color.tint_blue, PermissionType.OVERLAY),
            PermissionItem("麦克风", "查看哪些应用申请了麦克风权限",
                R.drawable.ic_microphone, R.color.tint_red, PermissionType.MICROPHONE),
            PermissionItem("相机", "查看哪些应用申请了相机权限",
                R.drawable.ic_camera, R.color.tint_orange, PermissionType.CAMERA),
            PermissionItem("位置", "查看哪些应用申请了位置权限",
                R.drawable.ic_location, R.color.tint_green, PermissionType.LOCATION),
            PermissionItem("通知", "允许应用向您发送通知消息",
                R.drawable.ic_notification, R.color.tint_yellow, PermissionType.NOTIFICATION),
            PermissionItem("存储", "允许应用读取和写入存储空间中的文件",
                R.drawable.ic_storage, R.color.tint_brown, PermissionType.STORAGE),
            PermissionItem("电话", "查看哪些应用申请了电话权限",
                R.drawable.ic_phone, R.color.tint_teal, PermissionType.PHONE),
            PermissionItem("联系人", "查看哪些应用申请了联系人权限",
                R.drawable.ic_contacts, R.color.tint_indigo, PermissionType.CONTACTS),
            PermissionItem("日历", "查看哪些应用申请了日历权限",
                R.drawable.ic_calendar, R.color.tint_pink, PermissionType.CALENDAR),
            PermissionItem("电池优化", "允许应用忽略电池优化在后台运行",
                R.drawable.ic_battery, R.color.tint_lime, PermissionType.BATTERY),
            PermissionItem("安装未知应用", "允许应用安装来自未知来源的 APK 文件",
                R.drawable.ic_install, R.color.tint_deep_orange, PermissionType.UNKNOWN_SOURCES),
            PermissionItem("通知使用权", "允许应用读取、清除系统中所有其他应用的通知内容，风险较高，请谨慎授权",
                R.drawable.ic_notification, R.color.tint_cyan, PermissionType.NOTIFICATION_LISTENER),
            PermissionItem("使用情况访问权限", "允许应用跟踪您使用其他应用的行为和频率，及运营商、语言等设备信息",
                R.drawable.ic_usage, R.color.tint_deep_purple, PermissionType.USAGE_ACCESS),
            PermissionItem("闹钟和提醒", "允许应用设置闹钟以及安排在特定时间执行某些操作",
                R.drawable.ic_alarm, R.color.tint_amber, PermissionType.ALARMS_REMINDERS),
            PermissionItem("修改系统设置", "允许应用修改系统设置",
                R.drawable.ic_settings_gear, R.color.tint_grey, PermissionType.WRITE_SETTINGS),
            PermissionItem("勿扰模式访问权限", "允许应用开启或关闭勿扰模式，以及修改相关的例外规则",
                R.drawable.ic_dnd, R.color.tint_deep_red, PermissionType.DND_ACCESS),
            PermissionItem("后台弹出界面", "允许后台运行的应用弹出新界面，并可能覆盖在正在使用的应用上方",
                R.drawable.ic_popup, R.color.tint_blue_grey, PermissionType.BACKGROUND_POPUP),
            PermissionItem("媒体管理应用", "允许应用在无需用户逐一确认的情况下修改或删除媒体文件（Android 11+）",
                R.drawable.ic_media, R.color.tint_light_green, PermissionType.MEDIA_MANAGEMENT),
            PermissionItem("发送全屏通知", "允许应用发送需要立即处理的全屏通知，例如来电或闹钟提醒（Android 14+）",
                R.drawable.ic_fullscreen, R.color.tint_light_blue, PermissionType.FULL_SCREEN_INTENT),
            PermissionItem("默认应用", "设置主屏幕、短信、电话、浏览器等各类操作的默认处理应用",
                R.drawable.ic_star, R.color.tint_gold, PermissionType.DEFAULT_APPS),
            PermissionItem("设备管理器", "查看和管理拥有设备管理员权限的应用（如远程锁定、擦除数据等高级权限）",
                R.drawable.ic_shield, R.color.tint_navy, PermissionType.DEVICE_ADMIN)
        )

        val adapter = PermissionAdapter(permissionList) { openPermissionSettings(it) }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            this.adapter = adapter
        }
    }

    /**
     * 麦克风 / 相机 / 位置 / 电话 / 联系人 / 日历 这几类权限对应的具体
     * Android 权限字符串。点这几个分类时不再直接跳系统设置，而是先在本
     * App 内展示"哪些已安装应用申请了这个权限"的列表（见 AppListActivity）。
     */
    private fun inAppPermissionStrings(type: PermissionType): Array<String>? {
        return when (type) {
            PermissionType.MICROPHONE -> arrayOf(
                android.Manifest.permission.RECORD_AUDIO
            )

            PermissionType.CAMERA -> arrayOf(
                android.Manifest.permission.CAMERA
            )

            PermissionType.LOCATION -> mutableListOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }.toTypedArray()

            PermissionType.PHONE -> mutableListOf(
                android.Manifest.permission.READ_PHONE_STATE,
                android.Manifest.permission.CALL_PHONE,
                android.Manifest.permission.READ_CALL_LOG,
                android.Manifest.permission.WRITE_CALL_LOG,
                android.Manifest.permission.ADD_VOICEMAIL,
                android.Manifest.permission.USE_SIP
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    add(android.Manifest.permission.ANSWER_PHONE_CALLS)
                    add(android.Manifest.permission.READ_PHONE_NUMBERS)
                }
            }.toTypedArray()

            PermissionType.CONTACTS -> arrayOf(
                android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.WRITE_CONTACTS,
                android.Manifest.permission.GET_ACCOUNTS
            )

            PermissionType.CALENDAR -> arrayOf(
                android.Manifest.permission.READ_CALENDAR,
                android.Manifest.permission.WRITE_CALENDAR
            )

            else -> null
        }
    }

    private fun openPermissionSettings(item: PermissionItem) {
        // 麦克风/相机/位置/电话/联系人/日历：不跳系统设置，改为跳到本 App 内的
        // "应用列表"页，展示哪些应用申请了这个权限。
        val inAppPermissions = inAppPermissionStrings(item.type)
        if (inAppPermissions != null) {
            startActivity(
                Intent(this, AppListActivity::class.java).apply {
                    putExtra(AppListActivity.EXTRA_TITLE, item.name)
                    putExtra(AppListActivity.EXTRA_PERMISSIONS, inAppPermissions)
                }
            )
            return
        }

        try {
            val intent = when (item.type) {
                PermissionType.ACCESSIBILITY ->
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

                PermissionType.OVERLAY ->
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )

                PermissionType.NOTIFICATION ->
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    }

                PermissionType.BATTERY ->
                    // ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS 跳到全部应用的电池优化
                    // 列表页（GMS 强制要求的公开特殊权限入口，跨厂商稳定）。
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

                PermissionType.UNKNOWN_SOURCES ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:$packageName")
                        )
                    } else {
                        Intent(Settings.ACTION_SECURITY_SETTINGS)
                    }

                PermissionType.MICROPHONE ->
                    permissionGroupIntent("android.permission-group.MICROPHONE")

                PermissionType.CAMERA ->
                    permissionGroupIntent("android.permission-group.CAMERA")

                PermissionType.LOCATION ->
                    permissionGroupIntent("android.permission-group.LOCATION")

                PermissionType.STORAGE ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        // 这个是真正公开、GMS 强制要求的特殊权限接口（和无障碍/悬浮窗同级别），
                        // 所有认证过 GMS 的手机（包括国产 ROM）都必须实现，可跨厂商稳定使用。
                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    } else {
                        permissionGroupIntent("android.permission-group.STORAGE")
                    }

                PermissionType.PHONE ->
                    permissionGroupIntent("android.permission-group.PHONE")

                PermissionType.CONTACTS ->
                    permissionGroupIntent("android.permission-group.CONTACTS")

                PermissionType.CALENDAR ->
                    permissionGroupIntent("android.permission-group.CALENDAR")

                PermissionType.NOTIFICATION_LISTENER ->
                    // 公开 SDK 常量，GMS 强制要求实现，跳到全部应用的通知使用权列表页。
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

                PermissionType.USAGE_ACCESS ->
                    // 同样是公开 SDK 常量（API 21+），GMS 强制要求，跳到全部应用的
                    // 使用情况访问权限列表页。
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

                PermissionType.ALARMS_REMINDERS ->
                    // ACTION_REQUEST_SCHEDULE_EXACT_ALARM 是公开常量（API 31+），
                    // 跳到全部应用的"闹钟和提醒"权限列表页。低于 API 31 的系统没有
                    // 精确闹钟这个概念，直接退回本应用详情页。
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    } else {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    }

                PermissionType.WRITE_SETTINGS ->
                    // 公开 SDK 常量（API 23+），GMS 强制要求，跳到全部应用的
                    // "修改系统设置"权限列表页。
                    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)

                PermissionType.DND_ACCESS ->
                    // 公开 SDK 常量（API 23+），GMS 强制要求，跳到全部应用的
                    // 勿扰模式访问权限列表页。
                    Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

                PermissionType.BACKGROUND_POPUP ->
                    // "后台弹出界面"是 ColorOS/OxygenOS 私有分类，没有公开的 AOSP 权限组
                    // 名称可用，只能走 ColorOS 权限管理主页兜底（跳过去后需要手动点这个分类）。
                    permissionGroupIntent("com.oplus.permission.opsafe.BACKGROUND_START_ACTIVITY")

                PermissionType.SPECIAL_ACCESS_OVERVIEW ->
                    // 这几个都是没有公开文档、各厂商各不相同的系统内部 Activity，靠已知
                    // 组件名硬跳，找不到就自动退回本应用详情页。
                    firstResolvable(
                        Intent().apply {
                            component = android.content.ComponentName(
                                "com.android.permissioncontroller",
                                "com.android.permissioncontroller.role.ui.SpecialAppAccessListActivity"
                            )
                        },
                        Intent().apply {
                            component = android.content.ComponentName(
                                "com.android.settings",
                                "com.oplus.settings.OplusSettingsActivity\$SpecialAccessSettingsMainActivity"
                            )
                        },
                        Intent().apply {
                            component = android.content.ComponentName(
                                "com.oplus.securitypermission",
                                "com.oplusos.securitypermission.privacycenter.specialaccess.ui.SpecialAccessOptimizeActivity"
                            )
                        }
                    ) ?: Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }

                PermissionType.MEDIA_MANAGEMENT ->
                    // ACTION_REQUEST_MANAGE_MEDIA 只会弹授权对话框，不是列表页。
                    // "android.settings.MEDIA_MANAGEMENT_SETTINGS" 才是跳到
                    // "媒体管理应用"应用列表页的正确 Action（API 30+ 均支持）。
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        firstResolvable(
                            // ① 标准 AOSP 列表页（一加 / 原生 / Pixel 均走这条）
                            Intent("android.settings.MEDIA_MANAGEMENT_SETTINGS"),
                            // ② ColorOS 备用入口（极少数旧版 ColorOS）
                            Intent().apply {
                                component = android.content.ComponentName(
                                    "com.android.settings",
                                    "com.android.settings.Settings\$MediaManagementAppsActivity"
                                )
                            }
                        ) ?: Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    } else {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    }

                PermissionType.FULL_SCREEN_INTENT ->
                    // Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT 是公开常量
                    // （API 34+），跳到全部应用的"发送全屏通知"列表页。
                    // ColorOS 的 resolveActivity 对 Action 查询受 <queries> 限制，
                    // 必须同时声明组件名备用路径才能正确跳转。
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        firstResolvable(
                            // ① 标准 AOSP Action（原生 / Pixel 走这条）
                            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT),
                            // ② AOSP Settings 组件名（ColorOS 底层仍基于 AOSP，部分版本走这条）
                            Intent().apply {
                                component = android.content.ComponentName(
                                    "com.android.settings",
                                    "com.android.settings.Settings\$ManageAppUseFullScreenIntentActivity"
                                )
                            },
                            // ③ ColorOS/OxygenOS 可能的私有入口
                            Intent().apply {
                                component = android.content.ComponentName(
                                    "com.oplus.securitypermission",
                                    "com.oplus.securitypermission.permission.ui.PermissionGroupAppsActivity"
                                )
                                putExtra("permissionGroup", "USE_FULL_SCREEN_INTENT")
                            }
                        ) ?: Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    } else {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    }

                PermissionType.DEFAULT_APPS ->
                    // 公开 SDK 常量（API 24+），跳到系统"默认应用"设置页。
                    Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)

                PermissionType.DEVICE_ADMIN ->
                    // 没有公开 Settings.ACTION_* 常量，只能用硬编码组件名，找不到
                    // 就自动退回本应用详情页。
                    firstResolvable(
                        Intent().apply {
                            component = android.content.ComponentName(
                                "com.android.settings",
                                "com.android.settings.DeviceAdminSettings"
                            )
                        }
                    ) ?: Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
            }
            // 【诊断】把实际发出的 Intent 打出来，方便确认走的是哪条分支
            val debugTarget = intent.component?.let { "组件: ${it.packageName}/${it.className}" }
                ?: "Action: ${intent.action}"
            android.util.Log.d("PermDebug", "[${item.type}] 即将跳转 -> $debugTarget")

            startActivity(intent)
        } catch (e: Exception) {
            // 【诊断】用可滚动、可长按复制的完整弹窗展示真实异常。
            // 关键修复：AlertDialog.show() 是非阻塞的，之前的写法是弹窗弹出后
            // 立刻紧接着 startActivity 跳应用详情——应用详情页几乎瞬间就会盖住
            // 这个弹窗，导致你根本看不到报错内容，表现就是"直接跳到应用详情"。
            // 现在改成等你点弹窗的"关闭"按钮之后，才执行兜底跳转。
            android.util.Log.e("PermDebug", "[${item.type}] 跳转失败", e)
            showDebugTextDialog(
                "跳转失败",
                "类型: ${e.javaClass.name}\n消息: ${e.message}\n\n${android.util.Log.getStackTraceString(e)}",
                onDismiss = {
                    try {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$packageName")
                        })
                    } catch (ex: Exception) {
                        Toast.makeText(this, "无法打开系统设置", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    /**
     * 【诊断】通用的可滚动、可选中复制的文本弹窗，用于展示可能很长的
     * 异常堆栈或诊断结果，避免 Toast 截断。onDismiss 会在用户点击"关闭"
     * 之后才执行，用来延迟兜底跳转，避免把弹窗盖住。
     */
    private fun showDebugTextDialog(title: String, content: String, onDismiss: (() -> Unit)? = null) {
        val scrollView = android.widget.ScrollView(this)
        val textView = android.widget.TextView(this).apply {
            text = content
            setPadding(32, 24, 32, 24)
            setTextIsSelectable(true)
            textSize = 12f
        }
        scrollView.addView(textView)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setView(scrollView)
            .setCancelable(false)
            .setPositiveButton("关闭") { _, _ -> onDismiss?.invoke() }
            .show()
    }

    /**
     * 依次尝试多个候选 Intent，返回第一个系统上真正能处理的（用 resolveActivity 检测）。
     * 都不行则返回 null，交给调用方走最终兜底。
     * 【诊断】把每个候选的检测结果打出来，方便确认到底是哪一个候选没通过。
     */
    private fun firstResolvable(vararg intents: Intent): Intent? {
        for (intent in intents) {
            val target = intent.component?.let { "${it.packageName}/${it.className}" }
                ?: (intent.action ?: "unknown")
            val resolved = intent.resolveActivity(packageManager)
            android.util.Log.d("PermDebug", "候选 $target -> ${if (resolved != null) "可用" else "不可用"}")
            if (resolved != null) return intent
        }
        android.util.Log.d("PermDebug", "所有候选都不可用，将走兜底")
        return null
    }

    /**
     * 跳转到系统"按权限查看应用"列表页（与无障碍/悬浮窗页面类似的效果）。
     *
     * - Android 10+ 的原生 Settings 理论上有 ACTION_MANAGE_ALL_APPLICATIONS_PERMISSION，
     *   但它是隐藏 API，多数国产 ROM（ColorOS/OxygenOS、MIUI/HyperOS 等）并未实现。
     * - 【已通过真机诊断确认，且是硬限制】com.oplus.securitypermission 包下的
     *   PermissionTabActivity 虽然存在，但被标记为签名级权限
     *   com.oplus.permission.safe.SECURITY 保护，第三方 App 无法直接启动
     *   （SecurityException），任何 extra 参数都无法绕过——这是 OS 层面的安全限制，
     *   不是猜错参数的问题，所以这里不再尝试它。
     * - 退而求其次尝试同包的"权限分类总览页" PermissionGroupsActivity，如果它的
     *   保护级别宽松一些，至少能跳到一个可以手动点进具体权限的页面；如果同样被拒绝，
     *   就说明这台设备上第三方 App 确实无法深链到任何按权限分类的列表页，
     *   只能退回本应用详情页（这是系统安全边界决定的，不是可修复的 bug）。
     */
    private fun permissionGroupIntent(permissionGroup: String): Intent {
        val candidates = mutableListOf<Intent>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            candidates += Intent("android.settings.MANAGE_ALL_APPLICATIONS_PERMISSION").apply {
                putExtra("android.intent.extra.PERMISSION_NAME", permissionGroup)
            }
        }

        // 权限分类总览页（比 PermissionTabActivity 更"上层"，值得一试，
        // 但同包的其他页面很可能也是同一签名保护级别）
        candidates += Intent().apply {
            component = android.content.ComponentName(
                "com.oplus.securitypermission",
                "com.oplusos.securitypermission.permission.PermissionGroupsActivity"
            )
        }

        return firstResolvable(*candidates.toTypedArray())
            ?: Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
    }
}
