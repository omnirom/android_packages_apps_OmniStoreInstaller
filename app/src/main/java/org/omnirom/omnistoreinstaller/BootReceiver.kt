package org.omnirom.omnistoreinstaller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    private val TAG = "OmniStoreInstaller:BootReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            if (context != null) {
                Log.d(TAG, "onReceive " + intent?.action)
            }
        }
    }
}