package br.com.anotacoes.domain.port

import br.com.anotacoes.domain.usecase.AdvanceReminderAlarm
import br.com.anotacoes.domain.usecase.UseScheduledAlarm

interface TaskAlarmPort {
    fun scheduleAlarm(alarm: UseScheduledAlarm)
    fun cancelAlarm(requestCode: Int)
    fun cancelAllAlarms()
    fun scheduleAdvanceReminder(alarm: AdvanceReminderAlarm)
    fun cancelAdvanceReminder(requestCode: Int)
}
