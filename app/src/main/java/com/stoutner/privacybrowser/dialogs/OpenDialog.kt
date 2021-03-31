/*
 * Copyright © 2019-2021 Soren Stoutner <soren@stoutner.com>.
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

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.activities.MainWebViewActivity

class OpenDialog : DialogFragment() {
    // Define the open listener.
    private lateinit var openListener: OpenListener

    // The public interface is used to send information back to the parent activity.
    interface OpenListener {
        fun onOpen(dialogFragment: DialogFragment)
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Get a handle for the open listener from the launching context.
        openListener = context as OpenListener
    }

    // `@SuppressLint("InflateParams")` removes the warning about using null as the parent view group when inflating the alert dialog.
    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use an alert dialog builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.PrivacyBrowserAlertDialog)

        // Get the current theme status.
        val currentThemeStatus = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        // Set the icon according to the theme.
        if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {
            dialogBuilder.setIcon(R.drawable.proxy_enabled_day)
        } else {
            dialogBuilder.setIcon(R.drawable.proxy_enabled_night)
        }

        // Set the title.
        dialogBuilder.setTitle(R.string.open)

        // Set the view.  The parent view is null because it will be assigned by the alert dialog.
        dialogBuilder.setView(layoutInflater.inflate(R.layout.open_dialog, null))

        // Set the cancel button listener.  Using `null` as the listener closes the dialog without doing anything else.
        dialogBuilder.setNegativeButton(R.string.cancel, null)

        // Set the open button listener.
        dialogBuilder.setPositiveButton(R.string.open) { _: DialogInterface?, _: Int ->
            // Return the dialog fragment to the parent activity.
            openListener.onOpen(this)
        }

        // Create an alert dialog from the builder.
        val alertDialog = dialogBuilder.create()

        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        // Get the screenshot preference.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)

        // Disable screenshots if not allowed.
        if (!allowScreenshots) {
            alertDialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // The alert dialog must be shown before items in the layout can be modified.
        alertDialog.show()

        // Get handles for the layout items.
        val fileNameEditText = alertDialog.findViewById<EditText>(R.id.file_name_edittext)!!
        val browseButton = alertDialog.findViewById<Button>(R.id.browse_button)!!
        val openButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)

        // Initially disable the open button.
        openButton.isEnabled = false

        // Update the status of the open button when the file name changes.
        fileNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                // Do nothing.
            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                // Do nothing.
            }

            override fun afterTextChanged(editable: Editable) {
                // Get the current file name.
                val fileNameString = fileNameEditText.text.toString()

                // Enable the open button if the file name is populated.
                openButton.isEnabled = fileNameString.isNotEmpty()
            }
        })

        // Handle clicks on the browse button.
        browseButton.setOnClickListener {
            // Create the file picker intent.
            val browseIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)

            // Only display files that can be opened.
            browseIntent.addCategory(Intent.CATEGORY_OPENABLE)

            // Set the intent MIME type to include all files so that everything is visible.
            browseIntent.type = "*/*"

            // Start the file picker.  This must be started under `activity` to that the request code is returned correctly.
            requireActivity().startActivityForResult(browseIntent, MainWebViewActivity.BROWSE_OPEN_REQUEST_CODE)
        }

        // Return the alert dialog.
        return alertDialog
    }
}