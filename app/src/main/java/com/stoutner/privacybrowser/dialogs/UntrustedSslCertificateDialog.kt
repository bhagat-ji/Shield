/*
 * Copyright © 2021 Soren Stoutner <soren@stoutner.com>.
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

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.WindowManager

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.stoutner.privacybrowser.R

class UntrustedSslCertificateDialog : DialogFragment() {
    // Declare the class variables.
    private lateinit var untrustedSslCertificateListener: UntrustedSslCertificateListener
    private var dismissDialog: Boolean = false

    // The public interface is used to send information back to the parent activity.
    interface UntrustedSslCertificateListener {
        fun loadAnyway()
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Get a handle for the listener form the launching context.
        untrustedSslCertificateListener = context as UntrustedSslCertificateListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Check to see if the app has been restarted.
        if (savedInstanceState == null) {  // The app has not been restarted.
            // Use a builder to create the alert dialog.
            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(requireContext(), R.style.PrivacyBrowserAlertDialog)

            // Set the icon according to the theme.
            dialogBuilder.setIconAttribute(R.attr.sslCertificateBlueIcon)

            // Set the title.
            dialogBuilder.setTitle(R.string.ssl_certificate_error)

            // Set the text.
            dialogBuilder.setMessage(R.string.untrusted_ssl_certificate)

            // Set the cancel button listener.  Using `null` as the listener closes the dialog without doing anything else.
            dialogBuilder.setNegativeButton(R.string.cancel, null)

            // Set the load anyway button listener.
            dialogBuilder.setPositiveButton(R.string.load_anyway) { _: DialogInterface, _: Int ->
                // Instruct the parent activity to load the URL anyway.
                untrustedSslCertificateListener.loadAnyway()
            }

            // Create an alert dialog from the builder.
            val alertDialog = dialogBuilder.create()

            // Get a handle for the shared preferences.
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

            // Get the screenshot preference.
            val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots), false)

            // Disable screenshots if not allowed.
            if (!allowScreenshots) {
                alertDialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            // Return the alert dialog.
            return alertDialog
        } else {  // The app has been restarted.  Close the dialog as a new one will automatically be created by GetSourceBackgroundTask.
            // Use an alert dialog builder to create an empty alert dialog.
            val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.PrivacyBrowserAlertDialog)

            // Create an empty alert dialog from the alert dialog builder.
            val alertDialog = dialogBuilder.create()

            // Set the flag to dismiss the dialog as soon as it is resumed.
            dismissDialog = true

            // Return the alert dialog.
            return alertDialog
        }
    }

    override fun onResume() {
        // Run the default commands.
        super.onResume()

        // Dismiss the alert dialog if the activity was restarted as a new one will automatically be created by GetSourceBackgroundTask.
        if (dismissDialog) {
            dialog!!.dismiss()
        }
    }
}