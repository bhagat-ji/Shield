/*
 * Copyright © 2018-2022 Soren Stoutner <soren@stoutner.com>.
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
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase

import androidx.preference.PreferenceManager

import com.stoutner.privacybrowser.R

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

// Define the private class constants.
private const val SCHEMA_VERSION = 14
private const val PREFERENCES_TABLE = "preferences"

// Define the private preferences constants.
private const val ID = "_id"
private const val JAVASCRIPT = "javascript"
private const val COOKIES = "cookies"
private const val DOM_STORAGE = "dom_storage"
private const val SAVE_FORM_DATA = "save_form_data"
private const val USER_AGENT = "user_agent"
private const val CUSTOM_USER_AGENT = "custom_user_agent"
private const val INCOGNITO_MODE = "incognito_mode"
private const val ALLOW_SCREENSHOTS = "allow_screenshots"
private const val EASYLIST = "easylist"
private const val EASYPRIVACY = "easyprivacy"
private const val FANBOYS_ANNOYANCE_LIST = "fanboys_annoyance_list"
private const val FANBOYS_SOCIAL_BLOCKING_LIST = "fanboys_social_blocking_list"
private const val ULTRALIST = "ultralist"
private const val ULTRAPRIVACY = "ultraprivacy"
private const val BLOCK_ALL_THIRD_PARTY_REQUESTS = "block_all_third_party_requests"
private const val GOOGLE_ANALYTICS = "google_analytics"
private const val FACEBOOK_CLICK_IDS = "facebook_click_ids"
private const val TWITTER_AMP_REDIRECTS = "twitter_amp_redirects"
private const val SEARCH = "search"
private const val SEARCH_CUSTOM_URL = "search_custom_url"
private const val PROXY = "proxy"
private const val PROXY_CUSTOM_URL = "proxy_custom_url"
private const val FULL_SCREEN_BROWSING_MODE = "full_screen_browsing_mode"
private const val HIDE_APP_BAR = "hide_app_bar"
private const val CLEAR_EVERYTHING = "clear_everything"
private const val CLEAR_COOKIES = "clear_cookies"
private const val CLEAR_DOM_STORAGE = "clear_dom_storage"
private const val CLEAR_FORM_DATA = "clear_form_data"
private const val CLEAR_LOGCAT = "clear_logcat"
private const val CLEAR_CACHE = "clear_cache"
private const val HOMEPAGE = "homepage"
private const val FONT_SIZE = "font_size"
private const val OPEN_INTENTS_IN_NEW_TAB = "open_intents_in_new_tab"
private const val SWIPE_TO_REFRESH = "swipe_to_refresh"
private const val DOWNLOAD_WITH_EXTERNAL_APP = "download_with_external_app"
private const val SCROLL_APP_BAR = "scroll_app_bar"
private const val BOTTOM_APP_BAR = "bottom_app_bar"
private const val DISPLAY_ADDITIONAL_APP_BAR_ICONS = "display_additional_app_bar_icons"
private const val APP_THEME = "app_theme"
private const val WEBVIEW_THEME = "webview_theme"
private const val WIDE_VIEWPORT = "wide_viewport"
private const val DISPLAY_WEBPAGE_IMAGES = "display_webpage_images"

class ImportExportDatabaseHelper {
    // Define the public companion object constants.  These can be moved to public class constants once the entire project has migrated to Kotlin.
    companion object {
        // Define the public class constants.
        const val EXPORT_SUCCESSFUL = "Export Successful"
        const val IMPORT_SUCCESSFUL = "Import Successful"
    }

    fun importUnencrypted(importFileInputStream: InputStream, context: Context): String {
        return try {
            // Create a temporary import file.
            val temporaryImportFile = File.createTempFile("temporary_import_file", null, context.cacheDir)

            // The file may be copied directly in Kotlin using `File.copyTo`.  <https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.io/java.io.-file/copy-to.html>
            // It can be copied in Android using `Files.copy` once the minimum API >= 26.
            // <https://developer.android.com/reference/java/nio/file/Files#copy(java.nio.file.Path,%20java.nio.file.Path,%20java.nio.file.CopyOption...)>
            // However, the file cannot be acquired from the content URI until the minimum API >= 29.  <https://developer.android.com/reference/kotlin/android/content/ContentResolver#openfile>

            // Create a temporary file output stream.
            val temporaryImportFileOutputStream = FileOutputStream(temporaryImportFile)

            // Create a transfer byte array.
            val transferByteArray = ByteArray(1024)

            // Create an integer to track the number of bytes read.
            var bytesRead: Int

            // Copy the import file to the temporary import file.
            while (importFileInputStream.read(transferByteArray).also { bytesRead = it } > 0) {
                temporaryImportFileOutputStream.write(transferByteArray, 0, bytesRead)
            }

            // Flush the temporary import file output stream.
            temporaryImportFileOutputStream.flush()

            // Close the file streams.
            importFileInputStream.close()
            temporaryImportFileOutputStream.close()


            // Get a handle for the shared preference.
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

            // Open the import database.  Once the minimum API >= 27 the file can be opened directly without using the string.
            val importDatabase = SQLiteDatabase.openDatabase(temporaryImportFile.toString(), null, SQLiteDatabase.OPEN_READWRITE)

            // Get the database version.
            val importDatabaseVersion = importDatabase.version

            // Upgrade from schema version 1, first used in Privacy Browser 2.13, to schema version 2, first used in Privacy Browser 2.14.
            // Previously this upgrade added `download_with_external_app` to the Preferences table.  But that is now removed in schema version 10.

            // Upgrade from schema version 2, first used in Privacy Browser 2.14, to schema version 3, first used in Privacy Browser 2.15.
            if (importDatabaseVersion < 3) {
                // Once the SQLite version is >= 3.25.0 (API >= 30) `ALTER TABLE RENAME COLUMN` can be used.  <https://www.sqlite.org/lang_altertable.html> <https://www.sqlite.org/changes.html>
                // <https://developer.android.com/reference/android/database/sqlite/package-summary>
                // In the meantime, a new column must be created with the new name.  There is no need to delete the old column on the temporary import database.

                // Create the new font size column.
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $FONT_SIZE TEXT")

                // Populate the preferences table with the current font size value.
                importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $FONT_SIZE = default_font_size")
            }

            // Upgrade from schema version 3, first used in Privacy Browser 2.15, to schema version 4, first used in Privacy Browser 2.16.
            if (importDatabaseVersion < 4) {
                // Add the Pinned IP Addresses columns to the domains table.
                importDatabase.execSQL("ALTER TABLE ${DomainsDatabaseHelper.DOMAINS_TABLE} ADD COLUMN ${DomainsDatabaseHelper.PINNED_IP_ADDRESSES}  BOOLEAN")
                importDatabase.execSQL("ALTER TABLE ${DomainsDatabaseHelper.DOMAINS_TABLE} ADD COLUMN ${DomainsDatabaseHelper.IP_ADDRESSES} TEXT")
            }

            // Upgrade from schema version 4, first used in Privacy Browser 2.16, to schema version 5, first used in Privacy Browser 2.17.
            if (importDatabaseVersion < 5) {
                // Add the hide and scroll app bar columns to the preferences table.
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $HIDE_APP_BAR BOOLEAN")
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $SCROLL_APP_BAR BOOLEAN")

                // Get the current hide and scroll app bar settings.
                val hideAppBar = sharedPreferences.getBoolean(HIDE_APP_BAR, true)
                val scrollAppBar = sharedPreferences.getBoolean(SCROLL_APP_BAR, true)

                // Populate the preferences table with the current app bar values.
                // This can switch to using the variables directly once the API >= 30.  <https://www.sqlite.org/datatype3.html#boolean_datatype>
                // <https://developer.android.com/reference/android/database/sqlite/package-summary>
                if (hideAppBar) {
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $HIDE_APP_BAR = 1")
                } else {
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $HIDE_APP_BAR = 0")
                }

                if (scrollAppBar) {
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $SCROLL_APP_BAR = 1")
                } else {
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $SCROLL_APP_BAR = 0")
                }
            }

            // Upgrade from schema version 5, first used in Privacy Browser 2.17, to schema version 6, first used in Privacy Browser 3.0.
            if (importDatabaseVersion < 6) {
                // Add the open intents in new tab column to the preferences table.
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $OPEN_INTENTS_IN_NEW_TAB BOOLEAN")

                // Get the current open intents in new tab preference.
                val openIntentsInNewTab = sharedPreferences.getBoolean(OPEN_INTENTS_IN_NEW_TAB, true)

                // Populate the preferences table with the current open intents value.
                // This can switch to using the variables directly once the API >= 30.  <https://www.sqlite.org/datatype3.html#boolean_datatype>
                // <https://developer.android.com/reference/android/database/sqlite/package-summary>
                if (openIntentsInNewTab) {
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $OPEN_INTENTS_IN_NEW_TAB = 1")
                } else {
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $OPEN_INTENTS_IN_NEW_TAB = 0")
                }
            }

            // Upgrade from schema version 6, first used in Privacy Browser 3.0, to schema version 7, first used in Privacy Browser 3.1.
            if (importDatabaseVersion < 7) {
                // Add the wide viewport column to the domains table.
                importDatabase.execSQL("ALTER TABLE ${DomainsDatabaseHelper.DOMAINS_TABLE} ADD COLUMN ${DomainsDatabaseHelper.WIDE_VIEWPORT} INTEGER")

                // Add the Google Analytics, Facebook Click IDs, Twitter AMP redirects, and wide viewport columns to the preferences table.
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $GOOGLE_ANALYTICS BOOLEAN")
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $FACEBOOK_CLICK_IDS BOOLEAN")
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $TWITTER_AMP_REDIRECTS BOOLEAN")
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $WIDE_VIEWPORT BOOLEAN")

                // Get the current preference values.
                val googleAnalytics = sharedPreferences.getBoolean(GOOGLE_ANALYTICS, true)
                val facebookClickIds = sharedPreferences.getBoolean(FACEBOOK_CLICK_IDS, true)
                val twitterAmpRedirects = sharedPreferences.getBoolean(TWITTER_AMP_REDIRECTS, true)
                val wideViewport = sharedPreferences.getBoolean(WIDE_VIEWPORT, true)

                // Populate the preferences with the current Google Analytics value.
                // This can switch to using the variables directly once the API >= 30.  <https://www.sqlite.org/datatype3.html#boolean_datatype>
                // <https://developer.android.com/reference/android/database/sqlite/package-summary>
                if (googleAnalytics) {
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $GOOGLE_ANALYTICS = 1")
                } else {
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $GOOGLE_ANALYTICS = 0")
                }

                // Populate the preferences with the current Facebook Click IDs value.
                if (facebookClickIds) {
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $FACEBOOK_CLICK_IDS = 1")
                } else {
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $FACEBOOK_CLICK_IDS = 0")
                }

                // Populate the preferences table with the current Twitter AMP redirects value.
                if (twitterAmpRedirects) {
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $TWITTER_AMP_REDIRECTS = 1")
                } else {
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $TWITTER_AMP_REDIRECTS = 0")
                }

                // Populate the preferences table with the current wide viewport value.
                if (wideViewport) {
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $WIDE_VIEWPORT = 1")
                } else {
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $WIDE_VIEWPORT = 0")
                }
            }

            // Upgrade from schema version 7, first used in Privacy Browser 3.1, to schema version 8, first used in Privacy Browser 3.2.
            if (importDatabaseVersion < 8) {
                // Add the UltraList column to the tables.
                importDatabase.execSQL("ALTER TABLE ${DomainsDatabaseHelper.DOMAINS_TABLE} ADD COLUMN ${DomainsDatabaseHelper.ULTRALIST} BOOLEAN")
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $ULTRALIST BOOLEAN")

                // Get the current preference values.
                val ultraList = sharedPreferences.getBoolean(ULTRALIST, true)

                // Populate the tables with the current UltraList value.
                // This can switch to using the variables directly once the API >= 30.  <https://www.sqlite.org/datatype3.html#boolean_datatype>
                // <https://developer.android.com/reference/android/database/sqlite/package-summary>
                if (ultraList) {
                    importDatabase.execSQL("UPDATE ${DomainsDatabaseHelper.DOMAINS_TABLE} SET ${DomainsDatabaseHelper.ULTRALIST}  =  1")
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $ULTRALIST = 1")
                } else {
                    importDatabase.execSQL("UPDATE ${DomainsDatabaseHelper.DOMAINS_TABLE} SET ${DomainsDatabaseHelper.ULTRALIST} = 0")
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $ULTRALIST = 0")
                }
            }

            // Upgrade from schema version 8, first used in Privacy Browser 3.2, to schema version 9, first used in Privacy Browser 3.3.
            if (importDatabaseVersion < 9) {
                // Add the new proxy columns to the preferences table.
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $PROXY TEXT")
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $PROXY_CUSTOM_URL TEXT")

                // Get the current proxy values.
                val proxy = sharedPreferences.getString(PROXY, context.getString(R.string.proxy_default_value))
                var proxyCustomUrl = sharedPreferences.getString(PROXY_CUSTOM_URL, context.getString(R.string.proxy_custom_url_default_value))

                // SQL escape the proxy custom URL string.
                proxyCustomUrl = DatabaseUtils.sqlEscapeString(proxyCustomUrl)

                // Populate the preferences table with the current proxy values. The proxy custom URL does not need to be surrounded by `'` because it was SLQ escaped above.
                importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $PROXY = '$proxy'")
                importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $PROXY_CUSTOM_URL = $proxyCustomUrl")
            }

            // Upgrade from schema version 9, first used in Privacy Browser 3.3, to schema version 10, first used in Privacy Browser 3.4.
            // Previously this upgrade added `download_location` and `download_custom_location` to the Preferences table.  But they were removed in schema version 13.

            // Upgrade from schema version 10, first used in Privacy Browser 3.4, to schema version 11, first used in Privacy Browser 3.5.
            if (importDatabaseVersion < 11) {
                // Add the app theme column to the preferences table.
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $APP_THEME TEXT")

                // Get a cursor for the dark theme preference.
                val darkThemePreferencesCursor = importDatabase.rawQuery("SELECT dark_theme FROM $PREFERENCES_TABLE", null)

                // Move to the first entry.
                darkThemePreferencesCursor.moveToFirst()

                // Get the old dark theme value, which is in column 0.
                val darkTheme = darkThemePreferencesCursor.getInt(0)

                // Close the dark theme preference cursor.
                darkThemePreferencesCursor.close()

                // Get the system default string.
                val systemDefault = context.getString(R.string.app_theme_default_value)

                // Get the theme entry values string array.
                val appThemeEntryValuesStringArray: Array<String> = context.resources.getStringArray(R.array.app_theme_entry_values)

                // Get the dark string.
                val dark = appThemeEntryValuesStringArray[2]

                // Populate the app theme according to the old dark theme preference.
                if (darkTheme == 0) {  // A light theme was selected.
                    // Set the app theme to be the system default.
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $APP_THEME = '$systemDefault'")
                } else {  // A dark theme was selected.
                    // Set the app theme to be dark.
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $APP_THEME = '$dark'")
                }

                // Add the WebView theme to the domains table.  This defaults to 0, which is `System default`, so a separate step isn't needed to populate the database.
                importDatabase.execSQL("ALTER TABLE ${DomainsDatabaseHelper.DOMAINS_TABLE} ADD COLUMN ${DomainsDatabaseHelper.WEBVIEW_THEME} INTEGER")

                // Add the WebView theme to the preferences table.
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $WEBVIEW_THEME TEXT")

                // Get the WebView theme default value string.
                val webViewThemeDefaultValue = context.getString(R.string.webview_theme_default_value)

                // Set the WebView theme in the preferences table to be the default.
                importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $WEBVIEW_THEME = '$webViewThemeDefaultValue'")
            }

            // Upgrade from schema version 11, first used in Privacy Browser 3.5, to schema version 12, first used in Privacy Browser 3.6.
            if (importDatabaseVersion < 12) {
                // Add the clear logcat column to the preferences table.
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $CLEAR_LOGCAT BOOLEAN")

                // Get the current clear logcat value.
                val clearLogcat = sharedPreferences.getBoolean(CLEAR_LOGCAT, true)

                // Populate the preferences table with the current clear logcat value.
                // This can switch to using the variables directly once the API >= 30.  <https://www.sqlite.org/datatype3.html#boolean_datatype>
                // <https://developer.android.com/reference/android/database/sqlite/package-summary>
                if (clearLogcat) {
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $CLEAR_LOGCAT = 1")
                } else {
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $CLEAR_LOGCAT = 0")
                }
            }

            // Upgrade from schema version 12, first used in Privacy Browser 3.6, to schema version 13, first used in Privacy Browser 3.7.
            // Do nothing.  `download_location` and `download_custom_location` were removed from the preferences table, but they can be left in the temporary import database without issue.

            // Upgrade from schema version 13, first used in Privacy Browser 3.7, to schema version 14, first used in Privacy Browser 3.8.
            if (importDatabaseVersion < 14) {
                // `enabledthirdpartycookies` was removed from the domains table.  `do_not_track` and `third_party_cookies` were removed from the preferences table.

                // Once the SQLite version is >= 3.25.0 `ALTER TABLE RENAME COLUMN` can be used.  <https://www.sqlite.org/lang_altertable.html> <https://www.sqlite.org/changes.html>
                // <https://developer.android.com/reference/android/database/sqlite/package-summary>
                // In the meantime, a new column must be created with the new name.  There is no need to delete the old column on the temporary import database.

                // Create the new cookies columns.
                importDatabase.execSQL("ALTER TABLE ${DomainsDatabaseHelper.DOMAINS_TABLE} ADD COLUMN ${DomainsDatabaseHelper.COOKIES} BOOLEAN")
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $COOKIES BOOLEAN")

                // Copy the data from the old cookies columns to the new ones.
                importDatabase.execSQL("UPDATE ${DomainsDatabaseHelper.DOMAINS_TABLE} SET ${DomainsDatabaseHelper.COOKIES} = enablefirstpartycookies")
                importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $COOKIES = first_party_cookies")

                // Create the new download with external app and bottom app bar columns.
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $DOWNLOAD_WITH_EXTERNAL_APP BOOLEAN")
                importDatabase.execSQL("ALTER TABLE $PREFERENCES_TABLE ADD COLUMN $BOTTOM_APP_BAR BOOLEAN")

                // Get the current values for the new columns.
                val downloadWithExternalApp = sharedPreferences.getBoolean(DOWNLOAD_WITH_EXTERNAL_APP, false)
                val bottomAppBar = sharedPreferences.getBoolean(BOTTOM_APP_BAR, false)

                // Populate the preferences table with the current download with external app value.
                // This can switch to using the variables directly once the API >= 30.  <https://www.sqlite.org/datatype3.html#boolean_datatype>
                // <https://developer.android.com/reference/android/database/sqlite/package-summary>
                if (downloadWithExternalApp) {
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $DOWNLOAD_WITH_EXTERNAL_APP = 1")
                } else {
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $DOWNLOAD_WITH_EXTERNAL_APP = 0")
                }

                // Populate the preferences table with the current bottom app bar value.
                if (bottomAppBar) {
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $BOTTOM_APP_BAR = 1")
                } else {
                    importDatabase.execSQL("UPDATE $PREFERENCES_TABLE SET $BOTTOM_APP_BAR = 0")
                }
            }

            // Get a cursor for the bookmarks table.
            val importBookmarksCursor = importDatabase.rawQuery("SELECT * FROM ${BookmarksDatabaseHelper.BOOKMARKS_TABLE}", null)

            // Delete the current bookmarks database.
            context.deleteDatabase(BookmarksDatabaseHelper.BOOKMARKS_DATABASE)

            // Create a new bookmarks database.
            val bookmarksDatabaseHelper = BookmarksDatabaseHelper(context)

            // Move to the first record.
            importBookmarksCursor.moveToFirst()

            // Copy the data from the import bookmarks cursor into the bookmarks database.
            for (i in 0 until importBookmarksCursor.count) {
                // Create a bookmark content values.
                val bookmarkContentValues = ContentValues()

                // Add the information for this bookmark to the content values.
                bookmarkContentValues.put(BookmarksDatabaseHelper.BOOKMARK_NAME, importBookmarksCursor.getString(importBookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.BOOKMARK_NAME)))
                bookmarkContentValues.put(BookmarksDatabaseHelper.BOOKMARK_URL, importBookmarksCursor.getString(importBookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.BOOKMARK_URL)))
                bookmarkContentValues.put(BookmarksDatabaseHelper.PARENT_FOLDER, importBookmarksCursor.getString(importBookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.PARENT_FOLDER)))
                bookmarkContentValues.put(BookmarksDatabaseHelper.DISPLAY_ORDER, importBookmarksCursor.getInt(importBookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.DISPLAY_ORDER)))
                bookmarkContentValues.put(BookmarksDatabaseHelper.IS_FOLDER, importBookmarksCursor.getInt(importBookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.IS_FOLDER)))
                bookmarkContentValues.put(BookmarksDatabaseHelper.FAVORITE_ICON, importBookmarksCursor.getBlob(importBookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.FAVORITE_ICON)))

                // Insert the content values into the bookmarks database.
                bookmarksDatabaseHelper.createBookmark(bookmarkContentValues)

                // Advance to the next record.
                importBookmarksCursor.moveToNext()
            }

            // Close the bookmarks cursor and database.
            importBookmarksCursor.close()
            bookmarksDatabaseHelper.close()


            // Get a cursor for the domains table.
            val importDomainsCursor = importDatabase.rawQuery("SELECT * FROM ${DomainsDatabaseHelper.DOMAINS_TABLE} ORDER BY ${DomainsDatabaseHelper.DOMAIN_NAME} ASC", null)

            // Delete the current domains database.
            context.deleteDatabase(DomainsDatabaseHelper.DOMAINS_DATABASE)

            // Create a new domains database.
            val domainsDatabaseHelper = DomainsDatabaseHelper(context)

            // Move to the first record.
            importDomainsCursor.moveToFirst()

            // Copy the data from the import domains cursor into the domains database.
            for (i in 0 until importDomainsCursor.count) {
                // Create a domain content values.
                val domainContentValues = ContentValues()

                // Populate the domain content values.
                domainContentValues.put(DomainsDatabaseHelper.DOMAIN_NAME, importDomainsCursor.getString(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.DOMAIN_NAME)))
                domainContentValues.put(DomainsDatabaseHelper.ENABLE_JAVASCRIPT, importDomainsCursor.getInt(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_JAVASCRIPT)))
                domainContentValues.put(DomainsDatabaseHelper.COOKIES, importDomainsCursor.getInt(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.COOKIES)))
                domainContentValues.put(DomainsDatabaseHelper.ENABLE_DOM_STORAGE, importDomainsCursor.getInt(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_DOM_STORAGE)))
                domainContentValues.put(DomainsDatabaseHelper.ENABLE_FORM_DATA, importDomainsCursor.getInt(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_FORM_DATA)))
                domainContentValues.put(DomainsDatabaseHelper.ENABLE_EASYLIST, importDomainsCursor.getInt(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_EASYLIST)))
                domainContentValues.put(DomainsDatabaseHelper.ENABLE_EASYPRIVACY, importDomainsCursor.getInt(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_EASYPRIVACY)))
                domainContentValues.put(DomainsDatabaseHelper.ENABLE_FANBOYS_ANNOYANCE_LIST,
                    importDomainsCursor.getInt(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_FANBOYS_ANNOYANCE_LIST)))
                domainContentValues.put(DomainsDatabaseHelper.ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST,
                    importDomainsCursor.getInt(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST)))
                domainContentValues.put(DomainsDatabaseHelper.ULTRALIST, importDomainsCursor.getInt(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ULTRALIST)))
                domainContentValues.put(DomainsDatabaseHelper.ENABLE_ULTRAPRIVACY, importDomainsCursor.getInt(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_ULTRAPRIVACY)))
                domainContentValues.put(DomainsDatabaseHelper.BLOCK_ALL_THIRD_PARTY_REQUESTS,
                    importDomainsCursor.getInt(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.BLOCK_ALL_THIRD_PARTY_REQUESTS)))
                domainContentValues.put(DomainsDatabaseHelper.USER_AGENT, importDomainsCursor.getString(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.USER_AGENT)))
                domainContentValues.put(DomainsDatabaseHelper.FONT_SIZE, importDomainsCursor.getInt(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.FONT_SIZE)))
                domainContentValues.put(DomainsDatabaseHelper.SWIPE_TO_REFRESH, importDomainsCursor.getInt(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SWIPE_TO_REFRESH)))
                domainContentValues.put(DomainsDatabaseHelper.WEBVIEW_THEME, importDomainsCursor.getInt(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.WEBVIEW_THEME)))
                domainContentValues.put(DomainsDatabaseHelper.WIDE_VIEWPORT, importDomainsCursor.getInt(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.WIDE_VIEWPORT)))
                domainContentValues.put(DomainsDatabaseHelper.DISPLAY_IMAGES, importDomainsCursor.getInt(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.DISPLAY_IMAGES)))
                domainContentValues.put(DomainsDatabaseHelper.PINNED_SSL_CERTIFICATE, importDomainsCursor.getInt(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.PINNED_SSL_CERTIFICATE)))
                domainContentValues.put(DomainsDatabaseHelper.SSL_ISSUED_TO_COMMON_NAME,
                    importDomainsCursor.getString(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_TO_COMMON_NAME)))
                domainContentValues.put(DomainsDatabaseHelper.SSL_ISSUED_TO_ORGANIZATION,
                    importDomainsCursor.getString(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_TO_ORGANIZATION)))
                domainContentValues.put(DomainsDatabaseHelper.SSL_ISSUED_TO_ORGANIZATIONAL_UNIT,
                    importDomainsCursor.getString(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_TO_ORGANIZATIONAL_UNIT)))
                domainContentValues.put(DomainsDatabaseHelper.SSL_ISSUED_BY_COMMON_NAME,
                    importDomainsCursor.getString(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_BY_COMMON_NAME)))
                domainContentValues.put(DomainsDatabaseHelper.SSL_ISSUED_BY_ORGANIZATION,
                    importDomainsCursor.getString(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_BY_ORGANIZATION)))
                domainContentValues.put(DomainsDatabaseHelper.SSL_ISSUED_BY_ORGANIZATIONAL_UNIT,
                    importDomainsCursor.getString(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_BY_ORGANIZATIONAL_UNIT)))
                domainContentValues.put(DomainsDatabaseHelper.SSL_START_DATE, importDomainsCursor.getLong(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_START_DATE)))
                domainContentValues.put(DomainsDatabaseHelper.SSL_END_DATE, importDomainsCursor.getLong(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_END_DATE)))
                domainContentValues.put(DomainsDatabaseHelper.PINNED_IP_ADDRESSES, importDomainsCursor.getInt(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.PINNED_IP_ADDRESSES)))
                domainContentValues.put(DomainsDatabaseHelper.IP_ADDRESSES, importDomainsCursor.getString(importDomainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.IP_ADDRESSES)))

                // Insert the content values into the domains database.
                domainsDatabaseHelper.addDomain(domainContentValues)

                // Advance to the next record.
                importDomainsCursor.moveToNext()
            }

            // Close the domains cursor and database.
            importDomainsCursor.close()
            domainsDatabaseHelper.close()


            // Get a cursor for the preferences table.
            val importPreferencesCursor = importDatabase.rawQuery("SELECT * FROM $PREFERENCES_TABLE", null)

            // Move to the first record.
            importPreferencesCursor.moveToFirst()

            // Import the preference data.
            sharedPreferences.edit()
                .putBoolean(JAVASCRIPT, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(JAVASCRIPT)) == 1)
                .putBoolean(COOKIES, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(COOKIES)) == 1)
                .putBoolean(DOM_STORAGE, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(DOM_STORAGE)) == 1)
                .putBoolean(SAVE_FORM_DATA, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(SAVE_FORM_DATA)) == 1)  // Save form data can be removed once the minimum API >= 26.
                .putString(USER_AGENT, importPreferencesCursor.getString(importPreferencesCursor.getColumnIndexOrThrow(USER_AGENT)))
                .putString(CUSTOM_USER_AGENT, importPreferencesCursor.getString(importPreferencesCursor.getColumnIndexOrThrow(CUSTOM_USER_AGENT)))
                .putBoolean(INCOGNITO_MODE, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(INCOGNITO_MODE)) == 1)
                .putBoolean(ALLOW_SCREENSHOTS, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(ALLOW_SCREENSHOTS)) == 1)
                .putBoolean(EASYLIST, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(EASYLIST)) == 1)
                .putBoolean(EASYPRIVACY, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(EASYPRIVACY)) == 1)
                .putBoolean(FANBOYS_ANNOYANCE_LIST, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(FANBOYS_ANNOYANCE_LIST)) == 1)
                .putBoolean(FANBOYS_SOCIAL_BLOCKING_LIST, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(FANBOYS_SOCIAL_BLOCKING_LIST)) == 1)
                .putBoolean(ULTRALIST, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(ULTRALIST)) == 1)
                .putBoolean(ULTRAPRIVACY, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(ULTRAPRIVACY)) == 1)
                .putBoolean(BLOCK_ALL_THIRD_PARTY_REQUESTS, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(BLOCK_ALL_THIRD_PARTY_REQUESTS)) == 1)
                .putBoolean(GOOGLE_ANALYTICS, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(GOOGLE_ANALYTICS)) == 1)
                .putBoolean(FACEBOOK_CLICK_IDS, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(FACEBOOK_CLICK_IDS)) == 1)
                .putBoolean(TWITTER_AMP_REDIRECTS, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(TWITTER_AMP_REDIRECTS)) == 1)
                .putString(SEARCH, importPreferencesCursor.getString(importPreferencesCursor.getColumnIndexOrThrow(SEARCH)))
                .putString(SEARCH_CUSTOM_URL, importPreferencesCursor.getString(importPreferencesCursor.getColumnIndexOrThrow(SEARCH_CUSTOM_URL)))
                .putString(PROXY, importPreferencesCursor.getString(importPreferencesCursor.getColumnIndexOrThrow(PROXY)))
                .putString(PROXY_CUSTOM_URL, importPreferencesCursor.getString(importPreferencesCursor.getColumnIndexOrThrow(PROXY_CUSTOM_URL)))
                .putBoolean(FULL_SCREEN_BROWSING_MODE, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(FULL_SCREEN_BROWSING_MODE)) == 1)
                .putBoolean(HIDE_APP_BAR, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(HIDE_APP_BAR)) == 1)
                .putBoolean(CLEAR_EVERYTHING, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(CLEAR_EVERYTHING)) == 1)
                .putBoolean(CLEAR_COOKIES, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(CLEAR_COOKIES)) == 1)
                .putBoolean(CLEAR_DOM_STORAGE, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(CLEAR_DOM_STORAGE)) == 1)
                .putBoolean(CLEAR_FORM_DATA, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(CLEAR_FORM_DATA)) == 1)  // Clear form data can be removed once the minimum API >= 26.
                .putBoolean(CLEAR_LOGCAT, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(CLEAR_LOGCAT)) == 1)
                .putBoolean(CLEAR_CACHE, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(CLEAR_CACHE)) == 1)
                .putString(HOMEPAGE, importPreferencesCursor.getString(importPreferencesCursor.getColumnIndexOrThrow(HOMEPAGE)))
                .putString(FONT_SIZE, importPreferencesCursor.getString(importPreferencesCursor.getColumnIndexOrThrow(FONT_SIZE)))
                .putBoolean(OPEN_INTENTS_IN_NEW_TAB, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(OPEN_INTENTS_IN_NEW_TAB)) == 1)
                .putBoolean(SWIPE_TO_REFRESH, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(SWIPE_TO_REFRESH)) == 1)
                .putBoolean(DOWNLOAD_WITH_EXTERNAL_APP, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(DOWNLOAD_WITH_EXTERNAL_APP)) == 1)
                .putBoolean(SCROLL_APP_BAR, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(SCROLL_APP_BAR)) == 1)
                .putBoolean(BOTTOM_APP_BAR, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(BOTTOM_APP_BAR)) == 1)
                .putBoolean(DISPLAY_ADDITIONAL_APP_BAR_ICONS, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(DISPLAY_ADDITIONAL_APP_BAR_ICONS)) == 1)
                .putString(APP_THEME, importPreferencesCursor.getString(importPreferencesCursor.getColumnIndexOrThrow(APP_THEME)))
                .putString(WEBVIEW_THEME, importPreferencesCursor.getString(importPreferencesCursor.getColumnIndexOrThrow(WEBVIEW_THEME)))
                .putBoolean(WIDE_VIEWPORT, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(WIDE_VIEWPORT)) == 1)
                .putBoolean(DISPLAY_WEBPAGE_IMAGES, importPreferencesCursor.getInt(importPreferencesCursor.getColumnIndexOrThrow(DISPLAY_WEBPAGE_IMAGES)) == 1)
                .apply()

            // Close the preferences cursor and database.
            importPreferencesCursor.close()
            importDatabase.close()

            // Delete the temporary import file database, journal, and other related auxiliary files.
            SQLiteDatabase.deleteDatabase(temporaryImportFile)

            // Return the import successful string.
            IMPORT_SUCCESSFUL
        } catch (exception: Exception) {
            // Return the import error.
            exception.toString()
        }
    }

    fun exportUnencrypted(exportFileOutputStream: OutputStream, context: Context): String {
        return try {
            // Create a temporary export file.
            val temporaryExportFile = File.createTempFile("temporary_export_file", null, context.cacheDir)

            // Create the temporary export database.
            val temporaryExportDatabase = SQLiteDatabase.openOrCreateDatabase(temporaryExportFile, null)

            // Set the temporary export database version number.
            temporaryExportDatabase.version = SCHEMA_VERSION


            // Create the temporary export database bookmarks table.
            temporaryExportDatabase.execSQL(BookmarksDatabaseHelper.CREATE_BOOKMARKS_TABLE)

            // Open the bookmarks database.
            val bookmarksDatabaseHelper = BookmarksDatabaseHelper(context)

            // Get a full bookmarks cursor.
            val bookmarksCursor = bookmarksDatabaseHelper.allBookmarks

            // Move to the first record.
            bookmarksCursor.moveToFirst()

            // Copy the data from the bookmarks cursor into the export database.
            for (i in 0 until bookmarksCursor.count) {
                // Create a bookmark content values.
                val bookmarkContentValues = ContentValues()

                // Populate the bookmark content values.
                bookmarkContentValues.put(BookmarksDatabaseHelper.BOOKMARK_NAME, bookmarksCursor.getString(bookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.BOOKMARK_NAME)))
                bookmarkContentValues.put(BookmarksDatabaseHelper.BOOKMARK_URL, bookmarksCursor.getString(bookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.BOOKMARK_URL)))
                bookmarkContentValues.put(BookmarksDatabaseHelper.PARENT_FOLDER, bookmarksCursor.getString(bookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.PARENT_FOLDER)))
                bookmarkContentValues.put(BookmarksDatabaseHelper.DISPLAY_ORDER, bookmarksCursor.getInt(bookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.DISPLAY_ORDER)))
                bookmarkContentValues.put(BookmarksDatabaseHelper.IS_FOLDER, bookmarksCursor.getInt(bookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.IS_FOLDER)))
                bookmarkContentValues.put(BookmarksDatabaseHelper.FAVORITE_ICON, bookmarksCursor.getBlob(bookmarksCursor.getColumnIndexOrThrow(BookmarksDatabaseHelper.FAVORITE_ICON)))

                // Insert the content values into the temporary export database.
                temporaryExportDatabase.insert(BookmarksDatabaseHelper.BOOKMARKS_TABLE, null, bookmarkContentValues)

                // Advance to the next record.
                bookmarksCursor.moveToNext()
            }

            // Close the bookmarks cursor and database.
            bookmarksCursor.close()
            bookmarksDatabaseHelper.close()


            // Create the temporary export database domains table.
            temporaryExportDatabase.execSQL(DomainsDatabaseHelper.CREATE_DOMAINS_TABLE)

            // Open the domains database.
            val domainsDatabaseHelper = DomainsDatabaseHelper(context)

            // Get a full domains database cursor.
            val domainsCursor = domainsDatabaseHelper.completeCursorOrderedByDomain

            // Move to the first record.
            domainsCursor.moveToFirst()

            // Copy the data from the domains cursor into the export database.
            for (i in 0 until domainsCursor.count) {
                // Create a domain content values.
                val domainContentValues = ContentValues()

                // Populate the domain content values.
                domainContentValues.put(DomainsDatabaseHelper.DOMAIN_NAME, domainsCursor.getString(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.DOMAIN_NAME)))
                domainContentValues.put(DomainsDatabaseHelper.ENABLE_JAVASCRIPT, domainsCursor.getInt(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_JAVASCRIPT)))
                domainContentValues.put(DomainsDatabaseHelper.COOKIES, domainsCursor.getInt(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.COOKIES)))
                domainContentValues.put(DomainsDatabaseHelper.ENABLE_DOM_STORAGE, domainsCursor.getInt(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_DOM_STORAGE)))
                domainContentValues.put(DomainsDatabaseHelper.ENABLE_FORM_DATA, domainsCursor.getInt(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_FORM_DATA)))
                domainContentValues.put(DomainsDatabaseHelper.ENABLE_EASYLIST, domainsCursor.getInt(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_EASYLIST)))
                domainContentValues.put(DomainsDatabaseHelper.ENABLE_EASYPRIVACY, domainsCursor.getInt(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_EASYPRIVACY)))
                domainContentValues.put(DomainsDatabaseHelper.ENABLE_FANBOYS_ANNOYANCE_LIST, domainsCursor.getInt(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_FANBOYS_ANNOYANCE_LIST)))
                domainContentValues.put(DomainsDatabaseHelper.ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST,
                    domainsCursor.getInt(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST)))
                domainContentValues.put(DomainsDatabaseHelper.ULTRALIST, domainsCursor.getInt(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ULTRALIST)))
                domainContentValues.put(DomainsDatabaseHelper.ENABLE_ULTRAPRIVACY, domainsCursor.getInt(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_ULTRAPRIVACY)))
                domainContentValues.put(DomainsDatabaseHelper.BLOCK_ALL_THIRD_PARTY_REQUESTS, domainsCursor.getInt(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.BLOCK_ALL_THIRD_PARTY_REQUESTS)))
                domainContentValues.put(DomainsDatabaseHelper.USER_AGENT, domainsCursor.getString(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.USER_AGENT)))
                domainContentValues.put(DomainsDatabaseHelper.FONT_SIZE, domainsCursor.getInt(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.FONT_SIZE)))
                domainContentValues.put(DomainsDatabaseHelper.SWIPE_TO_REFRESH, domainsCursor.getInt(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SWIPE_TO_REFRESH)))
                domainContentValues.put(DomainsDatabaseHelper.WEBVIEW_THEME, domainsCursor.getInt(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.WEBVIEW_THEME)))
                domainContentValues.put(DomainsDatabaseHelper.WIDE_VIEWPORT, domainsCursor.getInt(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.WIDE_VIEWPORT)))
                domainContentValues.put(DomainsDatabaseHelper.DISPLAY_IMAGES, domainsCursor.getInt(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.DISPLAY_IMAGES)))
                domainContentValues.put(DomainsDatabaseHelper.PINNED_SSL_CERTIFICATE, domainsCursor.getInt(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.PINNED_SSL_CERTIFICATE)))
                domainContentValues.put(DomainsDatabaseHelper.SSL_ISSUED_TO_COMMON_NAME, domainsCursor.getString(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_TO_COMMON_NAME)))
                domainContentValues.put(DomainsDatabaseHelper.SSL_ISSUED_TO_ORGANIZATION, domainsCursor.getString(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_TO_ORGANIZATION)))
                domainContentValues.put(DomainsDatabaseHelper.SSL_ISSUED_TO_ORGANIZATIONAL_UNIT,
                    domainsCursor.getString(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_TO_ORGANIZATIONAL_UNIT)))
                domainContentValues.put(DomainsDatabaseHelper.SSL_ISSUED_BY_COMMON_NAME, domainsCursor.getString(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_BY_COMMON_NAME)))
                domainContentValues.put(DomainsDatabaseHelper.SSL_ISSUED_BY_ORGANIZATION, domainsCursor.getString(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_BY_ORGANIZATION)))
                domainContentValues.put(DomainsDatabaseHelper.SSL_ISSUED_BY_ORGANIZATIONAL_UNIT,
                    domainsCursor.getString(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_BY_ORGANIZATIONAL_UNIT)))
                domainContentValues.put(DomainsDatabaseHelper.SSL_START_DATE, domainsCursor.getLong(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_START_DATE)))
                domainContentValues.put(DomainsDatabaseHelper.SSL_END_DATE, domainsCursor.getLong(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_END_DATE)))
                domainContentValues.put(DomainsDatabaseHelper.PINNED_IP_ADDRESSES, domainsCursor.getInt(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.PINNED_IP_ADDRESSES)))
                domainContentValues.put(DomainsDatabaseHelper.IP_ADDRESSES, domainsCursor.getString(domainsCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.IP_ADDRESSES)))

                // Insert the content values into the temporary export database.
                temporaryExportDatabase.insert(DomainsDatabaseHelper.DOMAINS_TABLE, null, domainContentValues)

                // Advance to the next record.
                domainsCursor.moveToNext()
            }

            // Close the domains cursor and database.
            domainsCursor.close()
            domainsDatabaseHelper.close()


            // Prepare the preferences table SQL creation string.
            val createPreferencesTable = "CREATE TABLE " + PREFERENCES_TABLE + " (" +
                    ID + " INTEGER PRIMARY KEY, " +
                    JAVASCRIPT + " BOOLEAN, " +
                    COOKIES + " BOOLEAN, " +
                    DOM_STORAGE + " BOOLEAN, " +
                    SAVE_FORM_DATA + " BOOLEAN, " +
                    USER_AGENT + " TEXT, " +
                    CUSTOM_USER_AGENT + " TEXT, " +
                    INCOGNITO_MODE + " BOOLEAN, " +
                    ALLOW_SCREENSHOTS + " BOOLEAN, " +
                    EASYLIST + " BOOLEAN, " +
                    EASYPRIVACY + " BOOLEAN, " +
                    FANBOYS_ANNOYANCE_LIST + " BOOLEAN, " +
                    FANBOYS_SOCIAL_BLOCKING_LIST + " BOOLEAN, " +
                    ULTRALIST + " BOOLEAN, " +
                    ULTRAPRIVACY + " BOOLEAN, " +
                    BLOCK_ALL_THIRD_PARTY_REQUESTS + " BOOLEAN, " +
                    GOOGLE_ANALYTICS + " BOOLEAN, " +
                    FACEBOOK_CLICK_IDS + " BOOLEAN, " +
                    TWITTER_AMP_REDIRECTS + " BOOLEAN, " +
                    SEARCH + " TEXT, " +
                    SEARCH_CUSTOM_URL + " TEXT, " +
                    PROXY + " TEXT, " +
                    PROXY_CUSTOM_URL + " TEXT, " +
                    FULL_SCREEN_BROWSING_MODE + " BOOLEAN, " +
                    HIDE_APP_BAR + " BOOLEAN, " +
                    CLEAR_EVERYTHING + " BOOLEAN, " +
                    CLEAR_COOKIES + " BOOLEAN, " +
                    CLEAR_DOM_STORAGE + " BOOLEAN, " +
                    CLEAR_FORM_DATA + " BOOLEAN, " +
                    CLEAR_LOGCAT + " BOOLEAN, " +
                    CLEAR_CACHE + " BOOLEAN, " +
                    HOMEPAGE + " TEXT, " +
                    FONT_SIZE + " TEXT, " +
                    OPEN_INTENTS_IN_NEW_TAB + " BOOLEAN, " +
                    SWIPE_TO_REFRESH + " BOOLEAN, " +
                    DOWNLOAD_WITH_EXTERNAL_APP + " BOOLEAN, " +
                    SCROLL_APP_BAR + " BOOLEAN, " +
                    BOTTOM_APP_BAR + " BOOLEAN, " +
                    DISPLAY_ADDITIONAL_APP_BAR_ICONS + " BOOLEAN, " +
                    APP_THEME + " TEXT, " +
                    WEBVIEW_THEME + " TEXT, " +
                    WIDE_VIEWPORT + " BOOLEAN, " +
                    DISPLAY_WEBPAGE_IMAGES + " BOOLEAN)"

            // Create the temporary export database preferences table.
            temporaryExportDatabase.execSQL(createPreferencesTable)

            // Get a handle for the shared preference.
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

            // Create a preferences content values.
            val preferencesContentValues = ContentValues()

            // Populate the preferences content values.
            preferencesContentValues.put(JAVASCRIPT, sharedPreferences.getBoolean(JAVASCRIPT, false))
            preferencesContentValues.put(COOKIES, sharedPreferences.getBoolean(COOKIES, false))
            preferencesContentValues.put(DOM_STORAGE, sharedPreferences.getBoolean(DOM_STORAGE, false))
            preferencesContentValues.put(SAVE_FORM_DATA, sharedPreferences.getBoolean(SAVE_FORM_DATA, false))  // Save form data can be removed once the minimum API >= 26.
            preferencesContentValues.put(USER_AGENT, sharedPreferences.getString(USER_AGENT, context.getString(R.string.user_agent_default_value)))
            preferencesContentValues.put(CUSTOM_USER_AGENT, sharedPreferences.getString(CUSTOM_USER_AGENT, context.getString(R.string.custom_user_agent_default_value)))
            preferencesContentValues.put(INCOGNITO_MODE, sharedPreferences.getBoolean(INCOGNITO_MODE, false))
            preferencesContentValues.put(ALLOW_SCREENSHOTS, sharedPreferences.getBoolean(ALLOW_SCREENSHOTS, false))
            preferencesContentValues.put(EASYLIST, sharedPreferences.getBoolean(EASYLIST, true))
            preferencesContentValues.put(EASYPRIVACY, sharedPreferences.getBoolean(EASYPRIVACY, true))
            preferencesContentValues.put(FANBOYS_ANNOYANCE_LIST, sharedPreferences.getBoolean(FANBOYS_ANNOYANCE_LIST, true))
            preferencesContentValues.put(FANBOYS_SOCIAL_BLOCKING_LIST, sharedPreferences.getBoolean(FANBOYS_SOCIAL_BLOCKING_LIST, true))
            preferencesContentValues.put(ULTRALIST, sharedPreferences.getBoolean(ULTRALIST, true))
            preferencesContentValues.put(ULTRAPRIVACY, sharedPreferences.getBoolean(ULTRAPRIVACY, true))
            preferencesContentValues.put(BLOCK_ALL_THIRD_PARTY_REQUESTS, sharedPreferences.getBoolean(BLOCK_ALL_THIRD_PARTY_REQUESTS, false))
            preferencesContentValues.put(GOOGLE_ANALYTICS, sharedPreferences.getBoolean(GOOGLE_ANALYTICS, true))
            preferencesContentValues.put(FACEBOOK_CLICK_IDS, sharedPreferences.getBoolean(FACEBOOK_CLICK_IDS, true))
            preferencesContentValues.put(TWITTER_AMP_REDIRECTS, sharedPreferences.getBoolean(TWITTER_AMP_REDIRECTS, true))
            preferencesContentValues.put(SEARCH, sharedPreferences.getString(SEARCH, context.getString(R.string.search_default_value)))
            preferencesContentValues.put(SEARCH_CUSTOM_URL, sharedPreferences.getString(SEARCH_CUSTOM_URL, context.getString(R.string.search_custom_url_default_value)))
            preferencesContentValues.put(PROXY, sharedPreferences.getString(PROXY, context.getString(R.string.proxy_default_value)))
            preferencesContentValues.put(PROXY_CUSTOM_URL, sharedPreferences.getString(PROXY_CUSTOM_URL, context.getString(R.string.proxy_custom_url_default_value)))
            preferencesContentValues.put(FULL_SCREEN_BROWSING_MODE, sharedPreferences.getBoolean(FULL_SCREEN_BROWSING_MODE, false))
            preferencesContentValues.put(HIDE_APP_BAR, sharedPreferences.getBoolean(HIDE_APP_BAR, true))
            preferencesContentValues.put(CLEAR_EVERYTHING, sharedPreferences.getBoolean(CLEAR_EVERYTHING, true))
            preferencesContentValues.put(CLEAR_COOKIES, sharedPreferences.getBoolean(CLEAR_COOKIES, true))
            preferencesContentValues.put(CLEAR_DOM_STORAGE, sharedPreferences.getBoolean(CLEAR_DOM_STORAGE, true))
            preferencesContentValues.put(CLEAR_FORM_DATA, sharedPreferences.getBoolean(CLEAR_FORM_DATA, true))  // Clear form data can be removed once the minimum API >= 26.
            preferencesContentValues.put(CLEAR_LOGCAT, sharedPreferences.getBoolean(CLEAR_LOGCAT, true))
            preferencesContentValues.put(CLEAR_CACHE, sharedPreferences.getBoolean(CLEAR_CACHE, true))
            preferencesContentValues.put(HOMEPAGE, sharedPreferences.getString(HOMEPAGE, context.getString(R.string.homepage_default_value)))
            preferencesContentValues.put(FONT_SIZE, sharedPreferences.getString(FONT_SIZE, context.getString(R.string.font_size_default_value)))
            preferencesContentValues.put(OPEN_INTENTS_IN_NEW_TAB, sharedPreferences.getBoolean(OPEN_INTENTS_IN_NEW_TAB, true))
            preferencesContentValues.put(SWIPE_TO_REFRESH, sharedPreferences.getBoolean(SWIPE_TO_REFRESH, true))
            preferencesContentValues.put(DOWNLOAD_WITH_EXTERNAL_APP, sharedPreferences.getBoolean(DOWNLOAD_WITH_EXTERNAL_APP, false))
            preferencesContentValues.put(SCROLL_APP_BAR, sharedPreferences.getBoolean(SCROLL_APP_BAR, true))
            preferencesContentValues.put(BOTTOM_APP_BAR, sharedPreferences.getBoolean(BOTTOM_APP_BAR, false))
            preferencesContentValues.put(DISPLAY_ADDITIONAL_APP_BAR_ICONS, sharedPreferences.getBoolean(DISPLAY_ADDITIONAL_APP_BAR_ICONS, false))
            preferencesContentValues.put(APP_THEME, sharedPreferences.getString(APP_THEME, context.getString(R.string.app_theme_default_value)))
            preferencesContentValues.put(WEBVIEW_THEME, sharedPreferences.getString(WEBVIEW_THEME, context.getString(R.string.webview_theme_default_value)))
            preferencesContentValues.put(WIDE_VIEWPORT, sharedPreferences.getBoolean(WIDE_VIEWPORT, true))
            preferencesContentValues.put(DISPLAY_WEBPAGE_IMAGES, sharedPreferences.getBoolean(DISPLAY_WEBPAGE_IMAGES, true))

            // Insert the preferences content values into the temporary export database.
            temporaryExportDatabase.insert(PREFERENCES_TABLE, null, preferencesContentValues)

            // Close the temporary export database.
            temporaryExportDatabase.close()


            // The file may be copied directly in Kotlin using `File.copyTo`.  <https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.io/java.io.-file/copy-to.html>
            // It can be copied in Android using `Files.copy` once the minimum API >= 26.
            // <https://developer.android.com/reference/java/nio/file/Files#copy(java.nio.file.Path,%20java.nio.file.Path,%20java.nio.file.CopyOption...)>
            // However, the file cannot be acquired from the content URI until the minimum API >= 29.  <https://developer.android.com/reference/kotlin/android/content/ContentResolver#openfile>

            // Create the temporary export file input stream.
            val temporaryExportFileInputStream = FileInputStream(temporaryExportFile)

            // Create a byte array.
            val transferByteArray = ByteArray(1024)

            // Create an integer to track the number of bytes read.
            var bytesRead: Int

            // Copy the temporary export file to the export file output stream.
            while (temporaryExportFileInputStream.read(transferByteArray).also { bytesRead = it } > 0) {
                exportFileOutputStream.write(transferByteArray, 0, bytesRead)
            }

            // Flush the export file output stream.
            exportFileOutputStream.flush()

            // Close the file streams.
            temporaryExportFileInputStream.close()
            exportFileOutputStream.close()

            // Delete the temporary export file database, journal, and other related auxiliary files.
            SQLiteDatabase.deleteDatabase(temporaryExportFile)

            // Return the export successful string.
            EXPORT_SUCCESSFUL
        } catch (exception: Exception) {
            // Return the export error.
            exception.toString()
        }
    }
}