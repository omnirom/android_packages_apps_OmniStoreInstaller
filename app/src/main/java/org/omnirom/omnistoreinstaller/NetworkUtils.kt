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

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    ) {
        val mPostAction: NetworkTaskCallback = postAction
        val mUrl: String = url

        fun run() {
            CoroutineScope(Dispatchers.IO).launch {
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
                withContext(Dispatchers.Main) {
                    mPostAction.postAction(mNetworkError)
                }
            }
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