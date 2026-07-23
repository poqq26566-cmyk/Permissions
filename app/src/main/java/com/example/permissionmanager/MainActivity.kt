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
                R.drawable.ic_install, R.color.tint_deep_orange, PermissionType.UNKNOWN_SOURCES)
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
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
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
                    permissionGroupIntent("android.permission-group.STORAGE")
                PermissionType.PHONE ->
                    permissionGroupIntent("android.permission-group.PHONE")
                PermissionType.CONTACTS ->
                    permissionGroupIntent("android.permission-group.CONTACTS")
                PermissionType.CALENDAR ->
                    permissionGroupIntent("android.permission-group.CALENDAR")
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
     * 跳转到系统"按权限查看应用"列表页（与无障碍/悬浮窗页面类似的效果）。
     * 该 Action 自 Android 10 (API 29) 起可用；更低版本系统没有对应的
     * 跨应用权限列表页，只能退回到本应用的详情页。
     */
    private fun permissionGroupIntent(permissionGroup: String): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // ACTION_MANAGE_ALL_APPLICATIONS_PERMISSION / EXTRA_PERMISSION_NAME 不是公开 SDK
            // 常量（仅供系统 PermissionController 内部使用），编译期无法引用，这里直接用
            // 对应的字符串字面量，效果相同。
            Intent("android.settings.MANAGE_ALL_APPLICATIONS_PERMISSION").apply {
                putExtra("android.intent.extra.PERMISSION_NAME", permissionGroup)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
        }
    }
}
