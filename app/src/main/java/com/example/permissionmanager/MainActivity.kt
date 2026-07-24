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

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val permissionList = listOf(
            PermissionItem("特殊应用权限（总览）", "跳到系统的\"特殊应用权限\"汇总页，包含悬浮窗、后台弹出、使用情况访问等全部分类（不同厂商实现不同，找不到会自动退回本应用详情页）",
                R.drawable.ic_grid, R.color.tint_black, PermissionType.SPECIAL_ACCESS_OVERVIEW),
            PermissionItem("无障碍", "允许应用使用无障碍服务，控制设备",
                R.drawable.ic_accessibility, R.color.tint_purple, PermissionType.ACCESSIBILITY),
            PermissionItem("悬浮窗", "允许应用在其他应用上层显示，可能影响其他应用",
                R.drawable.ic_overlay, R.color.tint_blue, PermissionType.OVERLAY),
            PermissionItem("麦克风", "允许应用使用麦克风录制音频",
                R.drawable.ic_microphone, R.color.tint_red, PermissionType.MICROPHONE),
            PermissionItem("相机", "允许应用使用相机拍照和录像",
                R.drawable.ic_camera, R.color.tint_orange, PermissionType.CAMERA),
            PermissionItem("位置", "允许应用访问设备的精确位置信息",
                R.drawable.ic_location, R.color.tint_green, PermissionType.LOCATION),
            PermissionItem("通知", "允许应用向您发送通知消息",
                R.drawable.ic_notification, R.color.tint_yellow, PermissionType.NOTIFICATION),
            PermissionItem("存储", "允许应用读取和写入存储空间中的文件",
                R.drawable.ic_storage, R.color.tint_brown, PermissionType.STORAGE),
            PermissionItem("电话", "允许应用拨打电话和管理通话记录",
                R.drawable.ic_phone, R.color.tint_teal, PermissionType.PHONE),
            PermissionItem("联系人", "允许应用读取和修改您的联系人信息",
                R.drawable.ic_contacts, R.color.tint_indigo, PermissionType.CONTACTS),
            PermissionItem("日历", "允许应用读取和修改您的日历事件",
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
            PermissionItem("媒体管理应用", "允许应用在无需用户逐一确认的情况下修改或删除媒体文件（Android 13+）",
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

    private fun openPermissionSettings(item: PermissionItem) {
        try {
            val intent = when (item.type) {
                PermissionType.ACCESSIBILITY ->
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                PermissionType.OVERLAY ->
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName"))
                PermissionType.NOTIFICATION ->
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    }
                PermissionType.BATTERY ->
                    // ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS 跳到全部应用的电池优化
                    // 列表页（GMS 强制要求的公开特殊权限入口，跨厂商稳定）。
                    // 之前用的 ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 只会为
                    // 本应用弹一个系统授权对话框，不是列表，效果完全不同。
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                PermissionType.UNKNOWN_SOURCES ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:$packageName"))
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
                    // "android.provider.action.REQUEST_MANAGE_MEDIA"（即 MediaStore.
                    // ACTION_REQUEST_MANAGE_MEDIA，API 33+）在当前编译环境里解析不到，
                    // 直接用字符串字面量，效果相同，跳到全部应用的"媒体管理应用"列表页。
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Intent("android.provider.action.REQUEST_MANAGE_MEDIA")
                    } else {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    }
                PermissionType.FULL_SCREEN_INTENT ->
                    // Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT 是公开常量
                    // （API 34+），跳到全部应用的"发送全屏通知"列表页。
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
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
            startActivity(intent)
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                })
            } catch (ex: Exception) {
                Toast.makeText(this, "无法打开系统设置", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 依次尝试多个候选 Intent，返回第一个系统上真正能处理的（用 resolveActivity 检测）。
     * 都不行则返回 null，交给调用方走最终兜底。
     */
    private fun firstResolvable(vararg intents: Intent): Intent? {
        for (intent in intents) {
            if (intent.resolveActivity(packageManager) != null) return intent
        }
        return null
    }

    /**
     * 跳转到系统"按权限查看应用"列表页（与无障碍/悬浮窗页面类似的效果）。
     *
     * - Android 10+ 的原生 Settings 理论上有 ACTION_MANAGE_ALL_APPLICATIONS_PERMISSION，
     *   但它是隐藏 API，多数国产 ROM（ColorOS/OxygenOS、MIUI/HyperOS 等）并未实现。
     * - ColorOS/OxygenOS（OPPO、一加、Realme）有自己的权限管理入口
     *   com.coloros.safecenter/.privacypermissionsentry.PermissionTopActivity，
     *   效果和你截图里悬浮窗/麦克风的列表页一致，但没有公开参数能直接定位到某一个
     *   权限类型，只能先跳到这个权限管理主页，再手动点进对应权限。
     * - 都打不开的话，最终退回本应用详情页。
     */
    private fun permissionGroupIntent(permissionGroup: String): Intent {
        val candidates = mutableListOf<Intent>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            candidates += Intent("android.settings.MANAGE_ALL_APPLICATIONS_PERMISSION").apply {
                putExtra("android.intent.extra.PERMISSION_NAME", permissionGroup)
            }
        }

        // ColorOS / OxygenOS（OPPO、一加、Realme）权限管理主页
        candidates += Intent().apply {
            component = android.content.ComponentName(
                "com.coloros.safecenter",
                "com.coloros.privacypermissionsentry.PermissionTopActivity"
            )
        }

        return firstResolvable(*candidates.toTypedArray())
            ?: Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
    }
}
