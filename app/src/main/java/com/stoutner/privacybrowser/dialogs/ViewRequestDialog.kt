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
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.helpers.BlocklistHelper

// Define the class constants.
private const val ID = "id"
private const val IS_LAST_REQUEST = "is_last_request"
private const val REQUEST_DETAILS = "request_details"

class ViewRequestDialog : DialogFragment() {
    // Define the class variables.
    private lateinit var viewRequestListener: ViewRequestListener

    // The public interface is used to send information back to the parent activity.
    interface ViewRequestListener {
        // Show the previous request.
        fun onPrevious(currentId: Int)

        // Show the next request.
        fun onNext(currentId: Int)
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Get a handle for the listener from the launching context.
        viewRequestListener = context as ViewRequestListener
    }

    companion object {
        // `@JvmStatic` will no longer be required once all the code has transitioned to Kotlin.
        @JvmStatic
        fun request(id: Int, isLastRequest: Boolean, requestDetails: Array<String>): ViewRequestDialog {
            // Create a bundle.
            val bundle = Bundle()

            // Store the request details.
            bundle.putInt(ID, id)
            bundle.putBoolean(IS_LAST_REQUEST, isLastRequest)
            bundle.putStringArray(REQUEST_DETAILS, requestDetails)

            // Create a new instance of the view request dialog.
            val viewRequestDialog = ViewRequestDialog()

            // Add the arguments to the new dialog.
            viewRequestDialog.arguments = bundle

            // Return the new dialog.
            return viewRequestDialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get the arguments from the bundle.
        val id = requireArguments().getInt(ID)
        val isLastRequest = requireArguments().getBoolean(IS_LAST_REQUEST)
        val requestDetails = requireArguments().getStringArray(REQUEST_DETAILS)!!

        // Use an alert dialog builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.PrivacyBrowserAlertDialog)

        // Get the current theme status.
        val currentThemeStatus = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        // Set the icon according to the theme.
        dialogBuilder.setIconAttribute(R.attr.blockAdsBlueIcon)

        // Set the title.
        dialogBuilder.setTitle(resources.getString(R.string.request_details) + " - " + id)

        // Set the view.
        dialogBuilder.setView(R.layout.view_request_dialog)

        // Set the close button.  Using `null` as the listener closes the dialog without doing anything else.
        dialogBuilder.setNeutralButton(R.string.close, null)

        // Set the previous button.
        dialogBuilder.setNegativeButton(R.string.previous) { _: DialogInterface?, _: Int ->
            // Load the previous request.
            viewRequestListener.onPrevious(id)
        }

        // Set the next button.
        dialogBuilder.setPositiveButton(R.string.next) { _: DialogInterface?, _: Int ->
            // Load the next request.
            viewRequestListener.onNext(id)
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

        //The alert dialog must be shown before the contents can be modified.
        alertDialog.show()

        // Get handles for the dialog views.
        val requestDisposition = alertDialog.findViewById<TextView>(R.id.request_disposition)!!
        val requestUrl = alertDialog.findViewById<TextView>(R.id.request_url)!!
        val requestBlockListLabel = alertDialog.findViewById<TextView>(R.id.request_blocklist_label)!!
        val requestBlockList = alertDialog.findViewById<TextView>(R.id.request_blocklist)!!
        val requestSubListLabel = alertDialog.findViewById<TextView>(R.id.request_sublist_label)!!
        val requestSubList = alertDialog.findViewById<TextView>(R.id.request_sublist)!!
        val requestBlockListEntriesLabel = alertDialog.findViewById<TextView>(R.id.request_blocklist_entries_label)!!
        val requestBlockListEntries = alertDialog.findViewById<TextView>(R.id.request_blocklist_entries)!!
        val requestBlockListOriginalEntryLabel = alertDialog.findViewById<TextView>(R.id.request_blocklist_original_entry_label)!!
        val requestBlockListOriginalEntry = alertDialog.findViewById<TextView>(R.id.request_blocklist_original_entry)!!
        val previousButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        val nextButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)

        // Disable the previous button if the first resource request is displayed.
        previousButton.isEnabled = (id != 1)

        // Disable the next button if the last resource request is displayed.
        nextButton.isEnabled = !isLastRequest

        // Set the request action text.
        when (requestDetails[BlocklistHelper.REQUEST_DISPOSITION]) {
            BlocklistHelper.REQUEST_DEFAULT -> {
                // Set the text.
                requestDisposition.setText(R.string.default_allowed)

                // Set the background color.  The deprecated `getColor()` must be used until the minimum API >= 23.
                @Suppress("DEPRECATION")
                requestDisposition.setBackgroundColor(resources.getColor(R.color.transparent))
            }

            BlocklistHelper.REQUEST_ALLOWED -> {
                // Set the text.
                requestDisposition.setText(R.string.allowed)

                // Set the background color according to the theme.  The deprecated `getColor()` must be used until the minimum API >= 23.
                if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {
                    @Suppress("DEPRECATION")
                    requestDisposition.setBackgroundColor(resources.getColor(R.color.blue_100))
                } else {
                    @Suppress("DEPRECATION")
                    requestDisposition.setBackgroundColor(resources.getColor(R.color.blue_700_50))
                }
            }

            BlocklistHelper.REQUEST_THIRD_PARTY -> {
                // Set the text.
                requestDisposition.setText(R.string.third_party_blocked)

                // Set the background color according to the theme.  The deprecated `getColor()` must be used until the minimum API >= 23.
                if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {
                    @Suppress("DEPRECATION")
                    requestDisposition.setBackgroundColor(resources.getColor(R.color.yellow_100))
                } else {
                    @Suppress("DEPRECATION")
                    requestDisposition.setBackgroundColor(resources.getColor(R.color.yellow_700_50))
                }
            }
            BlocklistHelper.REQUEST_BLOCKED -> {
                // Set the text.
                requestDisposition.setText(R.string.blocked)

                // Set the background color according to the theme.  The deprecated `getColor()` must be used until the minimum API >= 23.
                if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {
                    @Suppress("DEPRECATION")
                    requestDisposition.setBackgroundColor(resources.getColor(R.color.red_100))
                } else {
                    @Suppress("DEPRECATION")
                    requestDisposition.setBackgroundColor(resources.getColor(R.color.red_700_40))
                }
            }
        }

        // Display the request URL.
        requestUrl.text = requestDetails[BlocklistHelper.REQUEST_URL]

        // Modify the dialog based on the request action.
        if (requestDetails.size == 2) {  // A default request.
            // Hide the unused views.
            requestBlockListLabel.visibility = View.GONE
            requestBlockList.visibility = View.GONE
            requestSubListLabel.visibility = View.GONE
            requestSubList.visibility = View.GONE
            requestBlockListEntriesLabel.visibility = View.GONE
            requestBlockListEntries.visibility = View.GONE
            requestBlockListOriginalEntryLabel.visibility = View.GONE
            requestBlockListOriginalEntry.visibility = View.GONE
        } else {  // A blocked or allowed request.
            // Set the text on the text views.
            requestBlockList.text = requestDetails[BlocklistHelper.REQUEST_BLOCKLIST]
            requestBlockListEntries.text = requestDetails[BlocklistHelper.REQUEST_BLOCKLIST_ENTRIES]
            requestBlockListOriginalEntry.text = requestDetails[BlocklistHelper.REQUEST_BLOCKLIST_ORIGINAL_ENTRY]
            when (requestDetails[BlocklistHelper.REQUEST_SUBLIST]) {
                BlocklistHelper.MAIN_WHITELIST -> requestSubList.setText(R.string.main_whitelist)
                BlocklistHelper.FINAL_WHITELIST -> requestSubList.setText(R.string.final_whitelist)
                BlocklistHelper.DOMAIN_WHITELIST -> requestSubList.setText(R.string.domain_whitelist)
                BlocklistHelper.DOMAIN_INITIAL_WHITELIST -> requestSubList.setText(R.string.domain_initial_whitelist)
                BlocklistHelper.DOMAIN_FINAL_WHITELIST -> requestSubList.setText(R.string.domain_final_whitelist)
                BlocklistHelper.THIRD_PARTY_WHITELIST -> requestSubList.setText(R.string.third_party_whitelist)
                BlocklistHelper.THIRD_PARTY_DOMAIN_WHITELIST -> requestSubList.setText(R.string.third_party_domain_whitelist)
                BlocklistHelper.THIRD_PARTY_DOMAIN_INITIAL_WHITELIST -> requestSubList.setText(R.string.third_party_domain_initial_whitelist)
                BlocklistHelper.MAIN_BLACKLIST -> requestSubList.setText(R.string.main_blacklist)
                BlocklistHelper.INITIAL_BLACKLIST -> requestSubList.setText(R.string.initial_blacklist)
                BlocklistHelper.FINAL_BLACKLIST -> requestSubList.setText(R.string.final_blacklist)
                BlocklistHelper.DOMAIN_BLACKLIST -> requestSubList.setText(R.string.domain_blacklist)
                BlocklistHelper.DOMAIN_INITIAL_BLACKLIST -> requestSubList.setText(R.string.domain_initial_blacklist)
                BlocklistHelper.DOMAIN_FINAL_BLACKLIST -> requestSubList.setText(R.string.domain_final_blacklist)
                BlocklistHelper.DOMAIN_REGULAR_EXPRESSION_BLACKLIST -> requestSubList.setText(R.string.domain_regular_expression_blacklist)
                BlocklistHelper.THIRD_PARTY_BLACKLIST -> requestSubList.setText(R.string.third_party_blacklist)
                BlocklistHelper.THIRD_PARTY_INITIAL_BLACKLIST -> requestSubList.setText(R.string.third_party_initial_blacklist)
                BlocklistHelper.THIRD_PARTY_DOMAIN_BLACKLIST -> requestSubList.setText(R.string.third_party_domain_blacklist)
                BlocklistHelper.THIRD_PARTY_DOMAIN_INITIAL_BLACKLIST -> requestSubList.setText(R.string.third_party_domain_initial_blacklist)
                BlocklistHelper.THIRD_PARTY_REGULAR_EXPRESSION_BLACKLIST -> requestSubList.setText(R.string.third_party_regular_expression_blacklist)
                BlocklistHelper.THIRD_PARTY_DOMAIN_REGULAR_EXPRESSION_BLACKLIST -> requestSubList.setText(R.string.third_party_domain_regular_expression_blacklist)
                BlocklistHelper.REGULAR_EXPRESSION_BLACKLIST -> requestSubList.setText(R.string.regular_expression_blacklist)
            }
        }

        // Return the alert dialog.
        return alertDialog
    }
}