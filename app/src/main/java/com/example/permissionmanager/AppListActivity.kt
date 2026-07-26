package com.example.permissionmanager

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.permissionmanager.databinding.ActivityAppListBinding

/**
 * 麦克风 / 相机 / 位置 / 电话 / 联系人 / 日历 点击后不再直接跳系统设置，
 * 而是在本 App 内展示"哪些已安装应用申请了这个权限"的列表。
 *
 * 点进列表里的某一个具体应用后，才会打开系统的"应用详情"页——这是
 * Android 系统的硬限制：第三方 App 无法代替用户去开关别的 App 的权限，
 * 唯一能做到的入口就是那个应用自己的详情页，所以这一步无法避免，
 * 但入口本身（点击麦克风卡片）已经不再直接跳系统设置了。
 */
class AppListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppListBinding

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_PERMISSIONS = "extra_permissions"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "应用列表"
        val targetPermissions = intent.getStringArrayExtra(EXTRA_PERMISSIONS)?.toList() ?: emptyList()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = title
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        loadApps(targetPermissions)
    }

    private fun loadApps(targetPermissions: List<String>) {
        Thread {
            val pm = packageManager
            val result = mutableListOf<AppPermInfo>()

            try {
                @Suppress("DEPRECATION")
                val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)

                for (pkgInfo in packages) {
                    val requested = pkgInfo.requestedPermissions ?: continue
                    val flags = pkgInfo.requestedPermissionsFlags

                    var matchedIndex = -1
                    for (i in requested.indices) {
                        if (requested[i] in targetPermissions) {
                            matchedIndex = i
                            break
                        }
                    }
                    if (matchedIndex == -1) continue

                    // 只要该应用申请的权限里有任意一个当前处于已授权状态，就标记为"已授权"
                    var granted = false
                    for (i in requested.indices) {
                        if (requested[i] in targetPermissions && flags != null) {
                            if (flags[i] and PackageInfoCompatGranted == PackageInfoCompatGranted) {
                                granted = true
                                break
                            }
                        }
                    }

                    val appInfo = pkgInfo.applicationInfo ?: continue
                    if (appInfo.packageName == packageName) continue // 跳过本应用自己

                    val isSystemApp =
                        (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

                    try {
                        val label = appInfo.loadLabel(pm).toString()
                        val icon = appInfo.loadIcon(pm)
                        result.add(
                            AppPermInfo(
                                packageName = appInfo.packageName,
                                label = label,
                                icon = icon,
                                granted = granted,
                                isSystemApp = isSystemApp
                            )
                        )
                    } catch (e: Exception) {
                        // 个别应用取图标/名称失败时跳过，不影响整体列表
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AppListActivity", "枚举应用失败", e)
            }

            // 已授权的排前面，同状态下按名称排序；非系统应用优先展示在前面更符合用户关心的范围
            result.sortWith(
                compareBy<AppPermInfo> { it.isSystemApp }
                    .thenByDescending { it.granted }
                    .thenBy { it.label }
            )

            runOnUiThread {
                if (result.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.recyclerView.adapter = AppListAdapter(result) { app ->
                        openAppDetails(app)
                    }
                }
            }
        }.start()
    }

    /**
     * 点击列表里的某个具体应用时，跳到该应用自己的"应用详情"页去开关权限。
     * 这是 Android 系统层面的硬限制：第三方 App 没有 API 能直接打开
     * "别的应用的某个权限开关"，只能引导用户去这个应用的详情页里自己操作。
     */
    private fun openAppDetails(app: AppPermInfo) {
        try {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${app.packageName}")
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("AppListActivity", "打开应用详情失败: ${app.packageName}", e)
        }
    }
}

// PackageInfo.REQUESTED_PERMISSION_GRANTED 的值就是 1 shl 1 = 2
private const val PackageInfoCompatGranted =
    android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED
