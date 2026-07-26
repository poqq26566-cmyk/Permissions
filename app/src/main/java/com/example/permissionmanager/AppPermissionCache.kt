package com.example.permissionmanager

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper

/**
 * 全量已安装应用「申请了哪些权限 / 哪些已授权」的进程内缓存。
 *
 * 背景：之前每次点开一个权限分类（麦克风/相机/位置...）都会重新调用一遍
 * getInstalledPackages(GET_PERMISSIONS) 扫描全部已安装应用，这个调用本身
 * 在应用比较多的设备（尤其是国产 ROM 预装很多系统应用）上就比较慢，用户
 * 点开好几个分类就要重复等好几次，感觉很卡。
 *
 * 优化思路：
 * 1. 扫描"这个应用申请了哪些权限字符串、哪些已授权"这一步只做一次，结果
 *    缓存在内存里（不加载图标/名称，这一步本身很快）；
 * 2. App 启动时（MainActivity.onCreate）就提前在后台线程"预热"一次，
 *    等用户真正点进某个分类时，大概率已经加载完了；
 * 3. 每个分类页面只需要在这份缓存上做一次内存过滤（毫秒级），再只为
 *    "匹配上的那一小部分应用"去加载图标和名称（这部分应用数量通常很少，
 *    比如申请麦克风权限的应用可能只有二三十个，而不是全部几百个应用）。
 */
object AppPermissionCache {

    /** 轻量条目：不含图标/名称，扫描全部应用时用这个，足够快 */
    data class LiteEntry(
        val packageName: String,
        val isSystemApp: Boolean,
        val requestedPermissions: Set<String>,
        val grantedPermissions: Set<String>
    )

    @Volatile
    private var cached: List<LiteEntry>? = null

    @Volatile
    private var loading = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingCallbacks = mutableListOf<(List<LiteEntry>) -> Unit>()

    /** App 启动时调用一次，提前在后台把扫描做完 */
    fun warmUp(context: Context) {
        get(context) {}
    }

    /**
     * 获取全量缓存。已经有缓存时同步立刻回调；没有的话触发（或复用正在
     * 进行的）后台扫描，扫描完成后在主线程回调所有等待方。
     */
    fun get(context: Context, callback: (List<LiteEntry>) -> Unit) {
        val snapshot = cached
        if (snapshot != null) {
            callback(snapshot)
            return
        }
        synchronized(pendingCallbacks) {
            pendingCallbacks.add(callback)
        }
        if (!loading) {
            load(context)
        }
    }

    private fun load(context: Context) {
        loading = true
        val appContext = context.applicationContext
        Thread {
            val pm = appContext.packageManager
            val result = mutableListOf<LiteEntry>()
            try {
                @Suppress("DEPRECATION")
                val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)

                for (pkgInfo in packages) {
                    val requested = pkgInfo.requestedPermissions ?: continue
                    val flags = pkgInfo.requestedPermissionsFlags
                    val appInfo = pkgInfo.applicationInfo ?: continue
                    if (appInfo.packageName == appContext.packageName) continue // 跳过本应用自己

                    val requestedSet = HashSet<String>(requested.size)
                    val grantedSet = HashSet<String>()
                    for (i in requested.indices) {
                        requestedSet.add(requested[i])
                        if (flags != null &&
                            (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) ==
                            PackageInfo.REQUESTED_PERMISSION_GRANTED
                        ) {
                            grantedSet.add(requested[i])
                        }
                    }
                    if (requestedSet.isEmpty()) continue

                    val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                    result.add(
                        LiteEntry(
                            packageName = appInfo.packageName,
                            isSystemApp = isSystemApp,
                            requestedPermissions = requestedSet,
                            grantedPermissions = grantedSet
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AppPermissionCache", "枚举应用失败", e)
            }

            cached = result
            loading = false

            mainHandler.post {
                val callbacks: List<(List<LiteEntry>) -> Unit>
                synchronized(pendingCallbacks) {
                    callbacks = pendingCallbacks.toList()
                    pendingCallbacks.clear()
                }
                callbacks.forEach { it(result) }
            }
        }.start()
    }
}
