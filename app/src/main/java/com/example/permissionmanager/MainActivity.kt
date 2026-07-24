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

    private fun openPermissionSettings(item: PermissionItem) {
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
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

                PermissionType.USAGE_ACCESS ->
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

                PermissionType.ALARMS_REMINDERS ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    } else {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    }

                PermissionType.WRITE_SETTINGS ->
                    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)

                PermissionType.DND_ACCESS ->
                    Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

                PermissionType.BACKGROUND_POPUP ->
                    permissionGroupIntent("com.oplus.permission.opsafe.BACKGROUND_START_ACTIVITY")

                PermissionType.SPECIAL_ACCESS_OVERVIEW ->
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        firstResolvable(
                            Intent("android.settings.MEDIA_MANAGEMENT_SETTINGS"),
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        firstResolvable(
                            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT),
                            Intent().apply {
                                component = android.content.ComponentName(
                                    "com.android.settings",
                                    "com.android.settings.Settings\$ManageAppUseFullScreenIntentActivity"
                                )
                            },
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
                    Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)

                PermissionType.DEVICE_ADMIN ->
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

            val debugTarget = intent.component?.let { "组件: ${it.packageName}/${it.className}" }
                ?: "Action: ${intent.action}"
            android.util.Log.d("PermDebug", "[${item.type}] 即将跳转 -> $debugTarget")
            Toast.makeText(this, "跳转: $debugTarget", Toast.LENGTH_LONG).show()

            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("PermDebug", "[${item.type}] 跳转失败", e)
            Toast.makeText(
                this,
                "跳转失败: ${e.javaClass.simpleName} - ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                })
            } catch (ex: Exception) {
                Toast.makeText(this, "无法打开系统设置", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

    private fun permissionGroupIntent(permissionGroup: String): Intent {
        val candidates = mutableListOf<Intent>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            candidates += Intent("android.settings.MANAGE_ALL_APPLICATIONS_PERMISSION").apply {
                putExtra("android.intent.extra.PERMISSION_NAME", permissionGroup)
            }
        }

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
