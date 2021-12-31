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
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.helpers.BookmarksDatabaseHelper

import java.io.ByteArrayOutputStream

// Define the class constants.
private const val DATABASE_ID = "database_id"
private const val FAVORITE_ICON_BYTE_ARRAY = "favorite_icon_byte_array"

class EditBookmarkDialog : DialogFragment() {
    // Declare the class variables.
    private lateinit var editBookmarkListener: EditBookmarkListener

    // Declare the class views.
    private lateinit var webpageFavoriteIconRadioButton: RadioButton
    private lateinit var nameEditText: EditText
    private lateinit var urlEditText: EditText
    private lateinit var saveButton: Button

    // The public interface is used to send information back to the parent activity.
    interface EditBookmarkListener {
        fun onSaveBookmark(dialogFragment: DialogFragment, selectedBookmarkDatabaseId: Int, favoriteIconBitmap: Bitmap)
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Get a handle for the edit bookmark listener from the launching context.
        editBookmarkListener = context as EditBookmarkListener
    }

    companion object {
        // `@JvmStatic` will no longer be required once all the code has transitioned to Kotlin.
        @JvmStatic
        fun bookmarkDatabaseId(databaseId: Int, favoriteIconBitmap: Bitmap): EditBookmarkDialog {
            // Create a favorite icon byte array output stream.
            val favoriteIconByteArrayOutputStream = ByteArrayOutputStream()

            // Convert the favorite icon to a PNG and place it in the byte array output stream.  `0` is for lossless compression (the only option for a PNG).
            favoriteIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, favoriteIconByteArrayOutputStream)

            // Convert the byte array output stream to a byte array.
            val favoriteIconByteArray = favoriteIconByteArrayOutputStream.toByteArray()

            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the variables in the bundle.
            argumentsBundle.putInt(DATABASE_ID, databaseId)
            argumentsBundle.putByteArray(FAVORITE_ICON_BYTE_ARRAY, favoriteIconByteArray)

            // Create a new instance of the dialog.
            val editBookmarkDialog = EditBookmarkDialog()

            // Add the arguments bundle to the dialog.
            editBookmarkDialog.arguments = argumentsBundle

            // Return the new dialog.
            return editBookmarkDialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get the arguments.
        val arguments = requireArguments()

        // Get the variables from the arguments.
        val selectedBookmarkDatabaseId = arguments.getInt(DATABASE_ID)
        val favoriteIconByteArray = arguments.getByteArray(FAVORITE_ICON_BYTE_ARRAY)!!

        // Convert the favorite icon byte array to a bitmap.
        val favoriteIconBitmap = BitmapFactory.decodeByteArray(favoriteIconByteArray, 0, favoriteIconByteArray.size)

        // Initialize the bookmarks database helper.  The `0` specifies a database version, but that is ignored and set instead using a constant in `BookmarksDatabaseHelper`.
        val bookmarksDatabaseHelper = BookmarksDatabaseHelper(context, null, null, 0)

        // Get a cursor with the selected bookmark.
        val bookmarkCursor = bookmarksDatabaseHelper.getBookmark(selectedBookmarkDatabaseId)

        // Move the cursor to the first position.
        bookmarkCursor.moveToFirst()

        // Use an alert dialog builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.PrivacyBrowserAlertDialog)

        // Set the title.
        dialogBuilder.setTitle(R.string.edit_bookmark)

        // Set the view.
        dialogBuilder.setView(R.layout.edit_bookmark_dialog)

        // Set the cancel button listener.  Using `null` as the listener closes the dialog without doing anything else.
        dialogBuilder.setNegativeButton(R.string.cancel, null)

        // Set the save button listener.
        dialogBuilder.setPositiveButton(R.string.save) { _: DialogInterface?, _: Int ->
            // Return the dialog fragment to the parent activity.
            editBookmarkListener.onSaveBookmark(this, selectedBookmarkDatabaseId, favoriteIconBitmap)
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
        val currentIconLinearLayout = alertDialog.findViewById<LinearLayout>(R.id.current_icon_linearlayout)!!
        val currentIconRadioButton = alertDialog.findViewById<RadioButton>(R.id.current_icon_radiobutton)!!
        val currentIconImageView = alertDialog.findViewById<ImageView>(R.id.current_icon_imageview)!!
        val webpageFavoriteIconLinearLayout = alertDialog.findViewById<LinearLayout>(R.id.webpage_favorite_icon_linearlayout)!!
        webpageFavoriteIconRadioButton = alertDialog.findViewById(R.id.webpage_favorite_icon_radiobutton)!!
        val webpageFavoriteIconImageView = alertDialog.findViewById<ImageView>(R.id.webpage_favorite_icon_imageview)!!
        nameEditText = alertDialog.findViewById(R.id.bookmark_name_edittext)!!
        urlEditText = alertDialog.findViewById(R.id.bookmark_url_edittext)!!
        saveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)

        // Get the current favorite icon byte array from the cursor.
        val currentIconByteArray = bookmarkCursor.getBlob(bookmarkCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.FAVORITE_ICON))

        // Convert the byte array to a bitmap beginning at the first byte and ending at the last.
        val currentIconBitmap = BitmapFactory.decodeByteArray(currentIconByteArray, 0, currentIconByteArray.size)

        // Display the current icon bitmap.
        currentIconImageView.setImageBitmap(currentIconBitmap)

        // Set the webpage favorite icon bitmap.
        webpageFavoriteIconImageView.setImageBitmap(favoriteIconBitmap)

        // Store the current bookmark name and URL.
        val currentName = bookmarkCursor.getString(bookmarkCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.BOOKMARK_NAME))
        val currentUrl = bookmarkCursor.getString(bookmarkCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.BOOKMARK_URL))

        // Populate the edit texts.
        nameEditText.setText(currentName)
        urlEditText.setText(currentUrl)

        // Initially disable the save button.
        saveButton.isEnabled = false

        // Set the radio button listeners.  These perform a click on the linear layout, which contains the necessary logic.
        currentIconRadioButton.setOnClickListener { currentIconLinearLayout.performClick() }
        webpageFavoriteIconRadioButton.setOnClickListener { webpageFavoriteIconLinearLayout.performClick() }

        // Set the current icon linear layout click listener.
        currentIconLinearLayout.setOnClickListener {
            // Check the current icon radio button.
            currentIconRadioButton.isChecked = true

            // Uncheck the webpage favorite icon radio button.
            webpageFavoriteIconRadioButton.isChecked = false

            // Update the save button.
            updateSaveButton(currentName, currentUrl)
        }

        // Set the webpage favorite icon linear layout click listener.
        webpageFavoriteIconLinearLayout.setOnClickListener {
            // Check the webpage favorite icon radio button.
            webpageFavoriteIconRadioButton.isChecked = true

            // Uncheck the current icon radio button.
            currentIconRadioButton.isChecked = false

            // Update the save button.
            updateSaveButton(currentName, currentUrl)
        }

        // Update the save button if the bookmark name changes.
        nameEditText.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // Do nothing.
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // Do nothing.
            }

            override fun afterTextChanged(s: Editable) {
                // Update the save button.
                updateSaveButton(currentName, currentUrl)
            }
        })

        // Update the save button if the URL changes.
        urlEditText.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // Do nothing.
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // Do nothing.
            }

            override fun afterTextChanged(s: Editable) {
                // Update the edit button.
                updateSaveButton(currentName, currentUrl)
            }
        })

        // Allow the enter key on the keyboard to save the bookmark from the bookmark name edit text.
        nameEditText.setOnKeyListener { _: View?, keyCode: Int, event: KeyEvent ->
            // Check the key code, event, and button status.
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER && saveButton.isEnabled) {  // The enter key was pressed and the save button is enabled.
                // Trigger the listener and return the dialog fragment to the parent activity.
                editBookmarkListener.onSaveBookmark(this, selectedBookmarkDatabaseId, favoriteIconBitmap)

                // Manually dismiss the alert dialog.
                alertDialog.dismiss()

                // Consume the event.
                return@setOnKeyListener true
            } else {  // If any other key was pressed, or if the save button is currently disabled, do not consume the event.
                return@setOnKeyListener false
            }
        }

        // Allow the enter key on the keyboard to save the bookmark from the URL edit text.
        urlEditText.setOnKeyListener { _: View?, keyCode: Int, event: KeyEvent ->
            // Check the key code, event, and button status.
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER && saveButton.isEnabled) {  // The enter key was pressed and the save button is enabled.
                // Trigger the listener and return the dialog fragment to the parent activity.
                editBookmarkListener.onSaveBookmark(this, selectedBookmarkDatabaseId, favoriteIconBitmap)

                // Manually dismiss the alert dialog.
                alertDialog.dismiss()

                // Consume the event.
                return@setOnKeyListener true
            } else { // If any other key was pressed, or if the save button is currently disabled, do not consume the event.
                return@setOnKeyListener false
            }
        }

        // Return the alert dialog.
        return alertDialog
    }

    private fun updateSaveButton(currentName: String, currentUrl: String) {
        // Get the text from the edit texts.
        val newName = nameEditText.text.toString()
        val newUrl = urlEditText.text.toString()

        // Has the favorite icon changed?
        val iconChanged = webpageFavoriteIconRadioButton.isChecked

        // Has the name changed?
        val nameChanged = newName != currentName

        // Has the URL changed?
        val urlChanged = newUrl != currentUrl

        // Update the enabled status of the save button.
        saveButton.isEnabled = iconChanged || nameChanged || urlChanged
    }
}