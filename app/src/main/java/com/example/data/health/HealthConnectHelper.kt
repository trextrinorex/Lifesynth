package com.example.data.health

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object HealthConnectHelper {

    private const val HEALTH_CONNECT_PACKAGE_NAME = "com.google.android.apps.healthdata"
    const val ACTION_HEALTH_CONNECT_SETTINGS = "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"
    const val ACTION_HEALTH_HOME = "android.health.connect.action.HEALTH_HOME_SETTINGS"

    fun isHealthConnectInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(HEALTH_CONNECT_PACKAGE_NAME, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            // In Android 14+, Health Connect is a system component
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        }
    }

    fun openHealthConnect(context: Context) {
        // Try Android 14+ system health settings
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val intent = Intent(ACTION_HEALTH_HOME).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (_: Exception) { }
        }

        // Try Health Connect app settings intent
        try {
            val intent = Intent(ACTION_HEALTH_CONNECT_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        } catch (_: Exception) { }

        // Try direct package intent
        val launchIntent = context.packageManager.getLaunchIntentForPackage(HEALTH_CONNECT_PACKAGE_NAME)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return
        }

        // Fallback: Open Google Play Store page for Health Connect
        try {
            val playIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$HEALTH_CONNECT_PACKAGE_NAME")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(playIntent)
        } catch (_: Exception) {
            try {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$HEALTH_CONNECT_PACKAGE_NAME")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (_: Exception) { }
        }
    }
}
