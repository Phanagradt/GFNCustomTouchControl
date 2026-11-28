package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import com.example.service.NexusAccessibilityService
import com.example.service.NexusGamepadImeService

object SystemIntegrations {

    const val GEFORCE_NOW_PACKAGE = "com.nvidia.geforcenow"
    const val STEAM_LINK_PACKAGE = "com.valvesoftware.steamlink"
    const val MOONLIGHT_PACKAGE = "com.limelight"
    const val PARSEC_PACKAGE = "tv.parsec.client"

    fun canDrawOverlays(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    fun openOverlayPermissionSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun isImeEnabled(context: Context): Boolean {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            ?: return false
        val targetComponent = ComponentName(context, NexusGamepadImeService::class.java)
        return imm.enabledInputMethodList.any { info ->
            info.component == targetComponent || info.packageName == context.packageName
        }
    }

    fun isImeSelected(context: Context): Boolean {
        val current = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        ) ?: return false
        return current.contains(context.packageName)
    }

    fun openInputMethodSettings(context: Context) {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun showInputMethodPicker(context: Context) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showInputMethodPicker()
    }

    fun isAccessibilityEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val target = ComponentName(context, NexusAccessibilityService::class.java).flattenToString()
        val shortTarget = ComponentName(context, NexusAccessibilityService::class.java).flattenToShortString()
        return enabled.contains(target, ignoreCase = true) ||
            enabled.contains(shortTarget, ignoreCase = true) ||
            enabled.contains(NexusAccessibilityService::class.java.simpleName, ignoreCase = true)
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        if (packageName.isBlank()) return false
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        }.getOrDefault(false)
    }

    fun launchPackageOrPlayStore(context: Context, packageName: String): Boolean {
        if (packageName.isBlank()) return false
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return true
        }
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(marketIntent) }
        return false
    }

    fun isGeForceNowInstalled(context: Context): Boolean =
        isPackageInstalled(context, GEFORCE_NOW_PACKAGE)

    fun isSteamLinkInstalled(context: Context): Boolean =
        isPackageInstalled(context, STEAM_LINK_PACKAGE)

    fun isMoonlightInstalled(context: Context): Boolean =
        isPackageInstalled(context, MOONLIGHT_PACKAGE)

    fun isParsecInstalled(context: Context): Boolean =
        isPackageInstalled(context, PARSEC_PACKAGE)

    fun launchGeForceNowOrStore(context: Context): Boolean =
        launchPackageOrPlayStore(context, GEFORCE_NOW_PACKAGE)

    fun launchSteamLinkOrStore(context: Context): Boolean =
        launchPackageOrPlayStore(context, STEAM_LINK_PACKAGE)

    fun performKeyHaptic(
        view: View,
        vibrationMode: com.example.data.VibrationMode,
        controlEnabled: Boolean
    ) {
        if (!controlEnabled || vibrationMode == com.example.data.VibrationMode.NONE) return
        val constant = when (vibrationMode) {
            com.example.data.VibrationMode.NONE -> return
            com.example.data.VibrationMode.LIGHT -> HapticFeedbackConstants.KEYBOARD_TAP
            com.example.data.VibrationMode.STRONG -> HapticFeedbackConstants.VIRTUAL_KEY
        }
        view.performHapticFeedback(constant)
    }
}
