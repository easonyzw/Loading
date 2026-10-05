package com.example.checkinreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import java.time.LocalDate
import java.time.LocalTime

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return
        val period = ReminderPolicy.periodAt(LocalTime.now())
        if (period == ReminderPeriod.NONE) return

        val preferences = context.getSharedPreferences(
            GeofenceManager.PREFERENCES_NAME, Context.MODE_PRIVATE
        )
        val key = when (period) {
            ReminderPeriod.MORNING -> "last_morning_reminder"
            ReminderPeriod.EVENING -> "last_evening_reminder"
            ReminderPeriod.NONE -> return
        }
        val today = LocalDate.now().toString()
        if (preferences.getString(key, null) == today) return
        if (!preferences.edit().putString(key, today).commit()) return

        val message = when (period) {
            ReminderPeriod.MORNING -> "到公司了，别忘了上班打卡！"
            ReminderPeriod.EVENING -> "到大门附近了，别忘了下班打卡！"
            ReminderPeriod.NONE -> return
        }
        ReminderNotifier.vibrate(context)
        ReminderNotifier.showNotification(context, message, silent = true)
    }
}
