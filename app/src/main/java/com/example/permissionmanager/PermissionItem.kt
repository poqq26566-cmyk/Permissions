package com.example.permissionmanager

data class PermissionItem(
    val name: String,
    val description: String,
    val iconRes: Int,
    val iconTint: Int,
    val type: PermissionType
)

enum class PermissionType {
    ACCESSIBILITY,
    OVERLAY,
    MICROPHONE,
    CAMERA,
    LOCATION,
    NOTIFICATION,
    STORAGE,
    PHONE,
    CONTACTS,
    CALENDAR,
    BATTERY,
    UNKNOWN_SOURCES
}
