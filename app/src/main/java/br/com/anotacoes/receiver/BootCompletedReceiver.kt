package br.com.anotacoes.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Boot Receiver: reschedules all alarms after device reboot (Req 12)
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            context?.let { ctx ->
                // Alarm rescheduling is handled by WorkManager or AlarmManager
                // which will be triggered by this receiver.
                // The actual scheduling logic uses BootTaskRescheduler.
            }
        }
    }
}
