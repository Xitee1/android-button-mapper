package de.xitee.buttonmapper.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.xitee.buttonmapper.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Restarts the service after boot and after an app update, if the user had it enabled. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        val app = context.applicationContext as App
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (app.repository.current().serviceEnabled) MapperService.start(context)
            } finally {
                pending.finish()
            }
        }
    }
}
