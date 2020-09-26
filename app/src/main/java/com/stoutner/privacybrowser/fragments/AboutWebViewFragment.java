/*
 * Copyright © 2016-2020 Soren Stoutner <soren@stoutner.com>.
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

package com.stoutner.privacybrowser.fragments;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.stoutner.privacybrowser.R;

public class AboutWebViewFragment extends Fragment {
    // Declare the class constants.
    final static String TAB_NUMBER = "tab_number";

    // Declare the class variables.
    private int tabNumber;

    // Declare the class views.
    private View aboutWebViewLayout;

    public static AboutWebViewFragment createTab(int tabNumber) {
        // Create an arguments bundle.
        Bundle argumentsBundle = new Bundle();

        // Store the arguments in the bundle.
        argumentsBundle.putInt(TAB_NUMBER, tabNumber);

        // Create a new instance of the tab fragment.
        AboutWebViewFragment aboutWebViewFragment = new AboutWebViewFragment();

        // Add the arguments bundle to the fragment.
        aboutWebViewFragment.setArguments(argumentsBundle);

        // Return the new fragment.
        return aboutWebViewFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Run the default commands.
        super.onCreate(savedInstanceState);

        // Get a handle for the arguments.
        Bundle arguments = getArguments();

        // Remove the incorrect lint warning below that arguments might be null.
        assert arguments != null;

        // Store the arguments in class variables.
        tabNumber = arguments.getInt(TAB_NUMBER);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        // Get the current theme status.
        int currentThemeStatus = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        // Inflate the layout.  Setting false at the end of inflater.inflate does not attach the inflated layout as a child of container.  The fragment will take care of attaching the root automatically.
        aboutWebViewLayout = layoutInflater.inflate(R.layout.bare_webview, container, false);

        // Get a handle for tab WebView.
        WebView tabWebView = (WebView) aboutWebViewLayout;

        // Load the tabs according to the theme.
        if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {  // The light theme is applied.
            switch (tabNumber) {
                case 1:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/about_permissions_light.html");
                    break;

                case 2:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/about_privacy_policy_light.html");
                    break;

                case 3:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/about_changelog_light.html");
                    break;

                case 4:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/about_licenses_light.html");
                    break;

                case 5:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/about_contributors_light.html");
                    break;

                case 6:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/about_links_light.html");
                    break;
            }
        } else {  // The dark theme is applied.
            // Set the background color.  The deprecated `.getColor()` must be used until the minimum API >= 23.
            tabWebView.setBackgroundColor(getResources().getColor(R.color.gray_850));

            // Tab numbers start at 0, with the WebView tabs starting at 1.
            switch (tabNumber) {
                case 1:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/about_permissions_dark.html");
                    break;

                case 2:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/about_privacy_policy_dark.html");
                    break;

                case 3:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/about_changelog_dark.html");
                    break;

                case 4:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/about_licenses_dark.html");
                    break;

                case 5:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/about_contributors_dark.html");
                    break;

                case 6:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/about_links_dark.html");
                    break;
            }
        }

        // Scroll the tab if the saved instance state is not null.
        if (savedInstanceState != null) {
            aboutWebViewLayout.post(() -> {
                aboutWebViewLayout.setScrollX(savedInstanceState.getInt("scroll_x"));
                aboutWebViewLayout.setScrollY(savedInstanceState.getInt("scroll_y"));
            });
        }

        // Return the tab layout.
        return aboutWebViewLayout;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        // Run the default commands.
        super.onSaveInstanceState(savedInstanceState);

        // Save the scroll positions if the layout is not null, which can happen if a tab is not currently selected.
        if (aboutWebViewLayout != null) {
            savedInstanceState.putInt("scroll_x", aboutWebViewLayout.getScrollX());
            savedInstanceState.putInt("scroll_y", aboutWebViewLayout.getScrollY());
        }
    }
}