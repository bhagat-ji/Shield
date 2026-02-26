/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2018-2026 Soren Stoutner <soren@stoutner.com>
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
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
import com.stoutner.privacybrowser.adapters.RequestsArrayAdapter
import com.stoutner.privacybrowser.dataclasses.RequestDataClass
import com.stoutner.privacybrowser.dataclasses.RequestDisposition
import com.stoutner.privacybrowser.dialogs.ViewRequestDialog
import com.stoutner.privacybrowser.dialogs.ViewRequestDialog.ViewRequestListener

// Define the public constants.
const val BLOCK_ALL_THIRD_PARTY_REQUESTS = "block_all_third_party_requests"

// Define the private class constants.
private const val LISTVIEW_POSITION = "listview_position"

// Define the spinner entry class constants.
private const val ALL_RESOURCE_REQUESTS = 0
private const val DEFAULT_RESOURCE_REQUESTS = 1
private const val ALLOWED_RESOURCE_REQUESTS = 2
private const val BLOCKED_RESOURCE_REQUESTS = 3
private const val THIRD_PARTY_RESOURCE_REQUESTS = 4

class RequestsActivity : AppCompatActivity(), ViewRequestListener {
    companion object {
        // The resource requests are populated by `MainWebViewActivity` before `RequestsActivity` is launched.
        lateinit var resourceRequestsDataClassList: List<RequestDataClass>
    }

    // Define the class views.
    private lateinit var requestsListView: ListView

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

        // Get the launching intent
        val intent = intent

        // Get the status of the third-party filter list.
        val blockAllThirdPartyRequests = intent.getBooleanExtra(BLOCK_ALL_THIRD_PARTY_REQUESTS, false)

        // Set the content view.
        if (bottomAppBar)
            setContentView(R.layout.requests_bottom_appbar)
        else
            setContentView(R.layout.requests_top_appbar)

        // Get a handle for the toolbar.
        val toolbar = findViewById<Toolbar>(R.id.requests_toolbar)

        // Set the support action bar.
        setSupportActionBar(toolbar)

        // Get a handle for the app bar.
        val appBar = supportActionBar!!

        // Set the app bar custom view.
        appBar.setCustomView(R.layout.spinner)

        // Display the back arrow in the app bar.
        appBar.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM or ActionBar.DISPLAY_HOME_AS_UP

        // Get handles for the views.
        val appBarSpinner = findViewById<Spinner>(R.id.spinner)
        requestsListView = findViewById(R.id.requests_listview)

        // Initialize the resource requests data class lists.  A list is needed for all the resource requests, or the activity can crash if `MainWebViewActivity.resourceRequests` is modified after the activity loads.
        val allResourceRequestsDataClassList: MutableList<RequestDataClass> = mutableListOf()
        val defaultResourceRequestsDataClassList: MutableList<RequestDataClass> = mutableListOf()
        val allowedResourceRequestsDataClassList: MutableList<RequestDataClass> = mutableListOf()
        val blockedResourceRequestsDataClassList: MutableList<RequestDataClass> = mutableListOf()
        val thirdPartyResourceRequestsDataClassList: MutableList<RequestDataClass> = mutableListOf()

        // Populate the resource array lists.
        for (requestDataClass in resourceRequestsDataClassList) {
            // Add the request data class to the list of all requests.
            allResourceRequestsDataClassList.add(requestDataClass)

            // Add the request data class to the other, specific, list of requests.
            when (requestDataClass.disposition) {
                RequestDisposition.Default -> defaultResourceRequestsDataClassList.add(requestDataClass)
                RequestDisposition.Allowed -> allowedResourceRequestsDataClassList.add(requestDataClass)
                RequestDisposition.Blocked -> blockedResourceRequestsDataClassList.add(requestDataClass)
                RequestDisposition.ThirdParty -> thirdPartyResourceRequestsDataClassList.add(requestDataClass)
            }
        }

        // Setup a matrix cursor for the resource lists.
        val spinnerCursor = MatrixCursor(arrayOf("_id", "Requests"))
        spinnerCursor.addRow(arrayOf<Any>(ALL_RESOURCE_REQUESTS, getString(R.string.all) + " - " + allResourceRequestsDataClassList.size))
        spinnerCursor.addRow(arrayOf<Any>(DEFAULT_RESOURCE_REQUESTS, getString(R.string.default_label) + " - " + defaultResourceRequestsDataClassList.size))
        spinnerCursor.addRow(arrayOf<Any>(ALLOWED_RESOURCE_REQUESTS, getString(R.string.allowed_plural) + " - " + allowedResourceRequestsDataClassList.size))
        spinnerCursor.addRow(arrayOf<Any>(BLOCKED_RESOURCE_REQUESTS, getString(R.string.blocked_plural) + " - " + blockedResourceRequestsDataClassList.size))
        if (blockAllThirdPartyRequests)
            spinnerCursor.addRow(arrayOf<Any>(THIRD_PARTY_RESOURCE_REQUESTS, getString(R.string.third_party_plural) + " - " + thirdPartyResourceRequestsDataClassList.size))

        // Create a resource cursor adapter for the spinner.
        val spinnerCursorAdapter: ResourceCursorAdapter = object : ResourceCursorAdapter(this, R.layout.appbar_spinner_item, spinnerCursor, 0) {
            override fun bindView(view: View, context: Context, cursor: Cursor) {
                // Get a handle for the spinner item text view.
                val spinnerItemTextView = view.findViewById<TextView>(R.id.spinner_item_textview)

                // Set the text view to display the resource list.
                spinnerItemTextView.text = cursor.getString(1)
            }
        }

        // Set the resource cursor adapter drop down view resource.
        spinnerCursorAdapter.setDropDownViewResource(R.layout.appbar_spinner_dropdown_item)

        // Set the app bar spinner adapter.
        appBarSpinner.adapter = spinnerCursorAdapter

        // Get a handle for the context.
        val context: Context = this

        // Handle taps on the spinner dropdown.
        appBarSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (id.toInt()) {
                    ALL_RESOURCE_REQUESTS -> {
                        // Get an adapter for all the requests.
                        val allResourceRequestsArrayAdapter = RequestsArrayAdapter(context, allResourceRequestsDataClassList)

                        // Display the adapter in the list view.
                        requestsListView.adapter = allResourceRequestsArrayAdapter
                    }

                    DEFAULT_RESOURCE_REQUESTS -> {
                        // Get an adapter for the default requests.
                        val defaultResourceRequestsArrayAdapter = RequestsArrayAdapter(context, defaultResourceRequestsDataClassList)

                        // Display the adapter in the list view.
                        requestsListView.adapter = defaultResourceRequestsArrayAdapter
                    }

                    ALLOWED_RESOURCE_REQUESTS -> {
                        // Get an adapter for the allowed requests.
                        val allowedResourceRequestsArrayAdapter = RequestsArrayAdapter(context, allowedResourceRequestsDataClassList)

                        // Display the adapter in the list view.
                        requestsListView.adapter = allowedResourceRequestsArrayAdapter
                    }

                    BLOCKED_RESOURCE_REQUESTS -> {
                        // Get an adapter for the blocked requests.
                        val blockedResourceRequestsArrayAdapter = RequestsArrayAdapter(context, blockedResourceRequestsDataClassList)

                        // Display the adapter in the list view.
                        requestsListView.adapter = blockedResourceRequestsArrayAdapter
                    }

                    THIRD_PARTY_RESOURCE_REQUESTS -> {
                        // Get an adapter for the third-party requests.
                        val thirdPartyResourceRequestsArrayAdapter = RequestsArrayAdapter(context, thirdPartyResourceRequestsDataClassList)

                        //Display the adapter in the list view.
                        requestsListView.adapter = thirdPartyResourceRequestsArrayAdapter
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing.
            }
        }
        // Listen for taps on entries in the list view.
        requestsListView.onItemClickListener = AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            // Display the view request dialog.  The list view is 0 based, so the position must be incremented by 1.
            launchViewRequestDialog(position + 1)
        }

        // Check to see if the activity has been restarted.
        if (savedInstanceState != null) {
            // Scroll to the saved position.
            requestsListView.post { requestsListView.setSelection(savedInstanceState.getInt(LISTVIEW_POSITION)) }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu.
        menuInflater.inflate(R.menu.requests_options_menu, menu)

        // Success.
        return true
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        // Run the commands that correlate to the selected menu item
        return when (menuItem.itemId) {
            R.id.filter_lists -> {  // Filter lists.
                // Create an intent to launch the filter lists activity.
                val filterListsIntent = Intent(this, FilterListsActivity::class.java)

                // Make it so.
                startActivity(filterListsIntent)

                // Consume the event.
                true
            }

            else -> {  // There is no match with the options menu.  Pass the event up to the parent method.
                // Don't consume the event.
                super.onOptionsItemSelected(menuItem)
            }
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        // Run the default commands.
        super.onSaveInstanceState(outState)

        // Get the listview position.
        val listViewPosition = requestsListView.firstVisiblePosition

        // Store the listview position in the bundle.
        outState.putInt(LISTVIEW_POSITION, listViewPosition)
    }

    override fun onNext(currentId: Int) {
        // Show the next dialog.
        launchViewRequestDialog(currentId + 1)
    }

    override fun onPrevious(currentId: Int) {
        // Show the previous dialog.
        launchViewRequestDialog(currentId - 1)
    }

    private fun launchViewRequestDialog(id: Int) {
        // Determine if this is the last request in the list.
        val isLastRequest = (id == requestsListView.count)

        // Get the request data class.  The resource requests list view is zero based.
        val requestDataClass = (requestsListView.getItemAtPosition(id - 1) as RequestDataClass)

        // Create a view request dialog.
        val viewRequestDialogFragment: DialogFragment = ViewRequestDialog.request(id, isLastRequest, requestDataClass)

        // Make it so.
        viewRequestDialogFragment.show(supportFragmentManager, getString(R.string.request_details))
    }
}
