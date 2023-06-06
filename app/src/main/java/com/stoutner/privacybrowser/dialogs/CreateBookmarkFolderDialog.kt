/*
 * Copyright © 2016-2023 Soren Stoutner <soren@stoutner.com>.
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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.stoutner.privacybrowser.R

import java.io.ByteArrayOutputStream

// Define the class constants.
private const val FAVORITE_ICON_BYTE_ARRAY = "favorite_icon_byte_array"

class CreateBookmarkFolderDialog : DialogFragment() {
    companion object {
        fun createBookmarkFolder(favoriteIconBitmap: Bitmap): CreateBookmarkFolderDialog {
            // Create a favorite icon byte array output stream.
            val favoriteIconByteArrayOutputStream = ByteArrayOutputStream()

            // Convert the favorite icon to a PNG and place it in the byte array output stream.  `0` is for lossless compression (the only option for a PNG).
            favoriteIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, favoriteIconByteArrayOutputStream)

            // Convert the byte array output stream to a byte array.
            val favoriteIconByteArray = favoriteIconByteArrayOutputStream.toByteArray()

            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the favorite icon in the bundle.
            argumentsBundle.putByteArray(FAVORITE_ICON_BYTE_ARRAY, favoriteIconByteArray)

            // Create a new instance of the dialog.
            val createBookmarkFolderDialog = CreateBookmarkFolderDialog()

            // Add the bundle to the dialog.
            createBookmarkFolderDialog.arguments = argumentsBundle

            // Return the new dialog.
            return createBookmarkFolderDialog
        }
    }

    // Declare the class variables.
    private lateinit var createBookmarkFolderListener: CreateBookmarkFolderListener

    // The public interface is used to send information back to the parent activity.
    interface CreateBookmarkFolderListener {
        fun createBookmarkFolder(dialogFragment: DialogFragment, favoriteIconBitmap: Bitmap)
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Get a handle for the create bookmark folder listener from the launching context.
        createBookmarkFolderListener = context as CreateBookmarkFolderListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get the arguments.
        val arguments = requireArguments()

        // Get the favorite icon byte array.
        val favoriteIconByteArray = arguments.getByteArray(FAVORITE_ICON_BYTE_ARRAY)!!

        // Convert the favorite icon byte array to a bitmap.
        val favoriteIconBitmap = BitmapFactory.decodeByteArray(favoriteIconByteArray, 0, favoriteIconByteArray.size)

        // Use an alert dialog builder to create the dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.PrivacyBrowserAlertDialog)

        // Set the title.
        dialogBuilder.setTitle(R.string.create_folder)

        // Set the view.
        dialogBuilder.setView(R.layout.create_bookmark_folder_dialog)

        // Set a listener on the cancel button.  Using `null` as the listener closes the dialog without doing anything else.
        dialogBuilder.setNegativeButton(R.string.cancel, null)

        // Set the create button listener.
        dialogBuilder.setPositiveButton(R.string.create) { _: DialogInterface, _: Int ->
            // Return the dialog fragment to the parent activity on create.
            createBookmarkFolderListener.createBookmarkFolder(this, favoriteIconBitmap)
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

        // Display the keyboard.
        alertDialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        // The alert dialog must be shown before the content can be modified.
        alertDialog.show()

        // Get handles for the views in the dialog.
        val defaultIconLinearLayout = alertDialog.findViewById<LinearLayout>(R.id.default_icon_linearlayout)!!
        val defaultIconRadioButton = alertDialog.findViewById<RadioButton>(R.id.default_icon_radiobutton)!!
        val webpageFavoriteIconLinearLayout = alertDialog.findViewById<LinearLayout>(R.id.webpage_favorite_icon_linearlayout)!!
        val webpageFavoriteIconRadioButton = alertDialog.findViewById<RadioButton>(R.id.webpage_favorite_icon_radiobutton)!!
        val webpageFavoriteIconImageView = alertDialog.findViewById<ImageView>(R.id.webpage_favorite_icon_imageview)!!
        val folderNameEditText = alertDialog.findViewById<EditText>(R.id.folder_name_edittext)!!
        val createButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)

        // Display the current favorite icon.
        webpageFavoriteIconImageView.setImageBitmap(favoriteIconBitmap)

        // Initially disable the create button.
        createButton.isEnabled = false

        // Set the radio button listeners.  These perform a click on the linear layout, which contains the necessary logic.
        defaultIconRadioButton.setOnClickListener { defaultIconLinearLayout.performClick() }
        webpageFavoriteIconRadioButton.setOnClickListener { webpageFavoriteIconLinearLayout.performClick() }

        // Set the default icon linear layout click listener.
        defaultIconLinearLayout.setOnClickListener {
            // Check the default icon radio button.
            defaultIconRadioButton.isChecked = true

            // Uncheck the webpage favorite icon radio button.
            webpageFavoriteIconRadioButton.isChecked = false
        }

        // Set the webpage favorite icon linear layout click listener.
        webpageFavoriteIconLinearLayout.setOnClickListener {
            // Check the webpage favorite icon radio button.
            webpageFavoriteIconRadioButton.isChecked = true

            // Uncheck the default icon radio button.
            defaultIconRadioButton.isChecked = false
        }

        // Enable the create button if the folder name is populated.
        folderNameEditText.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // Do nothing.
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // Do nothing.
            }

            override fun afterTextChanged(editable: Editable) {
                // Convert the current text to a string.
                val folderName = editable.toString()

                // Enable the create button if the new folder name is not empty.
                createButton.isEnabled = folderName.isNotEmpty()
            }
        })

        // Set the enter key on the keyboard to create the folder from the edit text.
        folderNameEditText.setOnKeyListener { _: View?, keyCode: Int, keyEvent: KeyEvent ->
            // Check the key code, event, and button status.
            if (keyEvent.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER && createButton.isEnabled) {  // The event is a key-down on the enter key and the create button is enabled.
                // Trigger the create bookmark folder listener and return the dialog fragment to the parent activity.
                createBookmarkFolderListener.createBookmarkFolder(this, favoriteIconBitmap)

                // Manually dismiss the alert dialog.
                alertDialog.dismiss()

                // Consume the event.
                return@setOnKeyListener true
            } else {  // Some other key was pressed or the create button is disabled.
                return@setOnKeyListener false
            }
        }

        // Return the alert dialog.
        return alertDialog
    }
}
