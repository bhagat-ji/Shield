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

package com.stoutner.privacybrowser.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import android.widget.TextView

import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.activities.MainWebViewActivity
import com.stoutner.privacybrowser.adapters.HistoryArrayAdapter
import com.stoutner.privacybrowser.dataclasses.History
import com.stoutner.privacybrowser.views.NestedScrollWebView

// Define the class constants.
private const val WEBVIEW_FRAGMENT_ID = "webview_fragment_id"

class UrlHistoryDialog : DialogFragment() {
    // Declare the class variables.
    private lateinit var navigateHistoryListener: NavigateHistoryListener

    // The public interface is used to send information back to the parent activity.
    interface NavigateHistoryListener {
        fun navigateHistory(url: String, steps: Int)
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Get a handle for the listener from the launching context.
        navigateHistoryListener = context as NavigateHistoryListener
    }

    companion object {
        // `@JvmStatic` will no longer be required once all the code has transitioned to Kotlin.
        @JvmStatic
        fun loadBackForwardList(webViewFragmentId: Long): UrlHistoryDialog {
            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the WebView fragment ID in the bundle.
            argumentsBundle.putLong(WEBVIEW_FRAGMENT_ID, webViewFragmentId)

            // Create a new instance of the URL history dialog.
            val urlHistoryDialog = UrlHistoryDialog()

            // Add the arguments bundle to the new dialog.
            urlHistoryDialog.arguments = argumentsBundle

            // Return the new dialog.
            return urlHistoryDialog
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get the WebView fragment ID from the arguments.
        val webViewFragmentId = requireArguments().getLong(WEBVIEW_FRAGMENT_ID)

        // Get the current position of this WebView fragment.
        val webViewPosition = MainWebViewActivity.webViewPagerAdapter.getPositionForId(webViewFragmentId)

        // Get the WebView tab fragment.
        val webViewTabFragment = MainWebViewActivity.webViewPagerAdapter.getPageFragment(webViewPosition)

        // Get the fragment view.
        val fragmentView = webViewTabFragment.requireView()

        // Get a handle for the current nested scroll WebView.
        val nestedScrollWebView: NestedScrollWebView = fragmentView.findViewById(R.id.nestedscroll_webview)

        // Get the web back forward list from the nested scroll WebView.
        val webBackForwardList = nestedScrollWebView.copyBackForwardList()

        // Store the current page index.
        val currentPageIndex = webBackForwardList.currentIndex

        // Get the default favorite icon drawable.  `ContextCompat` must be used until the minimum API >= 21.
        val defaultFavoriteIconDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.world)

        // Convert the default favorite icon drawable to a bitmap drawable.
        val defaultFavoriteIconBitmapDrawable = (defaultFavoriteIconDrawable as BitmapDrawable)

        // Extract a bitmap from the default favorite icon bitmap drawable.
        val defaultFavoriteIcon = defaultFavoriteIconBitmapDrawable.bitmap

        // Create a history array list.
        val historyArrayList = ArrayList<History>()

        // Populate the history array list, descending from the end of the list so that the newest entries are at the top.  `-1` is needed because the history array list is zero-based.
        for (i in webBackForwardList.size - 1 downTo 0) {
            // Store the favorite icon bitmap.
            val favoriteIconBitmap = if (webBackForwardList.getItemAtIndex(i).favicon == null) {
                // If the web back forward list does not have a favorite icon, use Privacy Browser's default world icon.
                defaultFavoriteIcon
            } else {  // Use the icon from the web back forward list.
                webBackForwardList.getItemAtIndex(i).favicon
            }

            // Store the favorite icon and the URL in history entry.
            val historyEntry = History(favoriteIconBitmap!!, webBackForwardList.getItemAtIndex(i).url)

            // Add this history entry to the history array list.
            historyArrayList.add(historyEntry)
        }

        // Subtract the original current page ID from the array size because the order of the array is reversed so that the newest entries are at the top.  `-1` is needed because the array is zero-based.
        val currentPageId = webBackForwardList.size - 1 - currentPageIndex

        // Use an alert dialog builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.PrivacyBrowserAlertDialog)

        // Set the title.
        dialogBuilder.setTitle(R.string.history)

        // Set the view.
        dialogBuilder.setView(R.layout.url_history_dialog)

        // Setup the clear history button listener.
        dialogBuilder.setNegativeButton(R.string.clear_history) { _: DialogInterface, _: Int ->
            // Clear the history.
            nestedScrollWebView.clearHistory()
        }

        // Set the close button listener.  Using `null` as the listener closes the dialog without doing anything else.
        dialogBuilder.setPositiveButton(R.string.close, null)

        // Create an alert dialog from the alert dialog builder.
        val alertDialog = dialogBuilder.create()

        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Get the screenshot preference.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)

        // Disable screenshots if not allowed.
        if (!allowScreenshots) {
            // Disable screenshots.
            alertDialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        //The alert dialog must be shown before the contents can be modified.
        alertDialog.show()

        // Instantiate a history array adapter.
        val historyArrayAdapter = HistoryArrayAdapter(context, historyArrayList, currentPageId)

        // Get a handle for the list view.
        val listView = alertDialog.findViewById<ListView>(R.id.history_listview)!!

        // Set the list view adapter.
        listView.adapter = historyArrayAdapter

        // Listen for clicks on entries in the list view.
        listView.onItemClickListener = OnItemClickListener { _: AdapterView<*>?, view: View, _: Int, id: Long ->
            // Convert the long ID to an int.
            val itemId = id.toInt()

            // Only consume the click if it is not on the current page ID.
            if (itemId != currentPageId) {
                // Get a handle for the URL text view.
                val urlTextView = view.findViewById<TextView>(R.id.history_url_textview)

                // Get the URL.
                val url = urlTextView.text.toString()

                // Invoke the navigate history listener in the calling activity.  These commands cannot be run here because they need access to `applyDomainSettings()`.
                navigateHistoryListener.navigateHistory(url, currentPageId - itemId)

                // Dismiss the alert dialog.
                alertDialog.dismiss()
            }
        }

        // Return the alert dialog.
        return alertDialog
    }
}