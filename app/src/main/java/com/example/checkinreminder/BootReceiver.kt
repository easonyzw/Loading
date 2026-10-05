package com.example.checkinreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!GeofenceManager.hasRequiredPermissions(context)) return
        val preferences = context.getSharedPreferences(
            GeofenceManager.PREFERENCES_NAME, Context.MODE_PRIVATE
        )
        if (!preferences.contains(GeofenceManager.COMPANY_LAT_KEY) ||
            !preferences.contains(GeofenceManager.COMPANY_LNG_KEY)
        ) return

        val pendingResult = goAsync()
        GeofenceManager.registerCompanyGeofence(context) { pendingResult.finish() }
    }
}
