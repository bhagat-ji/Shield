/*
 * Copyright © 2017-2022 Soren Stoutner <soren@stoutner.com>.
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
import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.helpers.DomainsDatabaseHelper

// Define the class constants.
private const val URL_STRING = "url_string"

class AddDomainDialog : DialogFragment() {
    // Declare the class variables
    private lateinit var addDomainListener: AddDomainListener

    // The public interface is used to send information back to the parent activity.
    interface AddDomainListener {
        fun onAddDomain(dialogFragment: DialogFragment)
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Get a handle for the listener from the launching context.
        addDomainListener = context as AddDomainListener
    }

    companion object {
        // `@JvmStatic` will no longer be required once all the code has transitioned to Kotlin.
        @JvmStatic
        fun addDomain(urlString: String): AddDomainDialog {
            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the URL in the bundle.
            argumentsBundle.putString(URL_STRING, urlString)

            // Create a new instance of the dialog.
            val addDomainDialog = AddDomainDialog()

            // Add the arguments bundle to the dialog.
            addDomainDialog.arguments = argumentsBundle

            // Return the new dialog.
            return addDomainDialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get the arguments.
        val arguments = requireArguments()

        // Get the URL from the bundle.
        val urlString = arguments.getString(URL_STRING)

        // Use an alert dialog builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.PrivacyBrowserAlertDialog)

        // Set the icon.
        dialogBuilder.setIcon(R.drawable.domains)

        // Set the title.
        dialogBuilder.setTitle(R.string.add_domain)

        // Set the view.
        dialogBuilder.setView(R.layout.add_domain_dialog)

        // Set the cancel button listener.  Using `null` as the listener closes the dialog without doing anything else.
        dialogBuilder.setNegativeButton(R.string.cancel, null)

        // Set the add button listener.
        dialogBuilder.setPositiveButton(R.string.add) { _: DialogInterface, _: Int ->
            // Return the dialog fragment to the parent activity on add.
            addDomainListener.onAddDomain(this)
        }

        // Create an alert dialog from the builder.
        val alertDialog = dialogBuilder.create()

        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Get the screenshot preference.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots), false)

        // Disable screenshots if not allowed.
        if (!allowScreenshots) {
            alertDialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // The alert dialog must be shown before the contents can be modified.
        alertDialog.show()

        // Initialize the domains database helper.  The `0` specifies the database version, but that is ignored and set instead using a constant in domains database helper.
        val domainsDatabaseHelper = DomainsDatabaseHelper(context, null, null, 0)

        // Get handles for the views in the alert dialog.
        val addDomainEditText = alertDialog.findViewById<EditText>(R.id.domain_name_edittext)!!
        val domainNameAlreadyExistsTextView = alertDialog.findViewById<TextView>(R.id.domain_name_already_exists_textview)!!
        val addButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)

        //  Update the status of the warning text and the add button when the domain name changes.
        addDomainEditText.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // Do nothing.
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // Do nothing.
            }

            override fun afterTextChanged(s: Editable) {
                if (domainsDatabaseHelper.getCursorForDomainName(addDomainEditText.text.toString()).count > 0) {  // The domain already exists.
                    // Show the warning text.
                    domainNameAlreadyExistsTextView.visibility = View.VISIBLE

                    // Disable the add button.
                    addButton.isEnabled = false
                } else {  // The domain do not yet exist.
                    // Hide the warning text.
                    domainNameAlreadyExistsTextView.visibility = View.GONE

                    // Enable the add button.
                    addButton.isEnabled = true
                }
            }
        })

        // Convert the URL string to a URI.
        val currentUri = Uri.parse(urlString)

        // Display the host in the add domain edit text.
        addDomainEditText.setText(currentUri.host)

        // Allow the enter key on the keyboard to create the domain from the add domain edit text.
        addDomainEditText.setOnKeyListener { _: View, keyCode: Int, keyEvent: KeyEvent ->
            // Check the key code and event.
            if (keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_DOWN) {  // The event is a key-down on the enter key.
                // Trigger the add domain listener and return the dialog fragment to the parent activity.
                addDomainListener.onAddDomain(this)

                // Manually dismiss the alert dialog.
                alertDialog.dismiss()

                // Consume the event.
                return@setOnKeyListener true
            } else {  // Some other key was pressed.
                // Do not consume the event.
                return@setOnKeyListener false
            }
        }

        // Return the alert dialog.
        return alertDialog
    }
}