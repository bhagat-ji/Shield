/*
 * Copyright © 2020-2021 Soren Stoutner <soren@stoutner.com>.
 *
 * This file is part of Privacy Browser <https://www.stoutner.com/privacy-browser>.
 *
 * Privacy Browser is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Privacy Browser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Privacy Browser.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.stoutner.privacybrowser.asynctasks;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;
import android.webkit.CookieManager;

import com.google.android.material.snackbar.Snackbar;
import com.stoutner.privacybrowser.R;
import com.stoutner.privacybrowser.helpers.ProxyHelper;
import com.stoutner.privacybrowser.views.NoSwipeViewPager;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.text.NumberFormat;

public class SaveUrl extends AsyncTask<String, Long, String> {
    // Define a weak references.
    private final WeakReference<Context> contextWeakReference;
    private final WeakReference<Activity> activityWeakReference;

    // Define a success string constant.
    private final String SUCCESS = "Success";

    // Define the class variables.
    private final String filePathString;
    private final String userAgent;
    private final boolean cookiesEnabled;
    private Snackbar savingFileSnackbar;
    private long fileSize;
    private String formattedFileSize;
    private String urlString = "";

    // The public constructor.
    public SaveUrl(Context context, Activity activity, String filePathString, String userAgent, boolean cookiesEnabled) {
        // Populate weak references to the calling context and activity.
        contextWeakReference = new WeakReference<>(context);
        activityWeakReference = new WeakReference<>(activity);

        // Store the class variables.
        this.filePathString = filePathString;
        this.userAgent = userAgent;
        this.cookiesEnabled = cookiesEnabled;
    }

    // `onPreExecute()` operates on the UI thread.
    @Override
    protected void onPreExecute() {
        // Get a handle for the activity.
        Activity activity = activityWeakReference.get();

        // Abort if the activity is gone.
        if ((activity==null) || activity.isFinishing()) {
            return;
        }

        // Get a handle for the no swipe view pager.
        NoSwipeViewPager noSwipeViewPager = activity.findViewById(R.id.webviewpager);

        // Create a saving file snackbar.
        savingFileSnackbar = Snackbar.make(noSwipeViewPager, activity.getString(R.string.saving_file) + "  0%  -  " + urlString, Snackbar.LENGTH_INDEFINITE);

        // Display the saving file snackbar.
        savingFileSnackbar.show();
    }

    @Override
    protected String doInBackground(String... urlToSave) {
        // Get handles for the context and activity.
        Context context = contextWeakReference.get();
        Activity activity = activityWeakReference.get();

        // Abort if the activity is gone.
        if ((activity == null) || activity.isFinishing()) {
            return null;
        }

        // Define a save disposition string.
        String saveDisposition = SUCCESS;

        // Get the URL string.
        urlString = urlToSave[0];

        try {
            // Open an output stream.
            OutputStream outputStream = activity.getContentResolver().openOutputStream(Uri.parse(filePathString));

            // Save the URL.
            if (urlString.startsWith("data:")) {  // The URL contains the entire data of an image.
                // Get the Base64 data, which begins after a `,`.
                String base64DataString = urlString.substring(urlString.indexOf(",") + 1);

                // Decode the Base64 string to a byte array.
                byte[] base64DecodedDataByteArray = Base64.decode(base64DataString, Base64.DEFAULT);

                // Write the Base64 byte array to the output stream.
                outputStream.write(base64DecodedDataByteArray);
            } else {  // The URL points to the data location on the internet.
                // Get the URL from the calling activity.
                URL url = new URL(urlString);

                // Instantiate the proxy helper.
                ProxyHelper proxyHelper = new ProxyHelper();

                // Get the current proxy.
                Proxy proxy = proxyHelper.getCurrentProxy(context);

                // Open a connection to the URL.  No data is actually sent at this point.
                HttpURLConnection httpUrlConnection = (HttpURLConnection) url.openConnection(proxy);

                // Add the user agent to the header property.
                httpUrlConnection.setRequestProperty("User-Agent", userAgent);

                // Add the cookies if they are enabled.
                if (cookiesEnabled) {
                    // Get the cookies for the current domain.
                    String cookiesString = CookieManager.getInstance().getCookie(url.toString());

                    // Only add the cookies if they are not null.
                    if (cookiesString != null) {
                        // Add the cookies to the header property.
                        httpUrlConnection.setRequestProperty("Cookie", cookiesString);
                    }
                }

                // The actual network request is in a `try` bracket so that `disconnect()` is run in the `finally` section even if an error is encountered in the main block.
                try {
                    // Get the content length header, which causes the connection to the server to be made.
                    String contentLengthString = httpUrlConnection.getHeaderField("Content-Length");

                    // Make sure the content length isn't null.
                    if (contentLengthString != null) {  // The content length isn't null.
                        // Convert the content length to an long.
                        fileSize = Long.parseLong(contentLengthString);

                        // Format the file size for display.
                        formattedFileSize = NumberFormat.getInstance().format(fileSize);
                    } else {  // The content length is null.
                        // Set the file size to be `-1`.
                        fileSize = -1;
                    }

                    // Get the response body stream.
                    InputStream inputStream = new BufferedInputStream(httpUrlConnection.getInputStream());

                    // Initialize the conversion buffer byte array.
                    // This is set to a megabyte so that frequent updating of the snackbar doesn't freeze the interface on download.  <https://redmine.stoutner.com/issues/709>
                    byte[] conversionBufferByteArray = new byte[1048576];

                    // Initialize the downloaded kilobytes counter.
                    long downloadedKilobytesCounter = 0;

                    // Define the buffer length variable.
                    int bufferLength;

                    // Attempt to read data from the input stream and store it in the output stream.  Also store the amount of data read in the buffer length variable.
                    while ((bufferLength = inputStream.read(conversionBufferByteArray)) > 0) {  // Proceed while the amount of data stored in the buffer in > 0.
                        // Write the contents of the conversion buffer to the file output stream.
                        outputStream.write(conversionBufferByteArray, 0, bufferLength);

                        // Update the downloaded kilobytes counter.
                        downloadedKilobytesCounter = downloadedKilobytesCounter + bufferLength;

                        // Update the file download progress snackbar.
                        publishProgress(downloadedKilobytesCounter);
                    }

                    // Close the input stream.
                    inputStream.close();
                } finally {
                    // Disconnect the HTTP URL connection.
                    httpUrlConnection.disconnect();
                }
            }

            // Close the output stream.
            outputStream.close();
        } catch (Exception exception) {
            // Store the error in the save disposition string.
            saveDisposition = exception.toString();
        }

        // Return the save disposition string.
        return saveDisposition;
    }

    // `onProgressUpdate()` operates on the UI thread.
    @Override
    protected void onProgressUpdate(Long... numberOfBytesDownloaded) {
        // Get a handle for the activity.
        Activity activity = activityWeakReference.get();

        // Abort if the activity is gone.
        if ((activity == null) || activity.isFinishing()) {
            return;
        }

        // Format the number of bytes downloaded.
        String formattedNumberOfBytesDownloaded = NumberFormat.getInstance().format(numberOfBytesDownloaded[0]);

        // Check to see if the file size is known.
        if (fileSize == -1) {  // The size of the download file is not known.
            // Update the snackbar.
            savingFileSnackbar.setText(activity.getString(R.string.saving_file) + "  " + formattedNumberOfBytesDownloaded + " " + activity.getString(R.string.bytes) + "  -  " + urlString);
        } else {  // The size of the download file is known.
            // Calculate the download percentage.
            long downloadPercentage = (numberOfBytesDownloaded[0] * 100) / fileSize;

            // Update the snackbar.
            savingFileSnackbar.setText(activity.getString(R.string.saving_file) + "  " + downloadPercentage + "%  -  " + formattedNumberOfBytesDownloaded + " " + activity.getString(R.string.bytes) + " / " + formattedFileSize + " " +
                    activity.getString(R.string.bytes) + "  -  " + urlString);
        }
    }

    // `onPostExecute()` operates on the UI thread.
    @Override
    protected void onPostExecute(String saveDisposition) {
        // Get handles for the context and activity.
        Activity activity = activityWeakReference.get();

        // Abort if the activity is gone.
        if ((activity == null) || activity.isFinishing()) {
            return;
        }

        // Get a handle for the no swipe view pager.
        NoSwipeViewPager noSwipeViewPager = activity.findViewById(R.id.webviewpager);

        // Dismiss the saving file snackbar.
        savingFileSnackbar.dismiss();

        // Display a save disposition snackbar.
        if (saveDisposition.equals(SUCCESS)) {
            // Display the file saved snackbar.
            Snackbar.make(noSwipeViewPager, activity.getString(R.string.file_saved) + "  " + urlString, Snackbar.LENGTH_LONG).show();
        } else {
            // Display the file saving error.
            Snackbar.make(noSwipeViewPager, activity.getString(R.string.error_saving_file) + "  " + saveDisposition, Snackbar.LENGTH_INDEFINITE).show();
        }
    }
}