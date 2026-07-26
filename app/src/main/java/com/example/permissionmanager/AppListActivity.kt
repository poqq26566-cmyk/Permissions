package com.example.permissionmanager

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.permissionmanager.databinding.ActivityAppListBinding

/**
 * 麦克风 / 相机 / 位置 / 电话 / 联系人 / 日历 点击后不再直接跳系统设置，
 * 而是在本 App 内展示"哪些已安装应用申请了这个权限"的列表，并支持
 * 按应用名称/包名搜索，以及按"全部 / 第三方应用 / 系统应用"筛选。
 *
 * 点进列表里的某一个具体应用后，才会打开系统的"应用详情"页——这是
 * Android 系统的硬限制：第三方 App 无法代替用户去开关别的 App 的权限，
 * 唯一能做到的入口就是那个应用自己的详情页，所以这一步无法避免，
 * 但入口本身（点击麦克风卡片）已经不再直接跳系统设置了。
 */
class AppListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppListBinding

    /** 加载一次后缓存的全量数据，搜索/筛选都只在内存里过滤，不用重新扫描应用 */
    private var allApps: List<AppPermInfo> = emptyList()

    private enum class AppFilter { ALL, THIRD_PARTY, SYSTEM }
    private var currentFilter = AppFilter.ALL
    private var currentQuery = ""

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

        setupSearchAndFilter()
        loadApps(targetPermissions)
    }

    private fun setupSearchAndFilter() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentQuery = s?.toString()?.trim().orEmpty()
                applyFilterAndRender()
            }
        })

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                binding.chipThirdParty.id -> AppFilter.THIRD_PARTY
                binding.chipSystem.id -> AppFilter.SYSTEM
                else -> AppFilter.ALL
            }
            applyFilterAndRender()
        }
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
                allApps = result
                applyFilterAndRender()
            }
        }.start()
    }

    /** 把当前搜索关键字 + 筛选类型应用到 allApps 上，得到最终展示列表 */
    private fun applyFilterAndRender() {
        var filtered = allApps

        filtered = when (currentFilter) {
            AppFilter.ALL -> filtered
            AppFilter.THIRD_PARTY -> filtered.filter { !it.isSystemApp }
            AppFilter.SYSTEM -> filtered.filter { it.isSystemApp }
        }

        if (currentQuery.isNotEmpty()) {
            val q = currentQuery.lowercase()
            filtered = filtered.filter {
                it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }

        if (filtered.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
            binding.tvEmpty.text = if (allApps.isEmpty()) {
                "没有找到申请该权限的应用"
            } else {
                "没有匹配的应用，换个关键词试试"
            }
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            binding.recyclerView.adapter = AppListAdapter(filtered) { app ->
                openAppDetails(app)
            }
        }
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
