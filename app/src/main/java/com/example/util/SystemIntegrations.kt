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
import com.example.service.NexusGamepadImeService

object SystemIntegrations {

    const val GEFORCE_NOW_PACKAGE = "com.nvidia.geforcenow"

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

    fun isGeForceNowInstalled(context: Context): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    GEFORCE_NOW_PACKAGE,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(GEFORCE_NOW_PACKAGE, 0)
            }
            true
        }.getOrDefault(false)
    }

    fun launchGeForceNowOrStore(context: Context): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(GEFORCE_NOW_PACKAGE)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return true
        }
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$GEFORCE_NOW_PACKAGE")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(marketIntent) }
        return false
    }

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
