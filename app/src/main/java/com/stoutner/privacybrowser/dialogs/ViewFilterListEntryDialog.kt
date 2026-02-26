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
import android.view.WindowManager
import android.widget.TextView

import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.activities.EASYLIST
import com.stoutner.privacybrowser.activities.EASYPRIVACY
import com.stoutner.privacybrowser.activities.FANBOYS_ANNOYANCE_LIST
import com.stoutner.privacybrowser.activities.INITIAL_DOMAIN_ALLOWLIST
import com.stoutner.privacybrowser.activities.INITIAL_DOMAIN_BLOCKLIST
import com.stoutner.privacybrowser.activities.MAIN_ALLOWLIST
import com.stoutner.privacybrowser.activities.MAIN_BLOCKLIST
import com.stoutner.privacybrowser.activities.REGULAR_EXPRESSION_ALLOWLIST
import com.stoutner.privacybrowser.activities.REGULAR_EXPRESSION_BLOCKLIST
import com.stoutner.privacybrowser.activities.ULTRALIST
import com.stoutner.privacybrowser.activities.ULTRAPRIVACY
import com.stoutner.privacybrowser.dataclasses.FilterListDataClass
import com.stoutner.privacybrowser.dataclasses.FilterListEntryDataClass
import com.stoutner.privacybrowser.dataclasses.FilterOptionDisposition
import com.stoutner.privacybrowser.helpers.easyListDataClass
import com.stoutner.privacybrowser.helpers.easyPrivacyDataClass
import com.stoutner.privacybrowser.helpers.fanboysAnnoyanceDataClass
import com.stoutner.privacybrowser.helpers.ultraListDataClass
import com.stoutner.privacybrowser.helpers.ultraPrivacyDataClass

// Define the class constants.
private const val ENTRY_ID = "entry_id"
private const val FILTER_LIST_INT = "filter_list_int"
private const val IS_LAST_ENTRY = "is_last_entry"
private const val SUBLIST_INT = "sublist_int"

// Define the private variables.
private var blueColor = 0
private var redColor = 0

class ViewFilterListEntryDialog : DialogFragment() {
    companion object {
        fun entry(entryId: Int, isLastEntry: Boolean, filterListInt: Int, sublistInt: Int): ViewFilterListEntryDialog {
            // Create a bundle.
            val bundle = Bundle()

            // Store the request details.
            bundle.putInt(ENTRY_ID, entryId)
            bundle.putBoolean(IS_LAST_ENTRY, isLastEntry)
            bundle.putInt(FILTER_LIST_INT, filterListInt)
            bundle.putInt(SUBLIST_INT, sublistInt)

            // Create a new instance of the view filter list entry dialog.
            val viewFilterListEntryDialog = ViewFilterListEntryDialog()

            // Add the arguments to the new dialog.
            viewFilterListEntryDialog.arguments = bundle

            // Return the new dialog.
            return viewFilterListEntryDialog
        }
    }

    // Define the class variables.
    private lateinit var viewFilterListEntryListener: ViewFilterListEntryListener

    // The public interface is used to send information back to the parent activity.
    interface ViewFilterListEntryListener {
        // Show the previous request.
        fun onPrevious(currentId: Int)

        // Show the next request.
        fun onNext(currentId: Int)
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Get a handle for the listener from the launching context.
        viewFilterListEntryListener = context as ViewFilterListEntryListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get the arguments from the bundle.
        val entryId = requireArguments().getInt(ENTRY_ID)
        val isLastEntry = requireArguments().getBoolean(IS_LAST_ENTRY)
        val filterListInt = requireArguments().getInt(FILTER_LIST_INT)
        val sublistInt = requireArguments().getInt(SUBLIST_INT)

        // Use an alert dialog builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.PrivacyBrowserAlertDialog)

        // Set the icon.
        dialogBuilder.setIcon(R.drawable.block_ads_enabled)

        // Set the title.
        dialogBuilder.setTitle(getString(R.string.filterlist_entry, entryId))

        // Set the view.
        dialogBuilder.setView(R.layout.view_filter_list_entry_dialog)

        // Set the close button.  Using `null` as the listener closes the dialog without doing anything else.
        dialogBuilder.setNeutralButton(R.string.close, null)

        // Set the previous button.
        dialogBuilder.setNegativeButton(R.string.previous) { _: DialogInterface?, _: Int ->
            // Load the previous entry.
            viewFilterListEntryListener.onPrevious(entryId)
        }

        // Set the next button.
        dialogBuilder.setPositiveButton(R.string.next) { _: DialogInterface?, _: Int ->
            // Load the next entry.
            viewFilterListEntryListener.onNext(entryId)
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
        val filterListTextView = alertDialog.findViewById<TextView>(R.id.filter_list_textview)!!
        val sublistTextView = alertDialog.findViewById<TextView>(R.id.sublist_textview)!!
        val appliedEntryListTextView = alertDialog.findViewById<TextView>(R.id.applied_entry_list_textview)!!
        val domainTextView = alertDialog.findViewById<TextView>(R.id.domain_textview)!!
        val domainListTextView = alertDialog.findViewById<TextView>(R.id.domain_list_textview)!!
        val thirdPartyTextView = alertDialog.findViewById<TextView>(R.id.third_party_textview)!!
        val initialMatchTextView = alertDialog.findViewById<TextView>(R.id.initial_match_textview)!!
        val finalMatchTextView = alertDialog.findViewById<TextView>(R.id.final_match_textview)!!
        val originalEntryTextView = alertDialog.findViewById<TextView>(R.id.original_entry_textview)!!
        val appliedFilterOptionsTextView = alertDialog.findViewById<TextView>(R.id.applied_filter_options_textview)!!
        val originalFilterOptionsTextView = alertDialog.findViewById<TextView>(R.id.original_filter_options_textview)!!
        val previousButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        val nextButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)

        // Disable the previous button if the first filter list entry is displayed.
        previousButton.isEnabled = (entryId != 1)

        // Disable the next button if the last filter list entry is displayed.
        nextButton.isEnabled = !isLastEntry

        // Get the filter list entry data class.
        val filterListEntryDataClass = getFilterListEntryDataClass(filterListInt, sublistInt, entryId)

        // Populate the filter list entry views.
        filterListTextView.text = getFilterListName(filterListInt)
        sublistTextView.text = getSublistName(sublistInt)
        appliedEntryListTextView.text = convertListToString(filterListEntryDataClass.appliedEntryList, "    ")
        domainTextView.text = populateFilterOption(filterListEntryDataClass.domain)
        domainListTextView.text = convertListToString(filterListEntryDataClass.domainList, " | ")
        thirdPartyTextView.text = populateFilterOption(filterListEntryDataClass.thirdParty)
        appliedFilterOptionsTextView.text = convertListToString(filterListEntryDataClass.appliedFilterOptionsList, " , ")
        originalFilterOptionsTextView.text = filterListEntryDataClass.originalFilterOptionsString
        originalEntryTextView.text = filterListEntryDataClass.originalEntryString

        // Populate the matching text views with `yes` if they are applied.
        if (filterListEntryDataClass.initialMatch)
            initialMatchTextView.text = getString(R.string.yes)
        if (filterListEntryDataClass.finalMatch)
            finalMatchTextView.text = getString(R.string.yes)

        // Get handles for the colors.
        blueColor = getColor(requireContext(), R.color.requests_blue_background)
        redColor = getColor(requireContext(), R.color.red_background)

        // Set the background color for the text views.
        if (sublistInt <= 2) {  // This is an allow list.
            // Set the background to be blue.
            sublistTextView.setBackgroundColor(blueColor)
            appliedEntryListTextView.setBackgroundColor(blueColor)

            // Set the filter options background color.
            setFilterListBackgroundColor(filterListEntryDataClass.domain, domainTextView, true)
            setFilterListBackgroundColor(filterListEntryDataClass.thirdParty, thirdPartyTextView, true)

            // Set the domain list text view background color to be the same as the domain text view background color.
            setFilterListBackgroundColor(filterListEntryDataClass.domain, domainListTextView, true)

            // Set the matching text view background color to blue if they are populated.
            if (filterListEntryDataClass.initialMatch)
                initialMatchTextView.setBackgroundColor(blueColor)
            if (filterListEntryDataClass.finalMatch)
                finalMatchTextView.setBackgroundColor(blueColor)
        } else {  // This is a block list.
            // Set the background color to be red.
            sublistTextView.setBackgroundColor(redColor)
            appliedEntryListTextView.setBackgroundColor(redColor)

            // Set the filter options background color.
            setFilterListBackgroundColor(filterListEntryDataClass.domain, domainTextView, false)
            setFilterListBackgroundColor(filterListEntryDataClass.thirdParty, thirdPartyTextView, false)

            // Set the domain list text view background color to be the same as the domain text view background color.
            setFilterListBackgroundColor(filterListEntryDataClass.domain, domainListTextView, false)

            // Set the matching text view background color to red if they are populated.
            if (filterListEntryDataClass.initialMatch)
                initialMatchTextView.setBackgroundColor(redColor)
            if (filterListEntryDataClass.finalMatch)
                finalMatchTextView.setBackgroundColor(redColor)
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

    private fun getFilterListEntryDataClass(filterListInt: Int, sublistInt: Int, entryId: Int) : FilterListEntryDataClass {
        // Get the filter list entry according to the filter list.
        return when (filterListInt) {
            ULTRAPRIVACY -> getFilterListEntryDataClassFromSublist(ultraPrivacyDataClass, sublistInt, entryId)
            ULTRALIST -> getFilterListEntryDataClassFromSublist(ultraListDataClass, sublistInt, entryId)
            EASYPRIVACY -> getFilterListEntryDataClassFromSublist(easyPrivacyDataClass, sublistInt, entryId)
            EASYLIST -> getFilterListEntryDataClassFromSublist(easyListDataClass, sublistInt, entryId)
            FANBOYS_ANNOYANCE_LIST -> getFilterListEntryDataClassFromSublist(fanboysAnnoyanceDataClass!!, sublistInt, entryId)
            else -> getFilterListEntryDataClassFromSublist(FilterListDataClass(), sublistInt, entryId)
        }
    }

    private fun getFilterListEntryDataClassFromSublist(filterListDataClass: FilterListDataClass, sublistInt: Int, entryId: Int) : FilterListEntryDataClass {
        // Return the filter list entry data class.  The list is 0 based, so the entry ID needs to be decremented by 1.
        return when (sublistInt) {
            MAIN_ALLOWLIST -> filterListDataClass.mainAllowList[entryId - 1]
            INITIAL_DOMAIN_ALLOWLIST -> filterListDataClass.initialDomainAllowList[entryId - 1]
            REGULAR_EXPRESSION_ALLOWLIST -> filterListDataClass.regularExpressionAllowList[entryId - 1]
            MAIN_BLOCKLIST -> filterListDataClass.mainBlockList[entryId - 1]
            INITIAL_DOMAIN_BLOCKLIST -> filterListDataClass.initialDomainBlockList[entryId - 1]
            REGULAR_EXPRESSION_BLOCKLIST -> filterListDataClass.regularExpressionBlockList[entryId - 1]
            else -> FilterListEntryDataClass()
        }
    }

    private fun getFilterListName(filterListInt: Int) : String {
        // Return the filter list name.
        return when (filterListInt) {
            ULTRAPRIVACY -> getString(R.string.ultraprivacy)
            ULTRALIST -> getString(R.string.ultralist)
            EASYPRIVACY -> getString(R.string.easyprivacy)
            EASYLIST -> getString(R.string.easylist)
            FANBOYS_ANNOYANCE_LIST -> getString(R.string.fanboys_annoyance_list)
            else -> ""
        }
    }

    private fun getSublistName(sublistInt: Int) : String {
        // Return the sublist name.
        return when (sublistInt) {
            MAIN_ALLOWLIST -> getString(R.string.main_allowlist)
            INITIAL_DOMAIN_ALLOWLIST -> getString(R.string.initial_domain_allowlist)
            REGULAR_EXPRESSION_ALLOWLIST -> getString(R.string.regular_expression_allowlist)
            MAIN_BLOCKLIST -> getString(R.string.main_blocklist)
            INITIAL_DOMAIN_BLOCKLIST -> getString(R.string.initial_domain_blocklist)
            REGULAR_EXPRESSION_BLOCKLIST -> getString(R.string.regular_expression_blocklist)
            else -> ""
        }
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
