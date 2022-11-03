/*
 * Copyright 2016-2020,2022 Soren Stoutner <soren@stoutner.com>.
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
import com.stoutner.privacybrowser.fragments.GuideWebViewFragment

class GuidePagerAdapter(fragmentManager: FragmentManager, private val context: Context) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    // Get the count of the number of tabs.
    override fun getCount(): Int {
        return 10
    }

    // Get the name of each tab.  Tab numbers start at 0.
    override fun getPageTitle(tab: Int): CharSequence {
        return when (tab) {
            0 -> context.getString(R.string.overview)
            1 -> context.getString(R.string.javascript)
            2 -> context.getString(R.string.local_storage)
            3 -> context.getString(R.string.user_agent)
            4 -> context.getString(R.string.requests)
            5 -> context.getString(R.string.domain_settings)
            6 -> context.getString(R.string.ssl_certificates)
            7 -> context.getString(R.string.proxies)
            8 -> context.getString(R.string.tracking_ids)
            9 -> context.getString(R.string.gui)
            else -> ""
        }
    }

    // Setup each tab.
    override fun getItem(tabNumber: Int): Fragment {
        return GuideWebViewFragment.createTab(tabNumber)
    }
}
