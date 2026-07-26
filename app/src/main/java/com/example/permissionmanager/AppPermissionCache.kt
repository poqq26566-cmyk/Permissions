package com.example.permissionmanager

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject

/**
 * 全量已安装应用「申请了哪些权限 / 哪些已授权」的缓存。
 *
 * 分两层：
 * 1. 内存缓存（进程存活期间有效）——同一次打开 App 期间，点开麦克风、
 *    相机、位置等好几个分类只扫描一次，后面全部复用，秒开。
 * 2. 磁盘缓存（SharedPreferences，跨进程重启也有效）——App 被从后台
 *    划掉/杀掉后重新打开，内存缓存会丢失，之前的做法是老老实实重新
 *    扫一遍全部已安装应用，用户感觉"删个后台又要等"。现在改成优先读
 *    磁盘上次的扫描结果（读一个小文件很快，几毫秒级别），先把列表显示
 *    出来，同时在后台悄悄重新扫一遍最新数据、更新内存缓存并重新落盘，
 *    这样下次冷启动用的就是更新后的数据，同时这次也不用干等。
 */
object AppPermissionCache {

    /** 轻量条目：不含图标/名称，扫描/缓存全部应用时用这个，足够快 */
    data class LiteEntry(
        val packageName: String,
        val isSystemApp: Boolean,
        val requestedPermissions: Set<String>,
        val grantedPermissions: Set<String>
    )

    private const val PREFS_NAME = "app_permission_cache"
    private const val KEY_DATA = "data_v1"

    @Volatile
    private var cached: List<LiteEntry>? = null

    @Volatile
    private var loading = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingCallbacks = mutableListOf<(List<LiteEntry>) -> Unit>()

    /** App 启动时调用一次，提前在后台把数据准备好 */
    fun warmUp(context: Context) {
        get(context) {}
    }

    /**
     * 获取全量缓存。已经有内存缓存时同步立刻回调；否则触发（或复用正在
     * 进行的）后台加载流程：先尝试磁盘缓存快速返回一次，再在后台做一次
     * 全量重新扫描来更新数据。
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
        if (loading) return
        loading = true

        val appContext = context.applicationContext
        Thread {
            // 1) 磁盘缓存命中的话，先用它把等待中的回调都放行，避免每次杀
            //    后台重开都要干等一次全量扫描。
            val diskEntries = loadFromDisk(appContext)
            if (diskEntries != null) {
                cached = diskEntries
                flushCallbacks(diskEntries)
            }

            // 2) 不管磁盘有没有命中，都在后台重新扫一遍最新数据（应用可能被
            //    安装/卸载、权限状态可能变化），扫完后更新内存缓存并落盘，
            //    下次冷启动就能直接用上更新后的结果。
            val freshEntries = scanInstalledPackages(appContext)
            cached = freshEntries
            saveToDisk(appContext, freshEntries)
            loading = false

            if (diskEntries == null) {
                // 之前没有磁盘缓存可用，只能靠这次全量扫描的结果来放行回调
                flushCallbacks(freshEntries)
            }
        }.start()
    }

    private fun flushCallbacks(entries: List<LiteEntry>) {
        mainHandler.post {
            val callbacks: List<(List<LiteEntry>) -> Unit>
            synchronized(pendingCallbacks) {
                callbacks = pendingCallbacks.toList()
                pendingCallbacks.clear()
            }
            callbacks.forEach { it(entries) }
        }
    }

    private fun scanInstalledPackages(context: Context): List<LiteEntry> {
        val pm = context.packageManager
        val result = mutableListOf<LiteEntry>()
        try {
            @Suppress("DEPRECATION")
            val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)

            for (pkgInfo in packages) {
                val requested = pkgInfo.requestedPermissions ?: continue
                val flags = pkgInfo.requestedPermissionsFlags
                val appInfo = pkgInfo.applicationInfo ?: continue
                if (appInfo.packageName == context.packageName) continue // 跳过本应用自己

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
        return result
    }

    private fun loadFromDisk(context: Context): List<LiteEntry>? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_DATA, null) ?: return null
            val array = JSONArray(raw)
            val result = ArrayList<LiteEntry>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    LiteEntry(
                        packageName = obj.getString("p"),
                        isSystemApp = obj.getBoolean("s"),
                        requestedPermissions = obj.getJSONArray("r").toStringSet(),
                        grantedPermissions = obj.getJSONArray("g").toStringSet()
                    )
                )
            }
            result
        } catch (e: Exception) {
            android.util.Log.e("AppPermissionCache", "读取磁盘缓存失败", e)
            null
        }
    }

    private fun saveToDisk(context: Context, entries: List<LiteEntry>) {
        try {
            val array = JSONArray()
            for (entry in entries) {
                val obj = JSONObject()
                obj.put("p", entry.packageName)
                obj.put("s", entry.isSystemApp)
                obj.put("r", JSONArray(entry.requestedPermissions.toList()))
                obj.put("g", JSONArray(entry.grantedPermissions.toList()))
                array.put(obj)
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_DATA, array.toString())
                .apply()
        } catch (e: Exception) {
            android.util.Log.e("AppPermissionCache", "写入磁盘缓存失败", e)
        }
    }

    private fun JSONArray.toStringSet(): Set<String> {
        val set = HashSet<String>(length())
        for (i in 0 until length()) set.add(getString(i))
        return set
    }
}
