package com.luciano.ilmogiardino

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object Notifier {
    const val CHANNEL_ID = "watering_reminders"
    private var channelReady = false

    fun ensureChannel(ctx: Context) {
        if (channelReady) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Promemoria annaffiatura", NotificationManager.IMPORTANCE_DEFAULT)
        )
        channelReady = true
    }

    fun notifyWatering(ctx: Context, plantName: String, plantId: Long) {
        ensureChannel(ctx)
        val pi = PendingIntent.getBroadcast(
            ctx, plantId.hashCode(),
            Intent(ctx, WateringReceiver::class.java).putExtra("action", "done").putExtra("id", plantId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("È ora di annaffiare!")
            .setContentText("Annaffia \"$plantName\"")
            .addAction(0, "Fatto", pi)
            .setAutoCancel(true)
            .build()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(plantId.toInt(), n)
    }

    fun scheduleNext(ctx: Context, plant: Plant, delayMs: Long) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(ctx, plant)
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayMs, pi)
    }

    fun cancel(ctx: Context, plant: Plant) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(ctx, plant))
    }

    private fun pending(ctx: Context, plant: Plant): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, plant.id.toInt(),
            Intent(ctx, WateringReceiver::class.java).putExtra("id", plant.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun hasPermission(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

class WateringReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("id", -1L)
        val plants = Store.loadPlants(context)
        val plant = plants.firstOrNull { it.id == id } ?: return
        Notifier.notifyWatering(context, plant.name, plant.id)
        val intervalMs = plant.wateringIntervalDays * 24L * 3600L * 1000L
        Notifier.scheduleNext(context, plant, intervalMs)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val plants = Store.loadPlants(context)
        for (p in plants) {
            val intervalMs = p.wateringIntervalDays * 24L * 3600L * 1000L
            Notifier.scheduleNext(context, p, intervalMs)
        }
    }
}
