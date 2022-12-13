/*
 * Copyright 2020-2022 Soren Stoutner <soren@stoutner.com>.
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

import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.MimeTypeMap

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.activities.MainWebViewActivity
import com.stoutner.privacybrowser.dataclasses.PendingDialogDataClass
import com.stoutner.privacybrowser.dialogs.SaveDialog
import com.stoutner.privacybrowser.helpers.ProxyHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat

object PrepareSaveDialogCoroutine {
    @JvmStatic
    fun prepareSaveDialog(context: Context, supportFragmentManager: FragmentManager, urlString: String, userAgent: String, cookiesEnabled: Boolean) {
        // Use a coroutine to prepare the save dialog.
        CoroutineScope(Dispatchers.Main).launch {
            // Make the network requests on the IO thread.
            withContext(Dispatchers.IO) {
                // Define the strings.
                var formattedFileSize: String
                var fileNameString: String

                // Populate the file size and name strings.
                if (urlString.startsWith("data:")) {  // The URL contains the entire data of an image.
                    // Remove `data:` from the beginning of the URL.
                    val urlWithoutData = urlString.substring(5)

                    // Get the URL MIME type, which ends with a `;`.
                    val urlMimeType = urlWithoutData.substring(0, urlWithoutData.indexOf(";"))

                    // Get the Base64 data, which begins after a `,`.
                    val base64DataString = urlWithoutData.substring(urlWithoutData.indexOf(",") + 1)

                    // Calculate the file size of the data URL.  Each Base64 character represents 6 bits.
                    formattedFileSize = NumberFormat.getInstance().format(base64DataString.length * 3L / 4) + " " + context.getString(R.string.bytes)

                    // Set the file name according to the MIME type.
                    fileNameString = context.getString(R.string.file) + "." + MimeTypeMap.getSingleton().getExtensionFromMimeType(urlMimeType)
                } else {  // The URL refers to the location of the data.
                    // Initialize the formatted file size string.
                    formattedFileSize = context.getString(R.string.unknown_size)

                    // Because everything relating to requesting data from a webserver can throw errors, the entire section must catch exceptions.
                    try {
                        // Convert the URL string to a URL.
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

                            // Add the cookies if they are not null.
                            if (cookiesString != null)
                                httpUrlConnection.setRequestProperty("Cookie", cookiesString)
                        }

                        // The actual network request is in a `try` bracket so that `disconnect()` is run in the `finally` section even if an error is encountered in the main block.
                        try {
                            // Get the status code.  This initiates a network connection.
                            val responseCode = httpUrlConnection.responseCode

                            // Check the response code.
                            if (responseCode >= 400) {  // The response code is an error message.
                                // Set the formatted file size to indicate a bad URL.
                                formattedFileSize = context.getString(R.string.invalid_url)

                                // Set the file name according to the URL.
                                fileNameString = getFileNameFromUrl(context, urlString, null)
                            } else {  // The response code is not an error message.
                                // Get the headers.
                                val contentLengthString = httpUrlConnection.getHeaderField("Content-Length")
                                val contentDispositionString = httpUrlConnection.getHeaderField("Content-Disposition")
                                var contentTypeString = httpUrlConnection.contentType

                                // Remove anything after the MIME type in the content type string.
                                if (contentTypeString.contains(";"))
                                    contentTypeString = contentTypeString.substring(0, contentTypeString.indexOf(";"))

                                // Only process the content length string if it isn't null.
                                if (contentLengthString != null) {
                                    // Convert the content length string to a long.
                                    val fileSize = contentLengthString.toLong()

                                    // Format the file size.
                                    formattedFileSize = NumberFormat.getInstance().format(fileSize) + " " + context.getString(R.string.bytes)
                                }

                                // Get the file name string from the content disposition.
                                fileNameString = getFileNameFromHeaders(context, contentDispositionString, contentTypeString, urlString)
                            }
                        } finally {
                            // Disconnect the HTTP URL connection.
                            httpUrlConnection.disconnect()
                        }
                    } catch (exception: Exception) {
                        // Set the formatted file size to indicate a bad URL.
                        formattedFileSize = context.getString(R.string.invalid_url)

                        // Set the file name according to the URL.
                        fileNameString = getFileNameFromUrl(context, urlString, null)
                    }
                }

                // Display the dialog on the main thread.
                withContext(Dispatchers.Main) {
                    // Instantiate the save dialog.
                    val saveDialogFragment: DialogFragment = SaveDialog.saveUrl(urlString, formattedFileSize, fileNameString, userAgent, cookiesEnabled)

                    // Try to show the dialog.  Sometimes the window is not active.
                    try {
                        // Show the save dialog.
                        saveDialogFragment.show(supportFragmentManager, context.getString(R.string.save_dialog))
                    } catch (exception: Exception) {
                        // Add the dialog to the pending dialog array list.  It will be displayed in `onStart()`.
                        MainWebViewActivity.pendingDialogsArrayList.add(PendingDialogDataClass(saveDialogFragment, context.getString(R.string.save_dialog)))
                    }
                }
            }
        }
    }

    // Content dispositions can contain other text besides the file name, and they can be in any order.
    // Elements are separated by semicolons.  Sometimes the file names are contained in quotes.
    @JvmStatic
    fun getFileNameFromHeaders(context: Context, contentDispositionString: String?, contentTypeString: String?, urlString: String): String {
        // Define a file name string.
        var fileNameString: String

        // Only process the content disposition string if it isn't null.
        if (contentDispositionString != null) {  // The content disposition is not null.
            // Check to see if the content disposition contains a file name.
            if (contentDispositionString.contains("filename=")) {  // The content disposition contains a filename.
                // Get the part of the content disposition after `filename=`.
                fileNameString = contentDispositionString.substring(contentDispositionString.indexOf("filename=") + 9)

                // Remove any `;` and anything after it.  This removes any entries after the filename.
                if (fileNameString.contains(";"))
                    fileNameString = fileNameString.substring(0, fileNameString.indexOf(";") - 1)

                // Remove any `"` at the beginning of the string.
                if (fileNameString.startsWith("\""))
                    fileNameString = fileNameString.substring(1)

                // Remove any `"` at the end of the string.
                if (fileNameString.endsWith("\""))
                    fileNameString = fileNameString.substring(0, fileNameString.length - 1)
            } else {  // The headers contain no useful information.
                // Get the file name string from the URL.
                fileNameString = getFileNameFromUrl(context, urlString, contentTypeString)
            }
        } else {  // The content disposition is null.
            // Get the file name string from the URL.
            fileNameString = getFileNameFromUrl(context, urlString, contentTypeString)
        }

        // Return the file name string.
        return fileNameString
    }

    private fun getFileNameFromUrl(context: Context, urlString: String, contentTypeString: String?): String {
        // Convert the URL string to a URI.
        val uri = Uri.parse(urlString)

        // Get the last path segment.
        var lastPathSegment = uri.lastPathSegment

        // Use a default file name if the last path segment is null.
        if (lastPathSegment == null) {
            // Set the last path segment to be the generic file name.
            lastPathSegment = context.getString(R.string.file)

            // Add a file extension if it can be detected.
            if (MimeTypeMap.getSingleton().hasMimeType(contentTypeString))
                lastPathSegment = lastPathSegment + "." + MimeTypeMap.getSingleton().getExtensionFromMimeType(contentTypeString)
        }

        // Return the last path segment as the file name.
        return lastPathSegment
    }
}
