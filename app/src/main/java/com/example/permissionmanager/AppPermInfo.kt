package com.example.permissionmanager

import android.graphics.drawable.Drawable

/**
 * 表示"申请了某个权限分类（如麦克风）"的一个已安装应用。
 */
data class AppPermInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val granted: Boolean,
    val isSystemApp: Boolean
)
