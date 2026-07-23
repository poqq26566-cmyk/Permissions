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
                R.drawable.ic_usage, R.color.tint_deep_purple, PermissionType.USAGE_ACCESS)
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
