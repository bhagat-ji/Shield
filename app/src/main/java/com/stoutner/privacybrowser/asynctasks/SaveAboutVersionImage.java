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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.OpenableColumns;
import android.widget.LinearLayout;

import com.google.android.material.snackbar.Snackbar;

import com.stoutner.privacybrowser.R;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

public class SaveAboutVersionImage extends AsyncTask<Void, Void, String> {
    // Declare the class constants.
    private final String SUCCESS = "Success";

    // Declare the weak references.
    private final WeakReference<Activity> activityWeakReference;
    private final WeakReference<LinearLayout> aboutVersionLinearLayoutWeakReference;

    // Declare the class variables.
    private Snackbar savingImageSnackbar;
    private Bitmap aboutVersionBitmap;
    private final Uri fileUri;
    private final String fileNameString;

    // The public constructor.
    public SaveAboutVersionImage(Activity activity, Uri fileUri, LinearLayout aboutVersionLinearLayout) {
        // Populate the weak references.
        activityWeakReference = new WeakReference<>(activity);
        aboutVersionLinearLayoutWeakReference = new WeakReference<>(aboutVersionLinearLayout);

        // Store the class variables.
        this.fileUri = fileUri;

        // Query the exact file name if the API >= 26.
        if (Build.VERSION.SDK_INT >= 26) {
            // Get a cursor from the content resolver.
            Cursor contentResolverCursor = activity.getContentResolver().query(fileUri, null, null, null);

            // Move to the first row.
            contentResolverCursor.moveToFirst();

            // Get the file name from the cursor.
            fileNameString = contentResolverCursor.getString(contentResolverCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

            // Close the cursor.
            contentResolverCursor.close();
        } else {
            // Use the URI last path segment as the file name string.
            fileNameString = fileUri.getLastPathSegment();
        }
    }

    // `onPreExecute()` operates on the UI thread.
    @Override
    protected void onPreExecute() {
        // Get handles for the activity and the linear layout.
        Activity activity = activityWeakReference.get();
        LinearLayout aboutVersionLinearLayout = aboutVersionLinearLayoutWeakReference.get();

        // Abort if the activity or the linear layout is gone.
        if ((activity == null) || activity.isFinishing() || aboutVersionLinearLayout == null) {
            return;
        }

        // Create a saving image snackbar.
        savingImageSnackbar = Snackbar.make(aboutVersionLinearLayout, activity.getString(R.string.processing_image) + "  " + fileNameString, Snackbar.LENGTH_INDEFINITE);

        // Display the saving image snackbar.
        savingImageSnackbar.show();

        // Create the about version bitmap.  This can be replaced by PixelCopy once the minimum API >= 26.
        // Once the Minimum API >= 26 Bitmap.Config.RBGA_F16 can be used instead of ARGB_8888.  The linear layout commands must be run on the UI thread.
        aboutVersionBitmap = Bitmap.createBitmap(aboutVersionLinearLayout.getWidth(), aboutVersionLinearLayout.getHeight(), Bitmap.Config.ARGB_8888);

        // Create a canvas.
        Canvas aboutVersionCanvas = new Canvas(aboutVersionBitmap);

        // Draw the current about version onto the bitmap.  The linear layout commands must be run on the UI thread.
        aboutVersionLinearLayout.draw(aboutVersionCanvas);
    }

    @Override
    protected String doInBackground(Void... Void) {
        // Get a handle for the activity.
        Activity activity = activityWeakReference.get();

        // Abort if the activity is gone.
        if (((activity == null) || activity.isFinishing())) {
            return "";
        }

        // Create an about version PNG byte array output stream.
        ByteArrayOutputStream aboutVersionByteArrayOutputStream = new ByteArrayOutputStream();

        // Convert the bitmap to a PNG.  `0` is for lossless compression (the only option for a PNG).  This compression takes a long time.  Once the minimum API >= 30 this could be replaced with WEBP_LOSSLESS.
        aboutVersionBitmap.compress(Bitmap.CompressFormat.PNG, 0, aboutVersionByteArrayOutputStream);

        // Create a file creation disposition string.
        String fileCreationDisposition = SUCCESS;

        try {
            // Open an output stream.
            OutputStream outputStream = activity.getContentResolver().openOutputStream(fileUri);

            // Write the webpage image to the image file.
            aboutVersionByteArrayOutputStream.writeTo(outputStream);

            // Close the output stream.
            outputStream.close();
        } catch (Exception exception) {
            // Store the error in the file creation disposition string.
            fileCreationDisposition = exception.toString();
        }

        // return the file creation disposition string.
        return fileCreationDisposition;
    }

    // `onPostExecute()` operates on the UI thread.
    @Override
    protected void onPostExecute(String fileCreationDisposition) {
        // Get handles for the weak references.
        Activity activity = activityWeakReference.get();
        LinearLayout aboutVersionLinearLayout = aboutVersionLinearLayoutWeakReference.get();

        // Abort if the activity is gone.
        if ((activity == null) || activity.isFinishing()) {
            return;
        }

        // Dismiss the saving image snackbar.
        savingImageSnackbar.dismiss();

        // Display a file creation disposition snackbar.
        if (fileCreationDisposition.equals(SUCCESS)) {
            // Create a file saved snackbar.
            Snackbar.make(aboutVersionLinearLayout, activity.getString(R.string.file_saved) + "  " + fileNameString, Snackbar.LENGTH_SHORT).show();
        } else {
            Snackbar.make(aboutVersionLinearLayout, activity.getString(R.string.error_saving_file) + "  " + fileCreationDisposition, Snackbar.LENGTH_INDEFINITE).show();
        }
    }
}