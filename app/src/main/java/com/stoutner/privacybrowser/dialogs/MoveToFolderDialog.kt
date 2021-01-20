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
import android.content.res.Configuration
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.MatrixCursor
import android.database.MergeCursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView

import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.cursoradapter.widget.CursorAdapter
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.helpers.BookmarksDatabaseHelper

import java.io.ByteArrayOutputStream
import java.lang.StringBuilder

// Declare the class constants.
private const val CURRENT_FOLDER = "current_folder"
private const val SELECTED_BOOKMARKS_LONG_ARRAY = "selected_bookmarks_long_array"

class MoveToFolderDialog : DialogFragment() {
    // Declare the class variables.
    private lateinit var moveToFolderListener: MoveToFolderListener
    private lateinit var bookmarksDatabaseHelper: BookmarksDatabaseHelper
    private lateinit var exceptFolders: StringBuilder

    // The public interface is used to send information back to the parent activity.
    interface MoveToFolderListener {
        fun onMoveToFolder(dialogFragment: DialogFragment)
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Get a handle for the move to folder listener from the launching context.
        moveToFolderListener = context as MoveToFolderListener
    }

    companion object {
        // `@JvmStatic` will no longer be required once all the code has transitioned to Kotlin.
        @JvmStatic
        fun moveBookmarks(currentFolder: String, selectedBookmarksLongArray: LongArray): MoveToFolderDialog {
            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the arguments in the bundle.
            argumentsBundle.putString(CURRENT_FOLDER, currentFolder)
            argumentsBundle.putLongArray(SELECTED_BOOKMARKS_LONG_ARRAY, selectedBookmarksLongArray)

            // Create a new instance of the dialog.
            val moveToFolderDialog = MoveToFolderDialog()

            // And the bundle to the dialog.
            moveToFolderDialog.arguments = argumentsBundle

            // Return the new dialog.
            return moveToFolderDialog
        }
    }

    // `@SuppressLint("InflateParams")` removes the warning about using `null` as the parent view group when inflating the alert dialog.
    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get the data from the arguments.
        val currentFolder = requireArguments().getString(CURRENT_FOLDER)!!
        val selectedBookmarksLongArray = requireArguments().getLongArray(SELECTED_BOOKMARKS_LONG_ARRAY)!!

        // Initialize the database helper.  The `0` specifies a database version, but that is ignored and set instead using a constant in the bookmarks database helper.
        bookmarksDatabaseHelper = BookmarksDatabaseHelper(context, null, null, 0)

        // Use an alert dialog builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.PrivacyBrowserAlertDialog)

        // Get the current theme status.
        val currentThemeStatus = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        // Set the icon according to the theme.
        if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {
            dialogBuilder.setIcon(R.drawable.move_to_folder_blue_day)
        } else {
            dialogBuilder.setIcon(R.drawable.move_to_folder_blue_night)
        }

        // Set the title.
        dialogBuilder.setTitle(R.string.move_to_folder)

        // Set the view.  The parent view is `null` because it will be assigned by the alert dialog.
        dialogBuilder.setView(requireActivity().layoutInflater.inflate(R.layout.move_to_folder_dialog, null))

        // Set the listener for the cancel button.  Using `null` as the listener closes the dialog without doing anything else.
        dialogBuilder.setNegativeButton(R.string.cancel, null)

        // Set the listener fo the move button.
        dialogBuilder.setPositiveButton(R.string.move) { _: DialogInterface?, _: Int ->
            // Return the dialog fragment to the parent activity on move.
            moveToFolderListener.onMoveToFolder(this)
        }

        // Create an alert dialog from the alert dialog builder.
        val alertDialog = dialogBuilder.create()

        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        // Get the screenshot preference.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)

        // Disable screenshots if not allowed.
        if (!allowScreenshots) {
            // Disable screenshots.
            alertDialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // The alert dialog must be shown before items in the layout can be modified.
        alertDialog.show()

        // Get a handle for the positive button.
        val moveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)

        // Initially disable the positive button.
        moveButton.isEnabled = false

        // Initialize the except folders string builder.
        exceptFolders = StringBuilder()

        // Declare the cursor variables.
        val foldersCursor: Cursor
        val foldersCursorAdapter: CursorAdapter

        // Check to see if the bookmark is currently in the home folder.
        if (currentFolder.isEmpty()) {  // The bookmark is currently in the home folder.  Don't display `Home Folder` at the top of the list view.
            // If a folder is selected, add it and all children to the list of folders not to display.
            for (databaseIdLong in selectedBookmarksLongArray) {
                // Get the database ID int for each selected bookmark.
                val databaseIdInt = databaseIdLong.toInt()

                // Check to see if the bookmark is a folder.
                if (bookmarksDatabaseHelper.isFolder(databaseIdInt)) {
                    // Add the folder to the list of folders not to display.
                    addFolderToExceptFolders(databaseIdInt)
                }
            }

            // Get a cursor containing the folders to display.
            foldersCursor = bookmarksDatabaseHelper.getFoldersExcept(exceptFolders.toString())

            // Populate the folders cursor adapter.
            foldersCursorAdapter = populateFoldersCursorAdapter(requireContext(), foldersCursor)
        } else {  // The current folder is not directly in the home folder.  Display `Home Folder` at the top of the list view.
            // Get the home folder icon drawable.
            val homeFolderIconDrawable = ContextCompat.getDrawable(requireActivity().applicationContext, R.drawable.folder_gray_bitmap)

            // Convert the home folder icon drawable to a bitmap drawable.
            val homeFolderIconBitmapDrawable = homeFolderIconDrawable as BitmapDrawable

            // Convert the home folder bitmap drawable to a bitmap.
            val homeFolderIconBitmap = homeFolderIconBitmapDrawable.bitmap

            // Create a home folder icon byte array output stream.
            val homeFolderIconByteArrayOutputStream = ByteArrayOutputStream()

            // Convert the home folder bitmap to a byte array.  `0` is for lossless compression (the only option for a PNG).
            homeFolderIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, homeFolderIconByteArrayOutputStream)

            // Convert the home folder icon byte array output stream to a byte array.
            val homeFolderIconByteArray = homeFolderIconByteArrayOutputStream.toByteArray()

            // Setup the home folder matrix cursor column names.
            val homeFolderMatrixCursorColumnNames = arrayOf(BookmarksDatabaseHelper._ID, BookmarksDatabaseHelper.BOOKMARK_NAME, BookmarksDatabaseHelper.FAVORITE_ICON)

            // Setup a matrix cursor for the `Home Folder`.
            val homeFolderMatrixCursor = MatrixCursor(homeFolderMatrixCursorColumnNames)

            // Add the home folder to the home folder matrix cursor.
            homeFolderMatrixCursor.addRow(arrayOf<Any>(0, getString(R.string.home_folder), homeFolderIconByteArray))

            // Add the parent folder to the list of folders not to display.
            exceptFolders.append(DatabaseUtils.sqlEscapeString(currentFolder))

            // If a folder is selected, add it and all children to the list of folders not to display.
            for (databaseIdLong in selectedBookmarksLongArray) {
                // Get the database ID int for each selected bookmark.
                val databaseIdInt = databaseIdLong.toInt()

                // Check to see if the bookmark is a folder.
                if (bookmarksDatabaseHelper.isFolder(databaseIdInt)) {
                    // Add the folder to the list of folders not to display.
                    addFolderToExceptFolders(databaseIdInt)
                }
            }

            // Get a cursor containing the folders to display.
            foldersCursor = bookmarksDatabaseHelper.getFoldersExcept(exceptFolders.toString())

            // Combine the home folder matrix cursor and the folders cursor.
            val foldersMergeCursor = MergeCursor(arrayOf(homeFolderMatrixCursor, foldersCursor))

            // Populate the folders cursor adapter.
            foldersCursorAdapter = populateFoldersCursorAdapter(requireContext(), foldersMergeCursor)
        }

        // Get a handle for the folders list view.
        val foldersListView = alertDialog.findViewById<ListView>(R.id.move_to_folder_listview)!!

        // Set the folder list view adapter.
        foldersListView.adapter = foldersCursorAdapter

        // Enable the move button when a folder is selected.
        foldersListView.onItemClickListener = OnItemClickListener { _: AdapterView<*>?, _: View?, _: Int, _: Long ->
            // Enable the move button.
            moveButton.isEnabled = true
        }

        // Return the alert dialog.
        return alertDialog
    }

    private fun addFolderToExceptFolders(databaseIdInt: Int) {
        // Get the name of the selected folder.
        val folderName = bookmarksDatabaseHelper.getFolderName(databaseIdInt)

        // Populate the list of folders not to get.
        if (exceptFolders.isEmpty()) {
            // Add the selected folder to the list of folders not to display.
            exceptFolders.append(DatabaseUtils.sqlEscapeString(folderName))
        } else {
            // Add the selected folder to the end of the list of folders not to display.
            exceptFolders.append(",")
            exceptFolders.append(DatabaseUtils.sqlEscapeString(folderName))
        }

        // Add the selected folder's subfolders to the list of folders not to display.
        addSubfoldersToExceptFolders(folderName)
    }

    private fun addSubfoldersToExceptFolders(folderName: String) {
        // Get a cursor with all the immediate subfolders.
        val subfoldersCursor = bookmarksDatabaseHelper.getSubfolders(folderName)

        // Add each subfolder to the list of folders not to display.
        for (i in 0 until subfoldersCursor.count) {
            // Move the subfolder cursor to the current item.
            subfoldersCursor.moveToPosition(i)

            // Get the name of the subfolder.
            val subfolderName = subfoldersCursor.getString(subfoldersCursor.getColumnIndex(BookmarksDatabaseHelper.BOOKMARK_NAME))

            // Add the subfolder to except folders.
            exceptFolders.append(",")
            exceptFolders.append(DatabaseUtils.sqlEscapeString(subfolderName))

            // Run the same tasks for any subfolders of the subfolder.
            addSubfoldersToExceptFolders(subfolderName)
        }
    }

    private fun populateFoldersCursorAdapter(context: Context, cursor: Cursor): CursorAdapter {
        // Return the folders cursor adapter.
        return object : CursorAdapter(context, cursor, false) {
            override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
                // Inflate the individual item layout.
                return requireActivity().layoutInflater.inflate(R.layout.move_to_folder_item_linearlayout, parent, false)
            }

            override fun bindView(view: View, context: Context, cursor: Cursor) {
                // Get the data from the cursor.
                val folderIconByteArray = cursor.getBlob(cursor.getColumnIndex(BookmarksDatabaseHelper.FAVORITE_ICON))
                val folderName = cursor.getString(cursor.getColumnIndex(BookmarksDatabaseHelper.BOOKMARK_NAME))

                // Get handles for the views.
                val folderIconImageView = view.findViewById<ImageView>(R.id.move_to_folder_icon)
                val folderNameTextView = view.findViewById<TextView>(R.id.move_to_folder_name_textview)

                // Convert the byte array to a bitmap beginning at the first byte and ending at the last.
                val folderIconBitmap = BitmapFactory.decodeByteArray(folderIconByteArray, 0, folderIconByteArray.size)

                // Display the folder icon bitmap.
                folderIconImageView.setImageBitmap(folderIconBitmap)

                // Display the folder name.
                folderNameTextView.text = folderName
            }
        }
    }
}