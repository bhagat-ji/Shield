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
import android.os.AsyncTask
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.google.android.material.textfield.TextInputLayout

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.activities.MainWebViewActivity
import com.stoutner.privacybrowser.asynctasks.GetUrlSize

// Define the class constants.
private const val SAVE_TYPE = "save_type"
private const val URL_STRING = "url_string"
private const val FILE_SIZE_STRING = "file_size_string"
private const val FILE_NAME_STRING = "file_name_string"
private const val USER_AGENT_STRING = "user_agent_string"
private const val COOKIES_ENABLED = "cookies_enabled"

class SaveWebpageDialog : DialogFragment() {
    // Declare the class variables.
    private lateinit var saveWebpageListener: SaveWebpageListener

    // Define the class variables.
    private var getUrlSize: AsyncTask<*, *, *>? = null

    // The public interface is used to send information back to the parent activity.
    interface SaveWebpageListener {
        fun onSaveWebpage(saveType: Int, originalUrlString: String?, dialogFragment: DialogFragment)
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Get a handle for the save webpage listener from the launching context.
        saveWebpageListener = context as SaveWebpageListener
    }

    companion object {
        // Define the companion object constants.  These can be moved to class constants once all of the code has transitioned to Kotlin.
        const val SAVE_URL = 0
        const val SAVE_ARCHIVE = 1
        const val SAVE_IMAGE = 2

        // `@JvmStatic` will no longer be required once all the code has transitioned to Kotlin.
        @JvmStatic
        fun saveWebpage(saveType: Int, urlString: String?, fileSizeString: String?, fileNameString: String, userAgentString: String?, cookiesEnabled: Boolean): SaveWebpageDialog {
            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the arguments in the bundle.
            argumentsBundle.putInt(SAVE_TYPE, saveType)
            argumentsBundle.putString(URL_STRING, urlString)
            argumentsBundle.putString(FILE_SIZE_STRING, fileSizeString)
            argumentsBundle.putString(FILE_NAME_STRING, fileNameString)
            argumentsBundle.putString(USER_AGENT_STRING, userAgentString)
            argumentsBundle.putBoolean(COOKIES_ENABLED, cookiesEnabled)

            // Create a new instance of the save webpage dialog.
            val saveWebpageDialog = SaveWebpageDialog()

            // Add the arguments bundle to the new dialog.
            saveWebpageDialog.arguments = argumentsBundle

            // Return the new dialog.
            return saveWebpageDialog
        }
    }

    // `@SuppressLint("InflateParams")` removes the warning about using null as the parent view group when inflating the alert dialog.
    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get the arguments from the bundle.
        val saveType = requireArguments().getInt(SAVE_TYPE)
        val originalUrlString = requireArguments().getString(URL_STRING)
        val fileSizeString = requireArguments().getString(FILE_SIZE_STRING)
        val fileNameString = requireArguments().getString(FILE_NAME_STRING)!!
        val userAgentString = requireArguments().getString(USER_AGENT_STRING)
        val cookiesEnabled = requireArguments().getBoolean(COOKIES_ENABLED)

        // Use an alert dialog builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.PrivacyBrowserAlertDialog)

        // Get the current theme status.
        val currentThemeStatus = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        // Set the title and icon according to the save type.
        when (saveType) {
            SAVE_URL -> {
                // Set the title.
                dialogBuilder.setTitle(R.string.save_url)

                // Set the icon according to the theme.
                if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {
                    dialogBuilder.setIcon(R.drawable.copy_enabled_day)
                } else {
                    dialogBuilder.setIcon(R.drawable.copy_enabled_night)
                }
            }

            SAVE_ARCHIVE -> {
                // Set the title.
                dialogBuilder.setTitle(R.string.save_archive)

                // Set the icon according to the theme.
                if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {
                    dialogBuilder.setIcon(R.drawable.dom_storage_cleared_day)
                } else {
                    dialogBuilder.setIcon(R.drawable.dom_storage_cleared_night)
                }
            }

            SAVE_IMAGE -> {
                // Set the title.
                dialogBuilder.setTitle(R.string.save_image)

                // Set the icon according to the theme.
                if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {
                    dialogBuilder.setIcon(R.drawable.images_enabled_day)
                } else {
                    dialogBuilder.setIcon(R.drawable.images_enabled_night)
                }
            }
        }

        // Set the view.  The parent view is null because it will be assigned by the alert dialog.
        dialogBuilder.setView(layoutInflater.inflate(R.layout.save_webpage_dialog, null))

        // Set the cancel button listener.  Using `null` as the listener closes the dialog without doing anything else.
        dialogBuilder.setNegativeButton(R.string.cancel, null)

        // Set the save button listener.
        dialogBuilder.setPositiveButton(R.string.save) { _: DialogInterface, _: Int ->
            // Return the dialog fragment to the parent activity.
            saveWebpageListener.onSaveWebpage(saveType, originalUrlString, this)
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
        val urlTextInputLayout = alertDialog.findViewById<TextInputLayout>(R.id.url_textinputlayout)!!
        val urlEditText = alertDialog.findViewById<EditText>(R.id.url_edittext)!!
        val fileNameEditText = alertDialog.findViewById<EditText>(R.id.file_name_edittext)!!
        val browseButton = alertDialog.findViewById<Button>(R.id.browse_button)!!
        val fileSizeTextView = alertDialog.findViewById<TextView>(R.id.file_size_textview)!!
        val saveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)

        // Set the file size text view.
        fileSizeTextView.text = fileSizeString

        // Modify the layout based on the save type.
        if (saveType == SAVE_URL) {  // A URL is being saved.
            // Populate the URL edit text according to the type.  This must be done before the text change listener is created below so that the file size isn't requested again.
            if (originalUrlString!!.startsWith("data:")) {  // The URL contains the entire data of an image.
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

            // Update the file size and the status of the save button when the URL changes.
            urlEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                    // Do nothing.
                }

                override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                    // Do nothing.
                }

                override fun afterTextChanged(editable: Editable) {
                    // Cancel the get URL size AsyncTask if it is running.
                    if (getUrlSize != null) {
                        getUrlSize!!.cancel(true)
                    }

                    // Get the current URL to save.
                    val urlToSave = urlEditText.text.toString()

                    // Wipe the file size text view.
                    fileSizeTextView.text = ""

                    // Get the file size for the current URL.
                    getUrlSize = GetUrlSize(context, alertDialog, userAgentString, cookiesEnabled).execute(urlToSave)

                    // Enable the save button if the URL and file name are populated.
                    saveButton.isEnabled = urlToSave.isNotEmpty() && fileNameEditText.text.toString().isNotEmpty()
                }
            })
        } else {  // An archive or an image is being saved.
            // Hide the URL edit text and the file size text view.
            urlTextInputLayout.visibility = View.GONE
            fileSizeTextView.visibility = View.GONE
        }

        // Initially disable the save button.
        saveButton.isEnabled = false

        // Update the status of the save button when the file name changes.
        fileNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // Do nothing.
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // Do nothing.
            }

            override fun afterTextChanged(s: Editable) {
                // Enable the save button based on the save type.
                if (saveType == SAVE_URL) {  // A URL is being saved.
                    // Enable the save button if the file name and the URL are populated.
                    saveButton.isEnabled = fileNameEditText.text.toString().isNotEmpty() && urlEditText.text.toString().isNotEmpty()
                } else {  // An archive or an image is being saved.
                    // Enable the save button if the file name is populated.
                    saveButton.isEnabled = fileNameEditText.text.toString().isNotEmpty()
                }
            }
        })

        // Handle clicks on the browse button.
        browseButton.setOnClickListener {
            // Create the file picker intent.
            val browseIntent = Intent(Intent.ACTION_CREATE_DOCUMENT)

            // Set the intent MIME type to include all files so that everything is visible.
            browseIntent.type = "*/*"

            // Set the initial file name according to the type.
            browseIntent.putExtra(Intent.EXTRA_TITLE, fileNameString)

            // Request a file that can be opened.
            browseIntent.addCategory(Intent.CATEGORY_OPENABLE)

            // Start the file picker.  This must be started under `activity` so that the request code is returned correctly.
            requireActivity().startActivityForResult(browseIntent, MainWebViewActivity.BROWSE_SAVE_WEBPAGE_REQUEST_CODE)
        }

        // Return the alert dialog.
        return alertDialog
    }
}