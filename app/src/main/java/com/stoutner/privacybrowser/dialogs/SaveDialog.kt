/*
 * Copyright 2019-2022 Soren Stoutner <soren@stoutner.com>.
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
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.helpers.GetUrlSizeHelper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.net.URL

// Define the class constants.
private const val URL_STRING = "url_string"
private const val FILE_SIZE_STRING = "file_size_string"
private const val FILE_NAME_STRING = "file_name_string"
private const val USER_AGENT_STRING = "user_agent_string"
private const val COOKIES_ENABLED = "cookies_enabled"

class SaveDialog : DialogFragment() {
    // Declare the class variables.
    private lateinit var saveListener: SaveListener

    // The public interface is used to send information back to the parent activity.
    interface SaveListener {
        fun onSaveUrl(originalUrlString: String, fileNameString: String, dialogFragment: DialogFragment)
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Get a handle for the save webpage listener from the launching context.
        saveListener = context as SaveListener
    }

    companion object {
        // `@JvmStatic` will no longer be required once all the code has transitioned to Kotlin.
        @JvmStatic
        fun saveUrl(urlString: String, fileSizeString: String, fileNameString: String, userAgentString: String, cookiesEnabled: Boolean): SaveDialog {
            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the arguments in the bundle.
            argumentsBundle.putString(URL_STRING, urlString)
            argumentsBundle.putString(FILE_SIZE_STRING, fileSizeString)
            argumentsBundle.putString(FILE_NAME_STRING, fileNameString)
            argumentsBundle.putString(USER_AGENT_STRING, userAgentString)
            argumentsBundle.putBoolean(COOKIES_ENABLED, cookiesEnabled)

            // Create a new instance of the save webpage dialog.
            val saveDialog = SaveDialog()

            // Add the arguments bundle to the new dialog.
            saveDialog.arguments = argumentsBundle

            // Return the new dialog.
            return saveDialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get the arguments from the bundle.
        val originalUrlString = requireArguments().getString(URL_STRING)!!
        val fileSizeString = requireArguments().getString(FILE_SIZE_STRING)!!
        val fileNameString = requireArguments().getString(FILE_NAME_STRING)!!
        val userAgentString = requireArguments().getString(USER_AGENT_STRING)!!
        val cookiesEnabled = requireArguments().getBoolean(COOKIES_ENABLED)

        // Use an alert dialog builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.PrivacyBrowserAlertDialog)

        // Set the title.
        dialogBuilder.setTitle(R.string.save_url)

        // Set the icon.
        dialogBuilder.setIcon(R.drawable.download)

        // Set the view.
        dialogBuilder.setView(R.layout.save_dialog)

        // Set the cancel button listener.  Using `null` as the listener closes the dialog without doing anything else.
        dialogBuilder.setNegativeButton(R.string.cancel, null)

        // Set the save button listener.
        dialogBuilder.setPositiveButton(R.string.save) { _: DialogInterface, _: Int ->
            // Return the dialog fragment to the parent activity.
            saveListener.onSaveUrl(originalUrlString, fileNameString, this)
        }

        // Create an alert dialog from the builder.
        val alertDialog = dialogBuilder.create()

        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Get the screenshot preference.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)

        // Disable screenshots if not allowed.
        if (!allowScreenshots) {
            alertDialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // The alert dialog must be shown before items in the layout can be modified.
        alertDialog.show()

        // Get handles for the layout items.
        val urlEditText = alertDialog.findViewById<EditText>(R.id.url_edittext)!!
        val fileSizeTextView = alertDialog.findViewById<TextView>(R.id.file_size_textview)!!
        val saveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)

        // Set the file size text view.
        fileSizeTextView.text = fileSizeString

        // Populate the URL edit text according to the type.  This must be done before the text change listener is created below so that the file size isn't requested again.
        if (originalUrlString.startsWith("data:")) {  // The URL contains the entire data of an image.
            // Get a substring of the data URL with the first 100 characters.  Otherwise, the user interface will freeze while trying to layout the edit text.
            val urlSubstring = originalUrlString.substring(0, 100) + "…"

            // Populate the URL edit text with the truncated URL.
            urlEditText.setText(urlSubstring)

            // Disable the editing of the URL edit text.
            urlEditText.inputType = InputType.TYPE_NULL
        } else {  // The URL contains a reference to the location of the data.
            // Populate the URL edit text with the full URL.
            urlEditText.setText(originalUrlString)
        }

        // Update the file size when the URL changes.
        urlEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                // Do nothing.
            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                // Do nothing.
            }

            override fun afterTextChanged(editable: Editable) {
                // Get the current URL to save.
                val urlToSave = urlEditText.text.toString()

                // Enable the save button if the URL is populated.
                saveButton.isEnabled = urlToSave.isNotEmpty()

                CoroutineScope(Dispatchers.Main).launch {
                    // Create a URL size string.
                    var urlSize: String

                    // Get the URL size on the IO thread.
                    withContext(Dispatchers.IO) {
                        // Get the URL size.
                        urlSize = GetUrlSizeHelper.getUrl(requireContext(), URL(urlToSave), userAgentString, cookiesEnabled)
                    }

                    // Display the updated URL.
                    fileSizeTextView.text = urlSize
                }
            }
        })

        // Return the alert dialog.
        return alertDialog
    }
}
