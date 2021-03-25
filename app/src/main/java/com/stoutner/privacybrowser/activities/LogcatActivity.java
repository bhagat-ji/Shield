/*
 * Copyright © 2019-2021 Soren Stoutner <soren@stoutner.com>.
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

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import com.stoutner.privacybrowser.R;
import com.stoutner.privacybrowser.asynctasks.GetLogcat;
import com.stoutner.privacybrowser.dialogs.SaveDialog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class LogcatActivity extends AppCompatActivity implements SaveDialog.SaveListener {
    // Declare the class constants.
    private final String SCROLLVIEW_POSITION = "scrollview_position";

    // Define the class views.
    private TextView logcatTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Get a handle for the shared preferences.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Get the screenshot preference.
        boolean allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false);

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

        // Get handles for the views.
        Toolbar toolbar = findViewById(R.id.logcat_toolbar);
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.logcat_swiperefreshlayout);

        // Set the toolbar as the action bar.
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
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Get the current logcat.
            new GetLogcat(this, 0).execute();
        });

        // Get the current theme status.
        int currentThemeStatus = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        // Set the refresh color scheme according to the theme.
        if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {
            swipeRefreshLayout.setColorSchemeResources(R.color.blue_700);
        } else {
            swipeRefreshLayout.setColorSchemeResources(R.color.blue_500);
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
        if (menuItemId == R.id.copy) {  // Copy was selected.
            // Get a handle for the clipboard manager.
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

            // Remove the incorrect lint error below that the clipboard manager might be null.
            assert clipboardManager != null;

            // Save the logcat in a clip data.
            ClipData logcatClipData = ClipData.newPlainText(getString(R.string.logcat), logcatTextView.getText());

            // Place the clip data on the clipboard.
            clipboardManager.setPrimaryClip(logcatClipData);

            // Display a snackbar.
            Snackbar.make(logcatTextView, R.string.logcat_copied, Snackbar.LENGTH_SHORT).show();

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.save) {  // Save was selected.
            // Instantiate the save alert dialog.
            DialogFragment saveDialogFragment = SaveDialog.save(SaveDialog.SAVE_LOGCAT);

            // Show the save alert dialog.
            saveDialogFragment.show(getSupportFragmentManager(), getString(R.string.save_logcat));

            // Consume the event.
            return true;
        } else if (menuItemId == R.id.clear) {  // Clear was selected.
            try {
                // Clear the logcat.  `-c` clears the logcat.  `-b all` clears all the buffers (instead of just crash, main, and system).
                Process process = Runtime.getRuntime().exec("logcat -b all -c");

                // Wait for the process to finish.
                process.waitFor();

                // Reload the logcat.
                new GetLogcat(this, 0).execute();
            } catch (IOException | InterruptedException exception) {
                // Do nothing.
            }

            // Consume the event.
            return true;
        } else {  // The home button was pushed.
            // Do not consume the event.  The system will process the home command.
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

    // The activity result is called after browsing for a file in the save alert dialog.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent returnedIntent) {
        // Run the default commands.
        super.onActivityResult(requestCode, resultCode, returnedIntent);

        // Only do something if the user didn't press back from the file picker.
        if (resultCode == Activity.RESULT_OK) {
            // Get a handle for the save dialog fragment.
            DialogFragment saveDialogFragment = (DialogFragment) getSupportFragmentManager().findFragmentByTag(getString(R.string.save_logcat));

            // Only update the file name if the dialog still exists.
            if (saveDialogFragment != null) {
                // Get a handle for the save dialog.
                Dialog saveDialog = saveDialogFragment.getDialog();

                // Remove the lint warning below that the save dialog might be null.
                assert saveDialog != null;

                // Get a handle for the file name edit text.
                EditText fileNameEditText = saveDialog.findViewById(R.id.file_name_edittext);

                // Get the file name URI from the intent.
                Uri fileNameUri = returnedIntent.getData();

                // Get the file name string from the URI.
                String fileNameString = fileNameUri.toString();

                // Set the file name text.
                fileNameEditText.setText(fileNameString);

                // Move the cursor to the end of the file name edit text.
                fileNameEditText.setSelection(fileNameString.length());
            }
        }
    }

    @Override
    public void onSave(int saveType, DialogFragment dialogFragment) {
        // Get a handle for the dialog.
        Dialog dialog = dialogFragment.getDialog();

        // Remove the lint warning below that the dialog might be null.
        assert dialog != null;

        // Get a handle for the file name edit text.
        EditText fileNameEditText = dialog.findViewById(R.id.file_name_edittext);

        // Get the file path string.
        String fileNameString = fileNameEditText.getText().toString();

        try {
            // Get the logcat as a string.
            String logcatString = logcatTextView.getText().toString();

            // Create an input stream with the contents of the logcat.
            InputStream logcatInputStream = new ByteArrayInputStream(logcatString.getBytes(StandardCharsets.UTF_8));

            // Create a logcat buffered reader.
            BufferedReader logcatBufferedReader = new BufferedReader(new InputStreamReader(logcatInputStream));

            // Open an output stream.
            OutputStream outputStream = getContentResolver().openOutputStream(Uri.parse(fileNameString));

            // Create a file buffered writer.
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));

            // Create a transfer string.
            String transferString;

            // Use the transfer string to copy the logcat from the buffered reader to the buffered writer.
            while ((transferString = logcatBufferedReader.readLine()) != null) {
                // Append the line to the buffered writer.
                bufferedWriter.append(transferString);

                // Append a line break.
                bufferedWriter.append("\n");
            }

            // Flush the buffered writer.
            bufferedWriter.flush();

            // Close the inputs and outputs.
            logcatBufferedReader.close();
            logcatInputStream.close();
            bufferedWriter.close();
            outputStream.close();

            // Display a snackbar with the saved logcat information.
            Snackbar.make(logcatTextView, getString(R.string.file_saved) + "  " + fileNameString, Snackbar.LENGTH_SHORT).show();
        } catch (Exception exception) {
            // Display a snackbar with the error message.
            Snackbar.make(logcatTextView, getString(R.string.error_saving_file) + "  " + exception.toString(), Snackbar.LENGTH_INDEFINITE).show();
        }
    }
}