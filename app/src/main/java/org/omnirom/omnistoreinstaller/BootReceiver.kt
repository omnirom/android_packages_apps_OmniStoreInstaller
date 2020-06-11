package org.omnirom.omnistoreinstaller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    private val TAG = "OmniStoreInstaller:BootReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            if (context != null) {
                if (!isInstalled(context)) {
                    Log.d(TAG, "onReceive " + intent?.action)
                    val intent = Intent(context, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                }
            }
        }
    }

    private fun isInstalled(context: Context): Boolean {
        try {
            context.packageManager.getApplicationInfo("org.omnirom.omnistore", PackageManager.GET_META_DATA)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
    }
}