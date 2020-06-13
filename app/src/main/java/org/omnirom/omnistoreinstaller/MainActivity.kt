package org.omnirom.omnistoreinstaller

import android.Manifest
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val TAG = "OmniStoreInstaller:MainActivity"
    private lateinit var mDownloadManager: DownloadManager
    private val mDownloadReceiver: DownloadReceiver = DownloadReceiver()
    private val mPackageReceiver: PackageReceiver = PackageReceiver()
    private val APPS_BASE_URI = "https://dl.omnirom.org/store/"
    private val STORE_APP_APK = "OmniStore.apk"
    private val STORE_URI = APPS_BASE_URI + STORE_APP_APK
    private val OMNI_STORE_APP_PKG = "org.omnirom.omnistore"

    inner class DownloadReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "onReceive " + intent?.action)
            if (intent?.action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                stopProgress()
                val downloadId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (downloadId == -1L) {
                    return
                }
                installApp(downloadId!!)
            }
        }
    }

    inner class PackageReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "onReceive " + intent?.action)
            if (intent?.action == Intent.ACTION_PACKAGE_ADDED) {
                val pkg = intent.dataString
                if (pkg == "package:" + OMNI_STORE_APP_PKG) {
                    disableMe()
                    finish()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mDownloadManager = this.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val downloadFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        registerReceiver(mDownloadReceiver, downloadFilter)

        val packageFilter = IntentFilter(Intent.ACTION_PACKAGE_ADDED)
        packageFilter.addDataScheme("package")
        registerReceiver(mPackageReceiver, packageFilter)

        findViewById<Button>(R.id.install_store).setOnClickListener {
            downloadStore()

        }
        findViewById<TextView>(R.id.install_text).setOnClickListener {
            val name = ComponentName(OMNI_STORE_APP_PKG, OMNI_STORE_APP_PKG + ".MainActivity")
            val intent = Intent()
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.component = name
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        if (isInstalled()) {
            findViewById<Button>(R.id.install_store).visibility = View.GONE;
            findViewById<TextView>(R.id.install_text).text = getString(R.string.store_installed)
        } else {
            findViewById<Button>(R.id.install_store).visibility = View.VISIBLE;
            findViewById<TextView>(R.id.install_text).text = getString(R.string.store_welcome)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mDownloadReceiver)
        unregisterReceiver(mPackageReceiver)
    }

    private fun disableMe() {
        packageManager.setApplicationEnabledSetting(
            packageName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun downloadStore() {
        val url: String = STORE_URI
        val checkApp =
            NetworkUtils().CheckAppTask(
                url,
                object : NetworkUtils.NetworkTaskCallback {
                    override fun postAction(networkError: Boolean) {
                        if (networkError) {
                            showNetworkError(url)
                        } else {
                            val request: DownloadManager.Request =
                                DownloadManager.Request(Uri.parse(url))
                            request.setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                STORE_APP_APK
                            )
                            //request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                            //request.setNotificationVisibility()
                            mDownloadManager.enqueue(request)
                            startProgress()
                        }
                    }
                });
        checkApp.execute()
    }

    private fun installApp(downloadId: Long) {
        var uri: Uri? = mDownloadManager.getUriForDownloadedFile(downloadId)
        if (uri == null) {
            // includes also cancel
            return
        }

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(
            uri, "application/vnd.android.package-archive"
        )
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        startActivity(intent)
    }

    private fun isInstalled(): Boolean {
        try {
            packageManager.getApplicationInfo(OMNI_STORE_APP_PKG, PackageManager.GET_META_DATA)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            return false
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
}