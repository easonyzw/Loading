package com.example.checkinreminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

object GeofenceManager {
    const val PREFERENCES_NAME = "checkin_settings"
    const val COMPANY_LAT_KEY = "company_lat"
    const val COMPANY_LNG_KEY = "company_lng"
    const val GEOFENCE_ENABLED_KEY = "geofence_enabled"
    const val REQUEST_ID = "company_gate"
    private const val RADIUS_METERS = 200f
    private const val RESPONSIVENESS_MS = 120_000

    fun hasRequiredPermissions(context: Context): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val backgroundGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return fineGranted && backgroundGranted
    }

    @SuppressLint("MissingPermission")
    fun registerCompanyGeofence(context: Context, onResult: (String) -> Unit = {}) {
        val appContext = context.applicationContext
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            onResult("请先授予精确定位权限")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                appContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onResult("请先将位置权限设置为“始终允许”")
            return
        }
        val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val latitude = preferences.getString(COMPANY_LAT_KEY, null)?.toDoubleOrNull()
        val longitude = preferences.getString(COMPANY_LNG_KEY, null)?.toDoubleOrNull()
        if (latitude == null || longitude == null ||
            latitude !in -90.0..90.0 || longitude !in -180.0..180.0
        ) {
            onResult("请先设置有效的公司位置")
            return
        }
        val pendingIntent = geofencePendingIntent(appContext)
        val geofence = Geofence.Builder()
            .setRequestId(REQUEST_ID)
            .setCircularRegion(latitude, longitude, RADIUS_METERS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .setNotificationResponsiveness(RESPONSIVENESS_MS)
            .build()
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(0)
            .addGeofence(geofence)
            .build()
        val client = LocationServices.getGeofencingClient(appContext)
        client.removeGeofences(pendingIntent).addOnCompleteListener {
            client.addGeofences(request, pendingIntent)
                .addOnSuccessListener {
                    preferences.edit().putBoolean(GEOFENCE_ENABLED_KEY, true).apply()
                    onResult("打卡围栏已启用：公司大门 200 米")
                }
                .addOnFailureListener { error ->
                    onResult("启用打卡围栏失败：${error.localizedMessage ?: "未知错误"}")
                }
        }
    }

    private fun geofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
}
