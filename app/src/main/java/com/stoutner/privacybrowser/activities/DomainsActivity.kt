/*
 * Copyright 2017-2024 Soren Stoutner <soren@stoutner.com>.
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

package com.stoutner.privacybrowser.activities

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ListView
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView

import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import androidx.cursoradapter.widget.CursorAdapter
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.dialogs.AddDomainDialog
import com.stoutner.privacybrowser.dialogs.AddDomainDialog.AddDomainListener
import com.stoutner.privacybrowser.fragments.DomainSettingsFragment
import com.stoutner.privacybrowser.fragments.DomainsListFragment
import com.stoutner.privacybrowser.fragments.DomainsListFragment.DismissSnackbarInterface
import com.stoutner.privacybrowser.fragments.DomainsListFragment.SaveDomainSettingsInterface
import com.stoutner.privacybrowser.helpers.DOMAIN_NAME
import com.stoutner.privacybrowser.helpers.ID
import com.stoutner.privacybrowser.helpers.DomainsDatabaseHelper

// Define the public constants.
const val CLOSE_ON_BACK = "close_on_back"
const val CURRENT_IP_ADDRESSES = "current_ip_addresses"
const val LOAD_DOMAIN = "load_domain"
const val SSL_END_DATE = "ssl_end_date"
const val SSL_ISSUED_BY_CNAME = "ssl_issued_by_cname"
const val SSL_ISSUED_BY_ONAME = "ssl_issued_by_oname"
const val SSL_ISSUED_BY_UNAME = "ssl_issued_by_uname"
const val SSL_ISSUED_TO_CNAME = "ssl_issued_to_cname"
const val SSL_ISSUED_TO_ONAME = "ssl_issued_to_oname"
const val SSL_ISSUED_TO_UNAME = "ssl_issued_to_uname"
const val SSL_START_DATE = "ssl_start_date"

// Define the class constants.
private const val DOMAIN_SETTINGS_DATABASE_ID = "domain_settings_database_id"
private const val DOMAIN_SETTINGS_DISPLAYED = "domain_settings_displayed"
private const val DOMAIN_SETTINGS_SCROLL_Y = "domain_settings_scroll_y"
private const val LISTVIEW_POSITION = "listview_position"

class DomainsActivity : AppCompatActivity(), AddDomainListener, DismissSnackbarInterface, SaveDomainSettingsInterface {
    companion object {
        // Define the public variables.
        var currentDomainDatabaseId = 0  // Used in `DomainsListFragment`.
        var dismissingSnackbar = false  // Used in `DomainsListFragment`.
        var domainsListViewPosition = 0  // Used in `DomainsListFragment`.
        var sslEndDateLong: Long = 0  // Used in `DomainsSettingsFragment`.
        var sslStartDateLong: Long = 0  // Used in `DomainSettingsFragment`.
        var twoPanedMode = false  // Used in `DomainsListFragment`.

        // Declare the public views.  They are used in `DomainsListFragment`.
        lateinit var deleteMenuItem: MenuItem

        // Declare the SSL certificate and IP address strings.
        var currentIpAddresses: String? = null  // Used in `DomainSettingsFragment`.
        var sslIssuedToCName: String? = null  // Used in `DomainSettingsFragment`.
        var sslIssuedToOName: String? = null  // Used in `DomainSettingsFragment`.
        var sslIssuedToUName: String? = null  // Used in `DomainSettingsFragment`.
        var sslIssuedByCName: String? = null  // Used in `DomainSettingsFragment`.
        var sslIssuedByOName: String? = null  // Used in `DomainSettingsFragment`.
        var sslIssuedByUName: String? = null  // Used in `DomainSettingsFragment`.
    }

    // Declare the class views.
    private lateinit var addDomainFAB: FloatingActionButton
    private lateinit var coordinatorLayout: View
    private var domainsListView: ListView? = null
    private var undoDeleteSnackbar: Snackbar? = null

    // Declare the class variables.
    private lateinit var domainsDatabaseHelper: DomainsDatabaseHelper

    // Define the class variables.
    private var appRestarted = false
    private var closeActivityAfterDismissingSnackbar = false
    private var closeOnBack = false
    private var deletedDomainPosition = 0
    private var domainSettingsDatabaseIdBeforeRestart = 0
    private var domainSettingsDisplayedBeforeRestart = false
    private var domainSettingsScrollY = 0
    private var goDirectlyToDatabaseId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Get the preferences.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)
        val bottomAppBar = sharedPreferences.getBoolean(getString(R.string.bottom_app_bar_key), false)

        // Disable screenshots if not allowed.
        if (!allowScreenshots) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // Run the default commands.
        super.onCreate(savedInstanceState)

        // Process the saved instance state if it is not null.
        if (savedInstanceState != null) {
            // Extract the values from the saved instance state if it is not null.
            domainsListViewPosition = savedInstanceState.getInt(LISTVIEW_POSITION)
            domainSettingsDisplayedBeforeRestart = savedInstanceState.getBoolean(DOMAIN_SETTINGS_DISPLAYED)
            domainSettingsDatabaseIdBeforeRestart = savedInstanceState.getInt(DOMAIN_SETTINGS_DATABASE_ID)
            domainSettingsScrollY = savedInstanceState.getInt(DOMAIN_SETTINGS_SCROLL_Y)

            // Set the app restarted flag.
            appRestarted = true
        }

        // Get the launching intent
        val intent = intent

        // Extract the domain to load if there is one.  `-1` is the default value.
        goDirectlyToDatabaseId = intent.getIntExtra(LOAD_DOMAIN, -1)

        // Get the status of close-on-back, which is true when the domains activity is called from the options menu.
        closeOnBack = intent.getBooleanExtra(CLOSE_ON_BACK, false)

        // Store the current SSL certificate information in class variables.
        sslIssuedToCName = intent.getStringExtra(SSL_ISSUED_TO_CNAME)
        sslIssuedToOName = intent.getStringExtra(SSL_ISSUED_TO_ONAME)
        sslIssuedToUName = intent.getStringExtra(SSL_ISSUED_TO_UNAME)
        sslIssuedByCName = intent.getStringExtra(SSL_ISSUED_BY_CNAME)
        sslIssuedByOName = intent.getStringExtra(SSL_ISSUED_BY_ONAME)
        sslIssuedByUName = intent.getStringExtra(SSL_ISSUED_BY_UNAME)
        sslStartDateLong = intent.getLongExtra(SSL_START_DATE, 0)
        sslEndDateLong = intent.getLongExtra(SSL_END_DATE, 0)
        currentIpAddresses = intent.getStringExtra(CURRENT_IP_ADDRESSES)

        // Set the content view.
        if (bottomAppBar)
            setContentView(R.layout.domains_bottom_appbar)
        else
            setContentView(R.layout.domains_top_appbar)

        // Get handles for the views.
        coordinatorLayout = findViewById(R.id.domains_coordinatorlayout)
        val toolbar = findViewById<Toolbar>(R.id.domains_toolbar)
        addDomainFAB = findViewById(R.id.add_domain_fab)

        // Set the support action bar.
        setSupportActionBar(toolbar)

        // Get a handle for the action bar.
        val actionBar = supportActionBar!!

        // Set the back arrow on the action bar.
        actionBar.setDisplayHomeAsUpEnabled(true)

        // Initialize the database handler.
        domainsDatabaseHelper = DomainsDatabaseHelper(this)

        // Determine if the device is in two pane mode.  `domain_settings_fragment_container` does not exist on devices with a width less than 900dp.
        twoPanedMode = (findViewById<View?>(R.id.domain_settings_fragment_container) != null)

        // Configure the add domain floating action button.
        addDomainFAB.setOnClickListener {
            // Create an add domain dialog.
            val addDomainDialog: DialogFragment = AddDomainDialog()

            // Show the add domain dialog.
            addDomainDialog.show(supportFragmentManager, resources.getString(R.string.add_domain))
        }

        // Control what the system back command does.
        val onBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (twoPanedMode) {  // The device is in two-paned mode.
                    // Save the current domain settings if the domain settings fragment is displayed.
                    if (findViewById<View?>(R.id.domain_settings_scrollview) != null)
                        saveDomainSettings(coordinatorLayout)

                    // Dismiss the undo delete snackbar if it is shown.
                    if (undoDeleteSnackbar != null && undoDeleteSnackbar!!.isShown) {
                        // Set the close flag.
                        closeActivityAfterDismissingSnackbar = true

                        // Dismiss the snackbar.
                        undoDeleteSnackbar!!.dismiss()
                    } else {
                        // Go home.
                        finish()
                    }
                } else if (closeOnBack) {  // Go directly back to the main WebView activity because the domains activity was launched from the options menu.
                    // Save the current domain settings.
                    saveDomainSettings(coordinatorLayout)

                    // Go home.
                    finish()
                } else if (findViewById<View?>(R.id.domain_settings_scrollview) != null) {  // The device is in single-paned mode and domain settings fragment is displayed.
                    // Save the current domain settings.
                    saveDomainSettings(coordinatorLayout)

                    // Instantiate a new domains list fragment.
                    val domainsListFragment = DomainsListFragment()

                    // Display the domains list fragment.
                    supportFragmentManager.beginTransaction().replace(R.id.domains_listview_fragment_container, domainsListFragment).commitNow()

                    // Populate the list of domains.  `-1` highlights the first domain if in two-paned mode.  It has no effect in single-paned mode.
                    populateDomainsListView(-1, domainsListViewPosition)

                    // Show the add domain floating action button.
                    addDomainFAB.show()

                    // Hide the delete menu item.
                    deleteMenuItem.isVisible = false
                } else {  // The device is in single-paned mode and the domain list fragment is displayed.
                    // Dismiss the undo delete SnackBar if it is shown.
                    if (undoDeleteSnackbar != null && undoDeleteSnackbar!!.isShown) {
                        // Set the close flag.
                        closeActivityAfterDismissingSnackbar = true

                        // Dismiss the snackbar.
                        undoDeleteSnackbar!!.dismiss()
                    } else {
                        // Go home.
                        finish()
                    }
                }
            }
        }

        // Register the on back pressed callback.
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu.
        menuInflater.inflate(R.menu.domains_options_menu, menu)

        // Get a handle for the delete menu item.
        deleteMenuItem = menu.findItem(R.id.delete_domain)

        // Only display the delete menu item (initially) in two-paned mode.
        deleteMenuItem.isVisible = twoPanedMode

        // Display the fragments.  This must be done from `onCreateOptionsMenu()` instead of `onCreate()` because `populateDomainsListView()` needs `deleteMenuItem` to be inflated.
        if (appRestarted && domainSettingsDisplayedBeforeRestart) {  // The app was restarted, possibly because the device was rotated, and domain settings were displayed previously.
            // Reset the app restarted flag.
            appRestarted = false

            if (twoPanedMode) {  // The device is in two-paned mode.
                // Initialize the domains list fragment.
                val domainsListFragment = DomainsListFragment()

                // Display the domains list fragment.
                supportFragmentManager.beginTransaction().replace(R.id.domains_listview_fragment_container, domainsListFragment).commitNow()

                // Populate the list of domains and highlight the domain that was highlighted before the restart.
                populateDomainsListView(domainSettingsDatabaseIdBeforeRestart, domainsListViewPosition)
            } else {  // The device is in single-paned mode.
                // Store the current domain database ID.
                currentDomainDatabaseId = domainSettingsDatabaseIdBeforeRestart

                // Create an arguments bundle.
                val argumentsBundle = Bundle()

                // Add the domain settings arguments.
                argumentsBundle.putInt(DomainSettingsFragment.DATABASE_ID, currentDomainDatabaseId)
                argumentsBundle.putInt(DomainSettingsFragment.SCROLL_Y, domainSettingsScrollY)

                // Instantiate a new domain settings fragment.
                val domainSettingsFragment = DomainSettingsFragment()

                // Add the arguments bundle to the domain settings fragment.
                domainSettingsFragment.arguments = argumentsBundle

                // Show the delete menu item.
                deleteMenuItem.isVisible = true

                // Hide the add domain floating action button.
                addDomainFAB.hide()

                // Display the domain settings fragment.
                supportFragmentManager.beginTransaction().replace(R.id.domains_listview_fragment_container, domainSettingsFragment).commitNow()
            }
        } else {  // The device was not restarted or, if it was, domain settings were not displayed previously.
            if (goDirectlyToDatabaseId >= 0) {  // Load the indicated domain settings.
                // Store the current domain database ID.
                currentDomainDatabaseId = goDirectlyToDatabaseId

                // Check if the device is in two-paned mode.
                if (twoPanedMode) {  // The device is in two-paned mode.
                    // Instantiate a new domains list fragment.
                    val domainsListFragment = DomainsListFragment()

                    // Display the domains list fragment.
                    supportFragmentManager.beginTransaction().replace(R.id.domains_listview_fragment_container, domainsListFragment).commitNow()

                    // Populate the list of domains.
                    populateDomainsListView(goDirectlyToDatabaseId, domainsListViewPosition)
                } else {  // The device is in single-paned mode.
                    // Create an arguments bundle.
                    val argumentsBundle = Bundle()

                    // Add the domain settings to arguments bundle.
                    argumentsBundle.putInt(DomainSettingsFragment.DATABASE_ID, goDirectlyToDatabaseId)
                    argumentsBundle.putInt(DomainSettingsFragment.SCROLL_Y, domainSettingsScrollY)

                    // Instantiate a new domain settings fragment.
                    val domainSettingsFragment = DomainSettingsFragment()

                    // Add the arguments bundle to the domain settings fragment.
                    domainSettingsFragment.arguments = argumentsBundle

                    // Show the delete menu item.
                    deleteMenuItem.isVisible = true

                    // Hide the add domain floating action button.
                    addDomainFAB.hide()

                    // Display the domain settings fragment.
                    supportFragmentManager.beginTransaction().replace(R.id.domains_listview_fragment_container, domainSettingsFragment).commitNow()
                }
            } else {  // Highlight the first domain.
                // Instantiate a new domains list fragment.
                val domainsListFragment = DomainsListFragment()

                // Display the domain list fragment.
                supportFragmentManager.beginTransaction().replace(R.id.domains_listview_fragment_container, domainsListFragment).commitNow()

                // Populate the list of domains.  `-1` highlights the first domain.
                populateDomainsListView(-1, domainsListViewPosition)
            }
        }

        // Success!
        return true
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        // Run the command according to the selected menu item.
        when (menuItem.itemId) {
            android.R.id.home -> {  // The home arrow is identified as `android.R.id.home`, not just `R.id.home`.
                // Check if the device is in two-paned mode.
                if (twoPanedMode) {  // The device is in two-paned mode.
                    // Save the current domain settings if the domain settings fragment is displayed.
                    if (findViewById<View?>(R.id.domain_settings_scrollview) != null)
                        saveDomainSettings(coordinatorLayout)

                    // Dismiss the undo delete snackbar if it is shown.
                    if (undoDeleteSnackbar != null && undoDeleteSnackbar!!.isShown) {
                        // Set the close flag.
                        closeActivityAfterDismissingSnackbar = true

                        // Dismiss the snackbar.
                        undoDeleteSnackbar!!.dismiss()
                    } else {
                        // Go home.
                        finish()
                    }
                } else if (closeOnBack) {  // Go directly back to the main WebView activity because the domains activity was launched from the options menu.
                    // Save the current domain settings.
                    saveDomainSettings(coordinatorLayout)

                    // Go home.
                    finish()
                } else if (findViewById<View?>(R.id.domain_settings_scrollview) != null) {  // The device is in single-paned mode and the domain settings fragment is displayed.
                    // Save the current domain settings.
                    saveDomainSettings(coordinatorLayout)

                    // Instantiate a new domains list fragment.
                    val domainsListFragment = DomainsListFragment()

                    // Display the domains list fragment.
                    supportFragmentManager.beginTransaction().replace(R.id.domains_listview_fragment_container, domainsListFragment).commitNow()

                    // Populate the list of domains.  `-1` highlights the first domain if in two-paned mode.  It has no effect in single-paned mode.
                    populateDomainsListView(-1, domainsListViewPosition)

                    // Show the add domain floating action button.
                    addDomainFAB.show()

                    // Hide the delete menu item.
                    deleteMenuItem.isVisible = false
                } else {  // The device is in single-paned mode and domains list fragment is displayed.
                    // Dismiss the undo delete snackbar if it is shown.
                    if (undoDeleteSnackbar != null && undoDeleteSnackbar!!.isShown) {
                        // Set the close flag.
                        closeActivityAfterDismissingSnackbar = true

                        // Dismiss the snackbar.
                        undoDeleteSnackbar!!.dismiss()
                    } else {
                        // Go home.
                        finish()
                    }
                }
            }

            R.id.delete_domain -> {  // Delete.
                // Get a handle for the activity (used in an inner class below).
                val activity: Activity = this

                // Check to see if the domain settings were loaded directly for editing of this app in single-paned mode.
                if (closeOnBack && !twoPanedMode) {  // The activity should delete the domain settings and exit straight to the the main WebView activity.
                    // Delete the selected domain.
                    domainsDatabaseHelper.deleteDomain(currentDomainDatabaseId)

                    // Go home.
                    NavUtils.navigateUpFromSameTask(activity)
                } else {  // A snackbar should be shown before deleting the domain settings.
                    // Reset close-on-back, which otherwise can cause errors if the system attempts to save settings for a domain that no longer exists.
                    closeOnBack = false

                    // Store a copy of the current domain database ID because it could change while the snackbar is displayed.
                    val databaseIdToDelete = currentDomainDatabaseId

                    // Update the fragments and menu items.
                    if (twoPanedMode) {  // Two-paned mode.
                        // Store the deleted domain position, which is needed if undo is selected in the snackbar.
                        deletedDomainPosition = domainsListView!!.checkedItemPosition

                        // Disable the delete menu item.
                        deleteMenuItem.isEnabled = false

                        // Remove the domain settings fragment.
                        supportFragmentManager.beginTransaction().remove(supportFragmentManager.findFragmentById(R.id.domain_settings_fragment_container)!!).commitNow()
                    } else {  // Single-paned mode.
                        // Instantiate a new domains list fragment.
                        val domainsListFragment = DomainsListFragment()

                        // Display the domains list fragment.
                        supportFragmentManager.beginTransaction().replace(R.id.domains_listview_fragment_container, domainsListFragment).commitNow()

                        // Show the add domain floating action button.
                        addDomainFAB.show()

                        // Hide the delete menu item.
                        deleteMenuItem.isVisible = false
                    }

                    // Get a cursor that does not show the domain to be deleted.
                    val domainsPendingDeleteCursor = domainsDatabaseHelper.getDomainNameCursorOrderedByDomainExcept(databaseIdToDelete)

                    // Setup the domains pending delete cursor adapter.
                    val domainsPendingDeleteCursorAdapter: CursorAdapter = object : CursorAdapter(this, domainsPendingDeleteCursor, false) {
                        override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
                            // Inflate the individual item layout.
                            return layoutInflater.inflate(R.layout.domain_name_linearlayout, parent, false)
                        }

                        override fun bindView(view: View, context: Context, cursor: Cursor) {
                            // Get the domain name string.
                            val domainNameString = cursor.getString(cursor.getColumnIndexOrThrow(DOMAIN_NAME))

                            // Get a handle for the domain name text view.
                            val domainNameTextView = view.findViewById<TextView>(R.id.domain_name_textview)

                            // Display the domain name.
                            domainNameTextView.text = domainNameString
                        }
                    }

                    // Update the handle for the current domains list view.
                    domainsListView = findViewById(R.id.domains_listview)

                    // Update the list view.
                    domainsListView!!.adapter = domainsPendingDeleteCursorAdapter

                    // Display a snackbar.
                    undoDeleteSnackbar = Snackbar.make(domainsListView!!, R.string.domain_deleted, Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo) {}  // Do nothing because everything will be handled by `onDismissed()` below.
                        .addCallback(object : Snackbar.Callback() {
                            override fun onDismissed(snackbar: Snackbar, event: Int) {
                                // Run commands based on the event.
                                if (event == DISMISS_EVENT_ACTION) {  // The user pushed the `Undo` button.
                                    // Create an arguments bundle.
                                    val argumentsBundle = Bundle()

                                    // Store the domains settings in the arguments bundle.
                                    argumentsBundle.putInt(DomainSettingsFragment.DATABASE_ID, databaseIdToDelete)
                                    argumentsBundle.putInt(DomainSettingsFragment.SCROLL_Y, domainSettingsScrollY)

                                    // Instantiate a new domain settings fragment.
                                    val domainSettingsFragment = DomainSettingsFragment()

                                    // Add the arguments bundle to the domain settings fragment.
                                    domainSettingsFragment.arguments = argumentsBundle

                                    // Display the correct fragments.
                                    if (twoPanedMode) {  // The device in in two-paned mode.
                                        // Get a cursor with the current contents of the domains database.
                                        val undoDeleteDomainsCursor = domainsDatabaseHelper.domainNameCursorOrderedByDomain

                                        // Setup the domains cursor adapter.
                                        val undoDeleteDomainsCursorAdapter: CursorAdapter = object : CursorAdapter(applicationContext, undoDeleteDomainsCursor, false) {
                                            override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
                                                // Inflate the individual item layout.
                                                return layoutInflater.inflate(R.layout.domain_name_linearlayout, parent, false)
                                            }

                                            override fun bindView(view: View, context: Context, cursor: Cursor) {
                                                /// Get the domain name string.
                                                val domainNameString = cursor.getString(cursor.getColumnIndexOrThrow(DOMAIN_NAME))

                                                // Get a handle for the domain name text view.
                                                val domainNameTextView = view.findViewById<TextView>(R.id.domain_name_textview)

                                                // Display the domain name.
                                                domainNameTextView.text = domainNameString
                                            }
                                        }

                                        // Update the domains list view.
                                        domainsListView!!.adapter = undoDeleteDomainsCursorAdapter

                                        // Select the previously deleted domain in the list view.
                                        domainsListView!!.setItemChecked(deletedDomainPosition, true)

                                        // Display the domain settings fragment.
                                        supportFragmentManager.beginTransaction().replace(R.id.domain_settings_fragment_container, domainSettingsFragment).commitNow()

                                        // Enable the delete menu item.
                                        deleteMenuItem.isEnabled = true
                                    } else {  // The device in in one-paned mode.
                                        // Display the domain settings fragment.
                                        supportFragmentManager.beginTransaction().replace(R.id.domains_listview_fragment_container, domainSettingsFragment).commitNow()

                                        // Hide the add domain floating action button.
                                        addDomainFAB.hide()

                                        // Show and enable the delete menu item.
                                        deleteMenuItem.isVisible = true

                                        // Display the domain settings fragment.
                                        supportFragmentManager.beginTransaction().replace(R.id.domains_listview_fragment_container, domainSettingsFragment).commitNow()
                                    }
                                } else {  // The snackbar was dismissed without the undo button being pushed.
                                    // Delete the selected domain.
                                    domainsDatabaseHelper.deleteDomain(databaseIdToDelete)

                                    // Enable the delete menu item if the system was waiting for a snackbar to be dismissed.
                                    if (dismissingSnackbar) {
                                        // Create a runnable to enable the delete menu item.
                                        val enableDeleteMenuItemRunnable = Runnable {
                                            // Enable or show the delete menu item according to the display mode.
                                            if (twoPanedMode)
                                                deleteMenuItem.isEnabled = true
                                            else
                                                deleteMenuItem.isVisible = true

                                            // Reset the dismissing snackbar tracker.
                                            dismissingSnackbar = false
                                        }

                                        // Instantiate a handler running the main looper.
                                        val handler = Handler(mainLooper)

                                        // Enable or show the delete menu icon after 100 milliseconds to make sure that the previous domain has been deleted from the database.
                                        handler.postDelayed(enableDeleteMenuItemRunnable, 100)
                                    }

                                    // Close the activity if back was pressed.
                                    if (closeActivityAfterDismissingSnackbar)
                                        NavUtils.navigateUpFromSameTask(activity)
                                }
                            }
                        })

                    // Show the Snackbar.
                    undoDeleteSnackbar!!.show()
                }
            }
        }

        // Consume the event.
        return true
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        // Run the default commands.
        super.onSaveInstanceState(savedInstanceState)

        // Get a handle for the domain settings scrollview.
        val domainSettingsScrollView = findViewById<ScrollView>(R.id.domain_settings_scrollview)

        // Check to see if the domain settings scrollview exists.
        if (domainSettingsScrollView == null) {  // The domain settings are not displayed.
            // Store the domain settings status in the bundle.
            savedInstanceState.putBoolean(DOMAIN_SETTINGS_DISPLAYED, false)
            savedInstanceState.putInt(DOMAIN_SETTINGS_DATABASE_ID, -1)
            savedInstanceState.putInt(DOMAIN_SETTINGS_SCROLL_Y, 0)
        } else {  // The domain settings are displayed.
            // Save any changes that have been made to the domain settings.
            saveDomainSettings(coordinatorLayout)

            // Get the domain settings scroll Y.
            val domainSettingsScrollY = domainSettingsScrollView.scrollY

            // Store the domain settings status in the bundle.
            savedInstanceState.putBoolean(DOMAIN_SETTINGS_DISPLAYED, true)
            savedInstanceState.putInt(DOMAIN_SETTINGS_DATABASE_ID, DomainSettingsFragment.databaseId)
            savedInstanceState.putInt(DOMAIN_SETTINGS_SCROLL_Y, domainSettingsScrollY)
        }

        // Check to see if the domains listview exists.
        if (domainsListView != null) {
            // Get the domains listview position.
            val domainsListViewPosition = domainsListView!!.firstVisiblePosition

            // Store the listview position in the bundle.
            savedInstanceState.putInt(LISTVIEW_POSITION, domainsListViewPosition)
        }
    }

    override fun addDomain(dialogFragment: DialogFragment) {
        // Dismiss the undo delete snackbar if it is currently displayed.
        if (undoDeleteSnackbar != null && undoDeleteSnackbar!!.isShown)
            undoDeleteSnackbar!!.dismiss()

        // Get a handle for the domain name edit text.
        val domainNameEditText = dialogFragment.dialog!!.findViewById<EditText>(R.id.domain_name_edittext)

        // Get the domain name string.
        val domainNameString = domainNameEditText.text.toString()

        // Create the domain and store the database ID in the current domain database ID.
        currentDomainDatabaseId = domainsDatabaseHelper.addDomain(domainNameString)

        // Display the newly created domain.
        if (twoPanedMode) {  // The device in in two-paned mode.
            populateDomainsListView(currentDomainDatabaseId, 0)
        } else {  // The device is in single-paned mode.
            // Hide the add domain floating action button.
            addDomainFAB.hide()

            // Show and enable the delete menu item.
            deleteMenuItem.isVisible = true

            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Add the domain settings to the arguments bundle.  The scroll Y should always be `0` on a new domain.
            argumentsBundle.putInt(DomainSettingsFragment.DATABASE_ID, currentDomainDatabaseId)
            argumentsBundle.putInt(DomainSettingsFragment.SCROLL_Y, 0)

            // Instantiate a new domain settings fragment.
            val domainSettingsFragment = DomainSettingsFragment()

            // Add the arguments bundle to the domain setting fragment.
            domainSettingsFragment.arguments = argumentsBundle

            // Display the domain settings fragment.
            supportFragmentManager.beginTransaction().replace(R.id.domains_listview_fragment_container, domainSettingsFragment).commitNow()
        }
    }

    override fun saveDomainSettings(view: View) {
        // Get handles for the domain settings.
        val domainNameEditText = view.findViewById<EditText>(R.id.domain_settings_name_edittext)
        val javaScriptSpinner = view.findViewById<Spinner>(R.id.javascript_spinner)
        val cookiesSpinner = view.findViewById<Spinner>(R.id.cookies_spinner)
        val domStorageSpinner = view.findViewById<Spinner>(R.id.dom_storage_spinner)
        val userAgentSpinner = view.findViewById<Spinner>(R.id.user_agent_spinner)
        val customUserAgentEditText = view.findViewById<EditText>(R.id.custom_user_agent_edittext)
        val easyListSpinner = view.findViewById<Spinner>(R.id.easylist_spinner)
        val easyPrivacySpinner = view.findViewById<Spinner>(R.id.easyprivacy_spinner)
        val fanboysAnnoyanceSpinner = view.findViewById<Spinner>(R.id.fanboys_annoyance_list_spinner)
        val fanboysSocialBlockingSpinner = view.findViewById<Spinner>(R.id.fanboys_social_blocking_list_spinner)
        val ultraListSpinner = view.findViewById<Spinner>(R.id.ultralist_spinner)
        val ultraPrivacySpinner = view.findViewById<Spinner>(R.id.ultraprivacy_spinner)
        val blockAllThirdPartyRequestsSpinner = view.findViewById<Spinner>(R.id.block_all_third_party_requests_spinner)
        val fontSizeSpinner = view.findViewById<Spinner>(R.id.font_size_spinner)
        val customFontSizeEditText = view.findViewById<EditText>(R.id.custom_font_size_edittext)
        val swipeToRefreshSpinner = view.findViewById<Spinner>(R.id.swipe_to_refresh_spinner)
        val webViewThemeSpinner = view.findViewById<Spinner>(R.id.webview_theme_spinner)
        val wideViewportSpinner = view.findViewById<Spinner>(R.id.wide_viewport_spinner)
        val displayWebpageImagesSpinner = view.findViewById<Spinner>(R.id.display_images_spinner)
        val pinnedSslCertificateSwitch = view.findViewById<SwitchCompat>(R.id.pinned_ssl_certificate_switch)
        val currentWebsiteCertificateRadioButton = view.findViewById<RadioButton>(R.id.current_website_certificate_radiobutton)
        val pinnedIpAddressesSwitch = view.findViewById<SwitchCompat>(R.id.pinned_ip_addresses_switch)
        val currentIpAddressesRadioButton = view.findViewById<RadioButton>(R.id.current_ip_addresses_radiobutton)

        // Extract the data for the domain settings.
        val domainNameString = domainNameEditText.text.toString()
        val javaScriptInt = javaScriptSpinner.selectedItemPosition
        val cookiesInt = cookiesSpinner.selectedItemPosition
        val domStorageInt = domStorageSpinner.selectedItemPosition
        val userAgentSwitchPosition = userAgentSpinner.selectedItemPosition
        val easyListInt = easyListSpinner.selectedItemPosition
        val easyPrivacyInt = easyPrivacySpinner.selectedItemPosition
        val fanboysAnnoyanceInt = fanboysAnnoyanceSpinner.selectedItemPosition
        val fanboysSocialBlockingInt = fanboysSocialBlockingSpinner.selectedItemPosition
        val ultraListInt = ultraListSpinner.selectedItemPosition
        val ultraPrivacyInt = ultraPrivacySpinner.selectedItemPosition
        val blockAllThirdPartyRequestsInt = blockAllThirdPartyRequestsSpinner.selectedItemPosition
        val fontSizeSwitchPosition = fontSizeSpinner.selectedItemPosition
        val swipeToRefreshInt = swipeToRefreshSpinner.selectedItemPosition
        val webViewThemeInt = webViewThemeSpinner.selectedItemPosition
        val wideViewportInt = wideViewportSpinner.selectedItemPosition
        val displayWebpageImagesInt = displayWebpageImagesSpinner.selectedItemPosition
        val pinnedSslCertificate = pinnedSslCertificateSwitch.isChecked
        val pinnedIpAddress = pinnedIpAddressesSwitch.isChecked

        // Get the user agent name.
        val userAgentName: String = when (userAgentSwitchPosition) {
            // Set the user agent name to be `System default user agent`.
            DOMAINS_SYSTEM_DEFAULT_USER_AGENT -> resources.getString(R.string.system_default_user_agent)

            // Set the user agent name to be the custom user agent.
            DOMAINS_CUSTOM_USER_AGENT -> customUserAgentEditText.text.toString()

            else -> {
                // Get the array of user agent names.
                val userAgentNameArray = resources.getStringArray(R.array.user_agent_names)

                // Set the user agent name from the array.  The domain spinner has one more entry than the name array, so the position must be decremented.
                userAgentNameArray[userAgentSwitchPosition - 1]
            }
        }

        // Initialize the font size integer.  `0` indicates the system default font size.
        var fontSizeInt = 0

        // Use a custom font size if it is selected.
        if (fontSizeSwitchPosition == 1)
            fontSizeInt = customFontSizeEditText.text.toString().toInt()

        // Save the domain settings.
        domainsDatabaseHelper.updateDomain(currentDomainDatabaseId, domainNameString, javaScriptInt, cookiesInt, domStorageInt, userAgentName, easyListInt, easyPrivacyInt, fanboysAnnoyanceInt,
            fanboysSocialBlockingInt, ultraListInt, ultraPrivacyInt, blockAllThirdPartyRequestsInt, fontSizeInt, swipeToRefreshInt, webViewThemeInt, wideViewportInt, displayWebpageImagesInt,
            pinnedSslCertificate, pinnedIpAddress)

        // Update the pinned SSL certificate if a new one is checked.
        if (currentWebsiteCertificateRadioButton.isChecked)
            domainsDatabaseHelper.updatePinnedSslCertificate(currentDomainDatabaseId, sslIssuedToCName!!, sslIssuedToOName!!, sslIssuedToUName!!, sslIssuedByCName!!, sslIssuedByOName!!, sslIssuedByUName!!,
                sslStartDateLong, sslEndDateLong)

        // Update the pinned IP addresses if new ones are checked.
        if (currentIpAddressesRadioButton.isChecked)
            domainsDatabaseHelper.updatePinnedIpAddresses(currentDomainDatabaseId, currentIpAddresses!!)
    }

    private fun populateDomainsListView(highlightedDomainDatabaseId: Int, domainsListViewPosition: Int) {
        // Get a cursor with the current contents of the domains database.
        val domainsCursor = domainsDatabaseHelper.domainNameCursorOrderedByDomain

        // Setup the domains cursor adapter.
        val domainsCursorAdapter: CursorAdapter = object : CursorAdapter(applicationContext, domainsCursor, false) {
            override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
                // Inflate the individual item layout.
                return layoutInflater.inflate(R.layout.domain_name_linearlayout, parent, false)
            }

            override fun bindView(view: View, context: Context, cursor: Cursor) {
                // Get a handle for the domain name text view.
                val domainNameTextView = view.findViewById<TextView>(R.id.domain_name_textview)

                // Get the domain name string.
                val domainNameString = cursor.getString(cursor.getColumnIndexOrThrow(DOMAIN_NAME))

                // Set the domain name.
                domainNameTextView.text = domainNameString
            }
        }

        // get a handle for the current domains listview.
        domainsListView = findViewById(R.id.domains_listview)

        // Update the list view.
        domainsListView!!.adapter = domainsCursorAdapter

        // Restore the scroll position.
        domainsListView!!.setSelection(domainsListViewPosition)

        // Display the domain settings in the second pane if operating in two pane mode and the database contains at least one domain.
        if (twoPanedMode && domainsCursor.count > 0) {  // Two-paned mode is enabled and there is at least one domain.
            // Initialize the highlighted domain position tracker.
            var highlightedDomainPosition = 0

            // Get the cursor position for the highlighted domain.
            for (i in 0 until domainsCursor.count) {
                // Move to position `i` in the cursor.
                domainsCursor.moveToPosition(i)

                // Get the database ID for this position.
                val currentDatabaseId = domainsCursor.getInt(domainsCursor.getColumnIndexOrThrow(ID))

                // Set the highlighted domain position if the database ID for this matches the highlighted domain database ID.
                if (highlightedDomainDatabaseId == currentDatabaseId)
                    highlightedDomainPosition = i
            }

            // Select the highlighted domain.
            domainsListView!!.setItemChecked(highlightedDomainPosition, true)

            // Move to the highlighted domain.
            domainsCursor.moveToPosition(highlightedDomainPosition)

            // Get the database ID for the highlighted domain.
            currentDomainDatabaseId = domainsCursor.getInt(domainsCursor.getColumnIndexOrThrow(ID))

            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the domain settings in the arguments bundle.
            argumentsBundle.putInt(DomainSettingsFragment.DATABASE_ID, currentDomainDatabaseId)
            argumentsBundle.putInt(DomainSettingsFragment.SCROLL_Y, domainSettingsScrollY)

            // Instantiate a new domain settings fragment.
            val domainSettingsFragment = DomainSettingsFragment()

            // Add the arguments bundle to the domain settings fragment.
            domainSettingsFragment.arguments = argumentsBundle

            // Display the domain settings fragment.
            supportFragmentManager.beginTransaction().replace(R.id.domain_settings_fragment_container, domainSettingsFragment).commit()

            // Enable the delete options menu items.
            deleteMenuItem.isEnabled = true
        } else if (twoPanedMode) {  // Two-paned mode is enabled but there are no domains.
            // Disable the delete menu item.
            deleteMenuItem.isEnabled = false
        }
    }

    override fun dismissSnackbar() {
        // Dismiss the undo delete snackbar if it is shown.
        if (undoDeleteSnackbar != null && undoDeleteSnackbar!!.isShown) {
            // Dismiss the snackbar.
            undoDeleteSnackbar!!.dismiss()
        }
    }

    public override fun onDestroy() {
        // Close the domains database helper.
        domainsDatabaseHelper.close()

        // Run the default commands.
        super.onDestroy()
    }
}
