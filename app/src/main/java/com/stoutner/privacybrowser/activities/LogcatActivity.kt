/*
 * Copyright 2019-2024 Soren Stoutner <soren@stoutner.com>.
 *
 * This file is part of Privacy Browser Android <https://www.stoutner.com/privacy-browser-android/>.
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

package com.stoutner.privacybrowser.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.webkit.WebView

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

import com.google.android.material.snackbar.Snackbar

import com.stoutner.privacybrowser.BuildConfig
import com.stoutner.privacybrowser.R

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

import java.nio.charset.StandardCharsets

// Define the class constants.
private const val SCROLL_Y = "A"

class LogcatActivity : AppCompatActivity() {
    // Declare the class variables.
    private lateinit var logcatPlainTextStringBuilder: StringBuilder

    // Declare the class views.
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var logcatWebView: WebView

    // Define the save logcat activity result launcher.  It must be defined before `onCreate()` is run or the app will crash.
    private val saveLogcatActivityResultLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { fileUri ->
        // Only save the file if the URI is not null, which happens if the user exited the file picker by pressing back.
        if (fileUri != null) {
            try {
                // Open an output stream.
                val outputStream = contentResolver.openOutputStream(fileUri)!!

                // Save the logcat using a coroutine with Dispatchers.IO.
                CoroutineScope(Dispatchers.Main).launch {
                    withContext(Dispatchers.IO) {
                        // Write the logcat string to the output stream.
                        outputStream.write(logcatPlainTextStringBuilder.toString().toByteArray(StandardCharsets.UTF_8))

                        // Close the output stream.
                        outputStream.close()
                    }
                }

                // Get a cursor from the content resolver.
                val contentResolverCursor = contentResolver.query(fileUri, null, null, null)!!

                // Move to the fist row.
                contentResolverCursor.moveToFirst()

                // Get the file name from the cursor.
                val fileNameString = contentResolverCursor.getString(contentResolverCursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))

                // Close the cursor.
                contentResolverCursor.close()

                // Display a snackbar with the saved logcat information.
                Snackbar.make(logcatWebView, getString(R.string.saved, fileNameString), Snackbar.LENGTH_SHORT).show()
            } catch (exception: Exception) {
                // Display a snackbar with the error message.
                Snackbar.make(logcatWebView, getString(R.string.error_saving_logcat, exception.toString()), Snackbar.LENGTH_INDEFINITE).show()
            }
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Get the preferences.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)
        val bottomAppBar = sharedPreferences.getBoolean(getString(R.string.bottom_app_bar_key), false)

        // Disable screenshots if not allowed.
        if (!allowScreenshots)
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        // Run the default commands.
        super.onCreate(savedInstanceState)

        // Set the content view.
        if (bottomAppBar)
            setContentView(R.layout.logcat_bottom_appbar)
        else
            setContentView(R.layout.logcat_top_appbar)

        // Get handles for the views.
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        swipeRefreshLayout = findViewById(R.id.swiperefreshlayout)
        logcatWebView = findViewById(R.id.logcat_webview)

        // Set the toolbar as the action bar.
        setSupportActionBar(toolbar)

        // Get a handle for the action bar.
        val actionBar = supportActionBar!!

        // Display the back arrow in the action bar.
        actionBar.setDisplayHomeAsUpEnabled(true)

        // Implement swipe to refresh.
        swipeRefreshLayout.setOnRefreshListener {
            // Populate the current logcat.
            populateLogcat()
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

        // Restore the WebView scroll position if the activity has been restarted.
        if (savedInstanceState != null)
            logcatWebView.scrollY = savedInstanceState.getInt(SCROLL_Y)

        // Allow loading of file:// URLs.
        logcatWebView.settings.allowFileAccess = true

        // Populate the logcat.
        populateLogcat()
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
                val logcatClipData = ClipData.newPlainText(getString(R.string.logcat), logcatPlainTextStringBuilder)

                // Place the clip data on the clipboard.
                clipboardManager.setPrimaryClip(logcatClipData)

                // Display a snackbar if the API <= 32 (Android 12L).  Beginning in Android 13 the OS displays a notification that covers up the snackbar.
                if (Build.VERSION.SDK_INT <= 32)
                    Snackbar.make(logcatWebView, R.string.logcat_copied, Snackbar.LENGTH_SHORT).show()

                // Consume the event.
                true
            }

            R.id.save -> {  // Save was selected.
                // Open the file picker.
                saveLogcatActivityResultLauncher.launch(getString(R.string.privacy_browser_logcat_txt, BuildConfig.VERSION_NAME))

                // Consume the event.
                true
            }

            R.id.clear -> {  // Clear was selected.
                try {
                    // Clear the logcat.  `-c` clears the logcat.  `-b all` clears all the buffers (instead of just crash, main, and system).
                    val process = Runtime.getRuntime().exec("logcat -b all -c")

                    // Wait for the process to finish.
                    process.waitFor()

                    // Reload the logcat.
                    populateLogcat()
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

        // Store the scroll Y position in the bundle.
        savedInstanceState.putInt(SCROLL_Y, logcatWebView.scrollY)
    }

    private fun populateLogcat() {
        try {
            // Get the logcat.  `-b all` gets all the buffers (instead of just crash, main, and system).  `-v long` produces more complete information.  `-d` dumps the logcat and exits.
            val getLogcatProcess = Runtime.getRuntime().exec("logcat -b all -v long -d")

            // Wrap the logcat in a buffered reader.
            val logcatBufferedReader = BufferedReader(InputStreamReader(getLogcatProcess.inputStream))

            // Reset the logcat plain text string.
            logcatPlainTextStringBuilder = StringBuilder()

            // Create a logcat HTML string builder.
            val logcatHtmlStringBuilder = StringBuilder()

            // Populate the initial HTML.
            logcatHtmlStringBuilder.append("<html>")
            logcatHtmlStringBuilder.append("<head>")
            logcatHtmlStringBuilder.append("<style>")

            // Set the word break so that lines never exceed the width of the screen.
            logcatHtmlStringBuilder.append("body { word-break: break-word; }")

            // Set the colors.
            logcatHtmlStringBuilder.append("@media (prefers-color-scheme: dark) { body { color: #C1C1C1;  /* Gray 350 */ background-color: #303030;  /* Gray 860 */ } }")
            logcatHtmlStringBuilder.append("span.header { color: #0D47A1;  /* Blue 900 */ } @media (prefers-color-scheme: dark) { span.header { color: #8AB4F8;  /* Violet 500 */ } }")
            logcatHtmlStringBuilder.append("strong.crash { color: #B71C1C;  /* Red 900. */ } @media (prefers-color-scheme: dark) { strong.crash { color: #E24B4C;  /* Red Night. */ } }")
            logcatHtmlStringBuilder.append("span.crash { color: #EF5350;  /* Red 400. */ } @media (prefers-color-scheme: dark) { span.crash { color: #EF9A9A;  /* Red Night. */ } }")

            // Close the style tag.
            logcatHtmlStringBuilder.append("</style>")

            // Respect dark mode.
            logcatHtmlStringBuilder.append("<meta name=\"color-scheme\" content=\"light dark\">")

            // Start the HTML body.
            logcatHtmlStringBuilder.append("</head>")
            logcatHtmlStringBuilder.append("<body>")

            // Create a logcat line string.
            var logcatLineString: String?

            while (logcatBufferedReader.readLine().also { logcatLineString = it } != null) {
                // Populate the logcat plain text string builder.
                logcatPlainTextStringBuilder.append(logcatLineString)

                // Add a line break.
                logcatPlainTextStringBuilder.append("\n")

                // Trim the string, which is necessary for correct detection of lines that start with `at`.
                logcatLineString = logcatLineString!!.trim()

                // Apply syntax highlighting to the logcat.
                if (logcatLineString!!.contains("crash") || logcatLineString!!.contains("Exception") ) {  // Colorize crashes.
                    logcatHtmlStringBuilder.append("<strong class=\"crash\">")
                    logcatHtmlStringBuilder.append(logcatLineString)
                    logcatHtmlStringBuilder.append("</strong>")
                } else if (logcatLineString!!.startsWith("at") || logcatLineString!!.startsWith("Process:") || logcatLineString!!.contains("FATAL")) {  // Colorize lines relating to crashes.
                    logcatHtmlStringBuilder.append("<span class=\"crash\">")
                    logcatHtmlStringBuilder.append(logcatLineString)
                    logcatHtmlStringBuilder.append("</span>")
                } else if (logcatLineString!!.startsWith("-")) {  // Colorize the headers.
                    logcatHtmlStringBuilder.append("<span class=\"header\">")
                    logcatHtmlStringBuilder.append(logcatLineString)
                    logcatHtmlStringBuilder.append("</span>")
                } else if (logcatLineString!!.startsWith("[ ")) {  // Colorize the time stamps.
                    logcatHtmlStringBuilder.append("<span style=color:gray>")
                    logcatHtmlStringBuilder.append(logcatLineString)
                    logcatHtmlStringBuilder.append("</span>")
                } else {  // Display the standard lines.
                    logcatHtmlStringBuilder.append(logcatLineString)
                }

                // Add a line break.
                logcatHtmlStringBuilder.append("<br>")
            }

            // Close the HTML.
            logcatHtmlStringBuilder.append("</body>")
            logcatHtmlStringBuilder.append("</html>")

            // Encode the logcat HTML.
            val base64EncodedLogcatHtml: String = Base64.encodeToString(logcatHtmlStringBuilder.toString().toByteArray(Charsets.UTF_8), Base64.NO_PADDING)

            // Load the encoded logcat.
            logcatWebView.loadData(base64EncodedLogcatHtml, "text/html", "base64")

            // Close the buffered reader.
            logcatBufferedReader.close()
        } catch (exception: IOException) {
            // Do nothing.
        }

        // Stop the swipe to refresh animation if it is displayed.
        swipeRefreshLayout.isRefreshing = false
    }
}
