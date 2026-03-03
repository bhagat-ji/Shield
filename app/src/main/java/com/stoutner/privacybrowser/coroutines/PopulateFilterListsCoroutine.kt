/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2019, 2021-2026 Soren Stoutner <soren@stoutner.com>
 *
 * This file is part of Privacy Browser Android <https://www.stoutner.com/privacy-browser-android/>.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.stoutner.privacybrowser.coroutines

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.dataclasses.FilterList
import com.stoutner.privacybrowser.helpers.ParseFilterListHelper
import com.stoutner.privacybrowser.helpers.easyListDataClass
import com.stoutner.privacybrowser.helpers.easyPrivacyDataClass
import com.stoutner.privacybrowser.helpers.fanboysAnnoyanceDataClass
import com.stoutner.privacybrowser.helpers.ultraListDataClass
import com.stoutner.privacybrowser.helpers.ultraPrivacyDataClass

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Store the context as a local variable.
class PopulateFilterListsCoroutine(private val context: Context) {
    // The public interface is used to send information back to the parent activity.
    interface PopulateFilterListsListener {
        fun finishedPopulatingFilterLists()
    }

    // Get a handle for the populate filter lists listener from the launching activity.
    private val populateFilterListsListener: PopulateFilterListsListener = context as PopulateFilterListsListener

    fun populateFilterLists(activity: Activity) {
        // Use a coroutine to populate the filter lists.
        CoroutineScope(Dispatchers.Main).launch {
            // Get handles for the views.
            val drawerLayout = activity.findViewById<DrawerLayout>(R.id.drawerlayout)
            val loadingFilterListsRelativeLayout = activity.findViewById<RelativeLayout>(R.id.loading_filterlists_relativelayout)
            val loadingFilterListTextView = activity.findViewById<TextView>(R.id.loading_filterlist_textview)

            // Show the loading filter lists screen.
            loadingFilterListsRelativeLayout.visibility = View.VISIBLE

            // Instantiate the filter list helper.
            val parseFilterListHelper = ParseFilterListHelper()

            // Advertise the loading of EasyList.
            loadingFilterListTextView.text = context.getString(R.string.loading_easylist)

            // Populate the filter lists on the IO thread.
            withContext(Dispatchers.IO) {
                // Advertise the loading of UltraPrivacy.
                withContext(Dispatchers.Main) {
                    loadingFilterListTextView.text = context.getString(R.string.loading_ultraprivacy)
                }

                // Populate UltraPrivacy.
                ultraPrivacyDataClass = parseFilterListHelper.parseFilterList(context.assets, "filterlists/ultraprivacy.txt", FilterList.UltraPrivacy)

                // Advertise the loading of UltraList
                withContext(Dispatchers.Main) {
                    loadingFilterListTextView.text = context.getString(R.string.loading_ultralist)
                }

                // Populate UltraList.
                ultraListDataClass = parseFilterListHelper.parseFilterList(context.assets, "filterlists/ultralist.txt", FilterList.UltraList)

                // Advertise the loading of EasyPrivacy.
                withContext(Dispatchers.Main) {
                    loadingFilterListTextView.text = context.getString(R.string.loading_easyprivacy)
                }

                // Populate EasyPrivacy.
                easyPrivacyDataClass = parseFilterListHelper.parseFilterList(context.assets, "filterlists/easyprivacy.txt", FilterList.EasyPrivacy)

                // Advertise the loading of EasyList.
                withContext(Dispatchers.Main) {
                    loadingFilterListTextView.text = context.getString(R.string.loading_easylist)
                }

                // Populate EasyList.
                easyListDataClass = parseFilterListHelper.parseFilterList(context.assets, "filterlists/easylist.txt", FilterList.EasyList)

                // Advertise the loading of Fanboy's Annoyance List.
                withContext(Dispatchers.Main) {
                    loadingFilterListTextView.text = context.getString(R.string.loading_fanboys_annoyance_list)
                }

                // Populate Fanboy's Annoyance List.
                fanboysAnnoyanceDataClass = parseFilterListHelper.parseFilterList(context.assets, "filterlists/fanboy-annoyance.txt", FilterList.FanboysAnnoyanceList)

                // Update the UI.
                withContext(Dispatchers.Main) {
                    // Show the drawer layout.
                    drawerLayout.visibility = View.VISIBLE

                    // Hide the loading filter lists screen.
                    loadingFilterListsRelativeLayout.visibility = View.GONE

                    // Enable the sliding drawers.
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

                    // Continue loading the app.
                    populateFilterListsListener.finishedPopulatingFilterLists()
                }
            }
        }
    }
}
