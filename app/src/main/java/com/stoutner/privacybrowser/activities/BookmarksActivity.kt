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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.SparseBooleanArray
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
import android.widget.TextView

import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cursoradapter.widget.CursorAdapter
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.dialogs.CreateBookmarkDialog.Companion.createBookmark
import com.stoutner.privacybrowser.dialogs.CreateBookmarkDialog.CreateBookmarkListener
import com.stoutner.privacybrowser.dialogs.CreateBookmarkFolderDialog.Companion.createBookmarkFolder
import com.stoutner.privacybrowser.dialogs.CreateBookmarkFolderDialog.CreateBookmarkFolderListener
import com.stoutner.privacybrowser.dialogs.EditBookmarkDialog.Companion.bookmarkDatabaseId
import com.stoutner.privacybrowser.dialogs.EditBookmarkDialog.EditBookmarkListener
import com.stoutner.privacybrowser.dialogs.EditBookmarkFolderDialog.Companion.folderDatabaseId
import com.stoutner.privacybrowser.dialogs.EditBookmarkFolderDialog.EditBookmarkFolderListener
import com.stoutner.privacybrowser.dialogs.MoveToFolderDialog.Companion.moveBookmarks
import com.stoutner.privacybrowser.dialogs.MoveToFolderDialog.MoveToFolderListener
import com.stoutner.privacybrowser.helpers.BookmarksDatabaseHelper

import java.io.ByteArrayOutputStream
import java.util.function.Consumer

// Define the public constants.
const val CURRENT_FOLDER = "current_folder"
const val CURRENT_TITLE = "current_title"
const val CURRENT_FAVORITE_ICON_BYTE_ARRAY = "current_favorite_icon_byte_array"

// Define the private constants.
private const val CHECKED_BOOKMARKS_ARRAY_LIST = "checked_bookmarks_array_list"

class BookmarksActivity : AppCompatActivity(), CreateBookmarkListener, CreateBookmarkFolderListener, EditBookmarkListener, EditBookmarkFolderListener, MoveToFolderListener {
    companion object {
        // Define the public static variables, which are accessed from the bookmarks database view activity.
        var currentFolder: String = ""
        var restartFromBookmarksDatabaseViewActivity = false
    }

    // Define the class variables.
    private var bookmarksDeletedSnackbar: Snackbar? = null
    private var closeActivityAfterDismissingSnackbar = false
    private var contextualActionMode: ActionMode? = null

    // Declare the class variables.
    private lateinit var appBar: ActionBar
    private lateinit var bookmarksCursor: Cursor
    private lateinit var bookmarksCursorAdapter: CursorAdapter
    private lateinit var bookmarksDatabaseHelper: BookmarksDatabaseHelper
    private lateinit var bookmarksListView: ListView
    private lateinit var currentFavoriteIconByteArray: ByteArray
    private lateinit var moveBookmarkDownMenuItem: MenuItem
    private lateinit var moveBookmarkUpMenuItem: MenuItem
    private lateinit var oldFolderNameString: String

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

        // Get the intent that launched the activity.
        val launchingIntent = intent

        // Populate the variables from the launching intent.
        currentFolder = launchingIntent.getStringExtra(CURRENT_FOLDER)!!
        val currentTitle = launchingIntent.getStringExtra(CURRENT_TITLE)!!
        val currentUrl = launchingIntent.getStringExtra(CURRENT_URL)!!
        currentFavoriteIconByteArray = launchingIntent.getByteArrayExtra(CURRENT_FAVORITE_ICON_BYTE_ARRAY)!!

        /*  TODO.  Test if not needed.
        // Set the current folder variable.
        if (launchingIntent.getStringExtra(CURRENT_FOLDER) != null) {  // Set the current folder from the intent.
            currentFolder = launchingIntent.getStringExtra(CURRENT_FOLDER)
        } else {  // Set the current folder to be `""`, which is the home folder.
            currentFolder = ""
        }

         */

        // Convert the favorite icon byte array to a bitmap.
        val currentFavoriteIconBitmap = BitmapFactory.decodeByteArray(currentFavoriteIconByteArray, 0, currentFavoriteIconByteArray.size)

        // Set the content according to the app bar position.
        if (bottomAppBar) {
            // Set the content view.
            setContentView(R.layout.bookmarks_bottom_appbar)
        } else {
            // `Window.FEATURE_ACTION_MODE_OVERLAY` makes the contextual action mode cover the support action bar.  It must be requested before the content is set.
            supportRequestWindowFeature(Window.FEATURE_ACTION_MODE_OVERLAY)

            // Set the content view.
            setContentView(R.layout.bookmarks_top_appbar)
        }

        // Get handles for the views.
        val toolbar = findViewById<Toolbar>(R.id.bookmarks_toolbar)
        bookmarksListView = findViewById(R.id.bookmarks_listview)
        val createBookmarkFolderFab = findViewById<FloatingActionButton>(R.id.create_bookmark_folder_fab)
        val createBookmarkFab = findViewById<FloatingActionButton>(R.id.create_bookmark_fab)

        // Set the support action bar.
        setSupportActionBar(toolbar)

        // Get a handle for the app bar.
        appBar = supportActionBar!!

        // Display the home arrow on the app bar.
        appBar.setDisplayHomeAsUpEnabled(true)

        // Control what the system back command does.
        val onBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Prepare to finish the activity.
                prepareFinish()
            }
        }

        // Register the on back pressed callback.
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        // Initialize the database helper.
        bookmarksDatabaseHelper = BookmarksDatabaseHelper(this)

        // Set a listener so that tapping a list item loads the URL or folder.
        bookmarksListView.onItemClickListener = AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, _: Int, id: Long ->
            // Convert the id from long to int to match the format of the bookmarks database.
            val databaseId = id.toInt()

            // Get the bookmark cursor for this ID.
            val bookmarkCursor = bookmarksDatabaseHelper.getBookmark(databaseId)

            // Move the cursor to the first entry.
            bookmarkCursor.moveToFirst()

            // Act upon the bookmark according to the type.
            if (bookmarkCursor.getInt(bookmarkCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.IS_FOLDER)) == 1) {  // The selected bookmark is a folder.
                // Update the current folder.
                currentFolder = bookmarkCursor.getString(bookmarkCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.BOOKMARK_NAME))

                // Load the new folder.
                loadFolder()
            } else {  // The selected bookmark is not a folder.
                // Instantiate the edit bookmark dialog.
                val editBookmarkDialog: DialogFragment = bookmarkDatabaseId(databaseId, currentFavoriteIconBitmap)

                // Make it so.
                editBookmarkDialog.show(supportFragmentManager, resources.getString(R.string.edit_bookmark))
            }

            // Close the cursor.
            bookmarkCursor.close()
        }

        // Handle long-presses on the list view.
        bookmarksListView.setMultiChoiceModeListener(object : MultiChoiceModeListener {
            // Define the object variables.
            private var deletingBookmarks = false

            // Declare the object variables.
            private lateinit var editBookmarkMenuItem: MenuItem
            private lateinit var deleteBookmarksMenuItem: MenuItem
            private lateinit var selectAllBookmarksMenuItem: MenuItem

            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                // Inflate the menu for the contextual app bar.
                menuInflater.inflate(R.menu.bookmarks_context_menu, menu)

                // Set the title.
                if (currentFolder.isEmpty()) {  // Use `R.string.bookmarks` if in the home folder.
                    mode.setTitle(R.string.bookmarks)
                } else {  // Use the current folder name as the title.
                    mode.title = currentFolder
                }

                // Get handles for menu items that need to be selectively disabled.
                moveBookmarkUpMenuItem = menu.findItem(R.id.move_bookmark_up)
                moveBookmarkDownMenuItem = menu.findItem(R.id.move_bookmark_down)
                editBookmarkMenuItem = menu.findItem(R.id.edit_bookmark)
                deleteBookmarksMenuItem = menu.findItem(R.id.delete_bookmark)
                selectAllBookmarksMenuItem = menu.findItem(R.id.context_menu_select_all_bookmarks)

                // Disable the delete bookmarks menu item if a delete is pending.
                deleteBookmarksMenuItem.isEnabled = !deletingBookmarks

                // Store a handle for the contextual action bar so it can be closed programatically.
                contextualActionMode = mode

                // Make it so.
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                // Get a handle for the move to folder menu item.
                val moveToFolderMenuItem = menu.findItem(R.id.move_to_folder)

                // Get a Cursor with all of the folders.
                val folderCursor = bookmarksDatabaseHelper.allFolders

                // Display the move to folder menu item if at least one folder exists.
                moveToFolderMenuItem.isVisible = folderCursor.count > 0

                // Make it so.
                return true
            }

            override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) {
                // Get the number of selected bookmarks.
                val numberOfSelectedBookmarks = bookmarksListView.checkedItemCount

                // Only process commands if at least one bookmark is selected.  Otherwise, a context menu with 0 selected bookmarks is briefly displayed.
                if (numberOfSelectedBookmarks > 0) {
                    // Adjust the action mode and the menu according to the number of selected bookmarks.
                    if (numberOfSelectedBookmarks == 1) {  // One bookmark is selected.
                        // Show the applicable menu items.
                        moveBookmarkUpMenuItem.isVisible = true
                        moveBookmarkDownMenuItem.isVisible = true
                        editBookmarkMenuItem.isVisible = true

                        // Update the enabled status of the move icons.
                        updateMoveIcons()
                    } else {  // More than one bookmark is selected.
                        // Hide non-applicable `MenuItems`.
                        moveBookmarkUpMenuItem.isVisible = false
                        moveBookmarkDownMenuItem.isVisible = false
                        editBookmarkMenuItem.isVisible = false
                    }

                    // List the number of selected bookmarks in the subtitle.
                    mode.subtitle = getString(R.string.selected, numberOfSelectedBookmarks)

                    // Show the select all menu item if all the bookmarks are not selected.
                    selectAllBookmarksMenuItem.isVisible = (numberOfSelectedBookmarks != bookmarksListView.count)
                }
            }

            override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
                // Declare the variables.
                val selectedBookmarkNewPosition: Int
                val selectedBookmarksPositionsSparseBooleanArray: SparseBooleanArray

                // Initialize the selected bookmark position.
                var selectedBookmarkPosition = 0

                // Get the menu item ID.
                val menuItemId = menuItem.itemId

                // Run the commands according to the selected action item.
                if (menuItemId == R.id.move_bookmark_up) {  // Move the bookmark up.
                    // Get the array of checked bookmark positions.
                    selectedBookmarksPositionsSparseBooleanArray = bookmarksListView.checkedItemPositions

                    // Get the position of the bookmark that is selected.  If other bookmarks have previously been selected they will be included in the sparse boolean array with a value of `false`.
                    for (i in 0 until selectedBookmarksPositionsSparseBooleanArray.size()) {
                        // Check to see if the value for the bookmark is true, meaning it is currently selected.
                        if (selectedBookmarksPositionsSparseBooleanArray.valueAt(i)) {
                            // Only one bookmark should have a value of `true` when move bookmark up is enabled.
                            selectedBookmarkPosition = selectedBookmarksPositionsSparseBooleanArray.keyAt(i)
                        }
                    }

                    // Calculate the new position of the selected bookmark.
                    selectedBookmarkNewPosition = selectedBookmarkPosition - 1

                    // Iterate through the bookmarks.
                    for (i in 0 until bookmarksListView.count) {
                        // Get the database ID for the current bookmark.
                        val currentBookmarkDatabaseId = bookmarksListView.getItemIdAtPosition(i).toInt()

                        // Update the display order for the current bookmark.
                        if (i == selectedBookmarkPosition) {  // The current bookmark is the selected bookmark.
                            // Move the current bookmark up one.
                            bookmarksDatabaseHelper.updateDisplayOrder(currentBookmarkDatabaseId, i - 1)
                        } else if ((i + 1) == selectedBookmarkPosition) {  // The current bookmark is immediately above the selected bookmark.
                            // Move the current bookmark down one.
                            bookmarksDatabaseHelper.updateDisplayOrder(currentBookmarkDatabaseId, i + 1)
                        } else {  // The current bookmark is not changing positions.
                            // Move the bookmarks cursor to the current bookmark position.
                            bookmarksCursor.moveToPosition(i)

                            // Update the display order only if it is not correct in the database.  This fixes problems where the display order somehow got out of sync.
                            if (bookmarksCursor.getInt(bookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.DISPLAY_ORDER)) != i)
                                bookmarksDatabaseHelper.updateDisplayOrder(currentBookmarkDatabaseId, i)
                        }
                    }

                    // Update the bookmarks cursor with the current contents of the bookmarks database.
                    bookmarksCursor = bookmarksDatabaseHelper.getBookmarksByDisplayOrder(currentFolder)

                    // Update the list view.
                    bookmarksCursorAdapter.changeCursor(bookmarksCursor)

                    // Scroll to the new bookmark position.
                    scrollBookmarks(selectedBookmarkNewPosition)

                    // Update the enabled status of the move icons.
                    updateMoveIcons()
                } else if (menuItemId == R.id.move_bookmark_down) {  // Move the bookmark down.
                    // Get the array of checked bookmark positions.
                    selectedBookmarksPositionsSparseBooleanArray = bookmarksListView.checkedItemPositions

                    // Get the position of the bookmark that is selected.  If other bookmarks have previously been selected they will be included in the sparse boolean array with a value of `false`.
                    for (i in 0 until selectedBookmarksPositionsSparseBooleanArray.size()) {
                        // Check to see if the value for the bookmark is true, meaning it is currently selected.
                        if (selectedBookmarksPositionsSparseBooleanArray.valueAt(i)) {
                            // Only one bookmark should have a value of `true` when move bookmark down is enabled.
                            selectedBookmarkPosition = selectedBookmarksPositionsSparseBooleanArray.keyAt(i)
                        }
                    }

                    // Calculate the new position of the selected bookmark.
                    selectedBookmarkNewPosition = selectedBookmarkPosition + 1

                    // Iterate through the bookmarks.
                    for (i in 0 until bookmarksListView.count) {
                        // Get the database ID for the current bookmark.
                        val currentBookmarkDatabaseId = bookmarksListView.getItemIdAtPosition(i).toInt()

                        // Update the display order for the current bookmark.
                        if (i == selectedBookmarkPosition) {  // The current bookmark is the selected bookmark.
                            // Move the current bookmark down one.
                            bookmarksDatabaseHelper.updateDisplayOrder(currentBookmarkDatabaseId, i + 1)
                        } else if (i - 1 == selectedBookmarkPosition) {  // The current bookmark is immediately below the selected bookmark.
                            // Move the bookmark below the selected bookmark up one.
                            bookmarksDatabaseHelper.updateDisplayOrder(currentBookmarkDatabaseId, i - 1)
                        } else {  // The current bookmark is not changing positions.
                            // Move the bookmarks cursor to the current bookmark position.
                            bookmarksCursor.moveToPosition(i)

                            // Update the display order only if it is not correct in the database.  This fixes problems where the display order somehow got out of sync.
                            if (bookmarksCursor.getInt(bookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.DISPLAY_ORDER)) != i) {
                                bookmarksDatabaseHelper.updateDisplayOrder(currentBookmarkDatabaseId, i)
                            }
                        }
                    }

                    // Update the bookmarks cursor with the current contents of the bookmarks database.
                    bookmarksCursor = bookmarksDatabaseHelper.getBookmarksByDisplayOrder(currentFolder)

                    // Update the list view.
                    bookmarksCursorAdapter.changeCursor(bookmarksCursor)

                    // Scroll to the new bookmark position.
                    scrollBookmarks(selectedBookmarkNewPosition)

                    // Update the enabled status of the move icons.
                    updateMoveIcons()
                } else if (menuItemId == R.id.move_to_folder) {  // Move to folder.
                    // Instantiate the move to folder alert dialog.
                    val moveToFolderDialog: DialogFragment = moveBookmarks(currentFolder, bookmarksListView.checkedItemIds)

                    // Show the move to folder alert dialog.
                    moveToFolderDialog.show(supportFragmentManager, resources.getString(R.string.move_to_folder))
                } else if (menuItemId == R.id.edit_bookmark) {
                    // Get the array of checked bookmark positions.
                    selectedBookmarksPositionsSparseBooleanArray = bookmarksListView.checkedItemPositions

                    // Get the position of the bookmark that is selected.  If other bookmarks have previously been selected they will be included in the sparse boolean array with a value of `false`.
                    for (i in 0 until selectedBookmarksPositionsSparseBooleanArray.size()) {
                        // Check to see if the value for the bookmark is true, meaning it is currently selected.
                        if (selectedBookmarksPositionsSparseBooleanArray.valueAt(i)) {
                            // Only one bookmark should have a value of `true` when move edit bookmark is enabled.
                            selectedBookmarkPosition = selectedBookmarksPositionsSparseBooleanArray.keyAt(i)
                        }
                    }

                    // Move the cursor to the selected position.
                    bookmarksCursor.moveToPosition(selectedBookmarkPosition)

                    // Get the selected bookmark database ID.
                    val databaseId = bookmarksCursor.getInt(bookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.ID))

                    // Show the edit bookmark or edit bookmark folder dialog.
                    if (bookmarksCursor.getInt(bookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.IS_FOLDER)) == 1) {  // A folder is selected.
                        // Save the current folder name, which is used in `onSaveBookmarkFolder()`.
                        oldFolderNameString = bookmarksCursor.getString(bookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.BOOKMARK_NAME))

                        // Instantiate the edit bookmark folder dialog.
                        val editFolderDialog: DialogFragment = folderDatabaseId(databaseId, currentFavoriteIconBitmap)

                        // Make it so.
                        editFolderDialog.show(supportFragmentManager, resources.getString(R.string.edit_folder))
                    } else {  // A bookmark is selected.
                        // Instantiate the edit bookmark dialog.
                        val editBookmarkDialog: DialogFragment = bookmarkDatabaseId(databaseId, currentFavoriteIconBitmap)

                        // Make it so.
                        editBookmarkDialog.show(supportFragmentManager, resources.getString(R.string.edit_bookmark))
                    }
                } else if (menuItemId == R.id.delete_bookmark) {  // Delete bookmark.
                    // Set the deleting bookmarks flag, which prevents the delete menu item from being enabled until the current process finishes.
                    deletingBookmarks = true

                    // Get an array of the selected row IDs.
                    val selectedBookmarksIdsLongArray = bookmarksListView.checkedItemIds

                    // Initialize a variable to count the number of bookmarks to delete.
                    var numberOfBookmarksToDelete = 0

                    // Count the number of bookmarks to delete.
                    for (databaseIdLong in selectedBookmarksIdsLongArray) {
                        // Convert the database ID long to an int.
                        val databaseIdInt = databaseIdLong.toInt()

                        // Count the contents of the folder if the selected bookmark is a folder.
                        if (bookmarksDatabaseHelper.isFolder(databaseIdInt)) {
                            // Add the bookmarks from the folder to the running total.
                            numberOfBookmarksToDelete += countBookmarkFolderContents(databaseIdInt)
                        }

                        // Increment the count of the number of bookmarks to delete.
                        numberOfBookmarksToDelete++
                    }

                    // Get an array of checked bookmarks.  `.clone()` makes a copy that won't change if the list view is reloaded, which is needed for re-selecting the bookmarks on undelete.
                    selectedBookmarksPositionsSparseBooleanArray = bookmarksListView.checkedItemPositions.clone()

                    // Update the bookmarks cursor with the current contents of the bookmarks database except for the specified database IDs.
                    bookmarksCursor = bookmarksDatabaseHelper.getBookmarksByDisplayOrderExcept(selectedBookmarksIdsLongArray, currentFolder)

                    // Update the list view.
                    bookmarksCursorAdapter.changeCursor(bookmarksCursor)

                    // Create a Snackbar with the number of deleted bookmarks.
                    bookmarksDeletedSnackbar = Snackbar.make(findViewById(R.id.bookmarks_coordinatorlayout), getString(R.string.bookmarks_deleted, numberOfBookmarksToDelete), Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo) { }  // Do nothing because everything will be handled by `onDismissed()` below.
                        .addCallback(object : Snackbar.Callback() {
                            @SuppressLint("SwitchIntDef")  // Ignore the lint warning about not handling the other possible events as they are covered by `default:`.
                            override fun onDismissed(snackbar: Snackbar, event: Int) {
                                if (event == DISMISS_EVENT_ACTION) {  // The user pushed the undo button.
                                    // Update the bookmarks cursor with the current contents of the bookmarks database, including the "deleted" bookmarks.
                                    bookmarksCursor = bookmarksDatabaseHelper.getBookmarksByDisplayOrder(currentFolder)

                                    // Update the list view.
                                    bookmarksCursorAdapter.changeCursor(bookmarksCursor)

                                    // Re-select the previously selected bookmarks.
                                    for (i in 0 until selectedBookmarksPositionsSparseBooleanArray.size())
                                        bookmarksListView.setItemChecked(selectedBookmarksPositionsSparseBooleanArray.keyAt(i), true)
                                } else {  // The snackbar was dismissed without the undo button being pushed.
                                    // Delete each selected bookmark.
                                    for (databaseIdLong in selectedBookmarksIdsLongArray) {
                                        // Convert the database long ID to an int.
                                        val databaseIdInt = databaseIdLong.toInt()

                                        // Delete the contents of the folder if the selected bookmark is a folder.
                                        if (bookmarksDatabaseHelper.isFolder(databaseIdInt))
                                            deleteBookmarkFolderContents(databaseIdInt)

                                        // Delete the selected bookmark.
                                        bookmarksDatabaseHelper.deleteBookmark(databaseIdInt)
                                    }

                                    // Update the display order.
                                    for (i in 0 until bookmarksListView.count) {
                                        // Get the database ID for the current bookmark.
                                        val currentBookmarkDatabaseId = bookmarksListView.getItemIdAtPosition(i).toInt()

                                        // Move bookmarks cursor to the current bookmark position.
                                        bookmarksCursor.moveToPosition(i)

                                        // Update the display order only if it is not correct in the database.
                                        if (bookmarksCursor.getInt(bookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.DISPLAY_ORDER)) != i)
                                            bookmarksDatabaseHelper.updateDisplayOrder(currentBookmarkDatabaseId, i)
                                    }
                                }

                                // Reset the deleting bookmarks flag.
                                deletingBookmarks = false

                                // Enable the delete bookmarks menu item.
                                deleteBookmarksMenuItem.isEnabled = true

                                // Close the activity if back has been pressed.
                                if (closeActivityAfterDismissingSnackbar)
                                    finish()
                            }
                        })

                    // Show the Snackbar.
                    bookmarksDeletedSnackbar!!.show()
                } else if (menuItemId == R.id.context_menu_select_all_bookmarks) {  // Select all.
                    // Get the total number of bookmarks.
                    val numberOfBookmarks = bookmarksListView.count

                    // Select them all.
                    for (i in 0 until numberOfBookmarks) {
                        bookmarksListView.setItemChecked(i, true)
                    }
                }

                // Consume the click.
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                // Do nothing.
            }
        })

        // Set the create new bookmark folder FAB to display the alert dialog.
        createBookmarkFolderFab.setOnClickListener {
            // Create a create bookmark folder dialog.
            val createBookmarkFolderDialog: DialogFragment = createBookmarkFolder(currentFavoriteIconBitmap)

            // Show the create bookmark folder dialog.
            createBookmarkFolderDialog.show(supportFragmentManager, getString(R.string.create_folder))
        }

        // Set the create new bookmark FAB to display the alert dialog.
        createBookmarkFab.setOnClickListener {
            // Instantiate the create bookmark dialog.
            val createBookmarkDialog: DialogFragment = createBookmark(currentUrl, currentTitle, currentFavoriteIconBitmap)

            // Display the create bookmark dialog.
            createBookmarkDialog.show(supportFragmentManager, resources.getString(R.string.create_bookmark))
        }

        // Restore the state if the app has been restarted.
        if (savedInstanceState != null) {
            // Restore the current folder.
            currentFolder = savedInstanceState.getString(CURRENT_FOLDER)!!

            // Update the bookmarks list view after it has loaded.
            bookmarksListView.post {
                // Get the checked bookmarks array list.
                val checkedBookmarksArrayList = savedInstanceState.getIntegerArrayList(CHECKED_BOOKMARKS_ARRAY_LIST)!!

                // Check each previously checked bookmark in the list view.
                checkedBookmarksArrayList.forEach(Consumer { position: Int -> bookmarksListView.setItemChecked(position, true) })
            }
        }

        // Load the current folder.
        loadFolder()
    }

    public override fun onRestart() {
        // Run the default commands.
        super.onRestart()

        // Update the list view if returning from the bookmarks database view activity.
        if (restartFromBookmarksDatabaseViewActivity) {
            // Load the current folder in the list view.
            loadFolder()

            // Reset the restart from bookmarks database view activity flag.
            restartFromBookmarksDatabaseViewActivity = false
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        // Run the default commands.
        super.onSaveInstanceState(savedInstanceState)

        // Get the sparse boolean array of the checked items.
        val checkedBookmarksSparseBooleanArray = bookmarksListView.checkedItemPositions

        // Create a checked items array list.
        val checkedBookmarksArrayList = ArrayList<Int>()

        // Add each checked bookmark position to the array list.
        for (i in 0 until checkedBookmarksSparseBooleanArray.size()) {
            // Check to see if the bookmark is currently checked.  Bookmarks that have previously been checked but currently aren't will be populated in the sparse boolean array, but will return false.
            if (checkedBookmarksSparseBooleanArray.valueAt(i)) {
                // Add the bookmark position to the checked bookmarks array list.
                checkedBookmarksArrayList.add(checkedBookmarksSparseBooleanArray.keyAt(i))
            }
        }

        // Store the variables in the saved instance state.
        savedInstanceState.putString(CURRENT_FOLDER, currentFolder)
        savedInstanceState.putIntegerArrayList(CHECKED_BOOKMARKS_ARRAY_LIST, checkedBookmarksArrayList)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu.
        menuInflater.inflate(R.menu.bookmarks_options_menu, menu)

        // Success.
        return true
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        // Get a handle for the menu item ID.
        val menuItemId = menuItem.itemId

        // Run the command according to the selected option.
        if (menuItemId == android.R.id.home) {  // Home.  The home arrow is identified as `android.R.id.home`, not just `R.id.home`.
            if (currentFolder.isEmpty()) {  // Currently in the home folder.
                // Prepare to finish the activity.
                prepareFinish()
            } else {  // Currently in a subfolder.
                // Set the former parent folder as the current folder.
                currentFolder = bookmarksDatabaseHelper.getParentFolderName(currentFolder)

                // Load the new current folder.
                loadFolder()
            }
        } else if (menuItemId == R.id.options_menu_select_all_bookmarks) {  // Select all.
            // Get the total number of bookmarks.
            val numberOfBookmarks = bookmarksListView.count

            // Select them all.
            for (i in 0 until numberOfBookmarks) {
                bookmarksListView.setItemChecked(i, true)
            }
        } else if (menuItemId == R.id.bookmarks_database_view) {
            // Close the contextual action bar if it is displayed.  This can happen if the bottom app bar is enabled.
            contextualActionMode?.finish()

            // Create an intent to launch the bookmarks database view activity.
            val bookmarksDatabaseViewIntent = Intent(this, BookmarksDatabaseViewActivity::class.java)

            // Include the favorite icon byte array to the intent.
            bookmarksDatabaseViewIntent.putExtra(CURRENT_FAVORITE_ICON_BYTE_ARRAY, currentFavoriteIconByteArray)

            // Make it so.
            startActivity(bookmarksDatabaseViewIntent)
        }
        return true
    }

    override fun onCreateBookmark(dialogFragment: DialogFragment, favoriteIconBitmap: Bitmap) {
        // Get the alert dialog from the fragment.
        val dialog = dialogFragment.dialog!!

        // Get the views from the dialog fragment.
        val createBookmarkNameEditText = dialog.findViewById<EditText>(R.id.create_bookmark_name_edittext)
        val createBookmarkUrlEditText = dialog.findViewById<EditText>(R.id.create_bookmark_url_edittext)

        // Extract the strings from the edit texts.
        val bookmarkNameString = createBookmarkNameEditText.text.toString()
        val bookmarkUrlString = createBookmarkUrlEditText.text.toString()

        // Create a favorite icon byte array output stream.
        val favoriteIconByteArrayOutputStream = ByteArrayOutputStream()

        // Convert the favorite icon bitmap to a byte array.  `0` is for lossless compression (the only option for a PNG).
        favoriteIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, favoriteIconByteArrayOutputStream)

        // Convert the favorite icon byte array stream to a byte array.
        val favoriteIconByteArray = favoriteIconByteArrayOutputStream.toByteArray()

        // Display the new bookmark below the current items in the (0 indexed) list.
        val newBookmarkDisplayOrder = bookmarksListView.count

        // Create the bookmark.
        bookmarksDatabaseHelper.createBookmark(bookmarkNameString, bookmarkUrlString, currentFolder, newBookmarkDisplayOrder, favoriteIconByteArray)

        // Update the bookmarks cursor with the current contents of this folder.
        bookmarksCursor = bookmarksDatabaseHelper.getBookmarksByDisplayOrder(currentFolder)

        // Update the list view.
        bookmarksCursorAdapter.changeCursor(bookmarksCursor)

        // Scroll to the new bookmark.
        bookmarksListView.setSelection(newBookmarkDisplayOrder)
    }

    override fun onCreateBookmarkFolder(dialogFragment: DialogFragment, favoriteIconBitmap: Bitmap) {
        // Get the dialog from the dialog fragment.
        val dialog = dialogFragment.dialog!!

        // Get handles for the views in the dialog fragment.
        val folderNameEditText = dialog.findViewById<EditText>(R.id.folder_name_edittext)
        val defaultIconRadioButton = dialog.findViewById<RadioButton>(R.id.default_icon_radiobutton)
        val defaultIconImageView = dialog.findViewById<ImageView>(R.id.default_icon_imageview)

        // Get new folder name string.
        val folderNameString = folderNameEditText.text.toString()

        // Set the folder icon bitmap according to the dialog.
        val folderIconBitmap = if (defaultIconRadioButton.isChecked) {  // Use the default folder icon.
            // Get the default folder icon drawable.
            val folderIconDrawable = defaultIconImageView.drawable

            // Convert the folder icon drawable to a bitmap drawable.
            val folderIconBitmapDrawable = folderIconDrawable as BitmapDrawable

            // Convert the folder icon bitmap drawable to a bitmap.
            folderIconBitmapDrawable.bitmap
        } else {  // Use the WebView favorite icon.
            // Copy the favorite icon bitmap to the folder icon bitmap.
            favoriteIconBitmap
        }

        // Create a folder icon byte array output stream.
        val folderIconByteArrayOutputStream = ByteArrayOutputStream()

        // Convert the folder icon bitmap to a byte array.  `0` is for lossless compression (the only option for a PNG).
        folderIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, folderIconByteArrayOutputStream)

        // Convert the folder icon byte array stream to a byte array.
        val folderIconByteArray = folderIconByteArrayOutputStream.toByteArray()

        // Move all the bookmarks down one in the display order.
        for (i in 0 until bookmarksListView.count) {
            val databaseId = bookmarksListView.getItemIdAtPosition(i).toInt()
            bookmarksDatabaseHelper.updateDisplayOrder(databaseId, i + 1)
        }

        // Create the folder, which will be placed at the top of the list view.
        bookmarksDatabaseHelper.createFolder(folderNameString, currentFolder, folderIconByteArray)

        // Update the bookmarks cursor with the contents of the current folder.
        bookmarksCursor = bookmarksDatabaseHelper.getBookmarksByDisplayOrder(currentFolder)

        // Update the list view.
        bookmarksCursorAdapter.changeCursor(bookmarksCursor)

        // Scroll to the new folder.
        bookmarksListView.setSelection(0)
    }

    override fun onSaveBookmark(dialogFragment: DialogFragment, selectedBookmarkDatabaseId: Int, favoriteIconBitmap: Bitmap) {
        // Get the dialog from the dialog fragment.
        val dialog = dialogFragment.dialog!!

        // Get handles for the views from the dialog fragment.
        val bookmarkNameEditText = dialog.findViewById<EditText>(R.id.bookmark_name_edittext)
        val bookmarkUrlEditText = dialog.findViewById<EditText>(R.id.bookmark_url_edittext)
        val currentIconRadioButton = dialog.findViewById<RadioButton>(R.id.current_icon_radiobutton)

        // Get the bookmark strings.
        val bookmarkNameString = bookmarkNameEditText.text.toString()
        val bookmarkUrlString = bookmarkUrlEditText.text.toString()

        // Update the bookmark.
        if (currentIconRadioButton.isChecked) {  // Update the bookmark without changing the favorite icon.
            bookmarksDatabaseHelper.updateBookmark(selectedBookmarkDatabaseId, bookmarkNameString, bookmarkUrlString)
        } else {  // Update the bookmark using the WebView favorite icon.
            // Create a favorite icon byte array output stream.
            val newFavoriteIconByteArrayOutputStream = ByteArrayOutputStream()

            // Convert the favorite icon bitmap to a byte array.  `0` is for lossless compression (the only option for a PNG).
            favoriteIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, newFavoriteIconByteArrayOutputStream)

            // Convert the favorite icon byte array stream to a byte array.
            val newFavoriteIconByteArray = newFavoriteIconByteArrayOutputStream.toByteArray()

            //  Update the bookmark and the favorite icon.
            bookmarksDatabaseHelper.updateBookmark(selectedBookmarkDatabaseId, bookmarkNameString, bookmarkUrlString, newFavoriteIconByteArray)
        }

        // Close the contextual action bar if it is displayed.
        contextualActionMode?.finish()

        // Update the bookmarks cursor with the contents of the current folder.
        bookmarksCursor = bookmarksDatabaseHelper.getBookmarksByDisplayOrder(currentFolder)

        // Update the list view.
        bookmarksCursorAdapter.changeCursor(bookmarksCursor)
    }

    override fun onSaveBookmarkFolder(dialogFragment: DialogFragment, selectedFolderDatabaseId: Int, favoriteIconBitmap: Bitmap) {
        // Get the dialog from the dialog fragment.
        val dialog = dialogFragment.dialog!!

        // Get handles for the views from the dialog fragment.
        val currentFolderIconRadioButton = dialog.findViewById<RadioButton>(R.id.current_icon_radiobutton)
        val defaultFolderIconRadioButton = dialog.findViewById<RadioButton>(R.id.default_icon_radiobutton)
        val defaultFolderIconImageView = dialog.findViewById<ImageView>(R.id.default_icon_imageview)
        val editFolderNameEditText = dialog.findViewById<EditText>(R.id.folder_name_edittext)

        // Get the new folder name.
        val newFolderNameString = editFolderNameEditText.text.toString()

        // Check if the favorite icon has changed.
        if (currentFolderIconRadioButton.isChecked) {  // Only the name has changed.
            // Update the name in the database.
            bookmarksDatabaseHelper.updateFolder(selectedFolderDatabaseId, oldFolderNameString, newFolderNameString)
        } else {  // The icon has changed.  TODO:  Test.
            // Populate the new folder icon bitmap.
            val folderIconBitmap: Bitmap = if (defaultFolderIconRadioButton.isChecked) {
                // Get the default folder icon drawable.
                val folderIconDrawable = defaultFolderIconImageView.drawable

                // Convert the folder icon drawable to a bitmap drawable.
                val folderIconBitmapDrawable = folderIconDrawable as BitmapDrawable

                // Convert the folder icon bitmap drawable to a bitmap.
                folderIconBitmapDrawable.bitmap
            } else {  // Use the WebView favorite icon.
                // Copy the favorite icon bitmap to the folder icon bitmap.
                favoriteIconBitmap
            }

            // Create a folder icon byte array output stream.
            val newFolderIconByteArrayOutputStream = ByteArrayOutputStream()

            // Convert the folder icon bitmap to a byte array.  `0` is for lossless compression (the only option for a PNG).
            folderIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, newFolderIconByteArrayOutputStream)

            // Convert the folder icon byte array stream to a byte array.
            val newFolderIconByteArray = newFolderIconByteArrayOutputStream.toByteArray()

            // Update the database.
            if (!currentFolderIconRadioButton.isChecked && newFolderNameString == oldFolderNameString)  // Only the icon has changed.
                bookmarksDatabaseHelper.updateFolder(selectedFolderDatabaseId, newFolderIconByteArray)
            else  // The folder icon and the name have changed.
                bookmarksDatabaseHelper.updateFolder(selectedFolderDatabaseId, oldFolderNameString, newFolderNameString, newFolderIconByteArray)
        }

        // Update the bookmarks cursor with the current contents of this folder.
        bookmarksCursor = bookmarksDatabaseHelper.getBookmarksByDisplayOrder(currentFolder)

        // Update the list view.
        bookmarksCursorAdapter.changeCursor(bookmarksCursor)

        // Close the contextual action mode.
        contextualActionMode!!.finish()
    }

    override fun onMoveToFolder(dialogFragment: DialogFragment) {
        // Get the dialog from the dialog fragment.
        val dialog = dialogFragment.dialog!!

        // Get a handle for the folder list view from the dialog.
        val folderListView = dialog.findViewById<ListView>(R.id.move_to_folder_listview)

        // Store a long array of the selected folders.
        val newFolderLongArray = folderListView.checkedItemIds

        // Get the new folder database ID.  Only one folder will be selected so it will be the first one.
        val newFolderDatabaseId = newFolderLongArray[0].toInt()

        // Set the new folder name.
        val newFolderName = if (newFolderDatabaseId == 0) {
            // The new folder is the home folder, represented as `""` in the database.
            ""
        } else {
            // Get the new folder name from the database.
            bookmarksDatabaseHelper.getFolderName(newFolderDatabaseId)
        }

        // Get a long array with the the database ID of the selected bookmarks.
        val selectedBookmarksLongArray = bookmarksListView.checkedItemIds

        // Move each of the selected bookmarks to the new folder.
        for (databaseIdLong in selectedBookmarksLongArray) {
            // Convert the database long ID to an int for each selected bookmark.
            val databaseIdInt = databaseIdLong.toInt()

            // Move the selected bookmark to the new folder.
            bookmarksDatabaseHelper.moveToFolder(databaseIdInt, newFolderName)
        }

        // Update the bookmarks cursor with the current contents of this folder.
        bookmarksCursor = bookmarksDatabaseHelper.getBookmarksByDisplayOrder(currentFolder)

        // Update the list view.
        bookmarksCursorAdapter.changeCursor(bookmarksCursor)

        // Close the contextual app bar.
        contextualActionMode!!.finish()
    }

    private fun countBookmarkFolderContents(folderDatabaseId: Int): Int {
        // Get the name of the folder.
        val folderName = bookmarksDatabaseHelper.getFolderName(folderDatabaseId)

        // Get the contents of the folder.
        val folderCursor = bookmarksDatabaseHelper.getBookmarkIds(folderName)

        // Initialize the bookmark counter.
        var bookmarkCounter = 0

        // Count each of the bookmarks in the folder.
        for (i in 0 until folderCursor.count) {
            // Move the folder cursor to the current row.
            folderCursor.moveToPosition(i)

            // Get the database ID of the item.
            val itemDatabaseId = folderCursor.getInt(folderCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.ID))

            // If this is a folder, recursively count the contents first.
            if (bookmarksDatabaseHelper.isFolder(itemDatabaseId))
                bookmarkCounter += countBookmarkFolderContents(itemDatabaseId)

            // Add the bookmark to the running total.
            bookmarkCounter++
        }

        // Return the bookmark counter.
        return bookmarkCounter
    }

    private fun deleteBookmarkFolderContents(folderDatabaseId: Int) {
        // Get the name of the folder.
        val folderName = bookmarksDatabaseHelper.getFolderName(folderDatabaseId)

        // Get the contents of the folder.
        val folderCursor = bookmarksDatabaseHelper.getBookmarkIds(folderName)

        // Delete each of the bookmarks in the folder.
        for (i in 0 until folderCursor.count) {
            // Move the folder cursor to the current row.
            folderCursor.moveToPosition(i)

            // Get the database ID of the item.
            val itemDatabaseId = folderCursor.getInt(folderCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.ID))

            // If this is a folder, recursively delete the contents first.
            if (bookmarksDatabaseHelper.isFolder(itemDatabaseId))
                deleteBookmarkFolderContents(itemDatabaseId)

            // Delete the bookmark.
            bookmarksDatabaseHelper.deleteBookmark(itemDatabaseId)
        }
    }

    private fun prepareFinish() {
        // Check to see if a snackbar is currently displayed.  If so, it must be closed before exiting so that a pending delete is completed before reloading the list view in the bookmarks drawer.
        if (bookmarksDeletedSnackbar != null && bookmarksDeletedSnackbar!!.isShown) {  // Close the bookmarks deleted snackbar before going home.
            // Set the close flag.
            closeActivityAfterDismissingSnackbar = true

            // Dismiss the snackbar.
            bookmarksDeletedSnackbar!!.dismiss()
        } else {  // Go home immediately.
            // Update the bookmarks folder for the bookmarks drawer in the main WebView activity.
            MainWebViewActivity.currentBookmarksFolder = currentFolder

            // Close the bookmarks drawer and reload the bookmarks list view when returning to the main WebView activity.
            MainWebViewActivity.restartFromBookmarksActivity = true

            // Exit the bookmarks activity.
            finish()
        }
    }

    private fun updateMoveIcons() {
        // Get a long array of the selected bookmarks.
        val selectedBookmarksLongArray = bookmarksListView.checkedItemIds

        // Get the database IDs for the first, last, and selected bookmarks.
        val firstBookmarkDatabaseId = bookmarksListView.getItemIdAtPosition(0).toInt()
        val lastBookmarkDatabaseId = bookmarksListView.getItemIdAtPosition(bookmarksListView.count - 1).toInt()  // The bookmarks list view is 0 indexed.
        val selectedBookmarkDatabaseId = selectedBookmarksLongArray[0].toInt()

        // Update the move bookmark up menu item.
        if (selectedBookmarkDatabaseId == firstBookmarkDatabaseId) {  // The selected bookmark is in the first position.
            // Disable the move bookmark up menu item.
            moveBookmarkUpMenuItem.isEnabled = false

            //  Set the icon.
            moveBookmarkUpMenuItem.setIcon(R.drawable.move_up_disabled)
        } else {  // The selected bookmark is not in the first position.
            // Enable the move bookmark up menu item.
            moveBookmarkUpMenuItem.isEnabled = true

            // Set the icon according to the theme.
            moveBookmarkUpMenuItem.setIcon(R.drawable.move_up_enabled)
        }

        // Update the move bookmark down menu item.
        if (selectedBookmarkDatabaseId == lastBookmarkDatabaseId) {  // The selected bookmark is in the last position.
            // Disable the move bookmark down menu item.
            moveBookmarkDownMenuItem.isEnabled = false

            // Set the icon.
            moveBookmarkDownMenuItem.setIcon(R.drawable.move_down_disabled)
        } else {  // The selected bookmark is not in the last position.
            // Enable the move bookmark down menu item.
            moveBookmarkDownMenuItem.isEnabled = true

            // Set the icon.
            moveBookmarkDownMenuItem.setIcon(R.drawable.move_down_enabled)
        }
    }

    private fun scrollBookmarks(selectedBookmarkPosition: Int) {
        // Get the first and last visible bookmark positions.
        val firstVisibleBookmarkPosition = bookmarksListView.firstVisiblePosition
        val lastVisibleBookmarkPosition = bookmarksListView.lastVisiblePosition

        // Calculate the number of bookmarks per screen.
        val numberOfBookmarksPerScreen = lastVisibleBookmarkPosition - firstVisibleBookmarkPosition

        // Scroll with the moved bookmark if necessary.
        if (selectedBookmarkPosition <= firstVisibleBookmarkPosition) {  // The selected bookmark position is at or above the top of the screen.
            // Scroll to the selected bookmark position.
            bookmarksListView.setSelection(selectedBookmarkPosition)
        } else if (selectedBookmarkPosition >= lastVisibleBookmarkPosition - 1) {  // The selected bookmark is at or below the bottom of the screen.
            // The `-1` handles partial bookmarks displayed at the bottom of the list view.  This command scrolls to display the selected bookmark at the bottom of the screen.
            // `+1` assures that the entire bookmark will be displayed in situations where only a partial bookmark fits at the bottom of the list view.
            bookmarksListView.setSelection(selectedBookmarkPosition - numberOfBookmarksPerScreen + 1)
        }
    }

    private fun loadFolder() {
        // Update the bookmarks cursor with the contents of the bookmarks database for the current folder.
        bookmarksCursor = bookmarksDatabaseHelper.getBookmarksByDisplayOrder(currentFolder)

        // Setup a cursor adapter.
        bookmarksCursorAdapter = object : CursorAdapter(this, bookmarksCursor, false) {
            override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
                // Inflate the individual item layout.
                return layoutInflater.inflate(R.layout.bookmarks_activity_item_linearlayout, parent, false)
            }

            override fun bindView(view: View, context: Context, cursor: Cursor) {
                // Get handles for the views.
                val bookmarkFavoriteIconImageView = view.findViewById<ImageView>(R.id.bookmark_favorite_icon)
                val bookmarkNameTextView = view.findViewById<TextView>(R.id.bookmark_name)

                // Get the favorite icon byte array from the cursor.
                val favoriteIconByteArray = cursor.getBlob(cursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.FAVORITE_ICON))

                // Convert the byte array to a bitmap beginning at the first byte and ending at the last.
                val favoriteIconBitmap = BitmapFactory.decodeByteArray(favoriteIconByteArray, 0, favoriteIconByteArray.size)

                // Display the bitmap in the bookmark favorite icon image view.
                bookmarkFavoriteIconImageView.setImageBitmap(favoriteIconBitmap)

                // Get the bookmark name from the cursor.
                val bookmarkNameString = cursor.getString(cursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.BOOKMARK_NAME))

                // Display the bookmark name.
                bookmarkNameTextView.text = bookmarkNameString

                // Make the font bold for folders.
                if (cursor.getInt(cursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.IS_FOLDER)) == 1)
                    bookmarkNameTextView.typeface = Typeface.DEFAULT_BOLD
                else  // Reset the font to default for normal bookmarks.
                    bookmarkNameTextView.typeface = Typeface.DEFAULT
            }
        }

        // Populate the list view with the adapter.
        bookmarksListView.adapter = bookmarksCursorAdapter

        // Set the app bar title.
        if (currentFolder.isEmpty())
            appBar.setTitle(R.string.bookmarks)
        else
            appBar.title = currentFolder
    }

    public override fun onDestroy() {
        // Close the bookmarks cursor and database.
        bookmarksCursor.close()
        bookmarksDatabaseHelper.close()

        // Run the default commands.
        super.onDestroy()
    }
}
