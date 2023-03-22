/*
 * Copyright © 2019-2023 Soren Stoutner <soren@stoutner.com>.
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

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.fragments.WebViewTabFragment
import com.stoutner.privacybrowser.fragments.WebViewTabFragment.Companion.createPage
import com.stoutner.privacybrowser.views.NestedScrollWebView

import java.util.LinkedList

class WebViewPagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    // Define the class values.
    private val webViewFragmentsList = LinkedList<WebViewTabFragment>()

    override fun getCount(): Int {
        // Return the number of pages.
        return webViewFragmentsList.size
    }

    override fun getItem(pageNumber: Int): Fragment {
        // Get the fragment for a particular page.  Page numbers are 0 indexed.
        return webViewFragmentsList[pageNumber]
    }

    override fun getItemId(position: Int): Long {
        // Return the unique ID for this page.
        return webViewFragmentsList[position].fragmentId
    }

    override fun getItemPosition(`object`: Any): Int {
        return if (webViewFragmentsList.contains(`object`)) {
            // Return the current page position.
            webViewFragmentsList.indexOf(`object`)
        } else {
            // The tab has been deleted.
            POSITION_NONE
        }
    }

    fun addPage(pageNumber: Int, webViewPager: ViewPager, url: String, moveToNewPage: Boolean) {
        // Add a new page.
        webViewFragmentsList.add(createPage(pageNumber, url))

        // Update the view pager.
        notifyDataSetChanged()

        // Move to the new page if indicated.
        if (moveToNewPage) {
            moveToNewPage(pageNumber, webViewPager)
        }
    }

    fun deletePage(pageNumber: Int, webViewPager: ViewPager): Boolean {
        // Get the WebView tab fragment.
        val webViewTabFragment = webViewFragmentsList[pageNumber]

        // Get the WebView frame layout.
        val webViewFrameLayout = (webViewTabFragment.view as FrameLayout?)!!

        // Get a handle for the nested scroll WebView.
        val nestedScrollWebView = webViewFrameLayout.findViewById<NestedScrollWebView>(R.id.nestedscroll_webview)

        // Pause the current WebView.
        nestedScrollWebView.onPause()

        // Remove all the views from the frame layout.
        webViewFrameLayout.removeAllViews()

        // Destroy the current WebView.
        nestedScrollWebView.destroy()

        // Delete the page.
        webViewFragmentsList.removeAt(pageNumber)

        // Update the view pager.
        notifyDataSetChanged()

        // Return true if the selected page number did not change after the delete (because the newly selected tab has has same number as the previously deleted tab).
        // This will cause the calling method to reset the current WebView to the new contents of this page number.
        return webViewPager.currentItem == pageNumber
    }

    fun getPageFragment(pageNumber: Int): WebViewTabFragment {
        // Return the page fragment.
        return webViewFragmentsList[pageNumber]
    }

    fun getPositionForId(fragmentId: Long): Int {
        // Initialize the position variable.
        var position = -1

        // Initialize the while counter.
        var i = 0

        // Find the current position of the WebView fragment with the given ID.
        while (position < 0 && i < webViewFragmentsList.size) {
            // Check to see if the tab ID of this WebView matches the page ID.
            if (webViewFragmentsList[i].fragmentId == fragmentId) {
                // Store the position if they are a match.
                position = i
            }

            // Increment the counter.
            i++
        }

        // Set the position to be the last tab if it is not found.
        // Sometimes there is a race condition in populating the webView fragments list when resuming Privacy Browser and displaying an SSL certificate error while loading a new intent.
        // In that case, the last tab should be the one it is looking for.
        if (position == -1) {
            position = webViewFragmentsList.size - 1
        }

        // Return the position.
        return position
    }

    fun restorePage(savedState: Bundle, savedNestedScrollWebViewState: Bundle) {
        // Restore the page.
        webViewFragmentsList.add(WebViewTabFragment.restorePage(savedState, savedNestedScrollWebViewState))

        // Update the view pager.
        notifyDataSetChanged()
    }

    private fun moveToNewPage(pageNumber: Int, webViewPager: ViewPager) {
        // Check to see if the new page has been populated.
        if (webViewPager.childCount >= pageNumber) {  // The new page is ready.
            // Move to the new page.
            webViewPager.currentItem = pageNumber
        } else {  // The new page is not yet ready.
            // Create a handler.
            val moveToNewPageHandler = Handler(Looper.getMainLooper())

            // Create a runnable.
            val moveToNewPageRunnable = Runnable {
                // Move to the new page.
                webViewPager.currentItem = pageNumber
            }

            // Try again to move to the new page after 50 milliseconds.
            moveToNewPageHandler.postDelayed(moveToNewPageRunnable, 50)
        }
    }
}
