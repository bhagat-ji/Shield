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

package com.stoutner.privacybrowser.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.ScrollView

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

import com.google.android.material.snackbar.Snackbar

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.dialogs.SaveDialog

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Exception
import java.nio.charset.StandardCharsets

// Define the class constants.
private const val SCROLLVIEW_POSITION = "scrollview_position"

class LogcatActivity : AppCompatActivity(), SaveDialog.SaveListener {
    // Define the class variables.
    private var scrollViewYPositionInt = 0

    // Define the class views.
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var logcatScrollView: ScrollView
    private lateinit var logcatTextView: TextView

    public override fun onCreate(savedInstanceState: Bundle?) {
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

        // Set the content view.
        if (bottomAppBar) {
            setContentView(R.layout.logcat_bottom_appbar)
        } else {
            setContentView(R.layout.logcat_top_appbar)
        }

        // Get handles for the views.
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        swipeRefreshLayout = findViewById(R.id.swiperefreshlayout)
        logcatScrollView = findViewById(R.id.scrollview)
        logcatTextView = findViewById(R.id.logcat_textview)

        // Set the toolbar as the action bar.
        setSupportActionBar(toolbar)

        // Get a handle for the action bar.
        val actionBar = supportActionBar!!

        // Display the back arrow in the action bar.
        actionBar.setDisplayHomeAsUpEnabled(true)

        // Implement swipe to refresh.
        swipeRefreshLayout.setOnRefreshListener {
            // Get the current logcat.
            getLogcat()
        }

        // Set the swipe refresh color scheme according to the theme.
        swipeRefreshLayout.setColorSchemeResources(R.color.blue_text)

        // Initialize a color background typed value.
        val colorBackgroundTypedValue = TypedValue()

        // Get the color background from the theme.
        theme.resolveAttribute(android.R.attr.colorBackground, colorBackgroundTypedValue, true)

        // Get the color background int from the typed value.
        val colorBackgroundInt = colorBackgroundTypedValue.data

        // Set the swipe refresh background color.
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(colorBackgroundInt)

        // Check to see if the activity has been restarted.
        if (savedInstanceState != null) {
            // Get the saved scrollview position.
            scrollViewYPositionInt = savedInstanceState.getInt(SCROLLVIEW_POSITION)
        }

        // Get the logcat.
        getLogcat()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu.  This adds items to the action bar.
        menuInflater.inflate(R.menu.logcat_options_menu, menu)

        // Display the menu.
        return true
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        // Run the commands that correlate to the selected menu item.
        return when (menuItem.itemId) {
            R.id.copy -> {  // Copy was selected.
                // Get a handle for the clipboard manager.
                val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

                // Save the logcat in a clip data.
                val logcatClipData = ClipData.newPlainText(getString(R.string.logcat), logcatTextView.text)

                // Place the clip data on the clipboard.
                clipboardManager.setPrimaryClip(logcatClipData)

                // Display a snackbar.
                Snackbar.make(logcatTextView, R.string.logcat_copied, Snackbar.LENGTH_SHORT).show()

                // Consume the event.
                true
            }

            R.id.save -> {  // Save was selected.
                // Instantiate the save alert dialog.
                val saveDialogFragment: DialogFragment = SaveDialog.save(SaveDialog.SAVE_LOGCAT)

                // Show the save alert dialog.
                saveDialogFragment.show(supportFragmentManager, getString(R.string.save_logcat))

                // Consume the event.
                true
            }

            R.id.clear -> {  // Clear was selected.
                try {
                    // Clear the logcat.  `-c` clears the logcat.  `-b all` clears all the buffers (instead of just crash, main, and system).
                    val process = Runtime.getRuntime().exec("logcat -b all -c")

                    // Wait for the process to finish.
                    process.waitFor()

                    // Reset the scroll view Y position int.
                    scrollViewYPositionInt = 0

                    // Reload the logcat.
                    getLogcat()
                } catch (exception: Exception) {
                    // Do nothing.
                }

                // Consume the event.
                true
            }

            else -> {  // The home button was pushed.
                // Do not consume the event.  The system will process the home command.
                super.onOptionsItemSelected(menuItem)
            }
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        // Run the default commands.
        super.onSaveInstanceState(savedInstanceState)

        // Get the scrollview Y position.
        val scrollViewYPositionInt = logcatScrollView.scrollY

        // Store the scrollview Y position in the bundle.
        savedInstanceState.putInt(SCROLLVIEW_POSITION, scrollViewYPositionInt)
    }

    private fun getLogcat() {
        try {
            // Get the logcat.  `-b all` gets all the buffers (instead of just crash, main, and system).  `-v long` produces more complete information.  `-d` dumps the logcat and exits.
            val getLogcatProcess = Runtime.getRuntime().exec("logcat -b all -v long -d")

            // Wrap the logcat in a buffered reader.
            val logcatBufferedReader = BufferedReader(InputStreamReader(getLogcatProcess.inputStream))

            // Display the logcat.
            logcatTextView.text = logcatBufferedReader.readText()

            // Close the buffered reader.
            logcatBufferedReader.close()
        } catch (exception: IOException) {
            // Do nothing.
        }

        // Update the scroll position after the text is populated.
        logcatTextView.post {
            // Set the scroll position.
            logcatScrollView.scrollY = scrollViewYPositionInt
        }

        // Stop the swipe to refresh animation if it is displayed.
        swipeRefreshLayout.isRefreshing = false
    }

    // The activity result is called after browsing for a file in the save alert dialog.
    public override fun onActivityResult(requestCode: Int, resultCode: Int, returnedIntent: Intent?) {
        // Run the default commands.
        super.onActivityResult(requestCode, resultCode, returnedIntent)

        // Only do something if the user didn't press back from the file picker.
        if (resultCode == RESULT_OK) {
            // Get a handle for the save dialog fragment.
            val saveDialogFragment = supportFragmentManager.findFragmentByTag(getString(R.string.save_logcat)) as DialogFragment?

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

        // Get the file path string.
        var fileNameString = fileNameEditText.text.toString()

        try {
            // Get the logcat as a string.
            val logcatString = logcatTextView.text.toString()

            // Open an output stream.
            val outputStream = contentResolver.openOutputStream(Uri.parse(fileNameString))!!

            // Write the logcat string to the output stream.
            outputStream.write(logcatString.toByteArray(StandardCharsets.UTF_8))

            // Close the output stream.
            outputStream.close()

            // Get the actual file name if the API >= 26.
            if (Build.VERSION.SDK_INT >= 26) {
                // Get a cursor from the content resolver.
                val contentResolverCursor = contentResolver.query(Uri.parse(fileNameString), null, null, null)!!

                // Move to the first row.
                contentResolverCursor.moveToFirst()

                // Get the file name from the cursor.
                fileNameString = contentResolverCursor.getString(contentResolverCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))

                // Close the cursor.
                contentResolverCursor.close()
            }

            // Display a snackbar with the saved logcat information.
            Snackbar.make(logcatTextView, getString(R.string.file_saved) + "  " + fileNameString, Snackbar.LENGTH_SHORT).show()
        } catch (exception: Exception) {
            // Display a snackbar with the error message.
            Snackbar.make(logcatTextView, getString(R.string.error_saving_file) + "  " + exception.toString(), Snackbar.LENGTH_INDEFINITE).show()
        }
    }
}