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
    STORAGE,
    PHONE,
    CONTACTS,
    CALENDAR,
    BATTERY,
    NOTIFICATION_LISTENER,
    USAGE_ACCESS,
    ALARMS_REMINDERS,
    WRITE_SETTINGS,
    DND_ACCESS,
    MEDIA_MANAGEMENT,
    DEFAULT_APPS,
    DEVICE_ADMIN,
    PHOTOS_VIDEOS
}
