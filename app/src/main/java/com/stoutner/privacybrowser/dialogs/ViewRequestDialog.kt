/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2018-2023, 2025-2026 Soren Stoutner <soren@stoutner.com>
 *
 * This file is part of Privacy Browser Android <https://www.stoutner.com/privacy-browser-android/>.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.stoutner.privacybrowser.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView

import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.dataclasses.FilterList
import com.stoutner.privacybrowser.dataclasses.FilterOptionDisposition
import com.stoutner.privacybrowser.dataclasses.MatchedUrlType
import com.stoutner.privacybrowser.dataclasses.RequestDataClass
import com.stoutner.privacybrowser.dataclasses.RequestDisposition
import com.stoutner.privacybrowser.dataclasses.Sublist

// Define the class constants.
private const val ID = "id"
private const val IS_LAST_REQUEST = "is_last_request"
private const val REQUEST_DATA_CLASS = "request_data_class"

// Define the private variables.
private var blueColor = 0
private var redColor = 0

class ViewRequestDialog : DialogFragment() {
    companion object {
        fun request(id: Int, isLastRequest: Boolean, requestDataClass: RequestDataClass): ViewRequestDialog {
            // Create a bundle.
            val bundle = Bundle()

            // Store the request details.
            bundle.putInt(ID, id)
            bundle.putBoolean(IS_LAST_REQUEST, isLastRequest)
            bundle.putParcelable(REQUEST_DATA_CLASS, requestDataClass)

            // Create a new instance of the view request dialog.
            val viewRequestDialog = ViewRequestDialog()

            // Add the arguments to the new dialog.
            viewRequestDialog.arguments = bundle

            // Return the new dialog.
            return viewRequestDialog
        }
    }

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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get the arguments from the bundle.
        // The deprecated `getParcelable()` method can be updated once the minimum API >= 33.
        val id = requireArguments().getInt(ID)
        val isLastRequest = requireArguments().getBoolean(IS_LAST_REQUEST)
        @Suppress("DEPRECATION") val requestDataClass = requireArguments().getParcelable<RequestDataClass>(REQUEST_DATA_CLASS)!!

        // Use an alert dialog builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.PrivacyBrowserAlertDialog)

        // Set the icon.
        dialogBuilder.setIcon(R.drawable.block_ads_enabled)

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
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Get the screenshot preference.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)

        // Disable screenshots if not allowed.
        if (!allowScreenshots)
            alertDialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        // The alert dialog must be shown before the contents can be modified.
        alertDialog.show()

        // Get handles for the dialog views.
        val requestDispositionTextView = alertDialog.findViewById<TextView>(R.id.request_disposition_textview)!!
        val webpageUrlTextView = alertDialog.findViewById<TextView>(R.id.webpage_url_textview)!!
        val requestUrlTextView = alertDialog.findViewById<TextView>(R.id.request_url_textview)!!
        val requestUrlWithSeparatorsTextView = alertDialog.findViewById<TextView>(R.id.request_url_with_separators_textview)!!
        val truncatedRequestUrlTextView = alertDialog.findViewById<TextView>(R.id.truncated_request_url_textview)!!
        val truncatedRequestUrlWithSeparatorsTextView = alertDialog.findViewById<TextView>(R.id.truncated_request_url_with_separators_textview)!!
        val thirdPartyRequestTextView = alertDialog.findViewById<TextView>(R.id.third_party_request_textview)!!
        val filterListEntryTextView = alertDialog.findViewById<TextView>(R.id.filterlist_entry_textview)!!
        val filterListLabelTextView = alertDialog.findViewById<TextView>(R.id.filterlist_label_textview)!!
        val filterListTextView = alertDialog.findViewById<TextView>(R.id.filterlist_textview)!!
        val sublistLabelTextView = alertDialog.findViewById<TextView>(R.id.sublist_label_textview)!!
        val sublistTextView = alertDialog.findViewById<TextView>(R.id.sublist_textview)!!
        val appliedEntryListLabelTextView = alertDialog.findViewById<TextView>(R.id.applied_entry_list_label_textview)!!
        val appliedEntryListTextView = alertDialog.findViewById<TextView>(R.id.applied_entry_list_textview)!!
        val domainLabelTextView = alertDialog.findViewById<TextView>(R.id.domain_label_textview)!!
        val domainTextView = alertDialog.findViewById<TextView>(R.id.domain_textview)!!
        val domainListLabelTextView = alertDialog.findViewById<TextView>(R.id.domain_list_label_textview)!!
        val domainListTextView = alertDialog.findViewById<TextView>(R.id.domain_list_textview)!!
        val thirdPartyFilterListEntryLabelTextView = alertDialog.findViewById<TextView>(R.id.third_party_filter_list_entry_label_textview)!!
        val thirdPartyFilterListEntryTextView = alertDialog.findViewById<TextView>(R.id.third_party_filter_list_entry_textview)!!
        val initialMatchLabelTextView = alertDialog.findViewById<TextView>(R.id.initial_match_label_textview)!!
        val initialMatchTextView = alertDialog.findViewById<TextView>(R.id.initial_match_textview)!!
        val finalMatchLabelTextView = alertDialog.findViewById<TextView>(R.id.final_match_label_textview)!!
        val finalMatchTextView = alertDialog.findViewById<TextView>(R.id.final_match_textview)!!
        val appliedFilterOptionsLabelTextView = alertDialog.findViewById<TextView>(R.id.applied_filter_options_label_textview)!!
        val appliedFilterOptionsTextView = alertDialog.findViewById<TextView>(R.id.applied_filter_options_textview)!!
        val originalFilterOptionsLabelTextView = alertDialog.findViewById<TextView>(R.id.original_filter_options_label_textview)!!
        val originalFilterOptionsTextView = alertDialog.findViewById<TextView>(R.id.original_filter_options_textview)!!
        val originalEntryLabelTextView = alertDialog.findViewById<TextView>(R.id.original_entry_label_textview)!!
        val originalEntryTextView = alertDialog.findViewById<TextView>(R.id.original_entry_textview)!!
        val previousButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        val nextButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)

        // Disable the previous button if the first resource request is displayed.
        previousButton.isEnabled = (id != 1)

        // Disable the next button if the last resource request is displayed.
        nextButton.isEnabled = !isLastRequest

        // Get handles for the colors.
        blueColor = getColor(requireContext(), R.color.requests_blue_background)
        redColor = getColor(requireContext(), R.color.red_background)
        val yellowColor = getColor(requireContext(), R.color.yellow_background)

        // Set the request disposition text and associated backgrounds.
        when (requestDataClass.disposition) {
            RequestDisposition.Default -> {
                // Set the request disposition text.
                requestDispositionTextView.setText(R.string.default_allowed)

                // Set the request disposition background color to be transparent.
                requestDispositionTextView.setBackgroundColor(getColor(requireContext(), R.color.transparent))
            }

            RequestDisposition.Allowed -> {
                // Set the request disposition text.
                requestDispositionTextView.setText(R.string.allowed)

                // Set the request disposition background color to be blue.
                requestDispositionTextView.setBackgroundColor(blueColor)

                // Set the matched URL background color.
                when (requestDataClass.matchedUrlType) {
                    MatchedUrlType.Url -> requestUrlTextView.setBackgroundColor(blueColor)
                    MatchedUrlType.TruncatedUrl -> truncatedRequestUrlTextView.setBackgroundColor(blueColor)
                    MatchedUrlType.UrlWithSeparators -> requestUrlWithSeparatorsTextView.setBackgroundColor(blueColor)
                    MatchedUrlType.TruncatedUrlWithSeparators -> truncatedRequestUrlWithSeparatorsTextView.setBackgroundColor(blueColor)
                }

                // Set the third party request background color.
                if (requestDataClass.isThirdPartyRequest)
                    thirdPartyRequestTextView.setBackgroundColor(blueColor)

                // Set the sublist and applied entry list background color.
                sublistTextView.setBackgroundColor(blueColor)
                appliedEntryListTextView.setBackgroundColor(blueColor)

                // Set the filter options background color.
                setFilterListBackgroundColor(requestDataClass.filterListDomain, domainTextView, true)
                setFilterListBackgroundColor(requestDataClass.filterListThirdParty, thirdPartyFilterListEntryTextView, true)

                // Set the domain list text view background color to be the same as the domain text view background color.
                setFilterListBackgroundColor(requestDataClass.filterListDomain, domainListTextView, true)

                // Set the matching text view background color to blue if they are populated.
                if (requestDataClass.filterListInitialMatch)
                    initialMatchTextView.setBackgroundColor(blueColor)
                if (requestDataClass.filterListFinalMatch)
                    finalMatchTextView.setBackgroundColor(blueColor)
            }

            RequestDisposition.Blocked -> {
                // Set the request disposition text.
                requestDispositionTextView.setText(R.string.blocked)

                // Set the request disposition background color to be red.
                requestDispositionTextView.setBackgroundColor(redColor)

                // Set the matched URL background color.
                when (requestDataClass.matchedUrlType) {
                    MatchedUrlType.Url -> requestUrlTextView.setBackgroundColor(redColor)
                    MatchedUrlType.TruncatedUrl -> truncatedRequestUrlTextView.setBackgroundColor(redColor)
                    MatchedUrlType.UrlWithSeparators -> requestUrlWithSeparatorsTextView.setBackgroundColor(redColor)
                    MatchedUrlType.TruncatedUrlWithSeparators -> truncatedRequestUrlWithSeparatorsTextView.setBackgroundColor(redColor)
                }

                // Set the third party request background color.
                if (requestDataClass.isThirdPartyRequest)
                    thirdPartyRequestTextView.setBackgroundColor(redColor)

                // Set the sublist and applied entry list background color.
                sublistTextView.setBackgroundColor(redColor)
                appliedEntryListTextView.setBackgroundColor(redColor)

                // Set the filter options background color.
                setFilterListBackgroundColor(requestDataClass.filterListDomain, domainTextView, false)
                setFilterListBackgroundColor(requestDataClass.filterListThirdParty, thirdPartyFilterListEntryTextView, false)

                // Set the domain list text view background color to be the same as the domain text view background color.
                setFilterListBackgroundColor(requestDataClass.filterListDomain, domainListTextView, false)

                // Set the matching text view background color to red if they are populated.
                if (requestDataClass.filterListInitialMatch)
                    initialMatchTextView.setBackgroundColor(redColor)
                if (requestDataClass.filterListFinalMatch)
                    finalMatchTextView.setBackgroundColor(redColor)
            }

            RequestDisposition.ThirdParty -> {
                // Set the text.
                requestDispositionTextView.setText(R.string.third_party_blocked)

                // Set the background color to be yellow.
                requestDispositionTextView.setBackgroundColor(yellowColor)

                // Set the matched URL background color.
                requestUrlTextView.setBackgroundColor(yellowColor)

                // Set the third party request background color.
                thirdPartyRequestTextView.setBackgroundColor(yellowColor)
            }
        }

        // Display the webpage URL.
        webpageUrlTextView.text = requestDataClass.webPageUrlString

        // Display the request URLs.
        requestUrlTextView.text = requestDataClass.requestUrlString
        requestUrlWithSeparatorsTextView.text = requestDataClass.requestUrlWithSeparatorsString
        truncatedRequestUrlTextView.text = requestDataClass.truncatedRequestUrlString
        truncatedRequestUrlWithSeparatorsTextView.text = requestDataClass.truncatedRequestUrlWithSeparatorsString

        // Display the third party request text.
        thirdPartyRequestTextView.text = if (requestDataClass.isThirdPartyRequest) getString(R.string.yes) else ""

        // Modify the dialog based on the request action.
        if ((requestDataClass.disposition == RequestDisposition.Default) || (requestDataClass.disposition == RequestDisposition.ThirdParty)) {  // A default or third-party request.
            // Hide the unused views.
            filterListEntryTextView.visibility = View.GONE
            filterListLabelTextView.visibility = View.GONE
            filterListTextView.visibility = View.GONE
            sublistLabelTextView.visibility = View.GONE
            sublistTextView.visibility = View.GONE
            appliedEntryListLabelTextView.visibility = View.GONE
            appliedEntryListTextView.visibility = View.GONE
            domainLabelTextView.visibility = View.GONE
            domainTextView.visibility = View.GONE
            domainListLabelTextView.visibility = View.GONE
            domainListTextView.visibility = View.GONE
            thirdPartyFilterListEntryLabelTextView.visibility = View.GONE
            thirdPartyFilterListEntryTextView.visibility = View.GONE
            initialMatchLabelTextView.visibility = View.GONE
            initialMatchTextView.visibility = View.GONE
            finalMatchLabelTextView.visibility = View.GONE
            finalMatchTextView.visibility = View.GONE
            appliedFilterOptionsLabelTextView.visibility = View.GONE
            appliedFilterOptionsTextView.visibility = View.GONE
            originalFilterOptionsLabelTextView.visibility = View.GONE
            originalFilterOptionsTextView.visibility = View.GONE
            originalEntryLabelTextView.visibility = View.GONE
            originalEntryTextView.visibility = View.GONE
        } else {  // A blocked or allowed request.
            // Populate the filter list.
            when (requestDataClass.filterList) {
                FilterList.UltraPrivacy -> filterListTextView.text = getString(R.string.ultraprivacy)
                FilterList.UltraList -> filterListTextView.text = getString(R.string.ultralist)
                FilterList.EasyPrivacy -> filterListTextView.text = getString(R.string.easyprivacy)
                FilterList.EasyList -> filterListTextView.text = getString(R.string.easylist)
                FilterList.FanboysAnnoyanceList -> filterListTextView.text = getString(R.string.fanboys_annoyance_list)
                FilterList.ThirdPartyRequests -> filterListTextView.text = getString(R.string.third_party)
            }

            // Populate the sublist.
            when (requestDataClass.sublist) {
                Sublist.MainAllowList -> sublistTextView.text = getString(R.string.main_allowlist)
                Sublist.InitialDomainAllowList -> sublistTextView.text = getString(R.string.initial_domain_allowlist)
                Sublist.RegularExpressionAllowList -> sublistTextView.text = getString(R.string.regular_expression_allowlist)
                Sublist.MainBlockList -> sublistTextView.text = getString(R.string.main_blocklist)
                Sublist.InitialDomainBlockList -> sublistTextView.text = getString(R.string.initial_domain_blocklist)
                Sublist.RegularExpressionBlockList -> sublistTextView.text = getString(R.string.regular_expression_blocklist)
            }

            // Populate the lists.
            appliedEntryListTextView.text = convertListToString(requestDataClass.filterListAppliedEntryList, "    ")
            domainListTextView.text = convertListToString(requestDataClass.filterListDomainList, " | ")
            appliedFilterOptionsTextView.text = convertListToString(requestDataClass.filterListAppliedFilterOptionsList, " , ")

            // Populate the filter options.
            domainTextView.text = populateFilterOption(requestDataClass.filterListDomain)
            thirdPartyFilterListEntryTextView.text = populateFilterOption(requestDataClass.filterListThirdParty)

            // Populate the initial and final matches.
            initialMatchTextView.text = if (requestDataClass.filterListInitialMatch) getString(R.string.yes) else ""
            finalMatchTextView.text = if (requestDataClass.filterListFinalMatch) getString(R.string.yes) else ""

            // Populate the strings.
            originalFilterOptionsTextView.text = requestDataClass.filterListOriginalFilterOptionsString
            originalEntryTextView.text = requestDataClass.filterListOriginalEntryString
        }

        // Return the alert dialog.
        return alertDialog
    }

    private fun convertListToString(stringList: List<String>, separatorString: String): String {
        // Create a string builder.
        val stringBuilder = StringBuilder()

        // Add each of the strings from the list.
        for (string in stringList) {
            // Append four space if the string builder is already populated.
            if (stringBuilder.isNotEmpty())
                stringBuilder.append(separatorString)

            // Append the string.
            stringBuilder.append(string)
        }

        // Return the string.
        return stringBuilder.toString()
    }

    private fun populateFilterOption(filterOptionDisposition: FilterOptionDisposition) : String {
        // Return the filter option text.
        return when (filterOptionDisposition) {
            FilterOptionDisposition.Null -> ""
            FilterOptionDisposition.Apply -> getString(R.string.apply)
            FilterOptionDisposition.Override -> getString(R.string.override)
        }
    }

    private fun setFilterListBackgroundColor(filterOptionDisposition: FilterOptionDisposition, textView: TextView, isAllowList: Boolean) {
        // Set the text view background.
        if (isAllowList) {  // This is an allow list.
            if (filterOptionDisposition == FilterOptionDisposition.Apply)  // The filter option is applied on an allow list, set the background to blue.
                textView.setBackgroundColor(blueColor)
            else if (filterOptionDisposition == FilterOptionDisposition.Override)  // The filter option is overridden on an allow list, set the background to red.
                textView.setBackgroundColor(redColor)
        } else {  // This is a block list.
            if (filterOptionDisposition == FilterOptionDisposition.Apply)  // The filter option is applied on a block list, set the background to be red.
                textView.setBackgroundColor(redColor)
            else if (filterOptionDisposition == FilterOptionDisposition.Override)  // The filter option is overridden on a block list, set the background to be blue.
                textView.setBackgroundColor(blueColor)
        }
    }
}
