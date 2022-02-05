/*
 * Copyright © 2019-2022 Soren Stoutner <soren@stoutner.com>.
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

package com.stoutner.privacybrowser.adapters;

import android.os.Bundle;
import android.os.Handler;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.stoutner.privacybrowser.R;
import com.stoutner.privacybrowser.fragments.WebViewTabFragment;
import com.stoutner.privacybrowser.views.NestedScrollWebView;

import java.util.LinkedList;

public class WebViewPagerAdapter extends FragmentPagerAdapter {
    // The WebView fragments list contains all the WebViews.
    private final LinkedList<WebViewTabFragment> webViewFragmentsList = new LinkedList<>();

    // Define the constructor.
    public WebViewPagerAdapter(FragmentManager fragmentManager) {
        // Run the default commands.
        super(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    @Override
    public int getCount() {
        // Return the number of pages.
        return webViewFragmentsList.size();
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        //noinspection SuspiciousMethodCalls
        if (webViewFragmentsList.contains(object)) {
            // Return the current page position.
            //noinspection SuspiciousMethodCalls
            return webViewFragmentsList.indexOf(object);
        } else {
            // The tab has been deleted.
            return POSITION_NONE;
        }
    }

    @Override
    @NonNull
    public Fragment getItem(int pageNumber) {
        // Get the fragment for a particular page.  Page numbers are 0 indexed.
        return webViewFragmentsList.get(pageNumber);
    }

    @Override
    public long getItemId(int position) {
        // Return the unique ID for this page.
        return webViewFragmentsList.get(position).fragmentId;
    }

    public int getPositionForId(long fragmentId) {
        // Initialize the position variable.
        int position = -1;

        // Initialize the while counter.
        int i = 0;

        // Find the current position of the WebView fragment with the given ID.
        while (position < 0 && i < webViewFragmentsList.size()) {
            // Check to see if the tab ID of this WebView matches the page ID.
            if (webViewFragmentsList.get(i).fragmentId == fragmentId) {
                // Store the position if they are a match.
                position = i;
            }

            // Increment the counter.
            i++;
        }

        // Set the position to be the last tab if it is not found.
        // Sometimes there is a race condition in populating the webView fragments list when resuming Privacy Browser and displaying an SSL certificate error while loading a new intent.
        // In that case, the last tab should be the one it is looking for.
        if (position == -1) {
            position = webViewFragmentsList.size() - 1;
        }

        // Return the position.
        return position;
    }

    public void addPage(int pageNumber, ViewPager webViewPager, String url, boolean moveToNewPage) {
        // Add a new page.
        webViewFragmentsList.add(WebViewTabFragment.createPage(pageNumber, url));

        // Update the view pager.
        notifyDataSetChanged();

        // Move to the new page if indicated.
        if (moveToNewPage) {
            moveToNewPage(pageNumber, webViewPager);
        }
    }

    public void restorePage(Bundle savedState, Bundle savedNestedScrollWebViewState) {
        // Restore the page.
        webViewFragmentsList.add(WebViewTabFragment.restorePage(savedState, savedNestedScrollWebViewState));

        // Update the view pager.
        notifyDataSetChanged();
    }

    public boolean deletePage(int pageNumber, ViewPager webViewPager) {
        // Get the WebView tab fragment.
        WebViewTabFragment webViewTabFragment = webViewFragmentsList.get(pageNumber);

        // Get the WebView frame layout.
        FrameLayout webViewFrameLayout = (FrameLayout) webViewTabFragment.getView();

        // Remove the warning below that the WebView frame layout might be null.
        assert webViewFrameLayout != null;

        // Get a handle for the nested scroll WebView.
        NestedScrollWebView nestedScrollWebView = webViewFrameLayout.findViewById(R.id.nestedscroll_webview);

        // Pause the current WebView.
        nestedScrollWebView.onPause();

        // Remove all the views from the frame layout.
        webViewFrameLayout.removeAllViews();

        // Destroy the current WebView.
        nestedScrollWebView.destroy();

        // Delete the page.
        webViewFragmentsList.remove(pageNumber);

        // Update the view pager.
        notifyDataSetChanged();

        // Return true if the selected page number did not change after the delete (because the newly selected tab has has same number as the previously deleted tab).
        // This will cause the calling method to reset the current WebView to the new contents of this page number.
        return (webViewPager.getCurrentItem() == pageNumber);
    }

    public WebViewTabFragment getPageFragment(int pageNumber) {
        // Return the page fragment.
        return webViewFragmentsList.get(pageNumber);
    }

    private void moveToNewPage(int pageNumber, ViewPager webViewPager) {
        // Check to see if the new page has been populated.
        if (webViewPager.getChildCount() >= pageNumber) {  // The new page is ready.
            // Move to the new page.
            webViewPager.setCurrentItem(pageNumber);
        } else {  // The new page is not yet ready.
            // Create a handler.
            Handler moveToNewPageHandler = new Handler();

            // Create a runnable.
            Runnable moveToNewPageRunnable = () -> {
                // Move to the new page.
                webViewPager.setCurrentItem(pageNumber);
            };

            // Try again to move to the new page after 50 milliseconds.
            moveToNewPageHandler.postDelayed(moveToNewPageRunnable, 50);
        }
    }
}