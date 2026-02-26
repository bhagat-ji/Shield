/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2025-2026 Soren Stoutner <soren@stoutner.com>
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

package com.stoutner.privacybrowser.activities

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView

import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cursoradapter.widget.ResourceCursorAdapter
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.adapters.FilterListArrayAdapter
import com.stoutner.privacybrowser.dataclasses.FilterListDataClass
import com.stoutner.privacybrowser.dialogs.ViewFilterListEntryDialog
import com.stoutner.privacybrowser.helpers.easyListDataClass
import com.stoutner.privacybrowser.helpers.easyPrivacyDataClass
import com.stoutner.privacybrowser.helpers.fanboysAnnoyanceDataClass
import com.stoutner.privacybrowser.helpers.ultraListDataClass
import com.stoutner.privacybrowser.helpers.ultraPrivacyDataClass

// Define the filter list class constants.
const val ULTRAPRIVACY = 0
const val ULTRALIST = 1
const val EASYPRIVACY = 2
const val EASYLIST = 3
const val FANBOYS_ANNOYANCE_LIST = 4

// Define the filter sublist class constants.
const val MAIN_ALLOWLIST = 0
const val INITIAL_DOMAIN_ALLOWLIST = 1
const val REGULAR_EXPRESSION_ALLOWLIST = 2
const val MAIN_BLOCKLIST = 3
const val INITIAL_DOMAIN_BLOCKLIST = 4
const val REGULAR_EXPRESSION_BLOCKLIST =5

// Define the private class constants.
private const val FILTER_SUBLIST_SPINNER_SELECTED_POSITION = "filter_sublist_spinner_selected_position"
private const val LISTVIEW_POSITION = "listview_position"

class FilterListsActivity : AppCompatActivity(), ViewFilterListEntryDialog.ViewFilterListEntryListener {
    // Declare the class views.
    private lateinit var appBarSpinner: Spinner
    private lateinit var activityContext: Context
    private lateinit var filterListListView: ListView
    private lateinit var filterSublistSpinner: Spinner

    // Define the class variables
    private var filterSublistSpinnerSelectedPosition = 0

    public override fun onCreate(savedInstanceState: Bundle?) {
        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

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
            setContentView(R.layout.filter_lists_bottom_appbar)
        else
            setContentView(R.layout.filter_lists_top_appbar)

        // Get a handle for the toolbar.
        val toolbar = findViewById<Toolbar>(R.id.filter_lists_toolbar)

        // Set the support action bar.
        setSupportActionBar(toolbar)

        // Get a handle for the app bar.
        val appBar = supportActionBar!!

        // Set the app bar custom view.
        appBar.setCustomView(R.layout.spinner)

        // Display the back arrow in the app bar.
        appBar.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM or ActionBar.DISPLAY_HOME_AS_UP

        // Get handles for the views.
        appBarSpinner = findViewById(R.id.spinner)
        filterSublistSpinner = findViewById(R.id.filter_sublist_spinner)
        filterListListView = findViewById(R.id.filter_list_listview)

        // Store the activity context.
        activityContext = this

        // Setup a matrix cursor for the app bar spinner.
        val appBarSpinnerCursor = MatrixCursor(arrayOf("_id", "Filter List"))
        appBarSpinnerCursor.addRow(arrayOf<Any>(ULTRAPRIVACY, getString(R.string.ultraprivacy) + " - " + ultraPrivacyDataClass.versionString))
        appBarSpinnerCursor.addRow(arrayOf<Any>(ULTRALIST, getString(R.string.ultralist) + " - " + ultraListDataClass.versionString))
        appBarSpinnerCursor.addRow(arrayOf<Any>(EASYPRIVACY, getString(R.string.easyprivacy) + " - " + easyPrivacyDataClass.versionString))
        appBarSpinnerCursor.addRow(arrayOf<Any>(EASYLIST, getString(R.string.easylist) + " - " + easyListDataClass.versionString))
        appBarSpinnerCursor.addRow(arrayOf<Any>(FANBOYS_ANNOYANCE_LIST, getString(R.string.fanboys_annoyance_list) + " - " + fanboysAnnoyanceDataClass!!.versionString))

        // Create a resource cursor adapter for the spinner.
        val spinnerCursorAdapter: ResourceCursorAdapter = object : ResourceCursorAdapter(this, R.layout.appbar_spinner_item, appBarSpinnerCursor, 0) {
            override fun bindView(view: View, context: Context, cursor: Cursor) {
                // Get a handle for the spinner item text view.
                val spinnerItemTextView = view.findViewById<TextView>(R.id.spinner_item_textview)

                // Set the text view text.
                spinnerItemTextView.text = cursor.getString(1)
            }
        }

        // Set the spinner cursor adapter drop down view resource.
        spinnerCursorAdapter.setDropDownViewResource(R.layout.appbar_spinner_dropdown_item)

        // Set the app bar spinner adapter.
        appBarSpinner.adapter = spinnerCursorAdapter

        // Handle taps on the main filter list spinner dropdown.
        appBarSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Populate the filter sublist spinner.
                when (id.toInt()) {
                    ULTRAPRIVACY -> populateFilterSublistSpinner(ultraPrivacyDataClass)
                    ULTRALIST -> populateFilterSublistSpinner(ultraListDataClass)
                    EASYPRIVACY -> populateFilterSublistSpinner(easyPrivacyDataClass)
                    EASYLIST -> populateFilterSublistSpinner(easyListDataClass)
                    FANBOYS_ANNOYANCE_LIST -> populateFilterSublistSpinner(fanboysAnnoyanceDataClass!!)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing.
            }
        }

        filterSublistSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Update the filter sublist spinner selected position.
                filterSublistSpinnerSelectedPosition = position

                // Get the current filter list data class.
                val currentFilterListDataClass = when (id.toInt()) {
                    MAIN_ALLOWLIST -> getCurrentFilterListDataClass().mainAllowList
                    INITIAL_DOMAIN_ALLOWLIST -> getCurrentFilterListDataClass().initialDomainAllowList
                    REGULAR_EXPRESSION_ALLOWLIST -> getCurrentFilterListDataClass().regularExpressionAllowList
                    MAIN_BLOCKLIST -> getCurrentFilterListDataClass().mainBlockList
                    INITIAL_DOMAIN_BLOCKLIST -> getCurrentFilterListDataClass().initialDomainBlockList
                    REGULAR_EXPRESSION_BLOCKLIST -> getCurrentFilterListDataClass().regularExpressionBlockList
                    else -> getCurrentFilterListDataClass().mainAllowList
                }

                // Get an adapter for the current filter list data class.
                val currentFilterListDataClassArrayAdapter = FilterListArrayAdapter(activityContext, currentFilterListDataClass)

                // Populate the list view with the current filter list data class adapter.
                filterListListView.adapter = currentFilterListDataClassArrayAdapter
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing.
            }
        }

        // Listen for taps on entries in the list view.
        filterListListView.onItemClickListener = AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            // Display the view filter list entry dialog.  the list view is 0 based, so the position must be incremented by 1.
            launchViewFilterListEntryDialog(position + 1)
        }

        // Check to see if the activity has been restarted.
        if (savedInstanceState != null) {
            // Get the filter sublist spinner selected position.
            filterSublistSpinnerSelectedPosition = savedInstanceState.getInt(FILTER_SUBLIST_SPINNER_SELECTED_POSITION)

            // Create a scroll handler.
            val scrollHandler = Handler(mainLooper)

            // Create a scroll runnable.
            val scrollRunnable = Runnable {
                // Scroll to the saved position.
                filterListListView.post { filterListListView.setSelection(savedInstanceState.getInt(LISTVIEW_POSITION))}
            }

            // Scroll the list view after 100 milliseconds to allow enough time for the correct list view to be populated.
            scrollHandler.postDelayed(scrollRunnable, 100)
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        // Run the default commands.
        super.onSaveInstanceState(outState)

        // Get the filter sublist spinner ID.
        val filterSublistSpinnerSelectedPosition = filterSublistSpinner.selectedItemPosition

        // Get the listview position.
        val listViewPosition = filterListListView.firstVisiblePosition

        // Store the variables in the bundle.
        outState.putInt(FILTER_SUBLIST_SPINNER_SELECTED_POSITION, filterSublistSpinnerSelectedPosition)
        outState.putInt(LISTVIEW_POSITION, listViewPosition)
    }

    override fun onNext(currentId: Int) {
        // Show the next dialog.
        launchViewFilterListEntryDialog(currentId + 1)
    }

    override fun onPrevious(currentId: Int) {
        // Show the previous dialog.
        launchViewFilterListEntryDialog(currentId - 1)
    }

    private fun getCurrentFilterListDataClass() : FilterListDataClass {
        // Return the filter list data class selected in the spinner.
        return when (appBarSpinner.selectedItemId.toInt()) {
            ULTRAPRIVACY -> ultraPrivacyDataClass
            ULTRALIST -> ultraListDataClass
            EASYPRIVACY -> easyPrivacyDataClass
            EASYLIST -> easyListDataClass
            FANBOYS_ANNOYANCE_LIST -> fanboysAnnoyanceDataClass!!
            else -> ultraPrivacyDataClass
        }
    }

    private fun launchViewFilterListEntryDialog(id: Int) {
        // Determine if this is the last entry in the list.
        val isLastEntry = (id == filterListListView.count)

        // Create a view filter list entry dialog.
        val viewFilterListsEntryDialogFragment: DialogFragment = ViewFilterListEntryDialog.entry(id, isLastEntry, appBarSpinner.selectedItemPosition,
                                                                                                 filterSublistSpinner.selectedItemPosition)

        // Make it so.
        viewFilterListsEntryDialogFragment.show(supportFragmentManager, getString(R.string.filterlist_entry))
    }

    private fun populateFilterSublistSpinner(filterListDataClass: FilterListDataClass) {
        // Set up a matrix cursor for the filter sublist spinner.
        val sublistSpinnerCursor = MatrixCursor(arrayOf("_id", "Filter Sublist"))
        sublistSpinnerCursor.addRow(arrayOf<Any>(MAIN_ALLOWLIST, getString(R.string.main_allowlist) + " - " + filterListDataClass.mainAllowList.size))
        sublistSpinnerCursor.addRow(arrayOf<Any>(INITIAL_DOMAIN_ALLOWLIST, getString(R.string.initial_domain_allowlist) + " - " + filterListDataClass.initialDomainAllowList.size))
        sublistSpinnerCursor.addRow(arrayOf<Any>(REGULAR_EXPRESSION_ALLOWLIST, getString(R.string.regular_expression_allowlist) + " - " + filterListDataClass.regularExpressionAllowList.size))
        sublistSpinnerCursor.addRow(arrayOf<Any>(MAIN_BLOCKLIST, getString(R.string.main_blocklist) + " - " + filterListDataClass.mainBlockList.size))
        sublistSpinnerCursor.addRow(arrayOf<Any>(INITIAL_DOMAIN_BLOCKLIST, getString(R.string.initial_domain_blocklist) + " - " + filterListDataClass.initialDomainBlockList.size))
        sublistSpinnerCursor.addRow(arrayOf<Any>(REGULAR_EXPRESSION_BLOCKLIST, getString(R.string.regular_expression_blocklist) + " - " + filterListDataClass.regularExpressionBlockList.size))

        // Create a resource cursor adapter for the spinner.
        val filterSublistSpinnerCursorAdapter: ResourceCursorAdapter = object : ResourceCursorAdapter(activityContext, R.layout.appbar_spinner_item, sublistSpinnerCursor,
            0) {
            override fun bindView(view: View, context: Context, cursor: Cursor) {
                // Get a handle for the spinner item text view.
                val spinnerItemTextView = view.findViewById<TextView>(R.id.spinner_item_textview)

                // Set the text view text.
                spinnerItemTextView.text = cursor.getString(1)
            }
        }

        // Set the filter sublist cursor adapter drop down view resource.
        filterSublistSpinnerCursorAdapter.setDropDownViewResource(R.layout.appbar_spinner_dropdown_item)

        // Set the filter sublist spinner adapter.
        filterSublistSpinner.adapter = filterSublistSpinnerCursorAdapter

        // Set the filter sublist spinner selected position
        filterSublistSpinner.setSelection(filterSublistSpinnerSelectedPosition)
    }
}
