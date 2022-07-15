/*
 * Copyright © 2017-2022 Soren Stoutner <soren@stoutner.com>.
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

import androidx.preference.PreferenceManager

import com.stoutner.privacybrowser.R

// The private constants.
private const val SCHEMA_VERSION = 13

class DomainsDatabaseHelper(private val appContext: Context) : SQLiteOpenHelper(appContext, DOMAINS_DATABASE, null, SCHEMA_VERSION) {
    // Define the public companion object constants.  These can be moved to public class constants once the entire project has migrated to Kotlin.
    companion object {
        // The database constants.
        const val DOMAINS_DATABASE = "domains.db"
        const val DOMAINS_TABLE = "domains"

        // The spinner constants.
        const val SYSTEM_DEFAULT = 0
        const val ENABLED = 1
        const val DISABLED = 2
        const val LIGHT_THEME = 1
        const val DARK_THEME = 2

        // The schema constants.
        const val ID = "_id"
        const val DOMAIN_NAME = "domainname"
        const val ENABLE_JAVASCRIPT = "enablejavascript"
        const val COOKIES = "cookies"
        const val ENABLE_DOM_STORAGE = "enabledomstorage"
        const val ENABLE_FORM_DATA = "enableformdata" // Form data can be removed once the minimum API >= 26.
        const val ENABLE_EASYLIST = "enableeasylist"
        const val ENABLE_EASYPRIVACY = "enableeasyprivacy"
        const val ENABLE_FANBOYS_ANNOYANCE_LIST = "enablefanboysannoyancelist"
        const val ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST = "enablefanboyssocialblockinglist"
        const val ULTRALIST = "ultralist"
        const val ENABLE_ULTRAPRIVACY = "enableultraprivacy"
        const val BLOCK_ALL_THIRD_PARTY_REQUESTS = "blockallthirdpartyrequests"
        const val USER_AGENT = "useragent"
        const val FONT_SIZE = "fontsize"
        const val SWIPE_TO_REFRESH = "swipetorefresh"
        const val WEBVIEW_THEME = "webview_theme"
        const val WIDE_VIEWPORT = "wide_viewport"
        const val DISPLAY_IMAGES = "displayimages"
        const val PINNED_SSL_CERTIFICATE = "pinnedsslcertificate"
        const val SSL_ISSUED_TO_COMMON_NAME = "sslissuedtocommonname"
        const val SSL_ISSUED_TO_ORGANIZATION = "sslissuedtoorganization"
        const val SSL_ISSUED_TO_ORGANIZATIONAL_UNIT = "sslissuedtoorganizationalunit"
        const val SSL_ISSUED_BY_COMMON_NAME = "sslissuedbycommonname"
        const val SSL_ISSUED_BY_ORGANIZATION = "sslissuedbyorganization"
        const val SSL_ISSUED_BY_ORGANIZATIONAL_UNIT = "sslissuedbyorganizationalunit"
        const val SSL_START_DATE = "sslstartdate"
        const val SSL_END_DATE = "sslenddate"
        const val PINNED_IP_ADDRESSES = "pinned_ip_addresses"
        const val IP_ADDRESSES = "ip_addresses"

        // The table creation constant.
        const val CREATE_DOMAINS_TABLE = "CREATE TABLE $DOMAINS_TABLE (" +
                "$ID INTEGER PRIMARY KEY, " +
                "$DOMAIN_NAME TEXT, " +
                "$ENABLE_JAVASCRIPT BOOLEAN, " +
                "$COOKIES BOOLEAN, " +
                "$ENABLE_DOM_STORAGE BOOLEAN, " +
                "$ENABLE_FORM_DATA BOOLEAN, " +
                "$ENABLE_EASYLIST BOOLEAN, " +
                "$ENABLE_EASYPRIVACY BOOLEAN, " +
                "$ENABLE_FANBOYS_ANNOYANCE_LIST BOOLEAN, " +
                "$ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST BOOLEAN, " +
                "$ULTRALIST BOOLEAN, " +
                "$ENABLE_ULTRAPRIVACY BOOLEAN, " +
                "$BLOCK_ALL_THIRD_PARTY_REQUESTS BOOLEAN, " +
                "$USER_AGENT TEXT, " +
                "$FONT_SIZE INTEGER, " +
                "$SWIPE_TO_REFRESH INTEGER, " +
                "$WEBVIEW_THEME INTEGER, " +
                "$WIDE_VIEWPORT INTEGER, " +
                "$DISPLAY_IMAGES INTEGER, " +
                "$PINNED_SSL_CERTIFICATE BOOLEAN, " +
                "$SSL_ISSUED_TO_COMMON_NAME TEXT, " +
                "$SSL_ISSUED_TO_ORGANIZATION TEXT, " +
                "$SSL_ISSUED_TO_ORGANIZATIONAL_UNIT TEXT, " +
                "$SSL_ISSUED_BY_COMMON_NAME TEXT, " +
                "$SSL_ISSUED_BY_ORGANIZATION TEXT, " +
                "$SSL_ISSUED_BY_ORGANIZATIONAL_UNIT TEXT, " +
                "$SSL_START_DATE INTEGER, " +
                "$SSL_END_DATE INTEGER, " +
                "$PINNED_IP_ADDRESSES BOOLEAN, " +
                "$IP_ADDRESSES TEXT)"
    }

    override fun onCreate(domainsDatabase: SQLiteDatabase) {
        // Create the domains table.
        domainsDatabase.execSQL(CREATE_DOMAINS_TABLE)
    }

    override fun onUpgrade(domainsDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Upgrade from schema version 1, first used in Privacy Browser 2.0, to schema version 2, first used in Privacy Browser 2.3.
        if (oldVersion < 2) {
            // Add the display images column.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $DISPLAY_IMAGES INTEGER")
        }

        // Upgrade from schema version 2, first used in Privacy Browser 2.3, to schema version 3, first used in Privacy Browser 2.5.
        if (oldVersion < 3) {
            //  Add the SSL certificate columns.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $PINNED_SSL_CERTIFICATE BOOLEAN")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $SSL_ISSUED_TO_COMMON_NAME TEXT")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $SSL_ISSUED_TO_ORGANIZATION TEXT")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $SSL_ISSUED_TO_ORGANIZATIONAL_UNIT TEXT")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $SSL_ISSUED_BY_COMMON_NAME TEXT")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $SSL_ISSUED_BY_ORGANIZATION TEXT")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $SSL_ISSUED_BY_ORGANIZATIONAL_UNIT TEXT")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $SSL_START_DATE INTEGER")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $SSL_END_DATE INTEGER")
        }

        // Upgrade from schema version 3, first used in Privacy Browser 2.5, to schema version 4, first used in Privacy Browser 2.6.
        if (oldVersion < 4) {
            // Add the night mode column.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN nightmode INTEGER")
        }

        // Upgrade from schema version 4, first used in Privacy Browser 2.6, to schema version 5, first used in Privacy Browser 2.9.
        if (oldVersion < 5) {
            // Add the block lists columns.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $ENABLE_EASYLIST BOOLEAN")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $ENABLE_EASYPRIVACY BOOLEAN")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $ENABLE_FANBOYS_ANNOYANCE_LIST BOOLEAN")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST BOOLEAN")

            // Get a handle for the shared preference.
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)

            // Get the default block list settings.
            val easyListEnabled = sharedPreferences.getBoolean("easylist", true)
            val easyPrivacyEnabled = sharedPreferences.getBoolean("easyprivacy", true)
            val fanboyAnnoyanceListEnabled = sharedPreferences.getBoolean("fanboys_annoyance_list", true)
            val fanboySocialBlockingListEnabled = sharedPreferences.getBoolean("fanboys_social_blocking_list", true)

            // Set EasyList for existing rows according to the current system-wide default.
            // This can switch to using the variables directly once the API >= 30.  <https://www.sqlite.org/datatype3.html#boolean_datatype>
            // <https://developer.android.com/reference/android/database/sqlite/package-summary>
            if (easyListEnabled) {
                domainsDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $ENABLE_EASYLIST = 1")
            } else {
                domainsDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $ENABLE_EASYLIST = 0")
            }

            // Set EasyPrivacy for existing rows according to the current system-wide default.
            if (easyPrivacyEnabled) {
                domainsDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $ENABLE_EASYPRIVACY = 1")
            } else {
                domainsDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $ENABLE_EASYPRIVACY = 0")
            }

            // Set Fanboy's Annoyance List for existing rows according to the current system-wide default.
            if (fanboyAnnoyanceListEnabled) {
                domainsDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $ENABLE_FANBOYS_ANNOYANCE_LIST = 1")
            } else {
                domainsDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $ENABLE_FANBOYS_ANNOYANCE_LIST = 0")
            }

            // Set Fanboy's Social Blocking List for existing rows according to the current system-wide default.
            if (fanboySocialBlockingListEnabled) {
                domainsDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST = 1")
            } else {
                domainsDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST = 0")
            }
        }

        // Upgrade from schema version 5, first used in Privacy Browser 2.9, to schema version 6, first used in Privacy Browser 2.11.
        if (oldVersion < 6) {
            // Add the swipe to refresh column.  This defaults to `0`, which is `System default`, so a separate step isn't needed to populate the column.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $SWIPE_TO_REFRESH INTEGER")
        }

        // Upgrade from schema version 6, first used in Privacy Browser 2.11, to schema version 7, first used in Privacy Browser 2.12.
        if (oldVersion < 7) {
            // Add the block all third-party requests column.  This defaults to `0`, which is off, so a separate step isn't needed to populate the column.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $BLOCK_ALL_THIRD_PARTY_REQUESTS BOOLEAN")
        }

        // Upgrade from schema version 7, first used in Privacy Browser 2.12, to schema version 8, first used in Privacy Browser 2.12.
        // For some reason (lack of planning or attention to detail), the 2.12 update included two schema version jumps.
        if (oldVersion < 8) {
            // Add the UltraPrivacy column.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $ENABLE_ULTRAPRIVACY BOOLEAN")

            // Enable it for all existing rows.
            domainsDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $ENABLE_ULTRAPRIVACY = 1")
        }

        // Upgrade from schema version 8, first used in Privacy Browser 2.12, to schema version 9, first used in Privacy Browser 2.16.
        if (oldVersion < 9) {
            // Add the pinned IP addresses columns.  These default to `0` and `""`, so a separate step isn't needed to populate the columns.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $PINNED_IP_ADDRESSES BOOLEAN")
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $IP_ADDRESSES TEXT")
        }

        // Upgrade from schema version 9, first used in Privacy Browser 2.16, to schema version 10, first used in Privacy Browser 3.1.
        if (oldVersion < 10) {
            // Add the wide viewport column.  This defaults to `0`, which is `System default`, so a separate step isn't needed to populate the column.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $WIDE_VIEWPORT INTEGER")
        }

        // Upgrade from schema version 10, first used in Privacy Browser 3.1, to schema version 11, first used in Privacy Browser 3.2.
        if (oldVersion < 11) {
            // Add the UltraList column.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $ULTRALIST BOOLEAN")

            // Enable it for all existing rows.
            domainsDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $ULTRALIST = 1")
        }

        // Upgrade from schema version 11, first used in Privacy Browser 3.2, to schema version 12, first used in Privacy Browser 3.5.
        if (oldVersion < 12) {
            // Add the WebView theme column.  This defaults to `0`, which is `System default`, so a separate step isn't needed to populate the column.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $WEBVIEW_THEME INTEGER")

            // SQLite amazingly only added a command to drop a column in version 3.35.0.  <https://www.sqlite.org/changes.html>
            // It will be a while before that is supported in Android.  <https://developer.android.com/reference/android/database/sqlite/package-summary>
            // Although a new table could be created and all the data copied to it, I think I will just leave the old night mode column.  It will be wiped out the next time an import is run.
        }

        // Upgrade from schema version 12, first used in Privacy Browser 3.5, to schema version 13, first used in Privacy Browser 3.8.
        if (oldVersion < 13) {
            // Add the cookies column.
            domainsDatabase.execSQL("ALTER TABLE $DOMAINS_TABLE ADD COLUMN $COOKIES BOOLEAN")

            // Copy the data from the old column to the new one.
            domainsDatabase.execSQL("UPDATE $DOMAINS_TABLE SET $COOKIES = enablefirstpartycookies")
        }
    }

    val completeCursorOrderedByDomain: Cursor
        get() {
            // Get a readable database handle.
            val domainsDatabase = this.readableDatabase

            // Return everything in the domains table ordered by the domain name.  The cursor can't be closed because it is needed in the calling activity.
            return domainsDatabase.rawQuery("SELECT * FROM $DOMAINS_TABLE ORDER BY $DOMAIN_NAME ASC", null)
        }

    val domainNameCursorOrderedByDomain: Cursor
        get() {
            // Get a readable database handle.
            val domainsDatabase = this.readableDatabase

            // Return the database id and the domain name in the domains table ordered by the domain name.  The cursor can't be closed because it is needed in the calling activity.
            return domainsDatabase.rawQuery("SELECT $ID, $DOMAIN_NAME FROM $DOMAINS_TABLE ORDER BY $DOMAIN_NAME ASC", null)
        }

    fun getDomainNameCursorOrderedByDomainExcept(databaseId: Int): Cursor {
        // Get a readable database handle.
        val domainsDatabase = this.readableDatabase

        // Return a cursor with the database IDs and domain names except for the specified ID ordered by domain name.  The cursor can't be closed because it is needed in the calling activity.
        return domainsDatabase.rawQuery("SELECT $ID, $DOMAIN_NAME FROM $DOMAINS_TABLE WHERE $ID IS NOT $databaseId ORDER BY $DOMAIN_NAME ASC", null)
    }

    fun getCursorForId(databaseId: Int): Cursor {
        // Get a readable database handle.
        val domainsDatabase = this.readableDatabase

        // Return a cursor for the specified database ID.  The cursor can't be closed because it is needed in the calling activity.
        return domainsDatabase.rawQuery("SELECT * FROM $DOMAINS_TABLE WHERE $ID = $databaseId", null)
    }

    fun getCursorForDomainName(domainName: String): Cursor {
        // Get a readable database handle.
        val domainsDatabase = this.readableDatabase

        // SQL escape the domain name.
        val sqlEscapedDomainName = DatabaseUtils.sqlEscapeString(domainName)

        // Return a cursor for the requested domain name.  The cursor can't be closed because it is needed in the calling activity.
        return domainsDatabase.rawQuery("SELECT * FROM $DOMAINS_TABLE WHERE $DOMAIN_NAME = $sqlEscapedDomainName", null)
    }

    fun addDomain(domainName: String): Int {
        // Instantiate a content values.
        val domainContentValues = ContentValues()

        // Get a handle for the shared preference.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)

        // Get the default settings.
        val javaScript = sharedPreferences.getBoolean("javascript", false)
        val cookies = sharedPreferences.getBoolean(appContext.getString(R.string.cookies_key), false)
        val domStorage = sharedPreferences.getBoolean("dom_storage", false)
        val saveFormData = sharedPreferences.getBoolean("save_form_data", false) // Form data can be removed once the minimum API >= 26.
        val easyList = sharedPreferences.getBoolean("easylist", true)
        val easyPrivacy = sharedPreferences.getBoolean("easyprivacy", true)
        val fanboyAnnoyanceList = sharedPreferences.getBoolean("fanboys_annoyance_list", true)
        val fanboySocialBlockingList = sharedPreferences.getBoolean("fanboys_social_blocking_list", true)
        val ultraList = sharedPreferences.getBoolean("ultralist", true)
        val ultraPrivacy = sharedPreferences.getBoolean("ultraprivacy", true)
        val blockAllThirdPartyRequests = sharedPreferences.getBoolean("block_all_third_party_requests", false)

        // Create entries for the database fields.  The ID is created automatically.  The pinned SSL certificate information is not created unless added by the user.
        domainContentValues.put(DOMAIN_NAME, domainName)
        domainContentValues.put(ENABLE_JAVASCRIPT, javaScript)
        domainContentValues.put(COOKIES, cookies)
        domainContentValues.put(ENABLE_DOM_STORAGE, domStorage)
        domainContentValues.put(ENABLE_FORM_DATA, saveFormData) // Form data can be removed once the minimum API >= 26.
        domainContentValues.put(ENABLE_EASYLIST, easyList)
        domainContentValues.put(ENABLE_EASYPRIVACY, easyPrivacy)
        domainContentValues.put(ENABLE_FANBOYS_ANNOYANCE_LIST, fanboyAnnoyanceList)
        domainContentValues.put(ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST, fanboySocialBlockingList)
        domainContentValues.put(ULTRALIST, ultraList)
        domainContentValues.put(ENABLE_ULTRAPRIVACY, ultraPrivacy)
        domainContentValues.put(BLOCK_ALL_THIRD_PARTY_REQUESTS, blockAllThirdPartyRequests)
        domainContentValues.put(USER_AGENT, "System default user agent")
        domainContentValues.put(FONT_SIZE, 0)
        domainContentValues.put(SWIPE_TO_REFRESH, 0)
        domainContentValues.put(WEBVIEW_THEME, 0)
        domainContentValues.put(WIDE_VIEWPORT, 0)
        domainContentValues.put(DISPLAY_IMAGES, 0)

        // Get a writable database handle.
        val domainsDatabase = this.writableDatabase

        // Insert a new row and store the resulting database ID.
        val newDomainDatabaseId = domainsDatabase.insert(DOMAINS_TABLE, null, domainContentValues).toInt()

        // Close the database handle.
        domainsDatabase.close()

        // Return the new domain database ID.
        return newDomainDatabaseId
    }

    fun addDomain(contentValues: ContentValues) {
        // Get a writable database handle.
        val domainsDatabase = this.writableDatabase

        // Add the new domain.
        domainsDatabase.insert(DOMAINS_TABLE, null, contentValues)

        // Close the database handle.
        domainsDatabase.close()
    }

    fun updateDomain(databaseId: Int, domainName: String, javaScript: Boolean, cookies: Boolean, domStorage: Boolean, formData: Boolean, easyList: Boolean, easyPrivacy: Boolean, fanboysAnnoyance: Boolean,
                     fanboysSocialBlocking: Boolean, ultraList: Boolean, ultraPrivacy: Boolean, blockAllThirdPartyRequests: Boolean, userAgent: String, fontSize: Int, swipeToRefresh: Int, webViewTheme: Int,
                     wideViewport: Int, displayImages: Int, pinnedSslCertificate: Boolean, pinnedIpAddresses: Boolean) {
        // Instantiate a content values.
        val domainContentValues = ContentValues()

        // Add entries for each field in the database.
        domainContentValues.put(DOMAIN_NAME, domainName)
        domainContentValues.put(ENABLE_JAVASCRIPT, javaScript)
        domainContentValues.put(COOKIES, cookies)
        domainContentValues.put(ENABLE_DOM_STORAGE, domStorage)
        domainContentValues.put(ENABLE_FORM_DATA, formData) // Form data can be removed once the minimum API >= 26.
        domainContentValues.put(ENABLE_EASYLIST, easyList)
        domainContentValues.put(ENABLE_EASYPRIVACY, easyPrivacy)
        domainContentValues.put(ENABLE_FANBOYS_ANNOYANCE_LIST, fanboysAnnoyance)
        domainContentValues.put(ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST, fanboysSocialBlocking)
        domainContentValues.put(ULTRALIST, ultraList)
        domainContentValues.put(ENABLE_ULTRAPRIVACY, ultraPrivacy)
        domainContentValues.put(BLOCK_ALL_THIRD_PARTY_REQUESTS, blockAllThirdPartyRequests)
        domainContentValues.put(USER_AGENT, userAgent)
        domainContentValues.put(FONT_SIZE, fontSize)
        domainContentValues.put(SWIPE_TO_REFRESH, swipeToRefresh)
        domainContentValues.put(WEBVIEW_THEME, webViewTheme)
        domainContentValues.put(WIDE_VIEWPORT, wideViewport)
        domainContentValues.put(DISPLAY_IMAGES, displayImages)
        domainContentValues.put(PINNED_SSL_CERTIFICATE, pinnedSslCertificate)
        domainContentValues.put(PINNED_IP_ADDRESSES, pinnedIpAddresses)

        // Get a writable database handle.
        val domainsDatabase = this.writableDatabase

        // Update the row for the specified database ID.
        domainsDatabase.update(DOMAINS_TABLE, domainContentValues, "$ID = $databaseId", null)

        // Close the database handle.
        domainsDatabase.close()
    }

    fun updatePinnedSslCertificate(databaseId: Int, sslIssuedToCommonName: String, sslIssuedToOrganization: String, sslIssuedToOrganizationalUnit: String, sslIssuedByCommonName: String,
                                   sslIssuedByOrganization: String, sslIssuedByOrganizationalUnit: String, sslStartDate: Long, sslEndDate: Long) {
        // Instantiate a content values.
        val pinnedSslCertificateContentValues = ContentValues()

        // Add entries for each field in the certificate.
        pinnedSslCertificateContentValues.put(SSL_ISSUED_TO_COMMON_NAME, sslIssuedToCommonName)
        pinnedSslCertificateContentValues.put(SSL_ISSUED_TO_ORGANIZATION, sslIssuedToOrganization)
        pinnedSslCertificateContentValues.put(SSL_ISSUED_TO_ORGANIZATIONAL_UNIT, sslIssuedToOrganizationalUnit)
        pinnedSslCertificateContentValues.put(SSL_ISSUED_BY_COMMON_NAME, sslIssuedByCommonName)
        pinnedSslCertificateContentValues.put(SSL_ISSUED_BY_ORGANIZATION, sslIssuedByOrganization)
        pinnedSslCertificateContentValues.put(SSL_ISSUED_BY_ORGANIZATIONAL_UNIT, sslIssuedByOrganizationalUnit)
        pinnedSslCertificateContentValues.put(SSL_START_DATE, sslStartDate)
        pinnedSslCertificateContentValues.put(SSL_END_DATE, sslEndDate)

        // Get a writable database handle.
        val domainsDatabase = this.writableDatabase

        // Update the row for the specified database ID.
        domainsDatabase.update(DOMAINS_TABLE, pinnedSslCertificateContentValues, "$ID = $databaseId", null)

        // Close the database handle.
        domainsDatabase.close()
    }

    fun updatePinnedIpAddresses(databaseId: Int, ipAddresses: String) {
        // Instantiate a content values.
        val pinnedIpAddressesContentValues = ContentValues()

        // Add the IP addresses to the content values.
        pinnedIpAddressesContentValues.put(IP_ADDRESSES, ipAddresses)

        // Get a writable database handle.
        val domainsDatabase = this.writableDatabase

        // Update the row for the database ID.
        domainsDatabase.update(DOMAINS_TABLE, pinnedIpAddressesContentValues, "$ID = $databaseId", null)

        // Close the database handle.
        domainsDatabase.close()
    }

    fun deleteDomain(databaseId: Int) {
        // Get a writable database handle.
        val domainsDatabase = this.writableDatabase

        // Delete the row for the specified database ID.
        domainsDatabase.delete(DOMAINS_TABLE, "$ID = $databaseId", null)

        // Close the database handle.
        domainsDatabase.close()
    }
}