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

public class GuideTabFragment extends Fragment {
    // Define the class variables.
    private int tabNumber;
    private View tabLayout;

    // Store the tab number in the arguments bundle.
    public static GuideTabFragment createTab (int tabNumber) {
        // Create a bundle.
        Bundle bundle = new Bundle();

        // Store the tab number in the bundle.
        bundle.putInt("tab_number", tabNumber);

        // Create a new guide tab fragment.
        GuideTabFragment guideTabFragment = new GuideTabFragment();

        // Add the bundle to the fragment.
        guideTabFragment.setArguments(bundle);

        // Return the new fragment.
        return guideTabFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Run the default commands.
        super.onCreate(savedInstanceState);

        // Get a handle for the arguments.
        Bundle arguments = getArguments();

        // Remove the lint warning below that arguments might be null.
        assert arguments != null;

        // Store the tab number in a class variable.
        tabNumber = arguments.getInt("tab_number");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout.  The fragment will take care of attaching the root automatically.
        tabLayout = inflater.inflate(R.layout.bare_webview, container, false);

        // Get a handle for the tab WebView.
        WebView tabWebView = (WebView) tabLayout;

        // Get the current theme status.
        int currentThemeStatus = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        // Load the tabs according to the theme.
        if (currentThemeStatus == Configuration.UI_MODE_NIGHT_YES) {  // The dark theme is applied.
            tabWebView.setBackgroundColor(getResources().getColor(R.color.gray_850));

            // Tab numbers start at 0.
            switch (tabNumber) {
                case 0:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/guide_overview_dark.html");
                    break;

                case 1:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/guide_javascript_dark.html");
                    break;

                case 2:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/guide_local_storage_dark.html");
                    break;

                case 3:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/guide_user_agent_dark.html");
                    break;

                case 4:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/guide_requests_dark.html");
                    break;

                case 5:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/guide_domain_settings_dark.html");
                    break;

                case 6:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/guide_ssl_certificates_dark.html");
                    break;

                case 7:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/guide_proxies_dark.html");
                    break;

                case 8:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/guide_tracking_ids_dark.html");
                    break;
            }
        } else {  // The light theme is applied.
            // Tab numbers start at 0.
            switch (tabNumber) {
                case 0:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/guide_overview_light.html");
                    break;

                case 1:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/guide_javascript_light.html");
                    break;

                case 2:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/guide_local_storage_light.html");
                    break;

                case 3:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/guide_user_agent_light.html");
                    break;

                case 4:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/guide_requests_light.html");
                    break;

                case 5:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/guide_domain_settings_light.html");
                    break;

                case 6:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/guide_ssl_certificates_light.html");
                    break;

                case 7:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/guide_proxies_light.html");
                    break;

                case 8:
                    tabWebView.loadUrl("file:///android_asset/" + getString(R.string.android_asset_path) + "/guide_tracking_ids_light.html");
                    break;
            }
        }

        // Scroll the WebView if the saved instance state is not null.
        if (savedInstanceState != null) {
            tabWebView.post(() -> tabWebView.setScrollY(savedInstanceState.getInt("scroll_y")));
        }

        // Return the formatted `tabLayout`.
        return tabLayout;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        // Run the default commands.
        super.onSaveInstanceState(savedInstanceState);

        // Get a handle for the tab WebView.  A class variable cannot be used because it gets out of sync when restarting.
        WebView tabWebView = (WebView) tabLayout;

        // Save the scroll Y position if the tab WebView is not null, which can happen if a tab is not currently selected.
        if (tabWebView != null) {
            savedInstanceState.putInt("scroll_y", tabWebView.getScrollY());
        }
    }
}