/*
 * Copyright 2017-2023 Soren Stoutner <soren@stoutner.com>.
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
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.WindowManager

import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.ViewPager

import com.google.android.material.tabs.TabLayout

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.activities.MainWebViewActivity
import com.stoutner.privacybrowser.adapters.PinnedMismatchPagerAdapter
import com.stoutner.privacybrowser.helpers.DomainsDatabaseHelper
import com.stoutner.privacybrowser.views.NestedScrollWebView

// Define the class constants.
private const val WEBVIEW_FRAGMENT_ID = "webview_fragment_id"

class PinnedMismatchDialog : DialogFragment() {
    companion object {
        fun displayDialog(webViewFragmentId: Long): PinnedMismatchDialog {
            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the WebView fragment ID in the bundle.
            argumentsBundle.putLong(WEBVIEW_FRAGMENT_ID, webViewFragmentId)

            // Create a new instance of the pinned mismatch dialog.
            val pinnedMismatchDialog = PinnedMismatchDialog()

            // Add the arguments bundle to the new instance.
            pinnedMismatchDialog.arguments = argumentsBundle

            // Make it so.
            return pinnedMismatchDialog
        }
    }

    // Declare the class variables.
    private lateinit var pinnedMismatchListener: PinnedMismatchListener

    // The public interface is used to send information back to the parent activity.
    interface PinnedMismatchListener {
        fun pinnedErrorGoBack()
    }

    override fun onAttach(context: Context) {
        // Run the default commands.
        super.onAttach(context)

        // Get a handle for the listener from the launching context.
        pinnedMismatchListener = context as PinnedMismatchListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get the WebView fragment ID.
        val webViewFragmentId = requireArguments().getLong(WEBVIEW_FRAGMENT_ID)

        // Get the current position of this WebView fragment.
        val webViewPosition = MainWebViewActivity.webViewPagerAdapter!!.getPositionForId(webViewFragmentId)

        // Get the WebView tab fragment.
        val webViewTabFragment = MainWebViewActivity.webViewPagerAdapter!!.getPageFragment(webViewPosition)

        // Get the fragment view.
        val fragmentView = webViewTabFragment.requireView()

        // Get a handle for the current WebView.
        val nestedScrollWebView = fragmentView.findViewById<NestedScrollWebView>(R.id.nestedscroll_webview)!!

        // Use an alert dialog builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.PrivacyBrowserAlertDialog)

        // Get the favorite icon.
        val favoriteIconBitmap = nestedScrollWebView.getFavoriteIcon()

        // Get the default favorite icon drawable.  `ContextCompat` must be used until API >= 21.
        val defaultFavoriteIconDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.world)

        // Cast the favorite icon drawable to a bitmap drawable.
        val defaultFavoriteIconBitmapDrawable = (defaultFavoriteIconDrawable as BitmapDrawable)

        // Store the default icon bitmap.
        val defaultFavoriteIconBitmap = defaultFavoriteIconBitmapDrawable.bitmap

        // Set the favorite icon as the dialog icon if it exists.
        if (favoriteIconBitmap.sameAs(defaultFavoriteIconBitmap)) {  // There is no website favorite icon.
            // Set the icon.
            dialogBuilder.setIcon(R.drawable.ssl_certificate)
        } else {  // There is a favorite icon.
            // Create a drawable version of the favorite icon.
            val favoriteIconDrawable: Drawable = BitmapDrawable(resources, favoriteIconBitmap)

            // Set the icon.
            dialogBuilder.setIcon(favoriteIconDrawable)
        }

        // Set the title.
        dialogBuilder.setTitle(R.string.pinned_mismatch)

        // Set the layout.
        dialogBuilder.setView(R.layout.pinned_mismatch_linearlayout)

        // Set the update button listener.
        dialogBuilder.setNeutralButton(R.string.update) { _: DialogInterface?, _: Int ->
            // Get the current SSL certificate.
            val currentSslCertificate = nestedScrollWebView.certificate!!

            // Get the dates from the certificate.
            val currentSslStartDate = currentSslCertificate.validNotBeforeDate
            val currentSslEndDate = currentSslCertificate.validNotAfterDate

            // Convert the dates into longs.  If the date is null, a long value of `0` will be stored in the domains database entry.
            val currentSslStartDateLong: Long = currentSslStartDate?.time ?: 0
            val currentSslEndDateLong: Long = currentSslEndDate?.time ?: 0

            // Initialize the database handler.
            val domainsDatabaseHelper = DomainsDatabaseHelper(requireContext())

            // Update the SSL certificate if it is pinned.
            if (nestedScrollWebView.hasPinnedSslCertificate()) {
                // Update the pinned SSL certificate in the domain database.
                domainsDatabaseHelper.updatePinnedSslCertificate(nestedScrollWebView.domainSettingsDatabaseId, currentSslCertificate.issuedTo.cName, currentSslCertificate.issuedTo.oName,
                        currentSslCertificate.issuedTo.uName, currentSslCertificate.issuedBy.cName, currentSslCertificate.issuedBy.oName, currentSslCertificate.issuedBy.uName, currentSslStartDateLong,
                        currentSslEndDateLong)

                // Update the pinned SSL certificate in the nested scroll WebView.
                nestedScrollWebView.setPinnedSslCertificate(currentSslCertificate.issuedTo.cName, currentSslCertificate.issuedTo.oName, currentSslCertificate.issuedTo.uName,
                        currentSslCertificate.issuedBy.cName, currentSslCertificate.issuedBy.oName, currentSslCertificate.issuedBy.uName, currentSslStartDate, currentSslEndDate)
            }

            // Update the IP addresses if they are pinned.
            if (nestedScrollWebView.pinnedIpAddresses != "") {
                // Update the pinned IP addresses in the domain database.
                domainsDatabaseHelper.updatePinnedIpAddresses(nestedScrollWebView.domainSettingsDatabaseId, nestedScrollWebView. currentIpAddresses)

                // Update the pinned IP addresses in the nested scroll WebView.
                nestedScrollWebView.pinnedIpAddresses = nestedScrollWebView.currentIpAddresses
            }
        }

        // Set the back button listener.
        dialogBuilder.setNegativeButton(R.string.back) { _: DialogInterface?, _: Int ->
            if (nestedScrollWebView.canGoBack()) {  // There is a back page in the history.
                // Invoke the navigate history listener in the calling activity.  These commands cannot be run here because they need access to `applyDomainSettings()`.
                pinnedMismatchListener.pinnedErrorGoBack()
            } else {  // There are no pages to go back to.
                // Load a blank page
                nestedScrollWebView.loadUrl("")
            }
        }

        // Set the proceed button listener.
        dialogBuilder.setPositiveButton(R.string.proceed) { _: DialogInterface?, _: Int ->
            // Do not check the pinned information for this domain again until the domain changes.
            nestedScrollWebView.ignorePinnedDomainInformation = true
        }

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

        // The alert dialog must be shown before items in the layout can be modified.
        alertDialog.show()

        //  Get handles for the views.
        val viewPager = alertDialog.findViewById<ViewPager>(R.id.pinned_ssl_certificate_mismatch_viewpager)!!
        val tabLayout = alertDialog.findViewById<TabLayout>(R.id.pinned_ssl_certificate_mismatch_tablayout)!!

        // Initialize the pinned mismatch pager adapter.
        val pinnedMismatchPagerAdapter = PinnedMismatchPagerAdapter(requireContext(), layoutInflater, webViewFragmentId)

        // Set the view pager adapter.
        viewPager.adapter = pinnedMismatchPagerAdapter

        // Connect the tab layout to the view pager.
        tabLayout.setupWithViewPager(viewPager)

        // Return the alert dialog.
        return alertDialog
    }
}
