/*
 * Copyright © 2019-2020 Soren Stoutner <soren@stoutner.com>.
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

package com.stoutner.privacybrowser.activities;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import com.stoutner.privacybrowser.R;
import com.stoutner.privacybrowser.asynctasks.GetLogcat;
import com.stoutner.privacybrowser.dialogs.StoragePermissionDialog;
import com.stoutner.privacybrowser.dialogs.SaveLogcatDialog;
import com.stoutner.privacybrowser.helpers.FileNameHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class LogcatActivity extends AppCompatActivity implements SaveLogcatDialog.SaveLogcatListener, StoragePermissionDialog.StoragePermissionDialogListener {
    // Initialize the saved instance state constants.
    private final String SCROLLVIEW_POSITION = "scrollview_position";

    // Define the class variables.
    private String filePathString;

    // Define the class views.
    private TextView logcatTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Get a handle for the shared preferences.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Get the screenshot preference.
        boolean allowScreenshots = sharedPreferences.getBoolean("allow_screenshots", false);

        // Disable screenshots if not allowed.
        if (!allowScreenshots) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        // Set the theme.
        setTheme(R.style.PrivacyBrowser);

        // Run the default commands.
        super.onCreate(savedInstanceState);

        // Set the content view.
        setContentView(R.layout.logcat_coordinatorlayout);

        // Set the toolbar as the action bar.
        Toolbar toolbar = findViewById(R.id.logcat_toolbar);
        setSupportActionBar(toolbar);

        // Get a handle for the action bar.
        ActionBar actionBar = getSupportActionBar();

        // Remove the incorrect lint warning that the action bar might be null.
        assert actionBar != null;

        // Display the the back arrow in the action bar.
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Populate the class views.
        logcatTextView = findViewById(R.id.logcat_textview);

        // Implement swipe to refresh.
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.logcat_swiperefreshlayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Get the current logcat.
            new GetLogcat(this, 0).execute();
        });

        // Get the current theme status.
        int currentThemeStatus = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        // Set the refresh color scheme according to the theme.
        if (currentThemeStatus == Configuration.UI_MODE_NIGHT_YES) {
            swipeRefreshLayout.setColorSchemeResources(R.color.blue_500);
        } else {
            swipeRefreshLayout.setColorSchemeResources(R.color.blue_700);
        }

        // Initialize a color background typed value.
        TypedValue colorBackgroundTypedValue = new TypedValue();

        // Get the color background from the theme.
        getTheme().resolveAttribute(android.R.attr.colorBackground, colorBackgroundTypedValue, true);

        // Get the color background int from the typed value.
        int colorBackgroundInt = colorBackgroundTypedValue.data;

        // Set the swipe refresh background color.
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(colorBackgroundInt);

        // Initialize the scrollview Y position int.
        int scrollViewYPositionInt = 0;

        // Check to see if the activity has been restarted.
        if (savedInstanceState != null) {
            // Get the saved scrollview position.
            scrollViewYPositionInt = savedInstanceState.getInt(SCROLLVIEW_POSITION);
        }

        // Get the logcat.
        new GetLogcat(this, scrollViewYPositionInt).execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu.  This adds items to the action bar.
        getMenuInflater().inflate(R.menu.logcat_options_menu, menu);

        // Display the menu.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        // Get the selected menu item ID.
        int menuItemId = menuItem.getItemId();

        // Run the commands that correlate to the selected menu item.
        switch (menuItemId) {
            case R.id.copy:
                // Get a handle for the clipboard manager.
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

                // Save the logcat in a ClipData.
                ClipData logcatClipData = ClipData.newPlainText(getString(R.string.logcat), logcatTextView.getText());

                // Remove the incorrect lint error that `clipboardManager.setPrimaryClip()` might produce a null pointer exception.
                assert clipboardManager != null;

                // Place the ClipData on the clipboard.
                clipboardManager.setPrimaryClip(logcatClipData);

                // Display a snackbar.
                Snackbar.make(logcatTextView, R.string.logcat_copied, Snackbar.LENGTH_SHORT).show();

                // Consume the event.
                return true;

            case R.id.save:
                // Instantiate the save alert dialog.
                DialogFragment saveDialogFragment = new SaveLogcatDialog();

                // Show the save alert dialog.
                saveDialogFragment.show(getSupportFragmentManager(), getString(R.string.save_logcat));

                // Consume the event.
                return true;

            case R.id.clear:
                try {
                    // Clear the logcat.  `-c` clears the logcat.  `-b all` clears all the buffers (instead of just crash, main, and system).
                    Process process = Runtime.getRuntime().exec("logcat -b all -c");

                    // Wait for the process to finish.
                    process.waitFor();

                    // Reload the logcat.
                    new GetLogcat(this, 0).execute();
                } catch (IOException|InterruptedException exception) {
                    // Do nothing.
                }

                // Consume the event.
                return true;

            default:
                // Don't consume the event.
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        // Run the default commands.
        super.onSaveInstanceState(savedInstanceState);

        // Get a handle for the logcat scrollview.
        ScrollView logcatScrollView = findViewById(R.id.logcat_scrollview);

        // Get the scrollview Y position.
        int scrollViewYPositionInt = logcatScrollView.getScrollY();

        // Store the scrollview Y position in the bundle.
        savedInstanceState.putInt(SCROLLVIEW_POSITION, scrollViewYPositionInt);
    }

    @Override
    public void onSaveLogcat(DialogFragment dialogFragment) {
        // Get a handle for the dialog fragment.
        Dialog dialog = dialogFragment.getDialog();

        // Remove the lint warning below that the dialog fragment might be null.
        assert dialog != null;

        // Get a handle for the file name edit text.
        EditText fileNameEditText = dialog.findViewById(R.id.file_name_edittext);

        // Get the file path string.
        filePathString = fileNameEditText.getText().toString();

        // Check to see if the storage permission is needed.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {  // The storage permission has been granted.
            // Save the logcat.
            saveLogcat(filePathString);
        } else {  // The storage permission has not been granted.
            // Get the external private directory `File`.
            File externalPrivateDirectoryFile = getExternalFilesDir(null);

            // Remove the incorrect lint error below that the file might be null.
            assert externalPrivateDirectoryFile != null;

            // Get the external private directory string.
            String externalPrivateDirectory = externalPrivateDirectoryFile.toString();

            // Check to see if the file path is in the external private directory.
            if (filePathString.startsWith(externalPrivateDirectory)) {  // The file path is in the external private directory.
                // Save the logcat.
                saveLogcat(filePathString);
            } else {  // The file path in in a public directory.
                // Check if the user has previously denied the storage permission.
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {  // Show a dialog explaining the request first.
                    // Instantiate the storage permission alert dialog.
                    DialogFragment storagePermissionDialogFragment = StoragePermissionDialog.displayDialog(0);

                    // Show the storage permission alert dialog.  The permission will be requested when the dialog is closed.
                    storagePermissionDialogFragment.show(getSupportFragmentManager(), getString(R.string.storage_permission));
                } else {  // Show the permission request directly.
                    // Request the write external storage permission.  The logcat will be saved when it finishes.
                    ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);

                }
            }
        }
    }

    @Override
    public void onCloseStoragePermissionDialog(int type) {
        // Request the write external storage permission.  The logcat will be saved when it finishes.
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Check to see if the storage permission was granted.  If the dialog was canceled the grant result will be empty.
        if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {  // The storage permission was granted.
            // Save the logcat.
            saveLogcat(filePathString);
        } else {  // The storage permission was not granted.
            // Display an error snackbar.
            Snackbar.make(logcatTextView, getString(R.string.cannot_use_location), Snackbar.LENGTH_LONG).show();
        }
    }

    private void saveLogcat(String fileNameString) {
        try {
            // Get the logcat as a string.
            String logcatString = logcatTextView.getText().toString();

            // Create an input stream with the contents of the logcat.
            InputStream logcatInputStream = new ByteArrayInputStream(logcatString.getBytes(StandardCharsets.UTF_8));

            // Create a logcat buffered reader.
            BufferedReader logcatBufferedReader = new BufferedReader(new InputStreamReader(logcatInputStream));

            // Create a file from the file name string.
            File saveFile = new File(fileNameString);

            // Create a file buffered writer.
            BufferedWriter fileBufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(saveFile)));

            // Create a transfer string.
            String transferString;

            // Use the transfer string to copy the logcat from the buffered reader to the buffered writer.
            while ((transferString = logcatBufferedReader.readLine()) != null) {
                // Append the line to the buffered writer.
                fileBufferedWriter.append(transferString);

                // Append a line break.
                fileBufferedWriter.append("\n");
            }

            // Close the buffered reader and writer.
            logcatBufferedReader.close();
            fileBufferedWriter.close();

            // Add the file to the list of recent files.  This doesn't currently work, but maybe it will someday.
            MediaScannerConnection.scanFile(this, new String[] {fileNameString}, new String[] {"text/plain"}, null);

            // Display a snackbar.
            Snackbar.make(logcatTextView, getString(R.string.file_saved_successfully), Snackbar.LENGTH_SHORT).show();
        } catch (Exception exception) {
            // Display a snackbar with the error message.
            Snackbar.make(logcatTextView, getString(R.string.save_failed) + "  " + exception.toString(), Snackbar.LENGTH_INDEFINITE).show();
        }
    }

    // The activity result is called after browsing for a file in the save alert dialog.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Run the default commands.
        super.onActivityResult(requestCode, resultCode, data);

        // Don't do anything if the user pressed back from the file picker.
        if (resultCode == Activity.RESULT_OK) {
            // Get a handle for the save dialog fragment.
            DialogFragment saveDialogFragment = (DialogFragment) getSupportFragmentManager().findFragmentByTag(getString(R.string.save_logcat));

            // Only update the file name if the dialog still exists.
            if (saveDialogFragment != null) {
                // Get a handle for the save dialog.
                Dialog saveDialog = saveDialogFragment.getDialog();

                // Remove the lint warning below that the save dialog might be null.
                assert saveDialog != null;

                // Get a handle for the dialog views.
                EditText fileNameEditText = saveDialog.findViewById(R.id.file_name_edittext);
                TextView fileExistsWarningTextView = saveDialog.findViewById(R.id.file_exists_warning_textview);

                // Instantiate the file name helper.
                FileNameHelper fileNameHelper = new FileNameHelper();

                // Get the file name URI from the intent.
                Uri fileNameUri= data.getData();

                // Process the file name URI if it is not null.
                if (fileNameUri != null) {
                    // Convert the file name URI to a file name path.
                    String fileNamePath = fileNameHelper.convertUriToFileNamePath(fileNameUri);

                    // Set the file name path as the text of the file name edit text.
                    fileNameEditText.setText(fileNamePath);

                    // Move the cursor to the end of the file name edit text.
                    fileNameEditText.setSelection(fileNamePath.length());

                    // Hide the file exists warning.
                    fileExistsWarningTextView.setVisibility(View.GONE);
                }
            }
        }
    }
}