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

package com.stoutner.privacybrowser.activities

import android.os.Bundle
import android.view.WindowManager

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.ViewPager

import com.google.android.material.tabs.TabLayout

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.adapters.GuidePagerAdapter

class GuideActivity : AppCompatActivity() {
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

        // Set the content view.
        if (bottomAppBar) {
            setContentView(R.layout.guide_bottom_appbar)
        } else {
            setContentView(R.layout.guide_top_appbar)
        }

        // Get handles for the views.
        val toolbar = findViewById<Toolbar>(R.id.guide_toolbar)
        val guideViewPager = findViewById<ViewPager>(R.id.guide_viewpager)
        val guideTabLayout = findViewById<TabLayout>(R.id.guide_tablayout)

        // Set the support action bar.
        setSupportActionBar(toolbar)

        // Get a handle for the action bar.
        val actionBar = supportActionBar!!

        // Display the home arrow on the action bar.
        actionBar.setDisplayHomeAsUpEnabled(true)

        // Initialize the guide pager adapter.
        val guidePagerAdapter = GuidePagerAdapter(supportFragmentManager, applicationContext)

        // Set the view pager adapter.
        guideViewPager.adapter = guidePagerAdapter

        // Keep all the tabs in memory.  This prevents the memory usage adapter from running multiple times.
        guideViewPager.offscreenPageLimit = 10

        // Link the tab layout to the view pager.
        guideTabLayout.setupWithViewPager(guideViewPager)
    }
}
