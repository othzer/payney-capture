package com.otzrlabs.payney.capture.ui.status

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

fun hasSmsPermission(context: Context): Boolean {
    val receive = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS)
    val read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
    return receive == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED
}

// NotificationListenerService access has no runtime-permission dialog -- it can
// only be granted by the user manually in Settings, so this just reflects
// whatever state that screen left us in.
fun hasNotificationAccess(context: Context): Boolean {
    val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
    return context.packageName in enabledPackages
}
