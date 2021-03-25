/*
 * Copyright © 2016-2021 Soren Stoutner <soren@stoutner.com>.
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

// Declare the class constants.
private const val SAVE_TYPE = "save_type"

class SaveDialog : DialogFragment() {
    // Declare the class variables.
    private lateinit var saveListener: SaveListener
    private lateinit var fileName: String

    // The public interface is used to send information back to the parent activity.
    interface SaveListener {
        fun onSave(saveType: Int, dialogFragment: DialogFragment)
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Get a handle for the save listener from the launching context.
        saveListener = context as SaveListener
    }

    companion object {
        // Declare the companion object constants.  These can be moved to class constants once all of the code has transitioned to Kotlin.
        const val SAVE_LOGCAT = 0
        const val SAVE_ABOUT_VERSION_TEXT = 1
        const val SAVE_ABOUT_VERSION_IMAGE = 2

        // `@JvmStatic` will no longer be required once all the code has transitioned to Kotlin.
        @JvmStatic
        fun save(saveType: Int): SaveDialog {
            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the arguments in the bundle.
            argumentsBundle.putInt(SAVE_TYPE, saveType)

            // Create a new instance of the save dialog.
            val saveDialog = SaveDialog()

            // Add the arguments bundle to the dialog.
            saveDialog.arguments = argumentsBundle

            // Return the new dialog.
            return saveDialog
        }
    }

    // `@SuppressLint("InflateParams")` removes the warning about using null as the parent view group when inflating the alert dialog.
    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get the arguments from the bundle.
        val saveType = requireArguments().getInt(SAVE_TYPE)

        // Use an alert dialog builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.PrivacyBrowserAlertDialog)

        // Get the current theme status.
        val currentThemeStatus = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        // Set the title and the icon according to the save type.
        when (saveType) {
            SAVE_LOGCAT -> {
                // Set the title.
                dialogBuilder.setTitle(R.string.save_logcat)

                // Set the icon according to the theme.
                if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {
                    dialogBuilder.setIcon(R.drawable.save_dialog_day)
                } else {
                    dialogBuilder.setIcon(R.drawable.save_dialog_night)
                }
            }

            SAVE_ABOUT_VERSION_TEXT -> {
                // Set the title.
                dialogBuilder.setTitle(R.string.save_text)

                // Set the icon according to the theme.
                if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {
                    dialogBuilder.setIcon(R.drawable.save_text_blue_day)
                } else {
                    dialogBuilder.setIcon(R.drawable.save_text_blue_night)
                }
            }

            SAVE_ABOUT_VERSION_IMAGE -> {
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
        dialogBuilder.setView(requireActivity().layoutInflater.inflate(R.layout.save_dialog, null))

        // Set the cancel button listener.  Using `null` as the listener closes the dialog without doing anything else.
        dialogBuilder.setNegativeButton(R.string.cancel, null)

        // Set the save button listener.
        dialogBuilder.setPositiveButton(R.string.save) { _: DialogInterface?, _: Int ->
            // Return the dialog fragment to the parent activity.
            saveListener.onSave(saveType, this)
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
        val saveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)

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
                // Get the current file name.
                val fileNameString = fileNameEditText.text.toString()

                // Enable the save button if the file name is populated.
                saveButton.isEnabled = fileNameString.isNotEmpty()
            }
        })

        // Set the file name according to the type.
        when (saveType) {
            SAVE_LOGCAT -> fileName = getString(R.string.privacy_browser_logcat_txt)
            SAVE_ABOUT_VERSION_TEXT -> fileName = getString(R.string.privacy_browser_version_txt)
            SAVE_ABOUT_VERSION_IMAGE -> fileName = getString(R.string.privacy_browser_version_png)
        }

        // Handle clicks on the browse button.
        browseButton.setOnClickListener {
            // Create the file picker intent.
            val browseIntent = Intent(Intent.ACTION_CREATE_DOCUMENT)

            // Set the intent MIME type to include all files so that everything is visible.
            browseIntent.type = "*/*"

            // Set the initial file name.
            browseIntent.putExtra(Intent.EXTRA_TITLE, fileName)

            // Request a file that can be opened.
            browseIntent.addCategory(Intent.CATEGORY_OPENABLE)

            // Launch the file picker.  There is only one `startActivityForResult()`, so the request code is simply set to 0, but it must be run under `activity` so the response is processed correctly.
            requireActivity().startActivityForResult(browseIntent, 0)
        }

        // Return the alert dialog.
        return alertDialog
    }
}