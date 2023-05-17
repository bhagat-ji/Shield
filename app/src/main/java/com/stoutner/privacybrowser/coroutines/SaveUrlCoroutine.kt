/*
 * Copyright 2020-2023 Soren Stoutner <soren@stoutner.com>.
 *
 * This file is part of Privacy Browser Android <https://www.stoutner.com/privacy-browser-android>.
 *
 * Privacy Browser Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Privacy Browser Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Privacy Browser Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.stoutner.privacybrowser.coroutines

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Base64
import android.webkit.CookieManager

import androidx.viewpager2.widget.ViewPager2

import com.google.android.material.snackbar.Snackbar

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.helpers.ProxyHelper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat

class SaveUrlCoroutine {
    fun save(context: Context, activity: Activity, urlString: String, fileUri: Uri, userAgent: String, cookiesEnabled: Boolean) {
        // Use a coroutine to save the URL.
        CoroutineScope(Dispatchers.Main).launch {
            // Create a file name string.
            val fileNameString: String

            // Query the exact file name if the API >= 26.
            if (Build.VERSION.SDK_INT >= 26) {
                // Get a cursor from the content resolver.
                val contentResolverCursor = activity.contentResolver.query(fileUri, null, null, null)!!

                // Move to the first row.
                contentResolverCursor.moveToFirst()

                // Get the file name from the cursor.
                fileNameString = contentResolverCursor.getString(contentResolverCursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))

                // Close the cursor.
                contentResolverCursor.close()
            } else {
                // Use the file URI last path segment as the file name string.
                fileNameString = fileUri.lastPathSegment!!
            }

            // Get a handle for the no swipe view pager.
            val webViewViewPager2 = activity.findViewById<ViewPager2>(R.id.webview_viewpager2)

            // Create a saving file snackbar.
            val savingFileSnackbar = Snackbar.make(webViewViewPager2, activity.getString(R.string.saving_file, 0, fileNameString), Snackbar.LENGTH_INDEFINITE)

            // Display the saving file snackbar.
            savingFileSnackbar.show()

            // Download the URL on the IO thread.
            withContext(Dispatchers.IO) {
                try {
                    // Open an output stream.
                    val outputStream = activity.contentResolver.openOutputStream(fileUri)!!

                    // Save the URL.
                    if (urlString.startsWith("data:")) {  // The URL contains the entire data of an image.
                        // Get the Base64 data, which begins after a `,`.
                        val base64DataString = urlString.substring(urlString.indexOf(",") + 1)

                        // Decode the Base64 string to a byte array.
                        val base64DecodedDataByteArray = Base64.decode(base64DataString, Base64.DEFAULT)

                        // Write the Base64 byte array to the output stream.
                        outputStream.write(base64DecodedDataByteArray)
                    } else {  // The URL points to the data location on the internet.
                        // Get the URL from the calling activity.
                        val url = URL(urlString)

                        // Instantiate the proxy helper.
                        val proxyHelper = ProxyHelper()

                        // Get the current proxy.
                        val proxy = proxyHelper.getCurrentProxy(context)

                        // Open a connection to the URL.  No data is actually sent at this point.
                        val httpUrlConnection = url.openConnection(proxy) as HttpURLConnection

                        // Add the user agent to the header property.
                        httpUrlConnection.setRequestProperty("User-Agent", userAgent)

                        // Add the cookies if they are enabled.
                        if (cookiesEnabled) {
                            // Get the cookies for the current domain.
                            val cookiesString = CookieManager.getInstance().getCookie(url.toString())

                            // Only add the cookies if they are not null.
                            if (cookiesString != null) {
                                // Add the cookies to the header property.
                                httpUrlConnection.setRequestProperty("Cookie", cookiesString)
                            }
                        }

                        // Create the file size value.
                        val fileSize: Long

                        // Create the formatted file size variable.
                        var formattedFileSize = ""

                        // The actual network request is in a `try` bracket so that `disconnect()` is run in the `finally` section even if an error is encountered in the main block.
                        try {
                            // Get the content length header, which causes the connection to the server to be made.
                            val contentLengthString = httpUrlConnection.getHeaderField("Content-Length")

                            // Check to see if the content length is populated.
                            if (contentLengthString != null) {  // The content length is populated.
                                // Convert the content length to an long.
                                fileSize = contentLengthString.toLong()

                                // Format the file size for display.
                                formattedFileSize = NumberFormat.getInstance().format(fileSize)
                            } else {  // The content length is null.
                                // Set the file size to be `-1`.
                                fileSize = -1
                            }

                            // Get the response body stream.
                            val inputStream: InputStream = BufferedInputStream(httpUrlConnection.inputStream)

                            // Initialize the conversion buffer byte array.
                            // This is set to a megabyte so that frequent updating of the snackbar doesn't freeze the interface on download.  <https://redmine.stoutner.com/issues/709>
                            val conversionBufferByteArray = ByteArray(1048576)

                            // Initialize the downloaded kilobytes counter.
                            var downloadedKilobytesCounter: Long = 0

                            // Define the buffer length variable.
                            var bufferLength: Int

                            // Attempt to read data from the input stream and store it in the output stream.  Also store the amount of data read in the buffer length variable.
                            while (inputStream.read(conversionBufferByteArray).also { bufferLength = it } > 0) {  // Proceed while the amount of data stored in the buffer in > 0.
                                // Write the contents of the conversion buffer to the file output stream.
                                outputStream.write(conversionBufferByteArray, 0, bufferLength)

                                // Update the downloaded kilobytes counter.
                                downloadedKilobytesCounter += bufferLength

                                // Format the number of bytes downloaded.
                                val formattedNumberOfBytesDownloadedString = NumberFormat.getInstance().format(downloadedKilobytesCounter)

                                // Update the UI.
                                withContext(Dispatchers.Main) {
                                    // Check to see if the file size is known.
                                    if (fileSize == -1L) {  // The size of the download file is not known.
                                        // Update the snackbar.
                                        savingFileSnackbar.setText(activity.getString(R.string.saving_file_progress, formattedNumberOfBytesDownloadedString, fileNameString))
                                    } else {  // The size of the download file is known.
                                        // Calculate the download percentage.
                                        val downloadPercentage = downloadedKilobytesCounter * 100 / fileSize

                                        // Update the snackbar.
                                        savingFileSnackbar.setText(activity.getString(R.string.saving_file_percentage_progress, downloadPercentage, formattedNumberOfBytesDownloadedString, formattedFileSize,
                                            fileNameString)
                                        )
                                    }
                                }
                            }

                            // Close the input stream.
                            inputStream.close()
                        } finally {
                            // Disconnect the HTTP URL connection.
                            httpUrlConnection.disconnect()
                        }
                    }

                    // Close the output stream.
                    outputStream.close()

                    // Update the UI.
                    withContext(Dispatchers.Main) {
                        // Dismiss the saving file snackbar.
                        savingFileSnackbar.dismiss()

                        // Display the file saved snackbar.
                        Snackbar.make(webViewViewPager2, activity.getString(R.string.saved, fileNameString), Snackbar.LENGTH_LONG).show()
                    }
                } catch (exception: Exception) {
                    // Update the UI.
                    withContext(Dispatchers.Main) {
                        // Dismiss the saving file snackbar.
                        savingFileSnackbar.dismiss()

                        // Display the file saving error.
                        Snackbar.make(webViewViewPager2, activity.getString(R.string.error_saving_file, fileNameString, exception), Snackbar.LENGTH_INDEFINITE).show()
                    }
                }
            }
        }
    }
}