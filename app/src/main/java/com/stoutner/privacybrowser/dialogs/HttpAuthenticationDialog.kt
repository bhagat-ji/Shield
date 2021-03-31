/*
 * Copyright © 2017-2021 Soren Stoutner <soren@stoutner.com>.
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

package com.stoutner.privacybrowser.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.HttpAuthHandler
import android.widget.EditText
import android.widget.TextView

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.activities.MainWebViewActivity
import com.stoutner.privacybrowser.views.NestedScrollWebView

// Define the class constants.
private const val HOST = "host"
private const val REALM = "realm"
private const val WEBVIEW_FRAGMENT_ID = "webview_fragment_id"

class HttpAuthenticationDialog : DialogFragment() {
    // Define the class variables.
    private var dismissDialog: Boolean = false

    // Declare the class views.
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText

    companion object {
        // `@JvmStatic` will no longer be required once all the code has transitioned to Kotlin.
        @JvmStatic
        fun displayDialog(host: String, realm: String, webViewFragmentId: Long): HttpAuthenticationDialog {
            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the variables in the bundle.
            argumentsBundle.putString(HOST, host)
            argumentsBundle.putString(REALM, realm)
            argumentsBundle.putLong(WEBVIEW_FRAGMENT_ID, webViewFragmentId)

            // Create a new instance of the HTTP authentication dialog.
            val httpAuthenticationDialog = HttpAuthenticationDialog()

            // Add the arguments bundle to the dialog.
            httpAuthenticationDialog.arguments = argumentsBundle

            // Return the new dialog.
            return httpAuthenticationDialog
        }
    }

    // `@SuppressLint("InflateParams")` removes the warning about using `null` as the parent view group when inflating the `AlertDialog`.
    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get a handle for the arguments.
        val arguments = requireArguments()

        // Get the variables from the bundle.
        val httpAuthHost = arguments.getString(HOST)
        val httpAuthRealm = arguments.getString(REALM)
        val webViewFragmentId = arguments.getLong(WEBVIEW_FRAGMENT_ID)

        // Try to populate the alert dialog.
        try {  // Getting the WebView tab fragment will fail if Privacy Browser has been restarted.
            // Get the current position of this WebView fragment.
            val webViewPosition = MainWebViewActivity.webViewPagerAdapter.getPositionForId(webViewFragmentId)

            // Get the WebView tab fragment.
            val webViewTabFragment = MainWebViewActivity.webViewPagerAdapter.getPageFragment(webViewPosition)

            // Get the fragment view.
            val fragmentView = webViewTabFragment.requireView()

            // Get a handle for the current WebView.
            val nestedScrollWebView = fragmentView.findViewById<NestedScrollWebView>(R.id.nestedscroll_webview)

            // Get a handle for the HTTP authentication handler.
            val httpAuthHandler = nestedScrollWebView.httpAuthHandler

            // Use an alert dialog builder to create the alert dialog.
            val dialogBuilder = AlertDialog.Builder(requireActivity(), R.style.PrivacyBrowserAlertDialog)

            // Set the icon according to the theme.
            dialogBuilder.setIconAttribute(R.attr.lockBlueIcon)

            // Set the title.
            dialogBuilder.setTitle(R.string.http_authentication)

            // Set the view.  The parent view is `null` because it will be assigned by the alert dialog.
            dialogBuilder.setView(layoutInflater.inflate(R.layout.http_authentication_dialog, null))

            // Set the close button listener.
            dialogBuilder.setNegativeButton(R.string.close) { _: DialogInterface?, _: Int ->
                // Cancel the HTTP authentication request.
                httpAuthHandler.cancel()

                // Reset the HTTP authentication handler.
                nestedScrollWebView.resetHttpAuthHandler()
            }// Set the proceed button listener.
            dialogBuilder.setPositiveButton(R.string.proceed) { _: DialogInterface?, _: Int ->
                // Send the login information
                login(httpAuthHandler)

                // Reset the HTTP authentication handler.
                nestedScrollWebView.resetHttpAuthHandler()
            }

            // Create an alert dialog from the alert dialog builder.
            val alertDialog = dialogBuilder.create()

            // Get the alert dialog window.
            val dialogWindow = alertDialog.window!!

            // Get a handle for the shared preferences.
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

            // Get the screenshot preference.
            val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)

            // Disable screenshots if not allowed.
            if (!allowScreenshots) {
                alertDialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            // Display the keyboard.
            dialogWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

            // The alert dialog needs to be shown before the contents can be modified.
            alertDialog.show()

            // Get handles for the views.
            val realmTextView = alertDialog.findViewById<TextView>(R.id.http_authentication_realm)!!
            val hostTextView = alertDialog.findViewById<TextView>(R.id.http_authentication_host)!!
            usernameEditText = alertDialog.findViewById(R.id.http_authentication_username)!!
            passwordEditText = alertDialog.findViewById(R.id.http_authentication_password)!!

            // Set the realm text.
            realmTextView.text = httpAuthRealm

            // Initialize the host label and the spannable string builder.
            val hostLabel = getString(R.string.host) + "  "
            val hostStringBuilder = SpannableStringBuilder(hostLabel + httpAuthHost)

            // Get the current theme status.
            val currentThemeStatus = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

            // Create a blue foreground color span.
            val blueColorSpan: ForegroundColorSpan

            // Set the blue color span according to the theme.  The deprecated `getColor()` must be used until API >= 23.
            blueColorSpan = if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {
                @Suppress("DEPRECATION")
                ForegroundColorSpan(resources.getColor(R.color.blue_700))
            } else {
                @Suppress("DEPRECATION")
                ForegroundColorSpan(resources.getColor(R.color.violet_700))
            }

            // Setup the span to display the host name in blue.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
            hostStringBuilder.setSpan(blueColorSpan, hostLabel.length, hostStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

            // Set the host text.
            hostTextView.text = hostStringBuilder

            // Allow the enter key on the keyboard to send the login information from the username edit text.
            usernameEditText.setOnKeyListener { _: View?, keyCode: Int, event: KeyEvent ->
                // Check the key code and event.
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {  // The enter key was pressed.
                    // Send the login information.
                    login(httpAuthHandler)

                    // Manually dismiss the alert dialog.
                    alertDialog.dismiss()

                    // Consume the event.
                    return@setOnKeyListener true
                } else {  // If any other key was pressed, do not consume the event.
                    return@setOnKeyListener false
                }
            }

            // Allow the enter key on the keyboard to send the login information from the password edit text.
            passwordEditText.setOnKeyListener { _: View?, keyCode: Int, event: KeyEvent ->
                // Check the key code and event.
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {  // The enter key was pressed.
                    // Send the login information.
                    login(httpAuthHandler)

                    // Manually dismiss the alert dialog.
                    alertDialog.dismiss()

                    // Consume the event.
                    return@setOnKeyListener true
                } else {  // If any other key was pressed, do not consume the event.
                    return@setOnKeyListener false
                }
            }

            // Return the alert dialog.
            return alertDialog
        } catch (exception: Exception) {  // Privacy Browser was restarted and the HTTP auth handler no longer exists.
            // Use an alert dialog builder to create an empty alert dialog.
            val dialogBuilder = AlertDialog.Builder(requireActivity(), R.style.PrivacyBrowserAlertDialog)

            // Create an empty alert dialog from the alert dialog builder.
            val alertDialog = dialogBuilder.create()

            // Set the flag to dismiss the dialog as soon as it is resumed.
            dismissDialog = true

            // Return the alert dialog.
            return alertDialog
        }
    }

    override fun onResume() {
        // Run the default command.
        super.onResume()

        // Dismiss the alert dialog if the activity was restarted and the HTTP auth handler no longer exists.
        if (dismissDialog) {
            dialog!!.dismiss()
        }
    }

    private fun login(httpAuthHandler: HttpAuthHandler) {
        // Send the login information.
        httpAuthHandler.proceed(usernameEditText.text.toString(), passwordEditText.text.toString())
    }
}