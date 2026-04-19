package br.com.anotacoes.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import br.com.anotacoes.R
import br.com.anotacoes.domain.repository.SettingsRepository
import br.com.anotacoes.domain.usecase.GetRemindersForDateUseCase
import br.com.anotacoes.domain.repository.ReminderRepository
import br.com.anotacoes.domain.model.ReminderStatus
import br.com.anotacoes.presentation.MainActivity
import br.com.anotacoes.receiver.TaskAlarmHandler
import br.com.anotacoes.receiver.TaskCompleteHandler
import br.com.anotacoes.receiver.TaskCompleteReceiver
import br.com.anotacoes.receiver.TaskDismissHandler
import br.com.anotacoes.receiver.TaskDismissReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class TaskLockScreenService : Service() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var reminderRepository: ReminderRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val ACTION_SHOW = "br.com.anotacoes.ACTION_SHOW_NOTIFICATION"
        const val ACTION_REMOVE = "br.com.anotacoes.ACTION_REMOVE_NOTIFICATION"
        const val ACTION_ADVANCE_REMINDER = "br.com.anotacoes.ACTION_ADVANCE_REMINDER_NOTIFICATION"

        private const val CHANNEL_ID = "task_lock_screen_channel"
        private const val CHANNEL_NAME = "Tarefas na Tela de Bloqueio"
        private const val ADVANCE_CHANNEL_ID = "task_advance_reminder_channel"
        private const val ADVANCE_CHANNEL_NAME = "Lembretes Antecipados"
        private const val FOREGROUND_NOTIFICATION_ID = 1

        const val EXTRA_TASK_ID = "extra_open_task_id"
    }

    private val notificationState = LockScreenNotificationState()
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> handleShowNotification(intent)
            ACTION_REMOVE -> handleRemoveNotification(intent)
            ACTION_ADVANCE_REMINDER -> handleAdvanceReminderNotification(intent)
        }

        if (!notificationState.hasActiveNotifications()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleShowNotification(intent: Intent) {
        val taskId = intent.getStringExtra(TaskAlarmHandler.EXTRA_TASK_ID) ?: return
        val title = intent.getStringExtra(TaskAlarmHandler.EXTRA_TASK_TITLE) ?: return
        val description = intent.getStringExtra(TaskAlarmHandler.EXTRA_TASK_DESCRIPTION)
        val timeText = intent.getStringExtra(TaskAlarmHandler.EXTRA_TASK_TIME)
        val recurrenceText = intent.getStringExtra(TaskAlarmHandler.EXTRA_TASK_RECURRENCE)
        val showInNotification = intent.getBooleanExtra(TaskAlarmHandler.EXTRA_SHOW_IN_NOTIFICATION, true)

        val globalOverride = runBlocking { settingsRepository.getShowAllInNotifications().first() }
        if (!showInNotification && !globalOverride) return

        notificationState.addNotification(taskId, title, description)

        startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification())

        val taskNotification = buildTaskNotification(taskId, title, description, timeText, recurrenceText)
        val notificationId = notificationState.getNotificationId(taskId)
        notificationManager.notify(notificationId, taskNotification)
    }

    private fun handleRemoveNotification(intent: Intent) {
        val taskId = intent.getStringExtra(TaskAlarmHandler.EXTRA_TASK_ID) ?: return

        val notificationId = notificationState.getNotificationId(taskId)
        notificationState.removeNotification(taskId)
        notificationManager.cancel(notificationId)

        if (notificationState.hasActiveNotifications()) {
            notificationManager.notify(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification())
        }
    }

    private fun handleAdvanceReminderNotification(intent: Intent) {
        val taskId = intent.getStringExtra(TaskAlarmHandler.EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(TaskAlarmHandler.EXTRA_TASK_TITLE) ?: return
        val daysRemaining = intent.getStringExtra(TaskAlarmHandler.EXTRA_DAYS_REMAINING)?.toIntOrNull() ?: 1

        val reminderId = "${taskId}_adv_${daysRemaining}"
        serviceScope.launch {
            val currentStatuses = reminderRepository.getAllStatuses().first()
            if (currentStatuses[reminderId] != ReminderStatus.DELETED) {
                reminderRepository.setStatus(reminderId, taskId, ReminderStatus.ACTIVE)
            }
        }

        val contentText = when (daysRemaining) {
            1 -> "$taskTitle amanhã"
            else -> "$taskTitle em $daysRemaining dias"
        }

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            taskId.hashCode() + 3,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ADVANCE_CHANNEL_ID)
            .setContentTitle("Lembrete antecipado")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openPendingIntent)
            .build()

        val notifId = "${taskId}_adv_${daysRemaining}".hashCode()
        notificationManager.notify(notifId, notification)
    }

    private fun createNotificationChannels() {
        val lockScreenChannel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
            description = "Notificações de tarefas ativas na tela de bloqueio"
        }
        notificationManager.createNotificationChannel(lockScreenChannel)

        val advanceChannel = NotificationChannel(
            ADVANCE_CHANNEL_ID,
            ADVANCE_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Lembretes antecipados de tarefas futuras"
        }
        notificationManager.createNotificationChannel(advanceChannel)
    }

    private fun buildForegroundNotification(): Notification {
        val summaryText = notificationState.buildSummaryText()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Anotações")
            .setContentText(summaryText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun buildTaskNotification(
        taskId: String,
        title: String,
        description: String?,
        timeText: String?,
        recurrenceText: String?
    ): Notification {
        val dismissIntent = Intent(this, TaskDismissReceiver::class.java).apply {
            action = TaskDismissHandler.ACTION_DISMISS_TASK
            putExtra(TaskDismissHandler.EXTRA_TASK_ID, taskId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            taskId.hashCode(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(this, TaskCompleteReceiver::class.java).apply {
            action = TaskCompleteHandler.ACTION_COMPLETE_TASK
            putExtra(TaskCompleteHandler.EXTRA_TASK_ID, taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            this,
            taskId.hashCode() + 1,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            taskId.hashCode() + 2,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyLines = buildList {
            if (!timeText.isNullOrBlank()) add(timeText)
            if (!recurrenceText.isNullOrBlank()) add(recurrenceText)
            if (!description.isNullOrBlank()) add(description)
        }
        val bodyText = bodyLines.joinToString(" · ")

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setContentIntent(openPendingIntent)
            .addAction(0, "Dispensar", dismissPendingIntent)
            .addAction(0, "Concluir", completePendingIntent)
            .build()
    }
}
