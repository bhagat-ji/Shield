/*
 * Copyright © 2016-2021 Soren Stoutner <soren@stoutner.com>.
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import com.stoutner.privacybrowser.adapters.AboutPagerAdapter;
import com.stoutner.privacybrowser.R;
import com.stoutner.privacybrowser.asynctasks.SaveAboutVersionImage;
import com.stoutner.privacybrowser.dialogs.SaveDialog;
import com.stoutner.privacybrowser.fragments.AboutVersionFragment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class AboutActivity extends AppCompatActivity implements SaveDialog.SaveListener {
    // Declare the class variables.
    private AboutPagerAdapter aboutPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        // Get the intent that launched the activity.
        Intent launchingIntent = getIntent();

        // Store the blocklist versions.
        String[] blocklistVersions = launchingIntent.getStringArrayExtra("blocklist_versions");

        // Remove the incorrect lint warning below that the blocklist versions might be null.
        assert blocklistVersions != null;

        // Set the content view.
        setContentView(R.layout.about_coordinatorlayout);

        // Get handles for the views.
        Toolbar toolbar = findViewById(R.id.about_toolbar);
        TabLayout aboutTabLayout = findViewById(R.id.about_tablayout);
        ViewPager aboutViewPager = findViewById(R.id.about_viewpager);

        // Set the action bar.  `SupportActionBar` must be used until the minimum API is >= 21.
        setSupportActionBar(toolbar);

        // Get a handle for the action bar.
        final ActionBar actionBar = getSupportActionBar();

        // Remove the incorrect lint warning that the action bar might be null.
        assert actionBar != null;  //

        // Display the home arrow on action bar.
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Initialize the about pager adapter.
        aboutPagerAdapter = new AboutPagerAdapter(getSupportFragmentManager(), getApplicationContext(), blocklistVersions);

        // Setup the ViewPager.
        aboutViewPager.setAdapter(aboutPagerAdapter);

        // Keep all the tabs in memory.  This prevents the memory usage updater from running multiple times.
        aboutViewPager.setOffscreenPageLimit(10);

        // Connect the tab layout to the view pager.
        aboutTabLayout.setupWithViewPager(aboutViewPager);
    }

    // The activity result is called after browsing for a file in the save alert dialog.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent returnedIntent) {
        // Run the default commands.
        super.onActivityResult(requestCode, resultCode, returnedIntent);

        // Only do something if the user didn't press back from the file picker.
        if (resultCode == Activity.RESULT_OK) {
            // Get a handle for the save dialog fragment.
            DialogFragment saveDialogFragment = (DialogFragment) getSupportFragmentManager().findFragmentByTag(getString(R.string.save_dialog));

            // Only update the file name if the dialog still exists.
            if (saveDialogFragment != null) {
                // Get a handle for the save dialog.
                Dialog saveDialog = saveDialogFragment.getDialog();

                // Remove the lint warning below that the dialog might be null.
                assert saveDialog != null;

                // Get a handle for the dialog view.
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

        // Get the file name string.
        String fileNameString = fileNameEditText.getText().toString();

        // Get a handle for the about version linear layout.
        LinearLayout aboutVersionLinearLayout = findViewById(R.id.about_version_linearlayout);

        // Save the file according to the type.
        switch (saveType) {
            case SaveDialog.SAVE_ABOUT_VERSION_TEXT:
                try {
                    // Get a handle for the about version fragment.
                    AboutVersionFragment aboutVersionFragment = (AboutVersionFragment) aboutPagerAdapter.getTabFragment(0);

                    // Get the about version text.
                    String aboutVersionString = aboutVersionFragment.getAboutVersionString();

                    // Create an input stream with the contents of about version.
                    InputStream aboutVersionInputStream = new ByteArrayInputStream(aboutVersionString.getBytes(StandardCharsets.UTF_8));

                    // Create an about version buffered reader.
                    BufferedReader aboutVersionBufferedReader = new BufferedReader(new InputStreamReader(aboutVersionInputStream));

                    // Open an output stream.
                    OutputStream outputStream = getContentResolver().openOutputStream(Uri.parse(fileNameString));

                    // Create a file buffered writer.
                    BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));

                    // Create a transfer string.
                    String transferString;

                    // Use the transfer string to copy the about version text from the buffered reader to the buffered writer.
                    while ((transferString = aboutVersionBufferedReader.readLine()) != null) {
                        // Append the line to the buffered writer.
                        bufferedWriter.append(transferString);

                        // Append a line break.
                        bufferedWriter.append("\n");
                    }

                    // Flush the buffered writer.
                    bufferedWriter.flush();

                    // Close the inputs and outputs.
                    aboutVersionBufferedReader.close();
                    aboutVersionInputStream.close();
                    bufferedWriter.close();
                    outputStream.close();

                    // Display a snackbar with the saved about version information.
                    Snackbar.make(aboutVersionLinearLayout, getString(R.string.file_saved) + "  " + fileNameString, Snackbar.LENGTH_SHORT).show();
                } catch (Exception exception) {
                    // Display a snackbar with the error message.
                    Snackbar.make(aboutVersionLinearLayout, getString(R.string.error_saving_file) + "  " + exception.toString(), Snackbar.LENGTH_INDEFINITE).show();
                }
                break;

            case SaveDialog.SAVE_ABOUT_VERSION_IMAGE:
                // Save the about version image.
                new SaveAboutVersionImage(this, fileNameString, aboutVersionLinearLayout).execute();
                break;
        }
    }
}