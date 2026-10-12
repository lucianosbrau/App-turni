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

    fun notifyTreatment(ctx: Context, plant: Plant, treatment: Treatment) {
        ensureChannel(ctx)
        val pi = PendingIntent.getBroadcast(
            ctx, (treatment.id + 100000L).toInt(),
            Intent(ctx, WateringReceiver::class.java)
                .putExtra("action", "tDone").putExtra("tid", treatment.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("Necessario ${treatment.type} a ${plant.name}")
            .setContentText("Trattamento in scadenza per una pianta del tuo giardino")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Necessario ${treatment.type} a ${plant.name} (trattamento programmato ogni ${treatment.frequencyDays} giorni)."))
            .addAction(0, "Fatto", pi)
            .setAutoCancel(true)
            .build()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify((treatment.id + 100000L).toInt(), n)
    }

    fun scheduleTreatment(ctx: Context, plant: Plant, treatment: Treatment) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            ctx, (treatment.id + 100000L).toInt(),
            Intent(ctx, WateringReceiver::class.java).putExtra("tid", treatment.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val at = treatment.lastDoneMillis + treatment.frequencyDays * 24L * 3600L * 1000L
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    fun notifyPruning(ctx: Context, plant: Plant, speciesName: String, monthName: String) {
        ensureChannel(ctx)
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("Periodo di potatura: $speciesName")
            .setContentText("È il mese giusto per potare ${plant.name} ($monthName)")
            .setStyle(NotificationCompat.BigTextStyle().bigText("È iniziato $monthName: periodo di potatura indicato per $speciesName. Tocca per aprire la scheda di ${plant.name} nella sezione Trattamenti."))
            .setAutoCancel(true)
            .build()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify((plant.id + 200000L).toInt(), n)
    }

    fun schedulePruningChecks(ctx: Context) {
        val plants = Store.loadPlants(ctx)
        val cal = java.util.Calendar.getInstance()
        for (p in plants) {
            val info = SpeciesDb.find(ctx, p.species) ?: continue
            for (m in info.potaturaMesi) {
                val c = java.util.Calendar.getInstance()
                c.set(java.util.Calendar.MONTH, (m - 1) % 12)
                c.set(java.util.Calendar.DAY_OF_MONTH, 1)
                c.set(java.util.Calendar.HOUR_OF_DAY, 9)
                c.set(java.util.Calendar.MINUTE, 0)
                if (c.timeInMillis <= System.currentTimeMillis())
                    c.add(java.util.Calendar.YEAR, 1)
                val pi = PendingIntent.getBroadcast(
                    ctx, (p.id * 100 + (m - 1) % 12 + 200000L).toInt(),
                    Intent(ctx, WateringReceiver::class.java)
                        .putExtra("prunePlant", p.id).putExtra("pruneMonth", m),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.timeInMillis, pi)
            }
        }
    }

    fun cancelTreatment(ctx: Context, treatment: Treatment) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(PendingIntent.getBroadcast(
            ctx, (treatment.id + 100000L).toInt(),
            Intent(ctx, WateringReceiver::class.java).putExtra("tid", treatment.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ))
    }

    fun hasPermission(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

class WateringReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prunePlant = intent.getLongExtra("prunePlant", -1L)
        if (prunePlant != -1L) {
            val month = intent.getIntExtra("pruneMonth", 0)
            val plant = Store.loadPlants(context).firstOrNull { it.id == prunePlant } ?: return
            val info = SpeciesDb.find(context, plant.species) ?: return
            Notifier.notifyPruning(context, plant, info.name, SpeciesDb.monthNames[(month - 1) % 12].replaceFirstChar { it.uppercase() })
            Notifier.schedulePruningChecks(context)
            return
        }
        val tid = intent.getLongExtra("tid", -1L)
        if (tid != -1L) {
            val t = TreatmentStore.load(context).firstOrNull { it.id == tid } ?: return
            val plant = Store.loadPlants(context).firstOrNull { it.id == t.plantId } ?: return
            Notifier.notifyTreatment(context, plant, t)
            Notifier.scheduleTreatment(context, plant, t)
            return
        }
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
        val plantById = plants.associateBy { it.id }
        for (t in TreatmentStore.load(context)) {
            plantById[t.plantId]?.let { Notifier.scheduleTreatment(context, it, t) }
        }
        Notifier.schedulePruningChecks(context)
    }
}
