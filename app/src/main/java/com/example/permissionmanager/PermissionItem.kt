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
    UNKNOWN_SOURCES,
    NOTIFICATION_LISTENER,
    USAGE_ACCESS,
    ALARMS_REMINDERS,
    WRITE_SETTINGS,
    DND_ACCESS,
    BACKGROUND_POPUP,
    SPECIAL_ACCESS_OVERVIEW,
    MEDIA_MANAGEMENT,
    FULL_SCREEN_INTENT,
    DEFAULT_APPS,
    DEVICE_ADMIN,
    CLIPBOARD_READ,
    CLIPBOARD_WRITE,
    APP_LIST,
    PHOTOS_VIDEOS,
    CREATE_SHORTCUT,
    DEVICE_MOTION,
    BATTERY_BEHAVIOR
}
