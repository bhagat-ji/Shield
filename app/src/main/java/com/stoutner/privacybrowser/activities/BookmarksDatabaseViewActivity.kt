/*
 * Copyright 2016-2023 Soren Stoutner <soren@stoutner.com>.
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

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.database.MergeCursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.AbsListView.MultiChoiceModeListener
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView

import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.cursoradapter.widget.CursorAdapter
import androidx.cursoradapter.widget.ResourceCursorAdapter
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.google.android.material.snackbar.Snackbar

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.dialogs.EditBookmarkDatabaseViewDialog.Companion.bookmarkDatabaseId
import com.stoutner.privacybrowser.dialogs.EditBookmarkDatabaseViewDialog.EditBookmarkDatabaseViewListener
import com.stoutner.privacybrowser.dialogs.EditBookmarkFolderDatabaseViewDialog.Companion.folderDatabaseId
import com.stoutner.privacybrowser.dialogs.EditBookmarkFolderDatabaseViewDialog.EditBookmarkFolderDatabaseViewListener
import com.stoutner.privacybrowser.helpers.BookmarksDatabaseHelper

import java.io.ByteArrayOutputStream

import java.util.Arrays

// Define the private class constants.
private const val ALL_FOLDERS_DATABASE_ID = -2
private const val CURRENT_FOLDER_DATABASE_ID = "current_folder_database_id"
private const val CURRENT_FOLDER_NAME = "current_folder_name"
private const val SORT_BY_DISPLAY_ORDER = "sort_by_display_order"

class BookmarksDatabaseViewActivity : AppCompatActivity(), EditBookmarkDatabaseViewListener, EditBookmarkFolderDatabaseViewListener {
    companion object {
        // Define the public class constants.
        const val HOME_FOLDER_DATABASE_ID = -1
    }

    // Define the class variables.
    private var closeActivityAfterDismissingSnackbar = false
    private var currentFolderDatabaseId = 0
    private var bookmarksCursorAdapter: CursorAdapter? = null
    private var bookmarksDeletedSnackbar: Snackbar? = null
    private var sortByDisplayOrder = false

    // Declare the class variables.
    private lateinit var bookmarksCursor: Cursor
    private lateinit var bookmarksDatabaseHelper: BookmarksDatabaseHelper
    private lateinit var bookmarksListView: ListView
    private lateinit var currentFolderName: String
    private lateinit var oldFolderNameString: String

    public override fun onCreate(savedInstanceState: Bundle?) {
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

        // Get the favorite icon byte array.
        val favoriteIconByteArray = intent.getByteArrayExtra(CURRENT_FAVORITE_ICON_BYTE_ARRAY)!!

        // Convert the favorite icon byte array to a bitmap and store it in a class variable.
        val favoriteIconBitmap = BitmapFactory.decodeByteArray(favoriteIconByteArray, 0, favoriteIconByteArray.size)

        // Set the view according to the theme.
        if (bottomAppBar) {
            // Set the content view.
            setContentView(R.layout.bookmarks_databaseview_bottom_appbar)
        } else {
            // `Window.FEATURE_ACTION_MODE_OVERLAY` makes the contextual action mode cover the support action bar.  It must be requested before the content is set.
            supportRequestWindowFeature(Window.FEATURE_ACTION_MODE_OVERLAY)

            // Set the content view.
            setContentView(R.layout.bookmarks_databaseview_top_appbar)
        }

        // Get a handle for the toolbar.
        val toolbar = findViewById<Toolbar>(R.id.bookmarks_databaseview_toolbar)
        val bookmarksListView = findViewById<ListView>(R.id.bookmarks_databaseview_listview)

        // Set the support action bar.
        setSupportActionBar(toolbar)

        // Get a handle for the app bar.
        val appBar = supportActionBar!!

        // Set the app bar custom view.
        appBar.setCustomView(R.layout.spinner)

        // Display the back arrow in the app bar.
        appBar.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM or ActionBar.DISPLAY_HOME_AS_UP

        // Control what the system back command does.
        val onBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Prepare to finish the activity.
                prepareFinish()
            }
        }

        // Register the on back pressed callback.
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        // Initialize the database handler.
        bookmarksDatabaseHelper = BookmarksDatabaseHelper(this)

        // Create a matrix cursor column name string array.
        val matrixCursorColumnNames = arrayOf(BookmarksDatabaseHelper.ID, BookmarksDatabaseHelper.BOOKMARK_NAME)

        // Setup the matrix cursor.
        MatrixCursor(matrixCursorColumnNames).use { matrixCursor ->
            // Add "All Folders" and "Home Folder" to the matrix cursor.
            matrixCursor.addRow(arrayOf<Any>(ALL_FOLDERS_DATABASE_ID, getString(R.string.all_folders)))
            matrixCursor.addRow(arrayOf<Any>(HOME_FOLDER_DATABASE_ID, getString(R.string.home_folder)))

            // Get a cursor with the list of all the folders.
            val foldersCursor = bookmarksDatabaseHelper.allFolders

            // Combine the matrix cursor and the folders cursor.
            val foldersMergeCursor = MergeCursor(arrayOf(matrixCursor, foldersCursor))

            // Get the default folder bitmap.
            val defaultFolderDrawable = AppCompatResources.getDrawable(this, R.drawable.folder_blue_bitmap)

            // Cast the default folder drawable to a bitmap drawable.
            val defaultFolderBitmapDrawable = (defaultFolderDrawable as BitmapDrawable)

            // Convert the default folder bitmap drawable to a bitmap.
            val defaultFolderBitmap = defaultFolderBitmapDrawable.bitmap

            // Create a resource cursor adapter for the spinner.
            val foldersCursorAdapter: ResourceCursorAdapter = object : ResourceCursorAdapter(this, R.layout.appbar_spinner_item, foldersMergeCursor, 0) {
                override fun bindView(view: View, context: Context, cursor: Cursor) {
                    // Get handles for the spinner views.
                    val spinnerItemImageView = view.findViewById<ImageView>(R.id.spinner_item_imageview)
                    val spinnerItemTextView = view.findViewById<TextView>(R.id.spinner_item_textview)

                    // Set the folder icon according to the type.
                    if (foldersMergeCursor.position > 1) {  // Set a user folder icon.
                        // Initialize a default folder icon byte array output stream.
                        val defaultFolderIconByteArrayOutputStream = ByteArrayOutputStream()

                        // Covert the default folder bitmap to a PNG and store it in the output stream.  `0` is for lossless compression (the only option for a PNG).
                        defaultFolderBitmap.compress(Bitmap.CompressFormat.PNG, 0, defaultFolderIconByteArrayOutputStream)

                        // Convert the default folder icon output stream to a byte array.
                        val defaultFolderIconByteArray = defaultFolderIconByteArrayOutputStream.toByteArray()

                        // Get the folder icon byte array from the cursor.
                        val folderIconByteArray = cursor.getBlob(cursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.FAVORITE_ICON))

                        // Convert the byte array to a bitmap beginning at the first byte and ending at the last.
                        val folderIconBitmap = BitmapFactory.decodeByteArray(folderIconByteArray, 0, folderIconByteArray.size)

                        // Set the icon according to the type.
                        if (Arrays.equals(folderIconByteArray, defaultFolderIconByteArray)) {  // The default folder icon is used.
                            // Set a smaller and darker folder icon, which works well with the spinner.
                            spinnerItemImageView.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.folder_dark_blue))
                        } else {  // A custom folder icon is uses.
                            // Set the folder image stored in the cursor.
                            spinnerItemImageView.setImageBitmap(folderIconBitmap)
                        }
                    } else {  // Set the `All Folders` or `Home Folder` icon.
                        // Set the gray folder image.
                        spinnerItemImageView.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.folder_gray))
                    }

                    // Set the text view to display the folder name.
                    spinnerItemTextView.text = cursor.getString(cursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.BOOKMARK_NAME))
                }
            }

            // Set the resource cursor adapter drop drown view resource.
            foldersCursorAdapter.setDropDownViewResource(R.layout.appbar_spinner_dropdown_item)

            // Get a handle for the folder spinner.
            val folderSpinner = findViewById<Spinner>(R.id.spinner)

            // Set the folder spinner adapter.
            folderSpinner.adapter = foldersCursorAdapter

            // Wait to set the on item selected listener until the spinner has been inflated.  Otherwise the activity will crash on restart.
            folderSpinner.post {
                // Handle taps on the spinner dropdown.
                folderSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                        // Store the current folder database ID.
                        currentFolderDatabaseId = id.toInt()

                        // Get a handle for the selected view.
                        val selectedFolderTextView = findViewById<TextView>(R.id.spinner_item_textview)

                        // Store the current folder name.
                        currentFolderName = selectedFolderTextView.text.toString()

                        // Update the list view.
                        updateBookmarksListView()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        // Do nothing.
                    }
                }
            }

            // Check to see if the activity was restarted.
            if (savedInstanceState == null) {  // The activity was not restarted.
                // Set the default current folder database ID.
                currentFolderDatabaseId = ALL_FOLDERS_DATABASE_ID
            } else {  // The activity was restarted.
                // Restore the class variables from the saved instance state.
                currentFolderDatabaseId = savedInstanceState.getInt(CURRENT_FOLDER_DATABASE_ID)
                currentFolderName = savedInstanceState.getString(CURRENT_FOLDER_NAME)!!
                sortByDisplayOrder = savedInstanceState.getBoolean(SORT_BY_DISPLAY_ORDER)

                // Update the spinner if the home folder is selected.  Android handles this by default for the main cursor but not the matrix cursor.
                if (currentFolderDatabaseId == HOME_FOLDER_DATABASE_ID) {
                    folderSpinner.setSelection(1)
                }
            }

            // Update the bookmarks listview.
            updateBookmarksListView()

            // Setup a cursor adapter.
            bookmarksCursorAdapter = object : CursorAdapter(this, bookmarksCursor, false) {
                override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
                    // Inflate the individual item layout.  `false` does not attach it to the root.
                    return layoutInflater.inflate(R.layout.bookmarks_databaseview_item_linearlayout, parent, false)
                }

                override fun bindView(view: View, context: Context, cursor: Cursor) {
                    // Get handles for the views.
                    val bookmarkDatabaseIdTextView = view.findViewById<TextView>(R.id.bookmarks_databaseview_database_id)
                    val bookmarkFavoriteIcon = view.findViewById<ImageView>(R.id.bookmarks_databaseview_favorite_icon)
                    val bookmarkNameTextView = view.findViewById<TextView>(R.id.bookmarks_databaseview_bookmark_name)
                    val bookmarkUrlTextView = view.findViewById<TextView>(R.id.bookmarks_databaseview_bookmark_url)
                    val bookmarkDisplayOrderTextView = view.findViewById<TextView>(R.id.bookmarks_databaseview_display_order)
                    val parentFolderImageView = view.findViewById<ImageView>(R.id.bookmarks_databaseview_parent_folder_icon)
                    val bookmarkParentFolderTextView = view.findViewById<TextView>(R.id.bookmarks_databaseview_parent_folder)

                    // Get the information from the cursor.
                    val bookmarkDatabaseId = cursor.getInt(cursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.ID))
                    val bookmarkFavoriteIconByteArray = cursor.getBlob(cursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.FAVORITE_ICON))
                    val bookmarkNameString = cursor.getString(cursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.BOOKMARK_NAME))
                    val bookmarkUrlString = cursor.getString(cursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.BOOKMARK_URL))
                    val bookmarkDisplayOrder = cursor.getInt(cursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.DISPLAY_ORDER))
                    val bookmarkParentFolder = cursor.getString(cursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.PARENT_FOLDER))

                    // Convert the byte array to a `Bitmap` beginning at the beginning at the first byte and ending at the last.
                    val bookmarkFavoriteIconBitmap = BitmapFactory.decodeByteArray(bookmarkFavoriteIconByteArray, 0, bookmarkFavoriteIconByteArray.size)

                    // Populate the views.
                    bookmarkDatabaseIdTextView.text = bookmarkDatabaseId.toString()
                    bookmarkFavoriteIcon.setImageBitmap(bookmarkFavoriteIconBitmap)
                    bookmarkNameTextView.text = bookmarkNameString
                    bookmarkUrlTextView.text = bookmarkUrlString
                    bookmarkDisplayOrderTextView.text = bookmarkDisplayOrder.toString()

                    // Check to see if the bookmark is a folder.
                    if (cursor.getInt(cursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.IS_FOLDER)) == 1) {  // The bookmark is a folder.
                        // Make the font bold.  When the first argument is null the font is not changed.
                        bookmarkNameTextView.setTypeface(null, Typeface.BOLD)

                        // Hide the URL.
                        bookmarkUrlTextView.visibility = View.GONE
                    } else {  // The bookmark is not a folder.
                        // Reset the font to default.
                        bookmarkNameTextView.typeface = Typeface.DEFAULT

                        // Show the URL.
                        bookmarkUrlTextView.visibility = View.VISIBLE
                    }

                    // Make the folder name gray if it is the home folder.
                    if (bookmarkParentFolder.isEmpty()) {  // The bookmark is in the home folder.
                        // Get the home folder icon.
                        parentFolderImageView.setImageDrawable(AppCompatResources.getDrawable(applicationContext, R.drawable.folder_gray))

                        // Set the parent folder text to be `Home Folder`.
                        bookmarkParentFolderTextView.setText(R.string.home_folder)

                        // Set the home folder text to be gray.
                        bookmarkParentFolderTextView.setTextColor(getColor(R.color.gray_500))
                    } else {  // The bookmark is in a subfolder.
                        // Get the parent folder icon.
                        parentFolderImageView.setImageDrawable(AppCompatResources.getDrawable(applicationContext, R.drawable.folder_dark_blue))

                        // Set the parent folder name.
                        bookmarkParentFolderTextView.text = bookmarkParentFolder

                        // Set the parent folder text color.
                        bookmarkParentFolderTextView.setTextColor(getColor(R.color.parent_folder_text))
                    }
                }
            }

            // Update the ListView.
            bookmarksListView.adapter = bookmarksCursorAdapter

            // Set a listener to edit a bookmark when it is tapped.
            bookmarksListView.onItemClickListener = AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, _: Int, id: Long ->
                // Convert the database ID to an int.
                val databaseId = id.toInt()

                // Show the edit bookmark or edit bookmark folder dialog.
                if (bookmarksDatabaseHelper.isFolder(databaseId)) {
                    // Save the current folder name, which is used in `onSaveBookmarkFolder()`.
                    oldFolderNameString = bookmarksCursor.getString(bookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.BOOKMARK_NAME))

                    // Instantiate the edit bookmark folder dialog.
                    val editBookmarkFolderDatabaseViewDialog: DialogFragment = folderDatabaseId(databaseId, favoriteIconBitmap)

                    // Make it so.
                    editBookmarkFolderDatabaseViewDialog.show(supportFragmentManager, resources.getString(R.string.edit_folder))
                } else {
                    // Instantiate the edit bookmark dialog.
                    val editBookmarkDatabaseViewDialog: DialogFragment = bookmarkDatabaseId(databaseId, favoriteIconBitmap)

                    // Make it so.
                    editBookmarkDatabaseViewDialog.show(supportFragmentManager, resources.getString(R.string.edit_bookmark))
                }
            }

            // Handle long presses on the list view.
            bookmarksListView.setMultiChoiceModeListener(object : MultiChoiceModeListener {
                // Instantiate the common variables.
                private lateinit var selectAllMenuItem: MenuItem
                private lateinit var deleteMenuItem: MenuItem
                private var deletingBookmarks = false

                override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                    // Inflate the menu for the contextual app bar.
                    menuInflater.inflate(R.menu.bookmarks_databaseview_context_menu, menu)

                    // Get handles for the menu items.
                    selectAllMenuItem = menu.findItem(R.id.select_all)
                    deleteMenuItem = menu.findItem(R.id.delete)

                    // Disable the delete menu item if a delete is pending.
                    deleteMenuItem.isEnabled = !deletingBookmarks

                    // Get the number of currently selected bookmarks.
                    val numberOfSelectedBookmarks = bookmarksListView.checkedItemCount

                    // Set the title.
                    mode.setTitle(R.string.bookmarks)

                    // Set the action mode subtitle according to the number of selected bookmarks.  This must be set here or it will be missing if the activity is restarted.
                    mode.subtitle = getString(R.string.selected, numberOfSelectedBookmarks)

                    // Do not show the select all menu item if all the bookmarks are already checked.
                    if (numberOfSelectedBookmarks == bookmarksListView.count)
                        selectAllMenuItem.isVisible = false

                    // Make it so.
                    return true
                }

                override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                    // Do nothing.
                    return false
                }

                override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) {
                    // Calculate the number of selected bookmarks.
                    val numberOfSelectedBookmarks = bookmarksListView.checkedItemCount

                    // Only run the commands if at least one bookmark is selected.  Otherwise, a context menu with 0 selected bookmarks is briefly displayed.
                    if (numberOfSelectedBookmarks > 0) {
                        // Update the action mode subtitle according to the number of selected bookmarks.
                        mode.subtitle = getString(R.string.selected, numberOfSelectedBookmarks)

                        // Only show the select all menu item if all of the bookmarks are not already selected.
                        selectAllMenuItem.isVisible = (bookmarksListView.checkedItemCount != bookmarksListView.count)

                        // Convert the database ID to an int.
                        val databaseId = id.toInt()

                        // If a folder was selected, also select all the contents.
                        if (checked && bookmarksDatabaseHelper.isFolder(databaseId))
                            selectAllBookmarksInFolder(databaseId)

                        // Do not allow a bookmark to be deselected if the folder is selected.
                        if (!checked) {
                            // Get the folder name.
                            val folderName = bookmarksDatabaseHelper.getParentFolderName(id.toInt())

                            // If the bookmark is not in the root folder, check to see if the folder is selected.
                            if (folderName.isNotEmpty()) {
                                // Get the database ID of the folder.
                                val folderDatabaseId = bookmarksDatabaseHelper.getFolderDatabaseId(folderName)

                                // Move the bookmarks cursor to the first position.
                                bookmarksCursor.moveToFirst()

                                // Initialize the folder position variable.
                                var folderPosition = -1

                                // Get the position of the folder in the bookmarks cursor.
                                while ((folderPosition < 0) && (bookmarksCursor.position < bookmarksCursor.count)) {
                                    // Check if the folder database ID matches the bookmark database ID.
                                    if (folderDatabaseId == bookmarksCursor.getInt(bookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.ID))) {
                                        // Get the folder position.
                                        folderPosition = bookmarksCursor.position

                                        // Check if the folder is selected.
                                        if (bookmarksListView.isItemChecked(folderPosition)) {
                                            // Reselect the bookmark.
                                            bookmarksListView.setItemChecked(position, true)

                                            // Display a snackbar explaining why the bookmark cannot be deselected.
                                            Snackbar.make(bookmarksListView, R.string.cannot_deselect_bookmark, Snackbar.LENGTH_LONG).show()
                                        }
                                    }

                                    // Increment the bookmarks cursor.
                                    bookmarksCursor.moveToNext()
                                }
                            }
                        }
                    }
                }

                override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean {
                    // Get a the menu item ID.
                    val menuItemId = menuItem.itemId

                    // Run the command that corresponds to the selected menu item.
                    if (menuItemId == R.id.select_all) {  // Select all the bookmarks.
                        // Get the total number of bookmarks.
                        val numberOfBookmarks = bookmarksListView.count

                        // Select them all.
                        for (i in 0 until numberOfBookmarks) {
                            bookmarksListView.setItemChecked(i, true)
                        }
                    } else if (menuItemId == R.id.delete) {  // Delete the selected bookmarks.
                        // Set the deleting bookmarks flag, which prevents the delete menu item from being enabled until the current process finishes.
                        deletingBookmarks = true

                        // Get an array of the selected row IDs.
                        val selectedBookmarksIdsLongArray = bookmarksListView.checkedItemIds

                        // Get an array of checked bookmarks.  `.clone()` makes a copy that won't change if the list view is reloaded, which is needed for re-selecting the bookmarks on undelete.
                        val selectedBookmarksPositionsSparseBooleanArray = bookmarksListView.checkedItemPositions.clone()

                        // Populate the bookmarks cursor.
                        bookmarksCursor = when (currentFolderDatabaseId) {
                            // Get all the bookmarks except the ones being deleted.
                            ALL_FOLDERS_DATABASE_ID ->
                                if (sortByDisplayOrder)
                                    bookmarksDatabaseHelper.getAllBookmarksByDisplayOrderExcept(selectedBookmarksIdsLongArray)
                                else
                                    bookmarksDatabaseHelper.getAllBookmarksExcept(selectedBookmarksIdsLongArray)

                            // Get the home folder bookmarks except the ones being deleted.
                            HOME_FOLDER_DATABASE_ID ->
                                if (sortByDisplayOrder)
                                    bookmarksDatabaseHelper.getBookmarksByDisplayOrderExcept(selectedBookmarksIdsLongArray, "")
                                else
                                    bookmarksDatabaseHelper.getBookmarksExcept(selectedBookmarksIdsLongArray, "")


                            // Get the current folder bookmarks except the ones being deleted.
                            else ->
                                if (sortByDisplayOrder)
                                    bookmarksDatabaseHelper.getBookmarksByDisplayOrderExcept(selectedBookmarksIdsLongArray, currentFolderName)
                                else
                                    bookmarksDatabaseHelper.getBookmarksExcept(selectedBookmarksIdsLongArray, currentFolderName)
                        }

                        // Update the list view.
                        bookmarksCursorAdapter!!.changeCursor(bookmarksCursor)

                        // Create a snackbar with the number of deleted bookmarks.
                        bookmarksDeletedSnackbar = Snackbar.make(findViewById(R.id.bookmarks_databaseview_coordinatorlayout), getString(R.string.bookmarks_deleted, selectedBookmarksIdsLongArray.size),
                            Snackbar.LENGTH_LONG)
                            .setAction(R.string.undo) {}  // Undo will be handles by `onDismissed()` below.
                            .addCallback(object : Snackbar.Callback() {
                                override fun onDismissed(snackbar: Snackbar, event: Int) {
                                    if (event == DISMISS_EVENT_ACTION) {  // The user pushed the undo button.
                                        // Update the bookmarks list view with the current contents of the bookmarks database, including the "deleted" bookmarks.
                                        updateBookmarksListView()

                                        // Re-select the previously selected bookmarks.
                                        for (i in 0 until selectedBookmarksPositionsSparseBooleanArray.size())
                                            bookmarksListView.setItemChecked(selectedBookmarksPositionsSparseBooleanArray.keyAt(i), true)
                                    } else {  // The snackbar was dismissed without the undo button being pushed.
                                        // Delete each selected bookmark.
                                        for (databaseIdLong in selectedBookmarksIdsLongArray) {
                                            // Convert the database long ID to an int.
                                            val databaseIdInt = databaseIdLong.toInt()

                                            // Delete the selected bookmark.
                                            bookmarksDatabaseHelper.deleteBookmark(databaseIdInt)
                                        }
                                    }

                                    // Reset the deleting bookmarks flag.
                                    deletingBookmarks = false

                                    // Enable the delete menu item.
                                    deleteMenuItem.isEnabled = true

                                    // Close the activity if back has been pressed.
                                    if (closeActivityAfterDismissingSnackbar)
                                        finish()
                                }
                            })

                        // Show the snackbar.
                        bookmarksDeletedSnackbar!!.show()
                    }

                    // Consume the click.
                    return false
                }

                override fun onDestroyActionMode(mode: ActionMode) {
                    // Do nothing.
                }
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu.
        menuInflater.inflate(R.menu.bookmarks_databaseview_options_menu, menu)

        // Get a handle for the sort menu item.
        val sortMenuItem = menu.findItem(R.id.sort)

        // Change the sort menu item icon if the listview is sorted by display order, which restores the state after a restart.
        if (sortByDisplayOrder)
            sortMenuItem.setIcon(R.drawable.sort_selected)

        // Success.
        return true
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        // Get the menu item ID.
        val menuItemId = menuItem.itemId

        // Run the command that corresponds to the selected menu item.
        if (menuItemId == android.R.id.home) {  // Go Home.  The home arrow is identified as `android.R.id.home`, not just `R.id.home`.
            // Prepare to finish the activity.
            prepareFinish()
        } else if (menuItemId == R.id.sort) {  // Toggle the sort mode.
            // Update the sort by display order tracker.
            sortByDisplayOrder = !sortByDisplayOrder

            // Update the icon and display a snackbar.
            if (sortByDisplayOrder) {  // Sort by display order.
                // Update the icon.
                menuItem.setIcon(R.drawable.sort_selected)

                // Display a snackbar indicating the current sort type.
                Snackbar.make(bookmarksListView, R.string.sorted_by_display_order, Snackbar.LENGTH_SHORT).show()
            } else {  // Sort by database id.
                // Update the icon.
                menuItem.setIcon(R.drawable.sort)

                // Display a snackbar indicating the current sort type.
                Snackbar.make(bookmarksListView, R.string.sorted_by_database_id, Snackbar.LENGTH_SHORT).show()
            }

            // Update the list view.
            updateBookmarksListView()
        }

        // Consume the event.
        return true
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        // Run the default commands.
        super.onSaveInstanceState(savedInstanceState)

        // Store the class variables in the bundle.
        savedInstanceState.putInt(CURRENT_FOLDER_DATABASE_ID, currentFolderDatabaseId)
        savedInstanceState.putString(CURRENT_FOLDER_NAME, currentFolderName)
        savedInstanceState.putBoolean(SORT_BY_DISPLAY_ORDER, sortByDisplayOrder)
    }

    private fun prepareFinish() {
        // Check to see if a snackbar is currently displayed.  If so, it must be closed before existing so that a pending delete is completed before reloading the list view in the bookmarks activity.
        if ((bookmarksDeletedSnackbar != null) && bookmarksDeletedSnackbar!!.isShown) { // Close the bookmarks deleted snackbar before going home.
            // Set the close flag.
            closeActivityAfterDismissingSnackbar = true

            // Dismiss the snackbar.
            bookmarksDeletedSnackbar!!.dismiss()
        } else {  // Go home immediately.
            // Update the current folder in the bookmarks activity.
            if (currentFolderDatabaseId == ALL_FOLDERS_DATABASE_ID || currentFolderDatabaseId == HOME_FOLDER_DATABASE_ID) {  // All folders or the the home folder are currently displayed.
                // Load the home folder.
                BookmarksActivity.currentFolder = ""
            } else {  // A subfolder is currently displayed.
                // Load the current folder.
                BookmarksActivity.currentFolder = currentFolderName
            }

            // Reload the bookmarks list view when returning to the bookmarks activity.
            BookmarksActivity.restartFromBookmarksDatabaseViewActivity = true

            // Exit the bookmarks database view activity.
            finish()
        }
    }

    private fun updateBookmarksListView() {
        // Populate the bookmarks list view based on the spinner selection.
        bookmarksCursor = when (currentFolderDatabaseId) {
            // Get all the bookmarks.
            ALL_FOLDERS_DATABASE_ID ->
                if (sortByDisplayOrder)
                    bookmarksDatabaseHelper.allBookmarksByDisplayOrder
                else
                    bookmarksDatabaseHelper.allBookmarks

            // Get the bookmarks in the current folder.
            HOME_FOLDER_DATABASE_ID ->
                if (sortByDisplayOrder)
                    bookmarksDatabaseHelper.getBookmarksByDisplayOrder("")
                else
                    bookmarksDatabaseHelper.getBookmarks("")

            // Get the bookmarks in the specified folder.
            else ->
                if (sortByDisplayOrder)
                    bookmarksDatabaseHelper.getBookmarksByDisplayOrder(currentFolderName)
                else
                    bookmarksDatabaseHelper.getBookmarks(currentFolderName)
        }

        // Update the cursor adapter if it isn't null, which happens when the activity is restarted.
        if (bookmarksCursorAdapter != null) {
            bookmarksCursorAdapter!!.changeCursor(bookmarksCursor)
        }
    }

    private fun selectAllBookmarksInFolder(folderId: Int) {
        // Get the folder name.
        val folderName = bookmarksDatabaseHelper.getFolderName(folderId)

        // Get a cursor with the contents of the folder.
        val folderCursor = bookmarksDatabaseHelper.getBookmarks(folderName)

        // Move to the beginning of the cursor.
        folderCursor.moveToFirst()

        while (folderCursor.position < folderCursor.count) {
            // Get the bookmark database ID.
            val bookmarkId = folderCursor.getInt(folderCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.ID))

            // Move the bookmarks cursor to the first position.
            bookmarksCursor.moveToFirst()

            // Initialize the bookmark position variable.
            var bookmarkPosition = -1

            // Get the position of this bookmark in the bookmarks cursor.
            while ((bookmarkPosition < 0) && (bookmarksCursor.position < bookmarksCursor.count)) {
                // Check if the bookmark IDs match.
                if (bookmarkId == bookmarksCursor.getInt(bookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.ID))) {
                    // Get the bookmark position.
                    bookmarkPosition = bookmarksCursor.position

                    // If this bookmark is a folder, select all the bookmarks inside it.
                    if (bookmarksDatabaseHelper.isFolder(bookmarkId))
                        selectAllBookmarksInFolder(bookmarkId)

                    // Select the bookmark.
                    bookmarksListView.setItemChecked(bookmarkPosition, true)
                }

                // Increment the bookmarks cursor position.
                bookmarksCursor.moveToNext()
            }

            // Move to the next position.
            folderCursor.moveToNext()
        }
    }

    override fun onSaveBookmark(dialogFragment: DialogFragment, selectedBookmarkDatabaseId: Int, favoriteIconBitmap: Bitmap) {
        // Get the dialog from the dialog fragment.
        val dialog = dialogFragment.dialog!!

        // Get handles for the views from dialog fragment.
        val currentIconRadioButton = dialog.findViewById<RadioButton>(R.id.current_icon_radiobutton)
        val bookmarkNameEditText = dialog.findViewById<EditText>(R.id.bookmark_name_edittext)
        val bookmarkUrlEditText = dialog.findViewById<EditText>(R.id.bookmark_url_edittext)
        val folderSpinner = dialog.findViewById<Spinner>(R.id.bookmark_folder_spinner)
        val displayOrderEditText = dialog.findViewById<EditText>(R.id.bookmark_display_order_edittext)

        // Extract the bookmark information.
        val bookmarkNameString = bookmarkNameEditText.text.toString()
        val bookmarkUrlString = bookmarkUrlEditText.text.toString()
        val folderDatabaseId = folderSpinner.selectedItemId.toInt()
        val displayOrderInt = displayOrderEditText.text.toString().toInt()

        // Get the parent folder name.
        val parentFolderNameString: String = if (folderDatabaseId == HOME_FOLDER_DATABASE_ID)  // The home folder is selected.  Use `""`.
            ""
        else  // Get the parent folder name from the database.
            bookmarksDatabaseHelper.getFolderName(folderDatabaseId)

        // Update the bookmark.
        if (currentIconRadioButton.isChecked) {  // Update the bookmark without changing the favorite icon.
            bookmarksDatabaseHelper.updateBookmark(selectedBookmarkDatabaseId, bookmarkNameString, bookmarkUrlString, parentFolderNameString, displayOrderInt)
        } else {  // Update the bookmark using the `WebView` favorite icon.
            // Create a favorite icon byte array output stream.
            val newFavoriteIconByteArrayOutputStream = ByteArrayOutputStream()

            // Convert the favorite icon bitmap to a byte array.  `0` is for lossless compression (the only option for a PNG).
            favoriteIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, newFavoriteIconByteArrayOutputStream)

            // Convert the favorite icon byte array stream to a byte array.
            val newFavoriteIconByteArray = newFavoriteIconByteArrayOutputStream.toByteArray()

            //  Update the bookmark and the favorite icon.
            bookmarksDatabaseHelper.updateBookmark(selectedBookmarkDatabaseId, bookmarkNameString, bookmarkUrlString, parentFolderNameString, displayOrderInt, newFavoriteIconByteArray)
        }

        // Update the list view.
        updateBookmarksListView()
    }

    override fun onSaveBookmarkFolder(dialogFragment: DialogFragment, selectedFolderDatabaseId: Int, favoriteIconBitmap: Bitmap) {
        // Get the dialog from the dialog fragment.
        val dialog = dialogFragment.dialog!!

        // Get handles for the views from dialog fragment.
        val currentIconRadioButton = dialog.findViewById<RadioButton>(R.id.current_icon_radiobutton)
        val defaultIconRadioButton = dialog.findViewById<RadioButton>(R.id.default_icon_radiobutton)
        val defaultIconImageView = dialog.findViewById<ImageView>(R.id.default_icon_imageview)
        val folderNameEditText = dialog.findViewById<EditText>(R.id.folder_name_edittext)
        val parentFolderSpinner = dialog.findViewById<Spinner>(R.id.parent_folder_spinner)
        val displayOrderEditText = dialog.findViewById<EditText>(R.id.display_order_edittext)

        // Extract the folder information.
        val newFolderNameString = folderNameEditText.text.toString()
        val parentFolderDatabaseId = parentFolderSpinner.selectedItemId.toInt()
        val displayOrderInt = displayOrderEditText.text.toString().toInt()

        // Set the parent folder name.
        val parentFolderNameString: String = if (parentFolderDatabaseId == HOME_FOLDER_DATABASE_ID)  // The home folder is selected.  Use `""`.
            ""
        else  // Get the parent folder name from the database.
            bookmarksDatabaseHelper.getFolderName(parentFolderDatabaseId)

        // Update the folder.
        if (currentIconRadioButton.isChecked) {  // Update the folder without changing the favorite icon.
            bookmarksDatabaseHelper.updateFolder(selectedFolderDatabaseId, oldFolderNameString, newFolderNameString, parentFolderNameString, displayOrderInt)
        } else {  // Update the folder and the icon.
            // Get the new folder icon bitmap.
            val folderIconBitmap = if (defaultIconRadioButton.isChecked) {
                // Get the default folder icon drawable.
                val folderIconDrawable = defaultIconImageView.drawable

                // Convert the folder icon drawable to a bitmap drawable.
                val folderIconBitmapDrawable = folderIconDrawable as BitmapDrawable

                // Convert the folder icon bitmap drawable to a bitmap.
                folderIconBitmapDrawable.bitmap
            } else {  // Use the `WebView` favorite icon.
                // Get a copy of the favorite icon bitmap.
                favoriteIconBitmap
            }

            // Create a folder icon byte array output stream.
            val newFolderIconByteArrayOutputStream = ByteArrayOutputStream()

            // Convert the folder icon bitmap to a byte array.  `0` is for lossless compression (the only option for a PNG).
            folderIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, newFolderIconByteArrayOutputStream)

            // Convert the folder icon byte array stream to a byte array.
            val newFolderIconByteArray = newFolderIconByteArrayOutputStream.toByteArray()

            //  Update the folder and the icon.
            bookmarksDatabaseHelper.updateFolder(selectedFolderDatabaseId, oldFolderNameString, newFolderNameString, parentFolderNameString, displayOrderInt, newFolderIconByteArray)
        }

        // Update the list view.
        updateBookmarksListView()
    }

    public override fun onDestroy() {
        // Close the bookmarks cursor and database.
        bookmarksCursor.close()
        bookmarksDatabaseHelper.close()

        // Run the default commands.
        super.onDestroy()
    }
}
