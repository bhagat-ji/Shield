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

package com.stoutner.privacybrowser.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.WindowManager;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import com.stoutner.privacybrowser.adapters.GuidePagerAdapter;
import com.stoutner.privacybrowser.R;

public class GuideActivity extends AppCompatActivity {
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

        // Set the content view.
        if (bottomAppBar) {
            setContentView(R.layout.guide_bottom_appbar);
        } else {
            setContentView(R.layout.guide_top_appbar);
        }

        // Get a handle for the toolbar.
        Toolbar toolbar = findViewById(R.id.guide_toolbar);

        // Set the support action bar.
        setSupportActionBar(toolbar);

        // Get a handle for the action bar.
        final ActionBar actionBar = getSupportActionBar();

        // Remove the incorrect lint warning that the action bar might be null.
        assert actionBar != null;

        // Display the home arrow on the action bar.
        actionBar.setDisplayHomeAsUpEnabled(true);

        //  Get a handle for the view pager and the tab layout.
        ViewPager aboutViewPager = findViewById(R.id.guide_viewpager);
        TabLayout aboutTabLayout = findViewById(R.id.guide_tablayout);

        // Remove the incorrect lint warnings that the views might be null
        assert aboutViewPager != null;
        assert aboutTabLayout != null;

        // Set the view pager adapter.
        aboutViewPager.setAdapter(new GuidePagerAdapter(getSupportFragmentManager(), getApplicationContext()));

        // Keep all the tabs in memory.
        aboutViewPager.setOffscreenPageLimit(10);

        // Link the tab layout to the view pager.
        aboutTabLayout.setupWithViewPager(aboutViewPager);
    }
}