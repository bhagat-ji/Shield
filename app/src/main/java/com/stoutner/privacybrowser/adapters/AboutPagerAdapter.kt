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

package com.stoutner.privacybrowser.adapters

import android.content.Context

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.fragments.AboutVersionFragment
import com.stoutner.privacybrowser.fragments.AboutWebViewFragment

import java.util.LinkedList

class AboutPagerAdapter(fragmentManager: FragmentManager, private val context: Context, private val blocklistVersions: Array<String>) :
        FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    // Define the class variables.
    private val aboutFragmentList = LinkedList<Fragment>()

    // Get the number of tabs.
    override fun getCount(): Int {
        // There are seven tabs.
        return 7
    }

    // Get the name of each tab.  Tab numbers start at 0.
    override fun getPageTitle(tab: Int): CharSequence {
        return when (tab) {
            0 -> context.getString(R.string.version)
            1 -> context.getString(R.string.permissions)
            2 -> context.getString(R.string.privacy_policy)
            3 -> context.getString(R.string.changelog)
            4 -> context.getString(R.string.licenses)
            5 -> context.getString(R.string.contributors)
            6 -> context.getString(R.string.links)
            else -> ""
        }
    }

    // Setup each tab.
    override fun getItem(tabNumber: Int): Fragment {
        // Create the tab fragment and add it to the list.
        if (tabNumber == 0) {
            // Add the version tab to the list.
            aboutFragmentList.add(AboutVersionFragment.createTab(blocklistVersions))
        } else {
            // Add the WebView tab to the list.
            aboutFragmentList.add(AboutWebViewFragment.createTab(tabNumber))
        }

        // Return the tab fragment.
        return aboutFragmentList[tabNumber]
    }

    // Get a tab.
    fun getTabFragment(tabNumber: Int): Fragment {
        // Return the tab fragment.
        return aboutFragmentList[tabNumber]
    }
}