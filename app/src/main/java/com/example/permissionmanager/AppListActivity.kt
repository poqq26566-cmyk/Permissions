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
 * 麦克风 / 相机 / 位置 / 电话 / 联系人 / 日历 / 剪贴板 / 应用列表 / 照片与视频 /
 * 创建桌面快捷方式 / 设备动作与方向 / 耗电行为管理 点击后不再直接跳系统设置，
 * 而是在本 App 内展示一份应用列表，并支持按应用名称/包名搜索，以及按
 * "全部 / 第三方应用 / 系统应用"筛选。
 *
 * 两种模式：
 * 1. 按权限过滤（默认）：只展示"申请了这个具体权限"的应用，并标注已授权/未授权
 *    （麦克风、相机、位置、照片与视频等大多数分类走这条）。
 * 2. 全部应用（MODE_ALL_APPS）：Android 没有对应的可声明权限字符串，没法用
 *    "谁申请了这个权限"来筛选（比如"耗电行为管理"是各厂商 ROM 自己的后台
 *    管控策略，不是一个 App 可以在 AndroidManifest 里申请的权限；"读取/写入
 *    剪贴板"同理，系统本身就没有对应的权限声明）。这种情况下展示全部已安装
 *    应用，不显示"已授权/未授权"标签。
 *
 * 点进列表里的某一个具体应用后，才会打开系统的"应用详情"页——这是
 * Android 系统的硬限制：第三方 App 无法代替用户去开关别的 App 的权限或
 * 行为策略，唯一能做到的入口就是那个应用自己的详情页，所以这一步无法
 * 避免，但入口本身（点击麦克风等卡片）已经不再直接跳系统设置了。
 *
 * 性能：全量应用的"申请了哪些权限"扫描由 AppPermissionCache 只做一次
 * 并常驻内存（App 启动时已经预热过一次，还落了一份磁盘缓存应对被杀
 * 后台重开），这里只需要在缓存上做一次内存过滤，再只为"匹配上的那一
 * 小部分应用"加载图标和名称，所以即使是第一次打开也比之前直接全量
 * 扫描快很多，后续再打开别的分类基本秒开。
 */
class AppListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppListBinding

    /** 加载一次后缓存的全量数据，搜索/筛选都只在内存里过滤，不用重新扫描应用 */
    private var allApps: List<AppPermInfo> = emptyList()

    private enum class AppFilter { ALL, THIRD_PARTY, SYSTEM }
    private var currentFilter = AppFilter.ALL
    private var currentQuery = ""

    private var showGrantedStatus = true

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_PERMISSIONS = "extra_permissions"
        const val EXTRA_HINT = "extra_hint"

        /**
         * 传给 EXTRA_PERMISSIONS 的特殊哨兵值：代表这个分类在 Android 上根本没有
         * 对应的可声明权限字符串（比如耗电行为管理、剪贴板），此时展示全部已
         * 安装应用，而不是按权限过滤。
         */
        const val MODE_ALL_APPS = "__ALL_APPS__"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "应用列表"
        val targetPermissions = intent.getStringArrayExtra(EXTRA_PERMISSIONS)?.toList() ?: emptyList()
        val customHint = intent.getStringExtra(EXTRA_HINT)
        val isAllAppsMode = targetPermissions.size == 1 && targetPermissions[0] == MODE_ALL_APPS
        showGrantedStatus = !isAllAppsMode

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = title
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.tvHint.text = customHint ?: if (isAllAppsMode) {
            "以下是已安装的应用，点击可查看该应用的详情设置"
        } else {
            "以下应用申请了该权限，点击可查看该应用的权限详情"
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        setupSearchAndFilter()
        loadApps(targetPermissions, isAllAppsMode)
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

    private fun loadApps(targetPermissions: List<String>, isAllAppsMode: Boolean) {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE

        // 第一步：拿到「全部应用申请了哪些权限」这份缓存（首次可能要等后台扫描，
        // 已经预热过的话这里几乎是立刻返回）。全部应用模式也复用这份缓存，
        // 只是不按权限过滤，直接取里面的包名列表。
        AppPermissionCache.get(this) { liteEntries ->
            // 第二步：过滤出需要展示的应用，只为这一部分应用去加载图标和
            // 名称（比较慢的部分），放到后台线程做。
            Thread {
                val pm = packageManager
                val result = mutableListOf<AppPermInfo>()

                for (entry in liteEntries) {
                    val matched = isAllAppsMode || entry.requestedPermissions.any { it in targetPermissions }
                    if (!matched) continue

                    val granted = !isAllAppsMode && entry.grantedPermissions.any { it in targetPermissions }

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

                // 已授权的排前面，同状态下按名称排序；非系统应用优先展示在前面。
                // 全部应用模式没有"已授权"这回事，直接按名称排序。
                result.sortWith(
                    if (isAllAppsMode) {
                        compareBy<AppPermInfo> { it.isSystemApp }.thenBy { it.label }
                    } else {
                        compareBy<AppPermInfo> { it.isSystemApp }
                            .thenByDescending { it.granted }
                            .thenBy { it.label }
                    }
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
            binding.recyclerView.adapter = AppListAdapter(filtered, showGrantedStatus) { app ->
                openAppDetails(app)
            }
        }
    }

    /**
     * 点击列表里的某个具体应用时，跳到该应用自己的"应用详情"页去开关权限
     * 或调整耗电行为等设置。这是 Android 系统层面的硬限制：第三方 App
     * 没有 API 能直接打开"别的应用的某个权限开关/行为策略"，只能引导
     * 用户去这个应用的详情页里自己操作。
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
