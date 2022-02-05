/*
 * Copyright © 2019-2022 Soren Stoutner <soren@stoutner.com>.
 *
 * This file is part of Privacy Browser Android <https://www.stoutner.com/privacy-browser-android>.
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

package com.stoutner.privacybrowser.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.helpers.ProxyHelper

// Define the class constants.
private const val PROXY_MODE = "proxy_mode"

class ProxyNotInstalledDialog : DialogFragment() {
    companion object {
        // `@JvmStatic` will no longer be required once all the code has transitioned to Kotlin.
        @JvmStatic
        fun displayDialog(proxyMode: String): ProxyNotInstalledDialog {
            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the proxy mode in the bundle.
            argumentsBundle.putString(PROXY_MODE, proxyMode)

            // Create a new instance of the dialog.
            val proxyNotInstalledDialog = ProxyNotInstalledDialog()

            // Add the bundle to the dialog.
            proxyNotInstalledDialog.arguments = argumentsBundle

            // Return the new dialog.
            return proxyNotInstalledDialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get the proxy mode from the arguments.
        val proxyMode = requireArguments().getString(PROXY_MODE)!!

        // Use a builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.PrivacyBrowserAlertDialog)

        // Set the icon according to the theme.
        dialogBuilder.setIconAttribute(R.attr.proxyBlueIcon)

        // Set the title and the message according to the proxy mode.
        when (proxyMode) {
            ProxyHelper.TOR -> {
                // Set the title.
                dialogBuilder.setTitle(R.string.orbot_not_installed_title)

                // Set the message.
                dialogBuilder.setMessage(R.string.orbot_not_installed_message)
            }

            ProxyHelper.I2P -> {
                // Set the title.
                dialogBuilder.setTitle(R.string.i2p_not_installed_title)

                // Set the message.
                dialogBuilder.setMessage(R.string.i2p_not_installed_message)
            }
        }

        // Set the close button listener.  Using `null` as the listener closes the dialog without doing anything else.
        dialogBuilder.setPositiveButton(R.string.close, null)

        // Create an alert dialog from the alert dialog builder.
        val alertDialog = dialogBuilder.create()

        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Get the screenshot preference.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)

        // Disable screenshots if not allowed.
        if (!allowScreenshots) {
            alertDialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // Return the alert dialog.
        return alertDialog
    }
}