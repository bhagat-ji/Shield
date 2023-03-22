/*
 * Copyright 2019,2021-2023 Soren Stoutner <soren@stoutner.com>.
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

package com.stoutner.privacybrowser.coroutines

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.helpers.BlocklistHelper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.util.ArrayList

class PopulateBlocklistsCoroutine(context: Context) {
    // The public interface is used to send information back to the parent activity.
    interface PopulateBlocklistsListener {
        fun finishedPopulatingBlocklists(combinedBlocklists: ArrayList<ArrayList<List<Array<String>>>>)
    }

    // Define a populate blocklists listener.
    private val populateBlocklistsListener: PopulateBlocklistsListener

    // Define the class variables.
    private val context: Context

    // The public constructor.
    init {
        // Get a handle for the populate blocklists listener from the launching activity.
        populateBlocklistsListener = context as PopulateBlocklistsListener

        // Store the context.
        this.context = context
    }

    fun populateBlocklists(activity: Activity) {
        // Use a coroutine to populate the blocklists.
        CoroutineScope(Dispatchers.Main).launch {
            // Get handles for the views.
            val drawerLayout = activity.findViewById<DrawerLayout>(R.id.drawerlayout)
            val loadingBlocklistsRelativeLayout = activity.findViewById<RelativeLayout>(R.id.loading_blocklists_relativelayout)
            val loadingBlocklistTextView = activity.findViewById<TextView>(R.id.loading_blocklist_textview)

            // Show the loading blocklists screen.
            loadingBlocklistsRelativeLayout.visibility = View.VISIBLE

            // Instantiate the blocklist helper.
            val blocklistHelper = BlocklistHelper()

            // Create a combined array list.
            val combinedBlocklists = ArrayList<ArrayList<List<Array<String>>>>()

            // Advertise the loading of EasyList.
            loadingBlocklistTextView.text = context.getString(R.string.loading_easylist)

            // Populate the blocklists on the IO thread.
            withContext(Dispatchers.IO) {
                // Populate EasyList.
                val easyList = blocklistHelper.parseBlocklist(context.assets, "blocklists/easylist.txt")

                // Advertise the loading of EasyPrivacy.
                withContext(Dispatchers.Main) {
                    loadingBlocklistTextView.text = context.getString(R.string.loading_easyprivacy)
                }

                // Populate EasyPrivacy.
                val easyPrivacy = blocklistHelper.parseBlocklist(context.assets, "blocklists/easyprivacy.txt")

                // Advertise the loading of Fanboy's Annoyance List.
                withContext(Dispatchers.Main) {
                    loadingBlocklistTextView.text = context.getString(R.string.loading_fanboys_annoyance_list)
                }

                // Populate Fanboy's Annoyance List.
                val fanboysAnnoyanceList = blocklistHelper.parseBlocklist(context.assets, "blocklists/fanboy-annoyance.txt")

                // Advertise the loading of Fanboy's social blocking list.
                withContext(Dispatchers.Main) {
                    loadingBlocklistTextView.text = context.getString(R.string.loading_fanboys_social_blocking_list)
                }

                // Populate Fanboy's Social Blocking List.
                val fanboysSocialList = blocklistHelper.parseBlocklist(context.assets, "blocklists/fanboy-social.txt")

                // Advertise the loading of UltraList
                withContext(Dispatchers.Main) {
                    loadingBlocklistTextView.text = context.getString(R.string.loading_ultralist)
                }

                // Populate UltraList.
                val ultraList = blocklistHelper.parseBlocklist(context.assets, "blocklists/ultralist.txt")

                // Advertise the loading of UltraPrivacy.
                withContext(Dispatchers.Main) {
                    loadingBlocklistTextView.text = context.getString(R.string.loading_ultraprivacy)
                }

                // Populate UltraPrivacy.
                val ultraPrivacy = blocklistHelper.parseBlocklist(context.assets, "blocklists/ultraprivacy.txt")

                // Populate the combined array list.
                combinedBlocklists.add(easyList)
                combinedBlocklists.add(easyPrivacy)
                combinedBlocklists.add(fanboysAnnoyanceList)
                combinedBlocklists.add(fanboysSocialList)
                combinedBlocklists.add(ultraList)
                combinedBlocklists.add(ultraPrivacy)

                // Update the UI.
                withContext(Dispatchers.Main) {
                    // Show the drawer layout.
                    drawerLayout.visibility = View.VISIBLE

                    // Hide the loading blocklists screen.
                    loadingBlocklistsRelativeLayout.visibility = View.GONE

                    // Enable the sliding drawers.
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

                    // Add the first tab.
                    populateBlocklistsListener.finishedPopulatingBlocklists(combinedBlocklists)
                }
            }
        }
    }
}
