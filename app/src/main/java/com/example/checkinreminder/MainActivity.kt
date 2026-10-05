package com.example.checkinreminder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.checkinreminder.ui.theme.CheckInReminderTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderNotifier.createNotificationChannel(this)
        setContent { CheckInReminderTheme { MainScreen() } }
    }
}

@Composable
private fun MainScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preferences = remember {
        context.getSharedPreferences(GeofenceManager.PREFERENCES_NAME, Context.MODE_PRIVATE)
    }
    var permissionStatus by remember { mutableStateOf(locationPermissionStatus(context)) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var coordinateText by remember { mutableStateOf("当前位置：尚未获取") }
    var companyLocationText by remember { mutableStateOf(loadCompanyLocationText(preferences)) }
    var geofenceStatus by remember {
        mutableStateOf(
            if (preferences.getBoolean(GeofenceManager.GEOFENCE_ENABLED_KEY, false)) {
                "打卡围栏已启用：公司大门 200 米"
            } else {
                "打卡围栏：尚未启用"
            }
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionStatus = locationPermissionStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            ReminderNotifier.showNotification(context, "到公司了，记得打卡！", silent = false)
        }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionStatus = locationPermissionStatus(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("打卡提醒")
        Spacer(Modifier.height(4.dp))
        ActionButton("测试震动") { ReminderNotifier.vibrate(context) }
        ActionButton("测试打卡通知") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                ReminderNotifier.showNotification(context, "到公司了，记得打卡！", silent = false)
            }
        }
        ActionButton("获取定位权限") {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else {
                permissionStatus = locationPermissionStatus(context)
            }
        }
        ActionButton("获取当前坐标") {
            coordinateText = "正在定位…"
            getCurrentLocation(context) { location ->
                currentLocation = location
                coordinateText = if (location == null) {
                    "暂时无法获取位置，请确认定位服务和权限已开启"
                } else {
                    String.format(
                        Locale.US,
                        "纬度：%.6f\n经度：%.6f\n精度：%.1f 米",
                        location.latitude,
                        location.longitude,
                        location.accuracy
                    )
                }
            }
        }
        ActionButton("将当前位置设为公司") {
            val location = currentLocation
            if (location == null) {
                companyLocationText = "请先获取当前坐标"
            } else {
                preferences.edit()
                    .putString(GeofenceManager.COMPANY_LAT_KEY, location.latitude.toString())
                    .putString(GeofenceManager.COMPANY_LNG_KEY, location.longitude.toString())
                    .putBoolean(GeofenceManager.GEOFENCE_ENABLED_KEY, false)
                    .apply()
                companyLocationText = loadCompanyLocationText(preferences)
                geofenceStatus = "公司位置已更新，请重新启用打卡围栏"
            }
        }
        ActionButton("设置后台定位权限") {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                )
            )
        }
        ActionButton("启用 200 米打卡围栏") {
            geofenceStatus = "正在启用打卡围栏…"
            GeofenceManager.registerCompanyGeofence(context) { geofenceStatus = it }
        }
        Spacer(Modifier.height(8.dp))
        Text(permissionStatus)
        Text(coordinateText)
        Text(companyLocationText)
        Text(geofenceStatus)
    }
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(text) }
}

private fun locationPermissionStatus(context: Context): String {
    val fineGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val backgroundGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    return when {
        fineGranted && backgroundGranted -> "定位权限：精确定位、始终允许均已开启"
        fineGranted -> "定位权限：已有精确定位；请在系统设置中选择“始终允许”"
        coarseGranted -> "定位权限：仅有大致位置；围栏需要精确定位"
        else -> "定位权限：未授权"
    }
}

private fun loadCompanyLocationText(preferences: SharedPreferences): String {
    val latitude = preferences.getString(GeofenceManager.COMPANY_LAT_KEY, null)
    val longitude = preferences.getString(GeofenceManager.COMPANY_LNG_KEY, null)
    return if (latitude != null && longitude != null) {
        "已保存公司位置\n纬度：$latitude\n经度：$longitude"
    } else {
        "公司位置：尚未设置"
    }
}

@SuppressLint("MissingPermission")
private fun getCurrentLocation(context: Context, onResult: (Location?) -> Unit) {
    val fineGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!fineGranted && !coarseGranted) {
        onResult(null)
        return
    }
    val client = LocationServices.getFusedLocationProviderClient(context)
    client.getCurrentLocation(
        Priority.PRIORITY_HIGH_ACCURACY,
        CancellationTokenSource().token
    ).addOnSuccessListener { location ->
        if (location != null) {
            onResult(location)
        } else {
            client.lastLocation
                .addOnSuccessListener(onResult)
                .addOnFailureListener { onResult(null) }
        }
    }.addOnFailureListener { onResult(null) }
}
