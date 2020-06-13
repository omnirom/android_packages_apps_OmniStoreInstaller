package org.omnirom.omnistoreinstaller

import android.os.AsyncTask
import android.util.Log
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class NetworkUtils {
    private val TAG = "OmniStoreInstaller:NetworkUtils"
    private val HTTP_READ_TIMEOUT = 30000
    private val HTTP_CONNECTION_TIMEOUT = 30000
    var mNetworkError = false

    interface NetworkTaskCallback {
        fun postAction(networkError: Boolean)
    }

    inner class CheckAppTask(
        url: String,
        postAction: NetworkTaskCallback
    ) : AsyncTask<String, Int, Int>() {
        val mPostAction: NetworkTaskCallback = postAction
        val mUrl: String = url

        override fun onPreExecute() {
            super.onPreExecute()
            mNetworkError = false
        }

        override fun doInBackground(vararg params: String?): Int {
            var urlConnection: HttpsURLConnection? = null
            try {
                urlConnection = setupHttpsRequest(mUrl)
                if (urlConnection == null) {
                    mNetworkError = true
                }
            } catch (e: Exception) {
                mNetworkError = true
            } finally {
                urlConnection?.disconnect()
            }
            return 0
        }

        override fun onPostExecute(result: Int?) {
            super.onPostExecute(result)
            mPostAction.postAction(mNetworkError)
        }
    }

    fun setupHttpsRequest(urlStr: String): HttpsURLConnection? {
        val url = URL(urlStr)
        val urlConnection = url.openConnection() as HttpsURLConnection
        urlConnection.setConnectTimeout(HTTP_CONNECTION_TIMEOUT)
        urlConnection.setReadTimeout(HTTP_READ_TIMEOUT)
        urlConnection.setRequestMethod("GET")
        urlConnection.setDoInput(true)
        urlConnection.connect()
        val code: Int = urlConnection.getResponseCode()
        if (code != HttpsURLConnection.HTTP_OK) {
            Log.d(TAG, "response: " + code)
            return null
        }
        return urlConnection
    }
}