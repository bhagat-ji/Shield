/*
 * Copyright © 2016-2022 Soren Stoutner <soren@stoutner.com>.
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

package com.stoutner.privacybrowser.helpers

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Define the class constants.
private const val SCHEMA_VERSION = 1

class BookmarksDatabaseHelper(context: Context) : SQLiteOpenHelper(context, BOOKMARKS_DATABASE, null, SCHEMA_VERSION) {
    // Define the public companion object constants.  These can be moved to public class constants once the entire project has migrated to Kotlin.
    companion object {
        // Define the public database constants.
        const val BOOKMARKS_DATABASE = "bookmarks.db"
        const val BOOKMARKS_TABLE = "bookmarks"

        // Define the public schema constants.
        const val ID = "_id"
        const val BOOKMARK_NAME = "bookmarkname"
        const val BOOKMARK_URL = "bookmarkurl"
        const val PARENT_FOLDER = "parentfolder"
        const val DISPLAY_ORDER = "displayorder"
        const val IS_FOLDER = "isfolder"
        const val FAVORITE_ICON = "favoriteicon"

        // Define the public table creation constant.
        const val CREATE_BOOKMARKS_TABLE = "CREATE TABLE $BOOKMARKS_TABLE (" +
                "$ID INTEGER PRIMARY KEY, " +
                "$BOOKMARK_NAME TEXT, " +
                "$BOOKMARK_URL TEXT, " +
                "$PARENT_FOLDER TEXT, " +
                "$DISPLAY_ORDER INTEGER, " +
                "$IS_FOLDER BOOLEAN, " +
                "$FAVORITE_ICON BLOB)"
    }

    override fun onCreate(bookmarksDatabase: SQLiteDatabase) {
        // Create the bookmarks table.
        bookmarksDatabase.execSQL(CREATE_BOOKMARKS_TABLE)
    }

    override fun onUpgrade(bookmarksDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Code for upgrading the database will be added here when the schema version > 1.
    }

    // Create a bookmark.
    fun createBookmark(bookmarkName: String, bookmarkURL: String, parentFolder: String, displayOrder: Int, favoriteIcon: ByteArray) {
        // Store the bookmark data in a content values.
        val bookmarkContentValues = ContentValues()

        // The ID is created automatically.
        bookmarkContentValues.put(BOOKMARK_NAME, bookmarkName)
        bookmarkContentValues.put(BOOKMARK_URL, bookmarkURL)
        bookmarkContentValues.put(PARENT_FOLDER, parentFolder)
        bookmarkContentValues.put(DISPLAY_ORDER, displayOrder)
        bookmarkContentValues.put(IS_FOLDER, false)
        bookmarkContentValues.put(FAVORITE_ICON, favoriteIcon)

        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Insert a new row.
        bookmarksDatabase.insert(BOOKMARKS_TABLE, null, bookmarkContentValues)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Create a bookmark from content values.
    fun createBookmark(contentValues: ContentValues) {
        // Get a writable database.
        val bookmarksDatabase = this.writableDatabase

        // Insert a new row.
        bookmarksDatabase.insert(BOOKMARKS_TABLE, null, contentValues)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Create a folder.
    fun createFolder(folderName: String, parentFolder: String, favoriteIcon: ByteArray) {
        // Store the bookmark folder data in a content values.
        val bookmarkFolderContentValues = ContentValues()

        // The ID is created automatically.  Folders are always created at the top of the list.
        bookmarkFolderContentValues.put(BOOKMARK_NAME, folderName)
        bookmarkFolderContentValues.put(PARENT_FOLDER, parentFolder)
        bookmarkFolderContentValues.put(DISPLAY_ORDER, 0)
        bookmarkFolderContentValues.put(IS_FOLDER, true)
        bookmarkFolderContentValues.put(FAVORITE_ICON, favoriteIcon)

        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Insert the new folder.
        bookmarksDatabase.insert(BOOKMARKS_TABLE, null, bookmarkFolderContentValues)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Get a cursor for the bookmark with the specified database ID.
    fun getBookmark(databaseId: Int): Cursor {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Return the cursor for the database ID.  The cursor can't be closed because it is used in the parent activity.
        return bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $ID = $databaseId", null)
    }

    // Get the folder name for the specified database ID.
    fun getFolderName(databaseId: Int): String {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Get the cursor for the folder with the specified database ID.
        val folderCursor = bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $ID = $databaseId", null)

        // Move to the first record.
        folderCursor.moveToFirst()

        // Get the folder name.
        val folderName = folderCursor.getString(folderCursor.getColumnIndexOrThrow(BOOKMARK_NAME))

        // Close the cursor and the database handle.
        folderCursor.close()
        bookmarksDatabase.close()

        // Return the folder name.
        return folderName
    }

    // Get the database ID for the specified folder name.
    fun getFolderDatabaseId(folderName: String): Int {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // SQL escape the folder name.
        val sqlEscapedFolderName = DatabaseUtils.sqlEscapeString(folderName)

        // Get the cursor for the folder with the specified name.
        val folderCursor = bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $BOOKMARK_NAME = $sqlEscapedFolderName AND $IS_FOLDER = 1", null)

        // Move to the first record.
        folderCursor.moveToFirst()

        // Get the database ID.
        val databaseId = folderCursor.getInt(folderCursor.getColumnIndexOrThrow(ID))

        // Close the cursor and the database handle.
        folderCursor.close()
        bookmarksDatabase.close()

        // Return the database ID.
        return databaseId
    }

    // Get a cursor for the specified folder name.
    fun getFolder(folderName: String): Cursor {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // SQL escape the folder name.
        val sqlEscapedFolderName = DatabaseUtils.sqlEscapeString(folderName)

        // Return the cursor for the specified folder.  The cursor can't be closed because it is used in the parent activity.
        return bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $BOOKMARK_NAME = $sqlEscapedFolderName AND $IS_FOLDER = 1", null)
    }

    // Get a cursor of all the folders except those specified.
    fun getFoldersExcept(exceptFolders: String): Cursor {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Return the cursor of all folders except those specified.  Each individual folder in the list has already been SQL escaped.  The cursor can't be closed because it is used in the parent activity.
        return bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $IS_FOLDER = 1 AND $BOOKMARK_NAME NOT IN ($exceptFolders) ORDER BY $BOOKMARK_NAME ASC", null)
    }

    // Get a cursor with all the subfolders of the specified folder.
    fun getSubfolders(currentFolder: String): Cursor {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // SQL escape the current folder.
        val sqlEscapedCurrentFolder = DatabaseUtils.sqlEscapeString(currentFolder)

        // Return the cursor with the subfolders.  The cursor can't be closed because it is used in the parent activity.
        return bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $PARENT_FOLDER = $sqlEscapedCurrentFolder AND $IS_FOLDER = 1", null)
    }

    // Get the name of the parent folder.
    fun getParentFolderName(currentFolder: String): String {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // SQL escape the current folder.
        val sqlEscapedCurrentFolder = DatabaseUtils.sqlEscapeString(currentFolder)

        // Get a cursor for the current folder.
        val bookmarkCursor = bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $IS_FOLDER = 1 AND $BOOKMARK_NAME = $sqlEscapedCurrentFolder", null)

        // Move to the first record.
        bookmarkCursor.moveToFirst()

        // Store the name of the parent folder.
        val parentFolder = bookmarkCursor.getString(bookmarkCursor.getColumnIndexOrThrow(PARENT_FOLDER))

        // Close the cursor and the database.
        bookmarkCursor.close()
        bookmarksDatabase.close()

        // Return the parent folder string.
        return parentFolder
    }

    // Get the name of the parent folder.
    fun getParentFolderName(databaseId: Int): String {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Get a cursor for the specified database ID.
        val bookmarkCursor = bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $ID = $databaseId", null)

        // Move to the first record.
        bookmarkCursor.moveToFirst()

        // Store the name of the parent folder.
        val parentFolder = bookmarkCursor.getString(bookmarkCursor.getColumnIndexOrThrow(PARENT_FOLDER))

        // Close the cursor and the database.
        bookmarkCursor.close()
        bookmarksDatabase.close()

        // Return the parent folder string.
        return parentFolder
    }

    // Get a cursor of all the folders.
    val allFolders: Cursor
        get() {
            // Get a readable database handle.
            val bookmarksDatabase = this.readableDatabase

            // Return the cursor with the all the folders.  The cursor cannot be closed because it is used in the parent activity.
            return bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $IS_FOLDER = 1 ORDER BY $BOOKMARK_NAME ASC", null)
        }

    // Get a cursor for all bookmarks and folders.
    val allBookmarks: Cursor
        get() {
            // Get a readable database handle.
            val bookmarksDatabase = this.readableDatabase

            // Return a cursor with the entire contents of the bookmarks table.  The cursor cannot be closed because it is used in the parent activity.
            return bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE", null)
        }

    // Get a cursor for all bookmarks and folders ordered by display order.
    val allBookmarksByDisplayOrder: Cursor
        get() {
            // Get a readable database handle.
            val bookmarksDatabase = this.readableDatabase

            // Return a cursor with the entire contents of the bookmarks table ordered by the display order.  The cursor cannot be closed because it is used in the parent activity.
            return bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE ORDER BY $DISPLAY_ORDER ASC", null)
        }

    // Get a cursor for all bookmarks and folders except those with the specified IDs.
    fun getAllBookmarksExcept(exceptIdLongArray: LongArray): Cursor {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Prepare a string builder to contain the comma-separated list of IDs not to get.
        val idsNotToGetStringBuilder = StringBuilder()

        // Extract the array of IDs not to get to the string builder.
        for (databaseIdLong in exceptIdLongArray) {
            // Check to see if there is already a number in the builder.
            if (idsNotToGetStringBuilder.isNotEmpty()) {
                // This is not the first number, so place a `,` before the new number.
                idsNotToGetStringBuilder.append(",")
            }

            // Add the new number to the builder.
            idsNotToGetStringBuilder.append(databaseIdLong)
        }

        // Return a cursor with all the bookmarks except those specified.  The cursor cannot be closed because it is used in the parent activity.
        return bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $ID NOT IN ($idsNotToGetStringBuilder)", null)
    }

    // Get a cursor for all bookmarks and folders by display order except those with the specified IDs.
    fun getAllBookmarksByDisplayOrderExcept(exceptIdLongArray: LongArray): Cursor {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Prepare a string builder to contain the comma-separated list of IDs not to get.
        val idsNotToGetStringBuilder = StringBuilder()

        // Extract the array of IDs not to get to the string builder.
        for (databaseIdLong in exceptIdLongArray) {
            // Check to see if there is already a number in the builder.
            if (idsNotToGetStringBuilder.isNotEmpty()) {
                // This is not the first number, so place a `,` before the new number.
                idsNotToGetStringBuilder.append(",")
            }

            // Add the new number to the builder.
            idsNotToGetStringBuilder.append(databaseIdLong)
        }

        // Return a cursor with all the bookmarks except those specified ordered by display order.  The cursor cannot be closed because it is used in the parent activity.
        return bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $ID NOT IN ($idsNotToGetStringBuilder) ORDER BY $DISPLAY_ORDER ASC", null)
    }

    // Get a cursor for bookmarks and folders in the specified folder.
    fun getBookmarks(folderName: String): Cursor {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // SQL escape the folder name.
        val sqlEscapedFolderName = DatabaseUtils.sqlEscapeString(folderName)

        // Return a cursor with all the bookmarks in a specified folder.  The cursor cannot be closed because it is used in the parent activity.
        return bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $PARENT_FOLDER = $sqlEscapedFolderName", null)
    }

    // Get a cursor for bookmarks and folders in the specified folder ordered by display order.
    fun getBookmarksByDisplayOrder(folderName: String): Cursor {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // SQL escape the folder name.
        val sqlEscapedFolderName = DatabaseUtils.sqlEscapeString(folderName)

        // Return a cursor with all the bookmarks in the specified folder ordered by display order.  The cursor cannot be closed because it is used in the parent activity.
        return bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $PARENT_FOLDER = $sqlEscapedFolderName ORDER BY $DISPLAY_ORDER ASC", null)
    }

    // Get a cursor with just database ID of bookmarks and folders in the specified folder.  This is useful for deleting folders with bookmarks that have favorite icons too large to fit in a cursor.
    fun getBookmarkIds(folderName: String): Cursor {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // SQL escape the folder name.
        val sqlEscapedFolderName = DatabaseUtils.sqlEscapeString(folderName)

        // Return a cursor with all the database IDs.  The cursor cannot be closed because it is used in the parent activity.
        return bookmarksDatabase.rawQuery("SELECT $ID FROM $BOOKMARKS_TABLE WHERE $PARENT_FOLDER = $sqlEscapedFolderName", null)
    }

    // Get a cursor for bookmarks and folders in the specified folder except those with the specified IDs.
    fun getBookmarksExcept(exceptIdLongArray: LongArray, folderName: String): Cursor {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Prepare a string builder to contain the comma-separated list of IDs not to get.
        val idsNotToGetStringBuilder = StringBuilder()

        // Extract the array of IDs not to get to the string builder.
        for (databaseIdLong in exceptIdLongArray) {
            // Check to see if there is already a number in the builder.
            if (idsNotToGetStringBuilder.isNotEmpty()) {
                // This is not the first number, so place a `,` before the new number.
                idsNotToGetStringBuilder.append(",")
            }

            // Add the new number to the builder.
            idsNotToGetStringBuilder.append(databaseIdLong)
        }

        // SQL escape the folder name.
        val sqlEscapedFolderName = DatabaseUtils.sqlEscapeString(folderName)

        // Return a cursor with all the bookmarks in the specified folder except for those database IDs specified.  The cursor cannot be closed because it is used in the parent activity.
        return bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $PARENT_FOLDER = $sqlEscapedFolderName AND $ID NOT IN ($idsNotToGetStringBuilder)", null)
    }

    // Get a cursor for bookmarks and folders in the specified folder by display order except those with the specified IDs.
    fun getBookmarksByDisplayOrderExcept(exceptIdLongArray: LongArray, folderName: String): Cursor {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Prepare a string builder to contain the comma-separated list of IDs not to get.
        val idsNotToGetStringBuilder = StringBuilder()

        // Extract the array of IDs not to get to the string builder.
        for (databaseIdLong in exceptIdLongArray) {
            // Check to see if there is already a number in the builder.
            if (idsNotToGetStringBuilder.isNotEmpty()) {
                // This is not the first number, so place a `,` before the new number.
                idsNotToGetStringBuilder.append(",")
            }

            // Add the new number to the builder.
            idsNotToGetStringBuilder.append(databaseIdLong)
        }

        // SQL escape the folder name.
        val sqlEscapedFolderName = DatabaseUtils.sqlEscapeString(folderName)

        // Return a cursor with all the bookmarks in the specified folder except for those database IDs specified ordered by display order.
        // The cursor cannot be closed because it will be used in the parent activity.
        return bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $PARENT_FOLDER = $sqlEscapedFolderName AND $ID NOT IN ($idsNotToGetStringBuilder) ORDER BY $DISPLAY_ORDER ASC",
            null)
    }

    // Check if a database ID is a folder.
    fun isFolder(databaseId: Int): Boolean {
        // Get a readable database handle.
        val bookmarksDatabase = this.readableDatabase

        // Get a cursor with the is folder field for the specified database ID.
        val folderCursor = bookmarksDatabase.rawQuery("SELECT $IS_FOLDER FROM $BOOKMARKS_TABLE WHERE $ID = $databaseId", null)

        // Move to the first record.
        folderCursor.moveToFirst()

        // Ascertain if this database ID is a folder.
        val isFolder = folderCursor.getInt(folderCursor.getColumnIndexOrThrow(IS_FOLDER)) == 1

        // Close the cursor and the database handle.
        folderCursor.close()
        bookmarksDatabase.close()

        // Return the folder status.
        return isFolder
    }

    // Update the bookmark name and URL.
    fun updateBookmark(databaseId: Int, bookmarkName: String, bookmarkUrl: String) {
        // Initialize a content values.
        val bookmarkContentValues = ContentValues()

        // Store the updated values.
        bookmarkContentValues.put(BOOKMARK_NAME, bookmarkName)
        bookmarkContentValues.put(BOOKMARK_URL, bookmarkUrl)

        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Update the bookmark.
        bookmarksDatabase.update(BOOKMARKS_TABLE, bookmarkContentValues, "$ID = $databaseId", null)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Update the bookmark name, URL, parent folder, and display order.
    fun updateBookmark(databaseId: Int, bookmarkName: String, bookmarkUrl: String, parentFolder: String, displayOrder: Int) {
        // Initialize a content values.
        val bookmarkContentValues = ContentValues()

        // Store the updated values.
        bookmarkContentValues.put(BOOKMARK_NAME, bookmarkName)
        bookmarkContentValues.put(BOOKMARK_URL, bookmarkUrl)
        bookmarkContentValues.put(PARENT_FOLDER, parentFolder)
        bookmarkContentValues.put(DISPLAY_ORDER, displayOrder)

        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Update the bookmark.
        bookmarksDatabase.update(BOOKMARKS_TABLE, bookmarkContentValues, "$ID = $databaseId", null)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Update the bookmark name, URL, and favorite icon.
    fun updateBookmark(databaseId: Int, bookmarkName: String, bookmarkUrl: String, favoriteIcon: ByteArray) {
        // Initialize a content values.
        val bookmarkContentValues = ContentValues()

        // Store the updated values.
        bookmarkContentValues.put(BOOKMARK_NAME, bookmarkName)
        bookmarkContentValues.put(BOOKMARK_URL, bookmarkUrl)
        bookmarkContentValues.put(FAVORITE_ICON, favoriteIcon)

        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Update the bookmark.
        bookmarksDatabase.update(BOOKMARKS_TABLE, bookmarkContentValues, "$ID = $databaseId", null)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Update the bookmark name, URL, parent folder, display order, and favorite icon.
    fun updateBookmark(databaseId: Int, bookmarkName: String, bookmarkUrl: String, parentFolder: String, displayOrder: Int, favoriteIcon: ByteArray) {
        // Initialize a content values.
        val bookmarkContentValues = ContentValues()

        // Store the updated values.
        bookmarkContentValues.put(BOOKMARK_NAME, bookmarkName)
        bookmarkContentValues.put(BOOKMARK_URL, bookmarkUrl)
        bookmarkContentValues.put(PARENT_FOLDER, parentFolder)
        bookmarkContentValues.put(DISPLAY_ORDER, displayOrder)
        bookmarkContentValues.put(FAVORITE_ICON, favoriteIcon)

        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Update the bookmark.
        bookmarksDatabase.update(BOOKMARKS_TABLE, bookmarkContentValues, "$ID = $databaseId", null)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Update the folder name.
    fun updateFolder(databaseId: Int, oldFolderName: String, newFolderName: String) {
        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Create a folder content values.
        val folderContentValues = ContentValues()

        // Store the new folder name.
        folderContentValues.put(BOOKMARK_NAME, newFolderName)

        // Run the update on the folder.
        bookmarksDatabase.update(BOOKMARKS_TABLE, folderContentValues, "$ID = $databaseId", null)

        // Create a bookmark content values.
        val bookmarkContentValues = ContentValues()

        // Store the new parent folder name.
        bookmarkContentValues.put(PARENT_FOLDER, newFolderName)

        // SQL escape the old folder name.
        val sqlEscapedOldFolderName = DatabaseUtils.sqlEscapeString(oldFolderName)

        // Run the update on all the bookmarks that currently list the old folder name as their parent folder.
        bookmarksDatabase.update(BOOKMARKS_TABLE, bookmarkContentValues, "$PARENT_FOLDER = $sqlEscapedOldFolderName", null)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Update the folder icon.
    fun updateFolder(databaseId: Int, folderIcon: ByteArray) {
        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Create a content values.
        val folderContentValues = ContentValues()

        // Store the updated icon.
        folderContentValues.put(FAVORITE_ICON, folderIcon)

        // Run the update on the folder.
        bookmarksDatabase.update(BOOKMARKS_TABLE, folderContentValues, "$ID = $databaseId", null)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Update the folder name, parent folder, and display order.
    fun updateFolder(databaseId: Int, oldFolderName: String, newFolderName: String, parentFolder: String, displayOrder: Int) {
        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Create a folder content values.
        val folderContentValues = ContentValues()

        // Store the new folder values.
        folderContentValues.put(BOOKMARK_NAME, newFolderName)
        folderContentValues.put(PARENT_FOLDER, parentFolder)
        folderContentValues.put(DISPLAY_ORDER, displayOrder)

        // Run the update on the folder.
        bookmarksDatabase.update(BOOKMARKS_TABLE, folderContentValues, "$ID = $databaseId", null)

        // Create a bookmark content values.
        val bookmarkContentValues = ContentValues()

        // Store the new parent folder name.
        bookmarkContentValues.put(PARENT_FOLDER, newFolderName)

        // SQL escape the old folder name.
        val sqlEscapedOldFolderName = DatabaseUtils.sqlEscapeString(oldFolderName)

        // Run the update on all the bookmarks that currently list the old folder name as their parent folder.
        bookmarksDatabase.update(BOOKMARKS_TABLE, bookmarkContentValues, "$PARENT_FOLDER = $sqlEscapedOldFolderName", null)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Update the folder name and icon.
    fun updateFolder(databaseId: Int, oldFolderName: String, newFolderName: String, folderIcon: ByteArray) {
        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Create a folder content values.
        val folderContentValues = ContentValues()

        // Store the updated values.
        folderContentValues.put(BOOKMARK_NAME, newFolderName)
        folderContentValues.put(FAVORITE_ICON, folderIcon)

        // Run the update on the folder.
        bookmarksDatabase.update(BOOKMARKS_TABLE, folderContentValues, "$ID = $databaseId", null)

        // Create a bookmark content values.
        val bookmarkContentValues = ContentValues()

        // Store the new parent folder name.
        bookmarkContentValues.put(PARENT_FOLDER, newFolderName)

        // SQL escape the old folder name.
        val sqlEscapedOldFolderName = DatabaseUtils.sqlEscapeString(oldFolderName)

        // Run the update on all the bookmarks that currently list the old folder name as their parent folder.
        bookmarksDatabase.update(BOOKMARKS_TABLE, bookmarkContentValues, "$PARENT_FOLDER = $sqlEscapedOldFolderName", null)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Update the folder name and icon.
    fun updateFolder(databaseId: Int, oldFolderName: String, newFolderName: String, parentFolder: String, displayOrder: Int, folderIcon: ByteArray) {
        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Create a folder content values.
        val folderContentValues = ContentValues()

        // Store the updated values.
        folderContentValues.put(BOOKMARK_NAME, newFolderName)
        folderContentValues.put(PARENT_FOLDER, parentFolder)
        folderContentValues.put(DISPLAY_ORDER, displayOrder)
        folderContentValues.put(FAVORITE_ICON, folderIcon)

        // Run the update on the folder.
        bookmarksDatabase.update(BOOKMARKS_TABLE, folderContentValues, "$ID = $databaseId", null)

        // Create a bookmark content values.
        val bookmarkContentValues = ContentValues()

        // Store the new parent folder name.
        bookmarkContentValues.put(PARENT_FOLDER, newFolderName)

        // SQL escape the old folder name.
        val sqlEscapedOldFolderName = DatabaseUtils.sqlEscapeString(oldFolderName)

        // Run the update on all the bookmarks that currently list the old folder name as their parent folder.
        bookmarksDatabase.update(BOOKMARKS_TABLE, bookmarkContentValues, "$PARENT_FOLDER = $sqlEscapedOldFolderName", null)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Update the display order for one bookmark or folder.
    fun updateDisplayOrder(databaseId: Int, displayOrder: Int) {
        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Create a content values.
        val bookmarkContentValues = ContentValues()

        // Store the new display order.
        bookmarkContentValues.put(DISPLAY_ORDER, displayOrder)

        // Update the database.
        bookmarksDatabase.update(BOOKMARKS_TABLE, bookmarkContentValues, "$ID = $databaseId", null)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Move one bookmark or folder to a new folder.
    fun moveToFolder(databaseId: Int, newFolder: String) {
        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // SQL escape the new folder name.
        val sqlEscapedNewFolder = DatabaseUtils.sqlEscapeString(newFolder)

        // Get a cursor for all the bookmarks in the new folder ordered by display order.
        val newFolderCursor = bookmarksDatabase.rawQuery("SELECT * FROM $BOOKMARKS_TABLE WHERE $PARENT_FOLDER = $sqlEscapedNewFolder ORDER BY $DISPLAY_ORDER ASC", null)

        // Set the new display order.
        val displayOrder: Int = if (newFolderCursor.count > 0) {  // There are already bookmarks in the folder.
            // Move to the last bookmark.
            newFolderCursor.moveToLast()

            // Set the display order to be one greater that the last bookmark.
            newFolderCursor.getInt(newFolderCursor.getColumnIndexOrThrow(DISPLAY_ORDER)) + 1
        } else {  // There are no bookmarks in the new folder.
            // Set the display order to be `0`.
            0
        }

        // Close the cursor.
        newFolderCursor.close()

        // Create a content values.
        val bookmarkContentValues = ContentValues()

        // Store the new values.
        bookmarkContentValues.put(DISPLAY_ORDER, displayOrder)
        bookmarkContentValues.put(PARENT_FOLDER, newFolder)

        // Update the database.
        bookmarksDatabase.update(BOOKMARKS_TABLE, bookmarkContentValues, "$ID = $databaseId", null)

        // Close the database handle.
        bookmarksDatabase.close()
    }

    // Delete one bookmark.
    fun deleteBookmark(databaseId: Int) {
        // Get a writable database handle.
        val bookmarksDatabase = this.writableDatabase

        // Deletes the row with the given database ID.
        bookmarksDatabase.delete(BOOKMARKS_TABLE, "$ID = $databaseId", null)

        // Close the database handle.
        bookmarksDatabase.close()
    }
}