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

package com.stoutner.privacybrowser.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.ViewPager

import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.adapters.AboutPagerAdapter
import com.stoutner.privacybrowser.asynctasks.SaveAboutVersionImage
import com.stoutner.privacybrowser.dialogs.SaveDialog
import com.stoutner.privacybrowser.dialogs.SaveDialog.SaveListener
import com.stoutner.privacybrowser.fragments.AboutVersionFragment

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.lang.Exception
import java.nio.charset.StandardCharsets

class AboutActivity : AppCompatActivity(), SaveListener {
    // Declare the class variables.
    private lateinit var aboutPagerAdapter: AboutPagerAdapter

    companion object {
        // Define the companion object constants.  These can be move to being public constants once MainWebViewActivity has been converted to Kotlin.
        const val BLOCKLIST_VERSIONS = "blocklist_versions"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Get the preferences.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)
        val bottomAppBar = sharedPreferences.getBoolean(getString(R.string.bottom_app_bar_key), false)

        // Disable screenshots if not allowed.
        if (!allowScreenshots) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // Set the theme.
        setTheme(R.style.PrivacyBrowser)

        // Run the default commands.
        super.onCreate(savedInstanceState)

        // Get the intent that launched the activity.
        val launchingIntent = intent

        // Store the blocklist versions.
        val blocklistVersions = launchingIntent.getStringArrayExtra(BLOCKLIST_VERSIONS)!!

        // Set the content view.
        if (bottomAppBar) {
            setContentView(R.layout.about_coordinatorlayout_bottom_appbar)
        } else {
            setContentView(R.layout.about_coordinatorlayout_top_appbar)
        }

        // Get handles for the views.
        val toolbar = findViewById<Toolbar>(R.id.about_toolbar)
        val aboutTabLayout = findViewById<TabLayout>(R.id.about_tablayout)
        val aboutViewPager = findViewById<ViewPager>(R.id.about_viewpager)

        // Set the action bar.  `SupportActionBar` must be used until the minimum API is >= 21.
        setSupportActionBar(toolbar)

        // Get a handle for the action bar.
        val actionBar = supportActionBar!!

        // Display the home arrow on action bar.
        actionBar.setDisplayHomeAsUpEnabled(true)

        // Initialize the about pager adapter.
        aboutPagerAdapter = AboutPagerAdapter(supportFragmentManager, applicationContext, blocklistVersions)

        // Set the view pager adapter.
        aboutViewPager.adapter = aboutPagerAdapter

        // Keep all the tabs in memory.  This prevents the memory usage updater from running multiple times.
        aboutViewPager.offscreenPageLimit = 10

        // Connect the tab layout to the view pager.
        aboutTabLayout.setupWithViewPager(aboutViewPager)
    }

    // The activity result is called after browsing for a file in the save alert dialog.
    public override fun onActivityResult(requestCode: Int, resultCode: Int, returnedIntent: Intent?) {
        // Run the default commands.
        super.onActivityResult(requestCode, resultCode, returnedIntent)

        // Only do something if the user didn't press back from the file picker.
        if (resultCode == RESULT_OK) {
            // Get a handle for the save dialog fragment.
            val saveDialogFragment = supportFragmentManager.findFragmentByTag(getString(R.string.save_dialog)) as DialogFragment?

            // Only update the file name if the dialog still exists.
            if (saveDialogFragment != null) {
                // Get a handle for the save dialog.
                val saveDialog = saveDialogFragment.dialog!!

                // Get a handle for the file name edit text.
                val fileNameEditText = saveDialog.findViewById<EditText>(R.id.file_name_edittext)

                // Get the file name URI from the intent.
                val fileNameUri = returnedIntent!!.data

                // Get the file name string from the URI.
                val fileNameString = fileNameUri.toString()

                // Set the file name text.
                fileNameEditText.setText(fileNameString)

                // Move the cursor to the end of the file name edit text.
                fileNameEditText.setSelection(fileNameString.length)
            }
        }
    }

    override fun onSave(saveType: Int, dialogFragment: DialogFragment) {
        // Get a handle for the dialog.
        val dialog = dialogFragment.dialog!!

        // Get a handle for the file name edit text.
        val fileNameEditText = dialog.findViewById<EditText>(R.id.file_name_edittext)

        // Get the file name string.
        val fileNameString = fileNameEditText.text.toString()

        // Get a handle for the about version linear layout.
        val aboutVersionLinearLayout = findViewById<LinearLayout>(R.id.about_version_linearlayout)

        // Process the save event according to the type.
        when (saveType) {
            SaveDialog.SAVE_ABOUT_VERSION_TEXT -> try {
                // Get a handle for the about version fragment.
                val aboutVersionFragment = aboutPagerAdapter.getTabFragment(0) as AboutVersionFragment

                // Get the about version text.
                val aboutVersionString = aboutVersionFragment.aboutVersionString

                // Create an input stream with the contents of about version.
                val aboutVersionInputStream: InputStream = ByteArrayInputStream(aboutVersionString.toByteArray(StandardCharsets.UTF_8))

                // Open an output stream.
                val outputStream = contentResolver.openOutputStream(Uri.parse(fileNameString))!!

                // Copy the input stream to the output stream.
                aboutVersionInputStream.copyTo(outputStream, 2048)

                // Close the streams.
                aboutVersionInputStream.close()
                outputStream.close()

                // Display a snackbar with the saved about version information.
                Snackbar.make(aboutVersionLinearLayout, getString(R.string.file_saved) + "  " + fileNameString, Snackbar.LENGTH_SHORT).show()
            } catch (exception: Exception) {
                // Display a snackbar with the error message.
                Snackbar.make(aboutVersionLinearLayout, getString(R.string.error_saving_file) + "  " + exception.toString(), Snackbar.LENGTH_INDEFINITE).show()
            }

            SaveDialog.SAVE_ABOUT_VERSION_IMAGE ->
                // Save the about version image.
                SaveAboutVersionImage(this, fileNameString, aboutVersionLinearLayout).execute()
        }
    }
}