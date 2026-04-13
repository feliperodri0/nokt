package br.com.anotacoes.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import br.com.anotacoes.domain.port.TaskAlarmPort
import br.com.anotacoes.domain.usecase.AdvanceReminderAlarm
import br.com.anotacoes.domain.usecase.UseScheduledAlarm
import br.com.anotacoes.receiver.TaskAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class TaskAlarmManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intentBuilder: AlarmIntentBuilder
) : TaskAlarmPort {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun scheduleAlarm(alarm: UseScheduledAlarm) {
        val startData = intentBuilder.buildStartIntentData(alarm)
        val endData = intentBuilder.buildEndIntentData(alarm)

        scheduleExactAlarm(startData)
        scheduleExactAlarm(endData)
    }

    override fun cancelAlarm(requestCode: Int) {
        cancelPendingIntent(requestCode)
        cancelPendingIntent(requestCode + AlarmIntentBuilder.END_REQUEST_CODE_OFFSET)
    }

    override fun cancelAllAlarms() {
        // Alarms are canceled individually by request code.
    }

    override fun scheduleAdvanceReminder(alarm: AdvanceReminderAlarm) {
        val intentData = intentBuilder.buildAdvanceReminderIntentData(alarm)
        scheduleExactAlarm(intentData)
    }

    override fun cancelAdvanceReminder(requestCode: Int) {
        cancelPendingIntent(requestCode)
    }

    private fun scheduleExactAlarm(intentData: AlarmIntentData) {
        val pendingIntent = buildPendingIntent(intentData)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            intentData.triggerTimeMillis,
            pendingIntent
        )
    }

    private fun buildPendingIntent(intentData: AlarmIntentData): PendingIntent {
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = intentData.action
            intentData.extras.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        return PendingIntent.getBroadcast(
            context,
            intentData.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelPendingIntent(requestCode: Int) {
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
