/*
 *  Copyright (C) 2020 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.omnirom.omnistoreinstaller

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.webkit.URLUtil
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val TAG = "OmniStoreInstaller:MainActivity"
    private lateinit var mDownloadManager: DownloadManager
    private val mDownloadReceiver: DownloadReceiver = DownloadReceiver()
    private val STORE_APP_APK = "OmniStore.apk"
    private val OMNI_STORE_APP_PKG = "org.omnirom.omnistore"
    private var mDownloadId: Long = -1

    private val getPermissions =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                downloadStore()
            }
        }

    inner class DownloadReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "onReceive " + intent?.action)
            if (intent?.action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                stopProgress()
                mDownloadId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)!!
                if (mDownloadId == -1L) {
                    return
                }
                installApp(mDownloadId)
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val appIcon = AdaptiveIconDrawable(
            ColorDrawable(getColor(R.color.omni_logo_color)),
            getDrawable(R.drawable.ic_launcher_foreground)
        )
        findViewById<ImageView>(R.id.install_image).setImageDrawable(appIcon)

        mDownloadManager = this.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val downloadFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(this, mDownloadReceiver, downloadFilter, ContextCompat.RECEIVER_EXPORTED)

        findViewById<Button>(R.id.install_store).setOnClickListener {
            if (!isInstalled()) {
                if (!hasInstallPermissions()) {
                    checkUnknownResourceInstallation()
                } else {
                    downloadStore()
                }
            } else {
                disableMe()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (isInstalled()) {
            findViewById<TextView>(R.id.install_text).text = getString(R.string.store_installed)
            findViewById<Button>(R.id.install_store).text = getString(R.string.disable_app_button)
        } else {
            findViewById<TextView>(R.id.install_text).text = getString(R.string.store_welcome)
            findViewById<Button>(R.id.install_store).text = getString(R.string.install_store_button)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mDownloadReceiver)
    }

    private fun disableMe() {
        packageManager.setApplicationEnabledSetting(
            packageName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun downloadStore() {
        startProgress()
        val url: String = getAppsRootUri(this) + STORE_APP_APK
        val checkApp =
            NetworkUtils().CheckAppTask(
                url,
                object : NetworkUtils.NetworkTaskCallback {
                    override fun postAction(networkError: Boolean) {
                        if (networkError) {
                            stopProgress()
                            showNetworkError(url)
                        } else {
                            val request: DownloadManager.Request =
                                DownloadManager.Request(Uri.parse(url))
                            request.setDestinationInExternalFilesDir(
                                applicationContext,
                                Environment.DIRECTORY_DOWNLOADS,
                                STORE_APP_APK
                            )
                            mDownloadManager.enqueue(request)
                        }
                    }
                });
        checkApp.run()
    }

    private fun installApp(downloadId: Long) {
        var uri: Uri? = mDownloadManager.getUriForDownloadedFile(downloadId)
            ?: // includes also cancel
            return

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(
            uri, "application/vnd.android.package-archive"
        )
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        startActivity(intent)
    }

    private fun isInstalled(): Boolean {
        return try {
            packageManager.getApplicationInfo(OMNI_STORE_APP_PKG, PackageManager.GET_META_DATA)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun startProgress() {
        findViewById<Button>(R.id.install_store).isEnabled = false
        findViewById<FrameLayout>(R.id.progress).visibility = View.VISIBLE
    }

    fun stopProgress() {
        findViewById<Button>(R.id.install_store).isEnabled = true
        findViewById<FrameLayout>(R.id.progress).visibility = View.GONE
    }

    private fun showNetworkError(url: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.dialog_title_network_error))
        builder.setMessage(getString(R.string.dialog_message_network_error));
        builder.setPositiveButton(android.R.string.ok, null)
        builder.create().show()
    }

    private fun getAppsBaseUrl(context: Context): String {
        var s: String? = Settings.System.getString(context.contentResolver, "store_base_url")
            ?: return "https://dl.omnirom.org/"
        return s!!
    }

    private fun getAppsRootUri(context: Context): String {
        var rootUri: String? = Settings.System.getString(context.contentResolver, "store_root_uri")
            ?: "store/"
        if (URLUtil.isNetworkUrl(rootUri)) {
            return rootUri!!;
        }
        val base: Uri = Uri.parse(getAppsBaseUrl(context))
        val u: Uri = Uri.withAppendedPath(base, rootUri)
        return u.toString()
    }

    private fun hasInstallPermissions(): Boolean {
        return packageManager.canRequestPackageInstalls()
    }

    private fun checkUnknownResourceInstallation() {
        getPermissions.launch(
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:$packageName")
            )
        )
    }
}