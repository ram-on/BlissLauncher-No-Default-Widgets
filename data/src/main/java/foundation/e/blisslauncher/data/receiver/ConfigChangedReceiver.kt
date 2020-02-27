package foundation.e.blisslauncher.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Process
import timber.log.Timber
import javax.inject.Inject

class ConfigChangedReceiver @Inject constructor(private val context: Context) :
    BroadcastReceiver() {

    private val fontScale = context.resources.configuration.fontScale
    private val density = context.resources.configuration.densityDpi

    override fun onReceive(context: Context, intent: Intent) {
        val config = context.resources.configuration
        if (fontScale != config.fontScale || density != config.densityDpi) {
            Timber.d("Configuration changed, restarting launcher")
            unregister()
            Process.killProcess(Process.myPid())
        }
    }

    fun register() {
        context.registerReceiver(this, IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED))
    }

    fun unregister() {
        context.unregisterReceiver(this)
    }
}