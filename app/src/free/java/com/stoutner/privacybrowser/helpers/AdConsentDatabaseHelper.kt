/*
 * Copyright © 2018,2021 Soren Stoutner <soren@stoutner.com>.
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

package com.stoutner.privacybrowser.helpers

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Define the class constants.
private const val SCHEMA_VERSION = 1
private const val AD_CONSENT_DATABASE = "ad_consent.db"
private const val AD_CONSENT_TABLE = "ad_consent"
private const val ID = "_id"
private const val AD_CONSENT = "ad_consent"
private const val CREATE_AD_CONSENT_TABLE = "CREATE TABLE $AD_CONSENT_TABLE ($ID INTEGER PRIMARY KEY, $AD_CONSENT BOOLEAN)"

class AdConsentDatabaseHelper (context: Context) : SQLiteOpenHelper(context, AD_CONSENT_DATABASE, null, SCHEMA_VERSION) {
    override fun onCreate(adConsentDatabase: SQLiteDatabase) {
        // Create the ad consent table.
        adConsentDatabase.execSQL(CREATE_AD_CONSENT_TABLE)

        // Create an ad consent content values.
        val adConsentContentValues = ContentValues()

        // Populate the ad consent content values with the default data.
        adConsentContentValues.put(AD_CONSENT, false)

        // Insert a new row.  The second argument is `null`, which makes it so that a completely null row cannot be created.
        adConsentDatabase.insert(AD_CONSENT_TABLE, null, adConsentContentValues)
    }

    override fun onUpgrade(adConsentDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Code for upgrading the database will be added here if the schema version ever increases above 1.
    }

    // Check to see if ad consent has been granted.
    val isGranted: Boolean get() {
        // Get a readable database handle.
        val adConsentDatabase = this.readableDatabase

        // Get the ad consent cursor.
        val adConsentCursor = adConsentDatabase.rawQuery("SELECT * FROM $AD_CONSENT_TABLE", null)

        // Move the cursor to the first entry.
        adConsentCursor.moveToFirst()

        // Get the ad consent boolean.
        val adConsent = adConsentCursor.getInt(adConsentCursor.getColumnIndex(AD_CONSENT)) == 1

        // Close the cursor.
        adConsentCursor.close()

        // Close the database.
        adConsentDatabase.close()

        // Return the ad consent boolean.
        return adConsent
    }

    // Update the ad consent.
    fun updateAdConsent(adConsent: Boolean) {
        // Get a writable database handle.
        val adConsentDatabase = this.writableDatabase

        // Set the ad consent integer according to the boolean.
        val adConsentInt = if (adConsent) 1 else 0

        // Update the ad consent in the database.
        adConsentDatabase.execSQL("UPDATE $AD_CONSENT_TABLE SET $AD_CONSENT = $adConsentInt")

        // Close the database.
        adConsentDatabase.close()
    }
}