package com.example.permissionmanager

import android.content.Intent
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
 *
 * 性能：全量应用的"申请了哪些权限"扫描由 AppPermissionCache 只做一次
 * 并常驻内存（App 启动时已经预热过一次），这里只需要在缓存上做一次
 * 内存过滤，再只为"匹配上的那一小部分应用"加载图标和名称，所以即使
 * 是第一次打开也比之前直接全量扫描快很多，后续再打开别的分类基本秒开。
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
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE

        // 第一步：拿到「全部应用申请了哪些权限」这份缓存（首次可能要等后台扫描，
        // 已经预热过的话这里几乎是立刻返回）。
        AppPermissionCache.get(this) { liteEntries ->
            // 第二步：在缓存上过滤出申请了目标权限的应用，只为这一小部分应用
            // 去加载图标和名称（比较慢的部分），放到后台线程做。
            Thread {
                val pm = packageManager
                val result = mutableListOf<AppPermInfo>()

                for (entry in liteEntries) {
                    val matched = entry.requestedPermissions.any { it in targetPermissions }
                    if (!matched) continue

                    val granted = entry.grantedPermissions.any { it in targetPermissions }

                    try {
                        @Suppress("DEPRECATION")
                        val appInfo = pm.getApplicationInfo(entry.packageName, 0)
                        val label = appInfo.loadLabel(pm).toString()
                        val icon = appInfo.loadIcon(pm)
                        result.add(
                            AppPermInfo(
                                packageName = entry.packageName,
                                label = label,
                                icon = icon,
                                granted = granted,
                                isSystemApp = entry.isSystemApp
                            )
                        )
                    } catch (e: Exception) {
                        // 应用在扫描后被卸载，或取图标/名称失败时跳过
                    }
                }

                // 已授权的排前面，同状态下按名称排序；非系统应用优先展示在前面
                result.sortWith(
                    compareBy<AppPermInfo> { it.isSystemApp }
                        .thenByDescending { it.granted }
                        .thenBy { it.label }
                )

                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    allApps = result
                    applyFilterAndRender()
                }
            }.start()
        }
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
