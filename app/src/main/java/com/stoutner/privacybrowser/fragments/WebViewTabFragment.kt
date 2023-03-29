/*
 * Copyright © 2019-2020,2022-2023 Soren Stoutner <soren@stoutner.com>.
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

package com.stoutner.privacybrowser.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar

import androidx.fragment.app.Fragment

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.views.NestedScrollWebView

import java.util.Calendar

// Define the class constants.
private const val CREATE_NEW_PAGE = "create_new_page"
private const val PAGE_NUMBER = "page_number"
private const val URL = "url"
private const val SAVED_STATE = "saved_state"
private const val SAVED_NESTED_SCROLL_WEBVIEW_STATE = "saved_nested_scroll_webview_state"

class WebViewTabFragment : Fragment() {
    // Define the public variables.
    var fragmentId = Calendar.getInstance().timeInMillis

    // The public interface is used to send information back to the parent activity.
    interface NewTabListener {
        @SuppressLint("ClickableViewAccessibility")
        fun initializeWebView(nestedScrollWebView: NestedScrollWebView, pageNumber: Int, progressBar: ProgressBar, url: String, restoringState: Boolean)
    }

    // Declare the class variables.
    private lateinit var newTabListener: NewTabListener

    // Declare the class views.
    private lateinit var nestedScrollWebView: NestedScrollWebView

    companion object {
        fun createPage(pageNumber: Int, url: String?): WebViewTabFragment {
            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the argument in the bundle.
            argumentsBundle.putBoolean(CREATE_NEW_PAGE, true)
            argumentsBundle.putInt(PAGE_NUMBER, pageNumber)
            argumentsBundle.putString(URL, url)

            // Create a new instance of the WebView tab fragment.
            val webViewTabFragment = WebViewTabFragment()

            // Add the arguments bundle to the fragment.
            webViewTabFragment.arguments = argumentsBundle

            // Return the new fragment.
            return webViewTabFragment
        }

        fun restorePage(savedState: Bundle, savedNestedScrollWebViewState: Bundle): WebViewTabFragment {
            // Create an arguments bundle
            val argumentsBundle = Bundle()

            // Store the saved states in the arguments bundle.
            argumentsBundle.putBundle(SAVED_STATE, savedState)
            argumentsBundle.putBundle(SAVED_NESTED_SCROLL_WEBVIEW_STATE, savedNestedScrollWebViewState)

            // Create a new instance of the WebView tab fragment.
            val webViewTabFragment = WebViewTabFragment()

            // Add the arguments bundle to the fragment.
            webViewTabFragment.arguments = argumentsBundle

            // Return the new fragment.
            return webViewTabFragment
        }
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Get a handle for the new tab listener from the launching context.
        newTabListener = context as NewTabListener
    }

    override fun onCreateView(layoutInflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Check to see if the fragment is being restarted without the app being killed.
        return if (savedInstanceState == null) {  // The fragment is not being restarted.  It is either new or is being restored after the app was killed.
            // Check to see if a new page is being created.
            if (requireArguments().getBoolean(CREATE_NEW_PAGE)) {  // A new page is being created.
                // Get the variables from the arguments
                val pageNumber = requireArguments().getInt(PAGE_NUMBER)
                val url = requireArguments().getString(URL)!!

                // Inflate the tab's WebView.  Setting false at the end of inflater.inflate does not attach the inflated layout as a child of container.
                // The fragment will take care of attaching the root automatically.
                val newPageView = layoutInflater.inflate(R.layout.webview_framelayout, container, false)

                // Get handles for the views.
                nestedScrollWebView = newPageView.findViewById(R.id.nestedscroll_webview)
                val progressBar = newPageView.findViewById<ProgressBar>(R.id.progress_bar)

                // Store the WebView fragment ID in the nested scroll WebView.
                nestedScrollWebView.webViewFragmentId = fragmentId

                // Request the main activity initialize the WebView.
                newTabListener.initializeWebView(nestedScrollWebView, pageNumber, progressBar, url, false)

                // Return the new page view.
                newPageView
            } else {  // A page is being restored after the app was killed.
                // Get the saved states from the arguments.
                val savedState = requireArguments().getBundle(SAVED_STATE)!!
                val savedNestedScrollWebViewState = requireArguments().getBundle(SAVED_NESTED_SCROLL_WEBVIEW_STATE)!!

                // Inflate the tab's WebView.  Setting false at the end of inflater.inflate does not attach the inflated layout as a child of container.
                // The fragment will take care of attaching the root automatically.
                val newPageView = layoutInflater.inflate(R.layout.webview_framelayout, container, false)

                // Get handles for the views.
                nestedScrollWebView = newPageView.findViewById(R.id.nestedscroll_webview)
                val progressBar = newPageView.findViewById<ProgressBar>(R.id.progress_bar)

                // Store the WebView fragment ID in the nested scroll WebView.
                nestedScrollWebView.webViewFragmentId = fragmentId

                // Restore the nested scroll WebView state.
                nestedScrollWebView.restoreNestedScrollWebViewState(savedNestedScrollWebViewState)

                // Restore the WebView state.
                nestedScrollWebView.restoreState(savedState)

                // Initialize the WebView.
                newTabListener.initializeWebView(nestedScrollWebView, 0, progressBar, "", true)

                // Return the new page view.
                newPageView
            }
        } else {  // The fragment is being restarted.
            // Return null.  Otherwise, the fragment will be inflated and initialized by the OS on a restart, discarded, and then recreated with saved settings by Privacy Browser.
            null
        }
    }
}
