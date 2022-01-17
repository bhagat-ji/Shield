/*
 * Copyright © 2019-2020,2022 Soren Stoutner <soren@stoutner.com>.
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

package com.stoutner.privacybrowser.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.stoutner.privacybrowser.R;
import com.stoutner.privacybrowser.views.NestedScrollWebView;

import java.util.Calendar;

public class WebViewTabFragment extends Fragment {
    // Set a unique ID for this tab based on the time it was created.
    public long fragmentId = Calendar.getInstance().getTimeInMillis();

    // The public interface is used to send information back to the parent activity.
    public interface NewTabListener {
        void initializeWebView(NestedScrollWebView nestedScrollWebView, int pageNumber, ProgressBar progressBar, String url, Boolean restoringState);
    }

    // The new tab listener is used in `onAttach()` and `onCreateView()`.
    private NewTabListener newTabListener;

    // Define the bundle constants.
    private final static String CREATE_NEW_PAGE = "create_new_page";
    private final static String PAGE_NUMBER = "page_number";
    private final static String URL = "url";
    private final static String SAVED_STATE = "saved_state";
    private final static String SAVED_NESTED_SCROLL_WEBVIEW_STATE = "saved_nested_scroll_webview_state";

    // Define the class views.
    NestedScrollWebView nestedScrollWebView;

    @Override
    public void onAttach(@NonNull Context context) {
        // Run the default commands.
        super.onAttach(context);

        // Get a handle for the new tab listener from the launching context.
        newTabListener = (NewTabListener) context;
    }

    public static WebViewTabFragment createPage(int pageNumber, String url) {
        // Create an arguments bundle.
        Bundle argumentsBundle = new Bundle();

        // Store the argument in the bundle.
        argumentsBundle.putBoolean(CREATE_NEW_PAGE, true);
        argumentsBundle.putInt(PAGE_NUMBER, pageNumber);
        argumentsBundle.putString(URL, url);

        // Create a new instance of the WebView tab fragment.
        WebViewTabFragment webViewTabFragment = new WebViewTabFragment();

        // Add the arguments bundle to the fragment.
        webViewTabFragment.setArguments(argumentsBundle);

        // Return the new fragment.
        return webViewTabFragment;
    }

    public static WebViewTabFragment restorePage(Bundle savedState, Bundle savedNestedScrollWebViewState) {
        // Create an arguments bundle
        Bundle argumentsBundle = new Bundle();

        // Store the saved states in the arguments bundle.
        argumentsBundle.putBundle(SAVED_STATE, savedState);
        argumentsBundle.putBundle(SAVED_NESTED_SCROLL_WEBVIEW_STATE, savedNestedScrollWebViewState);

        // Create a new instance of the WebView tab fragment.
        WebViewTabFragment webViewTabFragment = new WebViewTabFragment();

        // Add the arguments bundle to the fragment.
        webViewTabFragment.setArguments(argumentsBundle);

        // Return the new fragment.
        return webViewTabFragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        // Check to see if the fragment is being restarted.
        if (savedInstanceState == null) {  // The fragment is not being restarted.  Load and configure a new fragment.
            // Get the arguments.
            Bundle arguments = getArguments();

            // Remove the incorrect lint warning that the arguments might be null.
            assert arguments != null;

            // Check to see if a new page is being created.
            if (arguments.getBoolean(CREATE_NEW_PAGE)) {  // A new page is being created.
                // Get the variables from the arguments
                int pageNumber = arguments.getInt(PAGE_NUMBER);
                String url = arguments.getString(URL);

                // Inflate the tab's WebView.  Setting false at the end of inflater.inflate does not attach the inflated layout as a child of container.
                // The fragment will take care of attaching the root automatically.
                View newPageView = layoutInflater.inflate(R.layout.webview_framelayout, container, false);

                // Get handles for the views.
                nestedScrollWebView = newPageView.findViewById(R.id.nestedscroll_webview);
                ProgressBar progressBar = newPageView.findViewById(R.id.progress_bar);

                // Store the WebView fragment ID in the nested scroll WebView.
                nestedScrollWebView.setWebViewFragmentId(fragmentId);

                // Request the main activity initialize the WebView.
                newTabListener.initializeWebView(nestedScrollWebView, pageNumber, progressBar, url, false);

                // Return the new page view.
                return newPageView;
            } else {  // A page is being restored.
                // Get the saved states from the arguments.
                Bundle savedState = arguments.getBundle(SAVED_STATE);
                Bundle savedNestedScrollWebViewState = arguments.getBundle(SAVED_NESTED_SCROLL_WEBVIEW_STATE);

                // Remove the incorrect lint warning below that the saved nested scroll WebView state might be null.
                assert savedNestedScrollWebViewState != null;

                // Inflate the tab's WebView.  Setting false at the end of inflater.inflate does not attach the inflated layout as a child of container.
                // The fragment will take care of attaching the root automatically.
                View newPageView = layoutInflater.inflate(R.layout.webview_framelayout, container, false);

                // Get handles for the views.
                nestedScrollWebView = newPageView.findViewById(R.id.nestedscroll_webview);
                ProgressBar progressBar = newPageView.findViewById(R.id.progress_bar);

                // Store the WebView fragment ID in the nested scroll WebView.
                nestedScrollWebView.setWebViewFragmentId(fragmentId);

                // Restore the nested scroll WebView state.
                nestedScrollWebView.restoreNestedScrollWebViewState(savedNestedScrollWebViewState);

                // Restore the WebView state.
                nestedScrollWebView.restoreState(savedState);

                // Initialize the WebView.
                newTabListener.initializeWebView(nestedScrollWebView, 0, progressBar, null, true);

                // Return the new page view.
                return newPageView;
            }
        } else {  // The fragment is being restarted.
            // Return null.  Otherwise, the fragment will be inflated and initialized by the OS on a restart, discarded, and then recreated with saved settings by Privacy Browser.
            return null;
        }
    }
}