/*
 * Copyright © 2018-2021 Soren Stoutner <soren@stoutner.com>.
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
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.WindowManager

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.helpers.AdConsentDatabaseHelper
import com.stoutner.privacybrowser.helpers.AdHelper
import kotlin.system.exitProcess

class AdConsentDialog : DialogFragment() {
    // Declare the class variables.
    private lateinit var adConsentDatabaseHelper: AdConsentDatabaseHelper

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use a builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.PrivacyBrowserAlertDialog)

        // Set the icon according to the theme.
        dialogBuilder.setIconAttribute(R.attr.blockAdsBlueIcon)

        // Initialize the bookmarks database helper.
        adConsentDatabaseHelper = AdConsentDatabaseHelper(requireContext())

        // Set the title.
        dialogBuilder.setTitle(R.string.ad_consent)

        // Set the text.
        dialogBuilder.setMessage(R.string.ad_consent_text)

        // Set the close browser button.
        dialogBuilder.setNegativeButton(R.string.close_browser) { _: DialogInterface?, _: Int ->
            // Update the ad consent database.
            adConsentDatabaseHelper.updateAdConsent(false)

            // Close the browser.  `finishAndRemoveTask` also removes Privacy Browser from the recent app list.
            if (Build.VERSION.SDK_INT >= 21) {
                requireActivity().finishAndRemoveTask()
            } else {
                requireActivity().finish()
            }

            // Remove the terminated program from RAM.  The status code is `0`.
            exitProcess(0)
        }

        // Set the accept ads button.
        dialogBuilder.setPositiveButton(R.string.accept_ads) { _: DialogInterface?, _: Int ->
            // Update the ad consent database.
            adConsentDatabaseHelper.updateAdConsent(true)

            // Load an ad.
            AdHelper.loadAd(requireActivity().findViewById(R.id.adview), requireContext(), requireActivity(), getString(R.string.ad_unit_id))
        }

        // Create an alert dialog from the alert dialog builder.
        val alertDialog = dialogBuilder.create()

        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        // Get the screenshot preference.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)

        // Disable screenshots if not allowed.
        if (!allowScreenshots) {
            // Disable screenshots.
            alertDialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // Return the alert dialog.
        return alertDialog
    }

    // Close Privacy Browser Free if the dialog is cancelled without selecting a button (by tapping on the background).
    override fun onCancel(dialogInterface: DialogInterface) {
        // Update the ad consent database.
        adConsentDatabaseHelper.updateAdConsent(false)

        // Close the browser.  `finishAndRemoveTask()` also removes Privacy Browser from the recent app list.
        if (Build.VERSION.SDK_INT >= 21) {
            requireActivity().finishAndRemoveTask()
        } else {
            requireActivity().finish()
        }

        // Remove the terminated program from RAM.  The status code is `0`.
        exitProcess(0)
    }
}