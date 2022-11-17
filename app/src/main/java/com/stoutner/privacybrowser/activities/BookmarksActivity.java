/*
 * Copyright 2016-2022 Soren Stoutner <soren@stoutner.com>.
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

package com.stoutner.privacybrowser.activities;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import com.stoutner.privacybrowser.R;
import com.stoutner.privacybrowser.dialogs.CreateBookmarkDialog;
import com.stoutner.privacybrowser.dialogs.CreateBookmarkFolderDialog;
import com.stoutner.privacybrowser.dialogs.EditBookmarkDialog;
import com.stoutner.privacybrowser.dialogs.EditBookmarkFolderDialog;
import com.stoutner.privacybrowser.dialogs.MoveToFolderDialog;
import com.stoutner.privacybrowser.helpers.BookmarksDatabaseHelper;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class BookmarksActivity extends AppCompatActivity implements CreateBookmarkDialog.CreateBookmarkListener, CreateBookmarkFolderDialog.CreateBookmarkFolderListener, EditBookmarkDialog.EditBookmarkListener,
        EditBookmarkFolderDialog.EditBookmarkFolderListener, MoveToFolderDialog.MoveToFolderListener {

    // Declare the public static variables, which are accessed from the bookmarks database view activity.
    public static String currentFolder;
    public static boolean restartFromBookmarksDatabaseViewActivity;

    // Define the saved instance state constants.
    private final String CHECKED_BOOKMARKS_ARRAY_LIST = "checked_bookmarks_array_list";
    private final String CURRENT_FOLDER = "current_folder";

    // Define the class menu items.
    private MenuItem moveBookmarkUpMenuItem;
    private MenuItem moveBookmarkDownMenuItem;

    // Declare the class variables.
    private BookmarksDatabaseHelper bookmarksDatabaseHelper;
    private Snackbar bookmarksDeletedSnackbar;
    private boolean closeActivityAfterDismissingSnackbar;

    // `bookmarksListView` is used in `onCreate()`, `onOptionsItemSelected()`, `onCreateBookmark()`, `onCreateBookmarkFolder()`, `onSaveBookmark()`, `onSaveBookmarkFolder()`, `onMoveToFolder()`,
    // `updateMoveIcons()`, `scrollBookmarks()`, and `loadFolder()`.
    private ListView bookmarksListView;

    // `bookmarksCursor` is used in `onCreate()`, `onCreateBookmark()`, `onCreateBookmarkFolder()`, `onSaveBookmark()`, `onSaveBookmarkFolder()`, `onMoveToFolder()`, `deleteBookmarkFolderContents()`,
    // `loadFolder()`, and `onDestroy()`.
    private Cursor bookmarksCursor;

    // `bookmarksCursorAdapter` is used in `onCreate(), `onCreateBookmark()`, `onCreateBookmarkFolder()`, `onSaveBookmark()`, `onSaveBookmarkFolder()`, `onMoveToFolder()`, and `onLoadFolder()`.
    private CursorAdapter bookmarksCursorAdapter;

    // `contextualActionMode` is used in `onCreate()`, `onSaveEditBookmark()`, `onSaveEditBookmarkFolder()` and `onMoveToFolder()`.
    private ActionMode contextualActionMode;

    // `appBar` is used in `onCreate()` and `loadFolder()`.
    private ActionBar appBar;

    // `oldFolderName` is used in `onCreate()` and `onSaveBookmarkFolder()`.
    private String oldFolderNameString;

    // The favorite icon byte array is populated in `onCreate()` and used in `onOptionsItemSelected()`.
    private byte[] favoriteIconByteArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Get a handle for the shared preferences.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Get the preferences.
        boolean allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false);
        boolean bottomAppBar = sharedPreferences.getBoolean(getString(R.string.bottom_app_bar_key), false);

        // Disable screenshots if not allowed.
        if (!allowScreenshots) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        // Run the default commands.
        super.onCreate(savedInstanceState);

        // Get the intent that launched the activity.
        Intent launchingIntent = getIntent();

        // Store the current URL and title.
        String currentUrl = launchingIntent.getStringExtra("current_url");
        String currentTitle = launchingIntent.getStringExtra("current_title");

        // Set the current folder variable.
        if (launchingIntent.getStringExtra("current_folder") != null) {  // Set the current folder from the intent.
            currentFolder = launchingIntent.getStringExtra("current_folder");
        } else {  // Set the current folder to be `""`, which is the home folder.
            currentFolder = "";
        }

        // Get the favorite icon byte array.
        favoriteIconByteArray = launchingIntent.getByteArrayExtra("favorite_icon_byte_array");

        // Remove the incorrect lint warning that the favorite icon byte array might be null.
        assert favoriteIconByteArray != null;

        // Convert the favorite icon byte array to a bitmap.
        Bitmap favoriteIconBitmap = BitmapFactory.decodeByteArray(favoriteIconByteArray, 0, favoriteIconByteArray.length);

        // Set the content according to the app bar position.
        if (bottomAppBar) {
            // Set the content view.
            setContentView(R.layout.bookmarks_bottom_appbar);
        } else {
            // `Window.FEATURE_ACTION_MODE_OVERLAY` makes the contextual action mode cover the support action bar.  It must be requested before the content is set.
            supportRequestWindowFeature(Window.FEATURE_ACTION_MODE_OVERLAY);

            // Set the content view.
            setContentView(R.layout.bookmarks_top_appbar);
        }

        // Get a handle for the toolbar.
        final Toolbar toolbar = findViewById(R.id.bookmarks_toolbar);

        // Set the support action bar.
        setSupportActionBar(toolbar);

        // Get handles for the views.
        appBar = getSupportActionBar();
        bookmarksListView = findViewById(R.id.bookmarks_listview);

        // Remove the incorrect lint warning that `appBar` might be null.
        assert appBar != null;

        // Display the home arrow on the app bar.
        appBar.setDisplayHomeAsUpEnabled(true);

        // Control what the system back command does.
        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Prepare to finish the activity.
                prepareFinish();
            }
        };

        // Register the on back pressed callback.
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);

        // Initialize the database helper.
        bookmarksDatabaseHelper = new BookmarksDatabaseHelper(this);

        // Set a listener so that tapping a list item loads the URL or folder.
        bookmarksListView.setOnItemClickListener((parent, view, position, id) -> {
            // Convert the id from long to int to match the format of the bookmarks database.
            int databaseId = (int) id;

            // Get the bookmark cursor for this ID.
            Cursor bookmarkCursor = bookmarksDatabaseHelper.getBookmark(databaseId);

            // Move the cursor to the first entry.
            bookmarkCursor.moveToFirst();

            // Act upon the bookmark according to the type.
            if (bookmarkCursor.getInt(bookmarkCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.IS_FOLDER)) == 1) {  // The selected bookmark is a folder.
                // Update the current folder.
                currentFolder = bookmarkCursor.getString(bookmarkCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.BOOKMARK_NAME));

                // Load the new folder.
                loadFolder();
            } else {  // The selected bookmark is not a folder.
                // Instantiate the edit bookmark dialog.
                DialogFragment editBookmarkDialog = EditBookmarkDialog.bookmarkDatabaseId(databaseId, favoriteIconBitmap);

                // Make it so.
                editBookmarkDialog.show(getSupportFragmentManager(), getResources().getString(R.string.edit_bookmark));
            }

            // Close the cursor.
            bookmarkCursor.close();
        });

        // Handle long presses on the list view.
        bookmarksListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            // Instantiate the common variables.
            MenuItem editBookmarkMenuItem;
            MenuItem deleteBookmarksMenuItem;
            MenuItem selectAllBookmarksMenuItem;
            boolean deletingBookmarks;

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                // Inflate the menu for the contextual app bar.
                getMenuInflater().inflate(R.menu.bookmarks_context_menu, menu);

                // Set the title.
                if (currentFolder.isEmpty()) {  // Use `R.string.bookmarks` if in the home folder.
                    mode.setTitle(R.string.bookmarks);
                } else {  // Use the current folder name as the title.
                    mode.setTitle(currentFolder);
                }

                // Get handles for menu items that need to be selectively disabled.
                moveBookmarkUpMenuItem = menu.findItem(R.id.move_bookmark_up);
                moveBookmarkDownMenuItem = menu.findItem(R.id.move_bookmark_down);
                editBookmarkMenuItem = menu.findItem(R.id.edit_bookmark);
                deleteBookmarksMenuItem = menu.findItem(R.id.delete_bookmark);
                selectAllBookmarksMenuItem = menu.findItem(R.id.context_menu_select_all_bookmarks);

                // Disable the delete bookmarks menu item if a delete is pending.
                deleteBookmarksMenuItem.setEnabled(!deletingBookmarks);

                // Store a handle for the contextual action mode so it can be closed programatically.
                contextualActionMode = mode;

                // Make it so.
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                // Get a handle for the move to folder menu item.
                MenuItem moveToFolderMenuItem = menu.findItem(R.id.move_to_folder);

                // Get a Cursor with all of the folders.
                Cursor folderCursor = bookmarksDatabaseHelper.getAllFolders();

                // Enable the move to folder menu item if at least one folder exists.
                moveToFolderMenuItem.setVisible(folderCursor.getCount() > 0);

                // Make it so.
                return true;
            }

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                // Get the number of selected bookmarks.
                int numberOfSelectedBookmarks = bookmarksListView.getCheckedItemCount();

                // Only process commands if at least one bookmark is selected.  Otherwise, a context menu with 0 selected bookmarks is briefly displayed.
                if (numberOfSelectedBookmarks > 0) {
                    // Adjust the ActionMode and the menu according to the number of selected bookmarks.
                    if (numberOfSelectedBookmarks == 1) {  // One bookmark is selected.
                        // List the number of selected bookmarks in the subtitle.
                        mode.setSubtitle(getString(R.string.selected) + "  1");

                        // Show the `Move Up`, `Move Down`, and  `Edit` options.
                        moveBookmarkUpMenuItem.setVisible(true);
                        moveBookmarkDownMenuItem.setVisible(true);
                        editBookmarkMenuItem.setVisible(true);

                        // Update the enabled status of the move icons.
                        updateMoveIcons();
                    } else {  // More than one bookmark is selected.
                        // List the number of selected bookmarks in the subtitle.
                        mode.setSubtitle(getString(R.string.selected) + "  " + numberOfSelectedBookmarks);

                        // Hide non-applicable `MenuItems`.
                        moveBookmarkUpMenuItem.setVisible(false);
                        moveBookmarkDownMenuItem.setVisible(false);
                        editBookmarkMenuItem.setVisible(false);
                    }

                    // Show the select all menu item if all the bookmarks are not selected.
                    selectAllBookmarksMenuItem.setVisible(bookmarksListView.getCheckedItemCount() != bookmarksListView.getCount());
                }
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                // Declare the common variables.
                int selectedBookmarkNewPosition;
                final SparseBooleanArray selectedBookmarksPositionsSparseBooleanArray;

                // Initialize the selected bookmark position.
                int selectedBookmarkPosition = 0;

                // Get the menu item ID.
                int menuItemId = menuItem.getItemId();

                // Run the commands according to the selected action item.
                if (menuItemId == R.id.move_bookmark_up) {  // Move the bookmark up.
                    // Get the array of checked bookmark positions.
                    selectedBookmarksPositionsSparseBooleanArray = bookmarksListView.getCheckedItemPositions();

                    // Get the position of the bookmark that is selected.  If other bookmarks have previously been selected they will be included in the sparse boolean array with a value of `false`.
                    for (int i = 0; i < selectedBookmarksPositionsSparseBooleanArray.size(); i++) {
                        // Check to see if the value for the bookmark is true, meaning it is currently selected.
                        if (selectedBookmarksPositionsSparseBooleanArray.valueAt(i)) {
                            // Only one bookmark should have a value of `true` when move bookmark up is enabled.
                            selectedBookmarkPosition = selectedBookmarksPositionsSparseBooleanArray.keyAt(i);
                        }
                    }

                    // Calculate the new position of the selected bookmark.
                    selectedBookmarkNewPosition = selectedBookmarkPosition - 1;

                    // Iterate through the bookmarks.
                    for (int i = 0; i < bookmarksListView.getCount(); i++) {
                        // Get the database ID for the current bookmark.
                        int currentBookmarkDatabaseId = (int) bookmarksListView.getItemIdAtPosition(i);

                        // Update the display order for the current bookmark.
                        if (i == selectedBookmarkPosition) {  // The current bookmark is the selected bookmark.
                            // Move the current bookmark up one.
                            bookmarksDatabaseHelper.updateDisplayOrder(currentBookmarkDatabaseId, i - 1);
                        } else if ((i + 1) == selectedBookmarkPosition) {  // The current bookmark is immediately above the selected bookmark.
                            // Move the current bookmark down one.
                            bookmarksDatabaseHelper.updateDisplayOrder(currentBookmarkDatabaseId, i + 1);
                        } else {  // The current bookmark is not changing positions.
                            // Move `bookmarksCursor` to the current bookmark position.
                            bookmarksCursor.moveToPosition(i);

                            // Update the display order only if it is not correct in the database.
                            if (bookmarksCursor.getInt(bookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.DISPLAY_ORDER)) != i) {
                                bookmarksDatabaseHelper.updateDisplayOrder(currentBookmarkDatabaseId, i);
                            }
                        }
                    }

                    // Update the bookmarks cursor with the current contents of the bookmarks database.
                    bookmarksCursor = bookmarksDatabaseHelper.getBookmarksByDisplayOrder(currentFolder);

                    // Update the list view.
                    bookmarksCursorAdapter.changeCursor(bookmarksCursor);

                    // Scroll with the bookmark.
                    scrollBookmarks(selectedBookmarkNewPosition);

                    // Update the enabled status of the move icons.
                    updateMoveIcons();
                } else if (menuItemId == R.id.move_bookmark_down) {  // Move the bookmark down.
                    // Get the array of checked bookmark positions.
                    selectedBookmarksPositionsSparseBooleanArray = bookmarksListView.getCheckedItemPositions();

                    // Get the position of the bookmark that is selected.  If other bookmarks have previously been selected they will be included in the sparse boolean array with a value of `false`.
                    for (int i = 0; i < selectedBookmarksPositionsSparseBooleanArray.size(); i++) {
                        // Check to see if the value for the bookmark is true, meaning it is currently selected.
                        if (selectedBookmarksPositionsSparseBooleanArray.valueAt(i)) {
                            // Only one bookmark should have a value of `true` when move bookmark down is enabled.
                            selectedBookmarkPosition = selectedBookmarksPositionsSparseBooleanArray.keyAt(i);
                        }
                    }

                    // Calculate the new position of the selected bookmark.
                    selectedBookmarkNewPosition = selectedBookmarkPosition + 1;

                    // Iterate through the bookmarks.
                    for (int i = 0; i < bookmarksListView.getCount(); i++) {
                        // Get the database ID for the current bookmark.
                        int currentBookmarkDatabaseId = (int) bookmarksListView.getItemIdAtPosition(i);

                        // Update the display order for the current bookmark.
                        if (i == selectedBookmarkPosition) {  // The current bookmark is the selected bookmark.
                            // Move the current bookmark down one.
                            bookmarksDatabaseHelper.updateDisplayOrder(currentBookmarkDatabaseId, i + 1);
                        } else if ((i - 1) == selectedBookmarkPosition) {  // The current bookmark is immediately below the selected bookmark.
                            // Move the bookmark below the selected bookmark up one.
                            bookmarksDatabaseHelper.updateDisplayOrder(currentBookmarkDatabaseId, i - 1);
                        } else {  // The current bookmark is not changing positions.
                            // Move `bookmarksCursor` to the current bookmark position.
                            bookmarksCursor.moveToPosition(i);

                            // Update the display order only if it is not correct in the database.
                            if (bookmarksCursor.getInt(bookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.DISPLAY_ORDER)) != i) {
                                bookmarksDatabaseHelper.updateDisplayOrder(currentBookmarkDatabaseId, i);
                            }
                        }
                    }

                    // Update the bookmarks cursor with the current contents of the bookmarks database.
                    bookmarksCursor = bookmarksDatabaseHelper.getBookmarksByDisplayOrder(currentFolder);

                    // Update the list view.
                    bookmarksCursorAdapter.changeCursor(bookmarksCursor);

                    // Scroll with the bookmark.
                    scrollBookmarks(selectedBookmarkNewPosition);

                    // Update the enabled status of the move icons.
                    updateMoveIcons();
                } else if (menuItemId == R.id.move_to_folder) {  // Move to folder.
                    // Instantiate the move to folder alert dialog.
                    DialogFragment moveToFolderDialog = MoveToFolderDialog.moveBookmarks(currentFolder, bookmarksListView.getCheckedItemIds());

                    // Show the move to folder alert dialog.
                    moveToFolderDialog.show(getSupportFragmentManager(), getResources().getString(R.string.move_to_folder));
                } else if (menuItemId == R.id.edit_bookmark) {
                    // Get the array of checked bookmark positions.
                    selectedBookmarksPositionsSparseBooleanArray = bookmarksListView.getCheckedItemPositions();

                    // Get the position of the bookmark that is selected.  If other bookmarks have previously been selected they will be included in the sparse boolean array with a value of `false`.
                    for (int i = 0; i < selectedBookmarksPositionsSparseBooleanArray.size(); i++) {
                        // Check to see if the value for the bookmark is true, meaning it is currently selected.
                        if (selectedBookmarksPositionsSparseBooleanArray.valueAt(i)) {
                            // Only one bookmark should have a value of `true` when move edit bookmark is enabled.
                            selectedBookmarkPosition = selectedBookmarksPositionsSparseBooleanArray.keyAt(i);
                        }
                    }

                    // Move the cursor to the selected position.
                    bookmarksCursor.moveToPosition(selectedBookmarkPosition);

                    // Find out if this bookmark is a folder.
                    boolean isFolder = (bookmarksCursor.getInt(bookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.IS_FOLDER)) == 1);

                    // Get the selected bookmark database ID.
                    int databaseId = bookmarksCursor.getInt(bookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.ID));

                    // Show the edit bookmark or edit bookmark folder dialog.
                    if (isFolder) {
                        // Save the current folder name, which is used in `onSaveBookmarkFolder()`.
                        oldFolderNameString = bookmarksCursor.getString(bookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.BOOKMARK_NAME));

                        // Instantiate the edit bookmark folder dialog.
                        DialogFragment editFolderDialog = EditBookmarkFolderDialog.folderDatabaseId(databaseId, favoriteIconBitmap);

                        // Make it so.
                        editFolderDialog.show(getSupportFragmentManager(), getResources().getString(R.string.edit_folder));
                    } else {
                        // Instantiate the edit bookmark dialog.
                        DialogFragment editBookmarkDialog = EditBookmarkDialog.bookmarkDatabaseId(databaseId, favoriteIconBitmap);

                        // Make it so.
                        editBookmarkDialog.show(getSupportFragmentManager(), getResources().getString(R.string.edit_bookmark));
                    }
                } else if (menuItemId == R.id.delete_bookmark) {  // Delete bookmark.
                    // Set the deleting bookmarks flag, which prevents the delete menu item from being enabled until the current process finishes.
                    deletingBookmarks = true;

                    // Get an array of the selected row IDs.
                    final long[] selectedBookmarksIdsLongArray = bookmarksListView.getCheckedItemIds();

                    // Initialize a variable to count the number of bookmarks to delete.
                    int numberOfBookmarksToDelete = 0;

                    // Count the number of bookmarks.
                    for (long databaseIdLong : selectedBookmarksIdsLongArray) {
                        // Convert the database ID long to an int.
                        int databaseIdInt = (int) databaseIdLong;

                        // Count the contents of the folder if the selected bookmark is a folder.
                        if (bookmarksDatabaseHelper.isFolder(databaseIdInt)) {
                            // Add the bookmarks from the folder to the running total.
                            numberOfBookmarksToDelete = numberOfBookmarksToDelete + countBookmarkFolderContents(databaseIdInt);
                        }

                        // Increment the count of the number of bookmarks to delete.
                        numberOfBookmarksToDelete++;
                    }

                    // Get an array of checked bookmarks.  `.clone()` makes a copy that won't change if the list view is reloaded, which is needed for re-selecting the bookmarks on undelete.
                    selectedBookmarksPositionsSparseBooleanArray = bookmarksListView.getCheckedItemPositions().clone();

                    // Update the bookmarks cursor with the current contents of the bookmarks database except for the specified database IDs.
                    bookmarksCursor = bookmarksDatabaseHelper.getBookmarksByDisplayOrderExcept(selectedBookmarksIdsLongArray, currentFolder);

                    // Update the list view.
                    bookmarksCursorAdapter.changeCursor(bookmarksCursor);

                    // Create a Snackbar with the number of deleted bookmarks.
                    bookmarksDeletedSnackbar = Snackbar.make(findViewById(R.id.bookmarks_coordinatorlayout), getString(R.string.bookmarks_deleted) + "  " + numberOfBookmarksToDelete,
                            Snackbar.LENGTH_LONG)
                            .setAction(R.string.undo, view -> {
                                // Do nothing because everything will be handled by `onDismissed()` below.
                            })
                            .addCallback(new Snackbar.Callback() {
                                @SuppressLint("SwitchIntDef")  // Ignore the lint warning about not handling the other possible events as they are covered by `default:`.
                                @Override
                                public void onDismissed(Snackbar snackbar, int event) {
                                    if (event == Snackbar.Callback.DISMISS_EVENT_ACTION) {  // The user pushed the undo button.
                                        // Update the bookmarks cursor with the current contents of the bookmarks database, including the "deleted" bookmarks.
                                        bookmarksCursor = bookmarksDatabaseHelper.getBookmarksByDisplayOrder(currentFolder);

                                        // Update the list view.
                                        bookmarksCursorAdapter.changeCursor(bookmarksCursor);

                                        // Re-select the previously selected bookmarks.
                                        for (int i = 0; i < selectedBookmarksPositionsSparseBooleanArray.size(); i++) {
                                            bookmarksListView.setItemChecked(selectedBookmarksPositionsSparseBooleanArray.keyAt(i), true);
                                        }
                                    } else {  // The snackbar was dismissed without the undo button being pushed.
                                        // Delete each selected bookmark.
                                        for (long databaseIdLong : selectedBookmarksIdsLongArray) {
                                            // Convert `databaseIdLong` to an int.
                                            int databaseIdInt = (int) databaseIdLong;

                                            // Delete the contents of the folder if the selected bookmark is a folder.
                                            if (bookmarksDatabaseHelper.isFolder(databaseIdInt)) {
                                                deleteBookmarkFolderContents(databaseIdInt);
                                            }

                                            // Delete the selected bookmark.
                                            bookmarksDatabaseHelper.deleteBookmark(databaseIdInt);
                                        }

                                        // Update the display order.
                                        for (int i = 0; i < bookmarksListView.getCount(); i++) {
                                            // Get the database ID for the current bookmark.
                                            int currentBookmarkDatabaseId = (int) bookmarksListView.getItemIdAtPosition(i);

                                            // Move `bookmarksCursor` to the current bookmark position.
                                            bookmarksCursor.moveToPosition(i);

                                            // Update the display order only if it is not correct in the database.
                                            if (bookmarksCursor.getInt(bookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.DISPLAY_ORDER)) != i) {
                                                bookmarksDatabaseHelper.updateDisplayOrder(currentBookmarkDatabaseId, i);
                                            }
                                        }
                                    }

                                    // Reset the deleting bookmarks flag.
                                    deletingBookmarks = false;

                                    // Enable the delete bookmarks menu item.
                                    deleteBookmarksMenuItem.setEnabled(true);

                                    // Close the activity if back has been pressed.
                                    if (closeActivityAfterDismissingSnackbar) {
                                        finish();
                                    }
                                }
                            });

                    //Show the Snackbar.
                    bookmarksDeletedSnackbar.show();
                } else if (menuItemId == R.id.context_menu_select_all_bookmarks) {  // Select all.
                    // Get the total number of bookmarks.
                    int numberOfBookmarks = bookmarksListView.getCount();

                    // Select them all.
                    for (int i = 0; i < numberOfBookmarks; i++) {
                        bookmarksListView.setItemChecked(i, true);
                    }
                }

                // Consume the click.
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                // Do nothing.
            }
        });

        // Get handles for the floating action buttons.
        FloatingActionButton createBookmarkFolderFab = findViewById(R.id.create_bookmark_folder_fab);
        FloatingActionButton createBookmarkFab = findViewById(R.id.create_bookmark_fab);

        // Set the create new bookmark folder FAB to display the `AlertDialog`.
        createBookmarkFolderFab.setOnClickListener(v -> {
            // Create a create bookmark folder dialog.
            DialogFragment createBookmarkFolderDialog = CreateBookmarkFolderDialog.createBookmarkFolder(favoriteIconBitmap);

            // Show the create bookmark folder dialog.
            createBookmarkFolderDialog.show(getSupportFragmentManager(), getString(R.string.create_folder));
        });

        // Set the create new bookmark FAB to display the alert dialog.
        createBookmarkFab.setOnClickListener(view -> {
            // Remove the incorrect lint warning below.
            assert currentUrl != null;
            assert currentTitle != null;

            // Instantiate the create bookmark dialog.
            DialogFragment createBookmarkDialog = CreateBookmarkDialog.createBookmark(currentUrl, currentTitle, favoriteIconBitmap);

            // Display the create bookmark dialog.
            createBookmarkDialog.show(getSupportFragmentManager(), getResources().getString(R.string.create_bookmark));
        });

        // Restore the state if the app has been restarted.
        if (savedInstanceState != null) {
            // Restore the current folder.
            currentFolder = savedInstanceState.getString(CURRENT_FOLDER);

            // Update the bookmarks list view after it has loaded.
            bookmarksListView.post(() -> {
                // Get the checked bookmarks array list.
                ArrayList<Integer> checkedBookmarksArrayList = savedInstanceState.getIntegerArrayList(CHECKED_BOOKMARKS_ARRAY_LIST);

                // Check each previously checked bookmark in the list view.  When the minimum API >= 24 a `forEach()` command can be used instead.
                if (checkedBookmarksArrayList != null) {
                    for (int i = 0; i < checkedBookmarksArrayList.size(); i++) {
                        bookmarksListView.setItemChecked(checkedBookmarksArrayList.get(i), true);
                    }
                }
            });
        }

        // Load the home folder.
        loadFolder();
    }

    @Override
    public void onRestart() {
        // Run the default commands.
        super.onRestart();

        // Update the list view if returning from the bookmarks database view activity.
        if (restartFromBookmarksDatabaseViewActivity) {
            // Load the current folder in the list view.
            loadFolder();

            // Reset `restartFromBookmarksDatabaseViewActivity`.
            restartFromBookmarksDatabaseViewActivity = false;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        // Run the default commands.
        super.onSaveInstanceState(savedInstanceState);

        // Get the array of the checked items.
        SparseBooleanArray checkedBookmarksSparseBooleanArray = bookmarksListView.getCheckedItemPositions();

        // Create a checked items array list.
        ArrayList<Integer> checkedBookmarksArrayList = new ArrayList<>();

        // Add each checked bookmark position to the array list.
        for (int i = 0; i < checkedBookmarksSparseBooleanArray.size(); i++) {
            // Check to see if the bookmark is currently checked.  Bookmarks that have previously been checked but currently aren't will be populated in the sparse boolean array, but will return false.
            if (checkedBookmarksSparseBooleanArray.valueAt(i)) {
                // Add the bookmark position to the checked bookmarks array list.
                checkedBookmarksArrayList.add(checkedBookmarksSparseBooleanArray.keyAt(i));
            }
        }

        // Store the variables in the saved instance state.
        savedInstanceState.putString(CURRENT_FOLDER, currentFolder);
        savedInstanceState.putIntegerArrayList(CHECKED_BOOKMARKS_ARRAY_LIST, checkedBookmarksArrayList);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu.
        getMenuInflater().inflate(R.menu.bookmarks_options_menu, menu);

        // Success.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        // Get a handle for the menu item ID.
        int menuItemId = menuItem.getItemId();

        // Run the command according to the selected option.
        if (menuItemId == android.R.id.home) {  // Home.  The home arrow is identified as `android.R.id.home`, not just `R.id.home`.
            if (currentFolder.isEmpty()) {  // Currently in the home folder.
                // Prepare to finish the activity.
                prepareFinish();
            } else {  // Currently in a subfolder.
                // Place the former parent folder in `currentFolder`.
                currentFolder = bookmarksDatabaseHelper.getParentFolderName(currentFolder);

                // Load the new folder.
                loadFolder();
            }
        } else if (menuItemId == R.id.options_menu_select_all_bookmarks) {  // Select all.
            // Get the total number of bookmarks.
            int numberOfBookmarks = bookmarksListView.getCount();

            // Select them all.
            for (int i = 0; i < numberOfBookmarks; i++) {
                bookmarksListView.setItemChecked(i, true);
            }
        } else if (menuItemId == R.id.bookmarks_database_view) {
            // Close the contextual action bar if it is displayed.  This can happen if the bottom app bar is enabled.
            if (contextualActionMode != null) {
                contextualActionMode.finish();
            }

            // Create an intent to launch the bookmarks database view activity.
            Intent bookmarksDatabaseViewIntent = new Intent(this, BookmarksDatabaseViewActivity.class);

            // Include the favorite icon byte array to the intent.
            bookmarksDatabaseViewIntent.putExtra("favorite_icon_byte_array", favoriteIconByteArray);

            // Make it so.
            startActivity(bookmarksDatabaseViewIntent);
        }
        return true;
    }

    @Override
    public void onCreateBookmark(DialogFragment dialogFragment, Bitmap favoriteIconBitmap) {
        // Get the alert dialog from the fragment.
        Dialog dialog = dialogFragment.getDialog();

        // Remove the incorrect lint warning below that the dialog might be null.
        assert dialog != null;

        // Get the views from the dialog fragment.
        EditText createBookmarkNameEditText = dialog.findViewById(R.id.create_bookmark_name_edittext);
        EditText createBookmarkUrlEditText = dialog.findViewById(R.id.create_bookmark_url_edittext);

        // Extract the strings from the edit texts.
        String bookmarkNameString = createBookmarkNameEditText.getText().toString();
        String bookmarkUrlString = createBookmarkUrlEditText.getText().toString();

        // Create a favorite icon byte array output stream.
        ByteArrayOutputStream favoriteIconByteArrayOutputStream = new ByteArrayOutputStream();

        // Convert the favorite icon bitmap to a byte array.  `0` is for lossless compression (the only option for a PNG).
        favoriteIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, favoriteIconByteArrayOutputStream);

        // Convert the favorite icon byte array stream to a byte array.
        byte[] favoriteIconByteArray = favoriteIconByteArrayOutputStream.toByteArray();

        // Display the new bookmark below the current items in the (0 indexed) list.
        int newBookmarkDisplayOrder = bookmarksListView.getCount();

        // Create the bookmark.
        bookmarksDatabaseHelper.createBookmark(bookmarkNameString, bookmarkUrlString, currentFolder, newBookmarkDisplayOrder, favoriteIconByteArray);

        // Update the bookmarks cursor with the current contents of this folder.
        bookmarksCursor = bookmarksDatabaseHelper.getBookmarksByDisplayOrder(currentFolder);

        // Update the `ListView`.
        bookmarksCursorAdapter.changeCursor(bookmarksCursor);

        // Scroll to the new bookmark.
        bookmarksListView.setSelection(newBookmarkDisplayOrder);
    }

    @Override
    public void onCreateBookmarkFolder(DialogFragment dialogFragment, @NonNull Bitmap favoriteIconBitmap) {
        // Get the dialog from the dialog fragment.
        Dialog dialog = dialogFragment.getDialog();

        // Remove the incorrect lint warning below that the dialog might be null.
        assert dialog != null;

        // Get handles for the views in the dialog fragment.
        EditText folderNameEditText = dialog.findViewById(R.id.folder_name_edittext);
        RadioButton defaultIconRadioButton = dialog.findViewById(R.id.default_icon_radiobutton);
        ImageView defaultIconImageView = dialog.findViewById(R.id.default_icon_imageview);

        // Get new folder name string.
        String folderNameString = folderNameEditText.getText().toString();

        // Create a folder icon bitmap.
        Bitmap folderIconBitmap;

        // Set the folder icon bitmap according to the dialog.
        if (defaultIconRadioButton.isChecked()) {  // Use the default folder icon.
            // Get the default folder icon drawable.
            Drawable folderIconDrawable = defaultIconImageView.getDrawable();

            // Convert the folder icon drawable to a bitmap drawable.
            BitmapDrawable folderIconBitmapDrawable = (BitmapDrawable) folderIconDrawable;

            // Convert the folder icon bitmap drawable to a bitmap.
            folderIconBitmap = folderIconBitmapDrawable.getBitmap();
        } else {  // Use the WebView favorite icon.
            // Copy the favorite icon bitmap to the folder icon bitmap.
            folderIconBitmap = favoriteIconBitmap;
        }

        // Create a folder icon byte array output stream.
        ByteArrayOutputStream folderIconByteArrayOutputStream = new ByteArrayOutputStream();

        // Convert the folder icon bitmap to a byte array.  `0` is for lossless compression (the only option for a PNG).
        folderIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, folderIconByteArrayOutputStream);

        // Convert the folder icon byte array stream to a byte array.
        byte[] folderIconByteArray = folderIconByteArrayOutputStream.toByteArray();

        // Move all the bookmarks down one in the display order.
        for (int i = 0; i < bookmarksListView.getCount(); i++) {
            int databaseId = (int) bookmarksListView.getItemIdAtPosition(i);
            bookmarksDatabaseHelper.updateDisplayOrder(databaseId, i + 1);
        }

        // Create the folder, which will be placed at the top of the `ListView`.
        bookmarksDatabaseHelper.createFolder(folderNameString, currentFolder, folderIconByteArray);

        // Update the bookmarks cursor with the current contents of this folder.
        bookmarksCursor = bookmarksDatabaseHelper.getBookmarksByDisplayOrder(currentFolder);

        // Update the list view.
        bookmarksCursorAdapter.changeCursor(bookmarksCursor);

        // Scroll to the new folder.
        bookmarksListView.setSelection(0);
    }

    @Override
    public void onSaveBookmark(DialogFragment dialogFragment, int selectedBookmarkDatabaseId, @NonNull Bitmap favoriteIconBitmap) {
        // Get the dialog from the dialog fragment.
        Dialog dialog = dialogFragment.getDialog();

        // Remove the incorrect lint warning below that the dialog might be null.
        assert dialog != null;

        // Get handles for the views from the dialog fragment.
        EditText bookmarkNameEditText = dialog.findViewById(R.id.bookmark_name_edittext);
        EditText bookmarkUrlEditText = dialog.findViewById(R.id.bookmark_url_edittext);
        RadioButton currentIconRadioButton = dialog.findViewById(R.id.current_icon_radiobutton);

        // Store the bookmark strings.
        String bookmarkNameString = bookmarkNameEditText.getText().toString();
        String bookmarkUrlString = bookmarkUrlEditText.getText().toString();

        // Update the bookmark.
        if (currentIconRadioButton.isChecked()) {  // Update the bookmark without changing the favorite icon.
            bookmarksDatabaseHelper.updateBookmark(selectedBookmarkDatabaseId, bookmarkNameString, bookmarkUrlString);
        } else {  // Update the bookmark using the WebView favorite icon.
            // Create a favorite icon byte array output stream.
            ByteArrayOutputStream newFavoriteIconByteArrayOutputStream = new ByteArrayOutputStream();

            // Convert the favorite icon bitmap to a byte array.  `0` is for lossless compression (the only option for a PNG).
            favoriteIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, newFavoriteIconByteArrayOutputStream);

            // Convert the favorite icon byte array stream to a byte array.
            byte[] newFavoriteIconByteArray = newFavoriteIconByteArrayOutputStream.toByteArray();

            //  Update the bookmark and the favorite icon.
            bookmarksDatabaseHelper.updateBookmark(selectedBookmarkDatabaseId, bookmarkNameString, bookmarkUrlString, newFavoriteIconByteArray);
        }

        // Check to see if the contextual action mode has been created.
        if (contextualActionMode != null) {
            // Close the contextual action mode if it is open.
            contextualActionMode.finish();
        }

        // Update the bookmarks cursor with the contents of the current folder.
        bookmarksCursor = bookmarksDatabaseHelper.getBookmarksByDisplayOrder(currentFolder);

        // Update the `ListView`.
        bookmarksCursorAdapter.changeCursor(bookmarksCursor);
    }

    @Override
    public void onSaveBookmarkFolder(DialogFragment dialogFragment, int selectedFolderDatabaseId, @NonNull Bitmap favoriteIconBitmap) {
        // Get the dialog from the dialog fragment.
        Dialog dialog = dialogFragment.getDialog();

        // Remove the incorrect lint warning below that the dialog might be null.
        assert dialog != null;

        // Get handles for the views from the dialog fragment.
        RadioButton currentFolderIconRadioButton = dialog.findViewById(R.id.current_icon_radiobutton);
        RadioButton defaultFolderIconRadioButton = dialog.findViewById(R.id.default_icon_radiobutton);
        ImageView defaultFolderIconImageView = dialog.findViewById(R.id.default_icon_imageview);
        EditText editFolderNameEditText = dialog.findViewById(R.id.folder_name_edittext);

        // Get the new folder name.
        String newFolderNameString = editFolderNameEditText.getText().toString();

        // Check if the favorite icon has changed.
        if (currentFolderIconRadioButton.isChecked()) {  // Only the name has changed.
            // Update the name in the database.
            bookmarksDatabaseHelper.updateFolder(selectedFolderDatabaseId, oldFolderNameString, newFolderNameString);
        } else if (!currentFolderIconRadioButton.isChecked() && newFolderNameString.equals(oldFolderNameString)) {  // Only the icon has changed.
            // Create the new folder icon Bitmap.
            Bitmap folderIconBitmap;

            // Populate the new folder icon bitmap.
            if (defaultFolderIconRadioButton.isChecked()) {
                // Get the default folder icon drawable.
                Drawable folderIconDrawable = defaultFolderIconImageView.getDrawable();

                // Convert the folder icon drawable to a bitmap drawable.
                BitmapDrawable folderIconBitmapDrawable = (BitmapDrawable) folderIconDrawable;

                // Convert the folder icon bitmap drawable to a bitmap.
                folderIconBitmap = folderIconBitmapDrawable.getBitmap();
            } else {  // Use the WebView favorite icon.
                // Copy the favorite icon bitmap to the folder icon bitmap.
                folderIconBitmap = favoriteIconBitmap;
            }

            // Create a folder icon byte array output stream.
            ByteArrayOutputStream newFolderIconByteArrayOutputStream = new ByteArrayOutputStream();

            // Convert the folder icon bitmap to a byte array.  `0` is for lossless compression (the only option for a PNG).
            folderIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, newFolderIconByteArrayOutputStream);

            // Convert the folder icon byte array stream to a byte array.
            byte[] newFolderIconByteArray = newFolderIconByteArrayOutputStream.toByteArray();

            // Update the folder icon in the database.
            bookmarksDatabaseHelper.updateFolder(selectedFolderDatabaseId, newFolderIconByteArray);
        } else {  // The folder icon and the name have changed.
            // Instantiate the new folder icon `Bitmap`.
            Bitmap folderIconBitmap;

            // Populate the new folder icon bitmap.
            if (defaultFolderIconRadioButton.isChecked()) {
                // Get the default folder icon drawable.
                Drawable folderIconDrawable = defaultFolderIconImageView.getDrawable();

                // Convert the folder icon drawable to a bitmap drawable.
                BitmapDrawable folderIconBitmapDrawable = (BitmapDrawable) folderIconDrawable;

                // Convert the folder icon bitmap drawable to a bitmap.
                folderIconBitmap = folderIconBitmapDrawable.getBitmap();
            } else {  // Use the WebView favorite icon.
                // Copy the favorite icon bitmap to the folder icon bitmap.
                folderIconBitmap = favoriteIconBitmap;
            }

            // Create a folder icon byte array output stream.
            ByteArrayOutputStream newFolderIconByteArrayOutputStream = new ByteArrayOutputStream();

            // Convert the folder icon bitmap to a byte array.  `0` is for lossless compression (the only option for a PNG).
            folderIconBitmap.compress(Bitmap.CompressFormat.PNG, 0, newFolderIconByteArrayOutputStream);

            // Convert the folder icon byte array stream to a byte array.
            byte[] newFolderIconByteArray = newFolderIconByteArrayOutputStream.toByteArray();

            // Update the folder name and icon in the database.
            bookmarksDatabaseHelper.updateFolder(selectedFolderDatabaseId, oldFolderNameString, newFolderNameString, newFolderIconByteArray);
        }

        // Update the bookmarks cursor with the current contents of this folder.
        bookmarksCursor = bookmarksDatabaseHelper.getBookmarksByDisplayOrder(currentFolder);

        // Update the `ListView`.
        bookmarksCursorAdapter.changeCursor(bookmarksCursor);

        // Close the contextual action mode.
        contextualActionMode.finish();
    }

    @Override
    public void onMoveToFolder(DialogFragment dialogFragment) {
        // Get the dialog from the dialog fragment.
        Dialog dialog = dialogFragment.getDialog();

        // Remove the incorrect lint warning below that the dialog might be null.
        assert dialog != null;

        // Get a handle for the list view from the dialog.
        ListView folderListView = dialog.findViewById(R.id.move_to_folder_listview);

        // Store a long array of the selected folders.
        long[] newFolderLongArray = folderListView.getCheckedItemIds();

        // Get the new folder database ID.  Only one folder will be selected.
        int newFolderDatabaseId = (int) newFolderLongArray[0];

        // Instantiate `newFolderName`.
        String newFolderName;

        // Set the new folder name.
        if (newFolderDatabaseId == 0) {
            // The new folder is the home folder, represented as `""` in the database.
            newFolderName = "";
        } else {
            // Get the new folder name from the database.
            newFolderName = bookmarksDatabaseHelper.getFolderName(newFolderDatabaseId);
        }

        // Get a long array with the the database ID of the selected bookmarks.
        long[] selectedBookmarksLongArray = bookmarksListView.getCheckedItemIds();

        // Move each of the selected bookmarks to the new folder.
        for (long databaseIdLong : selectedBookmarksLongArray) {
            // Get `databaseIdInt` for each selected bookmark.
            int databaseIdInt = (int) databaseIdLong;

            // Move the selected bookmark to the new folder.
            bookmarksDatabaseHelper.moveToFolder(databaseIdInt, newFolderName);
        }

        // Update the bookmarks cursor with the current contents of this folder.
        bookmarksCursor = bookmarksDatabaseHelper.getBookmarksByDisplayOrder(currentFolder);

        // Update the `ListView`.
        bookmarksCursorAdapter.changeCursor(bookmarksCursor);

        // Close the contextual app bar.
        contextualActionMode.finish();
    }

    private int countBookmarkFolderContents(int databaseId) {
        // Initialize the bookmark counter.
        int bookmarkCounter = 0;

        // Get the name of the folder.
        String folderName = bookmarksDatabaseHelper.getFolderName(databaseId);

        // Get the contents of the folder.
        Cursor folderCursor = bookmarksDatabaseHelper.getBookmarkIds(folderName);

        // Count each of the bookmarks in the folder.
        for (int i = 0; i < folderCursor.getCount(); i++) {
            // Move the folder cursor to the current row.
            folderCursor.moveToPosition(i);

            // Get the database ID of the item.
            int itemDatabaseId = folderCursor.getInt(folderCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.ID));

            // If this is a folder, recursively count the contents first.
            if (bookmarksDatabaseHelper.isFolder(itemDatabaseId)) {
                // Add the bookmarks from the folder to the running total.
                bookmarkCounter = bookmarkCounter + countBookmarkFolderContents(itemDatabaseId);
            }

            // Add the bookmark to the running total.
            bookmarkCounter++;
        }

        // Return the bookmark counter.
        return bookmarkCounter;
    }

    private void deleteBookmarkFolderContents(int databaseId) {
        // Get the name of the folder.
        String folderName = bookmarksDatabaseHelper.getFolderName(databaseId);

        // Get the contents of the folder.
        Cursor folderCursor = bookmarksDatabaseHelper.getBookmarkIds(folderName);

        // Delete each of the bookmarks in the folder.
        for (int i = 0; i < folderCursor.getCount(); i++) {
            // Move the folder cursor to the current row.
            folderCursor.moveToPosition(i);

            // Get the database ID of the item.
            int itemDatabaseId = folderCursor.getInt(folderCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.ID));

            // If this is a folder, recursively delete the contents first.
            if (bookmarksDatabaseHelper.isFolder(itemDatabaseId)) {
                deleteBookmarkFolderContents(itemDatabaseId);
            }

            // Delete the bookmark.
            bookmarksDatabaseHelper.deleteBookmark(itemDatabaseId);
        }
    }

    private void prepareFinish() {
        // Check to see if a snackbar is currently displayed.  If so, it must be closed before exiting so that a pending delete is completed before reloading the list view in the bookmarks drawer.
        if ((bookmarksDeletedSnackbar != null) && bookmarksDeletedSnackbar.isShown()) {  // Close the bookmarks deleted snackbar before going home.
            // Set the close flag.
            closeActivityAfterDismissingSnackbar = true;

            // Dismiss the snackbar.
            bookmarksDeletedSnackbar.dismiss();
        } else {  // Go home immediately.
            // Update the bookmarks folder for the bookmarks drawer in the main WebView activity.
            MainWebViewActivity.currentBookmarksFolder = currentFolder;

            // Close the bookmarks drawer and reload the bookmarks ListView when returning to the main WebView activity.
            MainWebViewActivity.restartFromBookmarksActivity = true;

            // Exit the bookmarks activity.
            finish();
        }
    }

    private void updateMoveIcons() {
        // Get a long array of the selected bookmarks.
        long[] selectedBookmarksLongArray = bookmarksListView.getCheckedItemIds();

        // Get the database IDs for the first, last, and selected bookmarks.
        int selectedBookmarkDatabaseId = (int) selectedBookmarksLongArray[0];
        int firstBookmarkDatabaseId = (int) bookmarksListView.getItemIdAtPosition(0);
        // bookmarksListView is 0 indexed.
        int lastBookmarkDatabaseId = (int) bookmarksListView.getItemIdAtPosition(bookmarksListView.getCount() - 1);

        // Update the move bookmark up menu item.
        if (selectedBookmarkDatabaseId == firstBookmarkDatabaseId) {  // The selected bookmark is in the first position.
            // Disable the move bookmark up menu item.
            moveBookmarkUpMenuItem.setEnabled(false);

            //  Set the icon.
            moveBookmarkUpMenuItem.setIcon(R.drawable.move_up_disabled);
        } else {  // The selected bookmark is not in the first position.
            // Enable the move bookmark up menu item.
            moveBookmarkUpMenuItem.setEnabled(true);

            // Set the icon according to the theme.
            moveBookmarkUpMenuItem.setIcon(R.drawable.move_up_enabled);
        }

        // Update the move bookmark down menu item.
        if (selectedBookmarkDatabaseId == lastBookmarkDatabaseId) {  // The selected bookmark is in the last position.
            // Disable the move bookmark down menu item.
            moveBookmarkDownMenuItem.setEnabled(false);

            // Set the icon.
            moveBookmarkDownMenuItem.setIcon(R.drawable.move_down_disabled);
        } else {  // The selected bookmark is not in the last position.
            // Enable the move bookmark down menu item.
            moveBookmarkDownMenuItem.setEnabled(true);

            // Set the icon.
            moveBookmarkDownMenuItem.setIcon(R.drawable.move_down_enabled);
        }
    }

    private void scrollBookmarks(int selectedBookmarkPosition) {
        // Get the first and last visible bookmark positions.
        int firstVisibleBookmarkPosition = bookmarksListView.getFirstVisiblePosition();
        int lastVisibleBookmarkPosition = bookmarksListView.getLastVisiblePosition();

        // Calculate the number of bookmarks per screen.
        int numberOfBookmarksPerScreen = lastVisibleBookmarkPosition - firstVisibleBookmarkPosition;

        // Scroll with the moved bookmark if necessary.
        if (selectedBookmarkPosition <= firstVisibleBookmarkPosition) {  // The selected bookmark position is at or above the top of the screen.
            // Scroll to the selected bookmark position.
            bookmarksListView.setSelection(selectedBookmarkPosition);
        } else if (selectedBookmarkPosition >= (lastVisibleBookmarkPosition - 1)) {  // The selected bookmark is at or below the bottom of the screen.
            // The `-1` handles partial bookmarks displayed at the bottom of the list view.  This command scrolls to display the selected bookmark at the bottom of the screen.
            // `+1` assures that the entire bookmark will be displayed in situations where only a partial bookmark fits at the bottom of the list view.
            bookmarksListView.setSelection(selectedBookmarkPosition - numberOfBookmarksPerScreen + 1);
        }
    }

    private void loadFolder() {
        // Update bookmarks cursor with the contents of the bookmarks database for the current folder.
        bookmarksCursor = bookmarksDatabaseHelper.getBookmarksByDisplayOrder(currentFolder);

        // Setup a `CursorAdapter`.  `this` specifies the `Context`.  `false` disables `autoRequery`.
        bookmarksCursorAdapter = new CursorAdapter(this, bookmarksCursor, false) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                // Inflate the individual item layout.  `false` does not attach it to the root.
                return getLayoutInflater().inflate(R.layout.bookmarks_activity_item_linearlayout, parent, false);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                // Get handles for the views.
                ImageView bookmarkFavoriteIcon = view.findViewById(R.id.bookmark_favorite_icon);
                TextView bookmarkNameTextView = view.findViewById(R.id.bookmark_name);

                // Get the favorite icon byte array from the `Cursor`.
                byte[] favoriteIconByteArray = cursor.getBlob(cursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.FAVORITE_ICON));

                // Convert the byte array to a `Bitmap` beginning at the first byte and ending at the last.
                Bitmap favoriteIconBitmap = BitmapFactory.decodeByteArray(favoriteIconByteArray, 0, favoriteIconByteArray.length);

                // Display the bitmap in `bookmarkFavoriteIcon`.
                bookmarkFavoriteIcon.setImageBitmap(favoriteIconBitmap);

                // Get the bookmark name from the cursor and display it in `bookmarkNameTextView`.
                String bookmarkNameString = cursor.getString(cursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.BOOKMARK_NAME));
                bookmarkNameTextView.setText(bookmarkNameString);

                // Make the font bold for folders.
                if (cursor.getInt(cursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.IS_FOLDER)) == 1) {
                    bookmarkNameTextView.setTypeface(Typeface.DEFAULT_BOLD);
                } else {  // Reset the font to default for normal bookmarks.
                    bookmarkNameTextView.setTypeface(Typeface.DEFAULT);
                }
            }
        };

        // Populate the list view with the adapter.
        bookmarksListView.setAdapter(bookmarksCursorAdapter);

        // Set the `AppBar` title.
        if (currentFolder.isEmpty()) {
            appBar.setTitle(R.string.bookmarks);
        } else {
            appBar.setTitle(currentFolder);
        }
    }

    @Override
    public void onDestroy() {
        // Close the bookmarks cursor and database.
        bookmarksCursor.close();
        bookmarksDatabaseHelper.close();

        // Run the default commands.
        super.onDestroy();
    }
}
