/*
 * Copyright © 2016-2021 Soren Stoutner <soren@stoutner.com>.
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

package com.stoutner.privacybrowser.helpers

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.RelativeLayout

import androidx.fragment.app.FragmentManager

import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.dialogs.AdConsentDialog

object AdHelper {
    // Define the class variables.
    private var initialized = false

    // The `@JvmStatic` notation can be removed once all the code has migrated to Kotlin.
    @JvmStatic
    fun initializeAds(view: View, context: Context, activity: Activity, fragmentManager: FragmentManager, adUnitId: String) {
        // Check to see if the ads have been initialized.
        if (!initialized) {  // This is the first run; the ads have not yet been initialized.
            // Initialize mobile ads.
            MobileAds.initialize(context)

            // Initialize the bookmarks database helper.
            val adConsentDatabaseHelper = AdConsentDatabaseHelper(context)

            // Check to see if consent has been granted.
            val adConsentGranted = adConsentDatabaseHelper.isGranted

            // Display the ad consent dialog if needed.
            if (!adConsentGranted) {  // Ad consent has not been granted.
                // Instantiate the ad consent dialog.
                val adConsentDialogFragment = AdConsentDialog()

                // Display the ad consent dialog.
                adConsentDialogFragment.show(fragmentManager,"Ad Consent")
            } else {  // Ad consent has already been granted.
                // Load an ad.
                loadAd(view, context, activity, adUnitId)
            }

            // Set the initialized variable to true so this section doesn't run again.
            initialized = true
        } else {  // Ads have previously been initialized.
            // Load an ad.
            loadAd(view, context, activity, adUnitId)
        }
    }

    // The `@JvmStatic` notation can be removed once all the code has migrated to Kotlin.
    @JvmStatic
    fun loadAd(view: View, context: Context, activity: Activity, adUnitId: String) {
        // Cast the generic view to an AdView.
        var adView = view as AdView

        // Save the layout parameters.  They are used when programatically recreating the ad below.
        val adViewLayoutParameters = adView.layoutParams as RelativeLayout.LayoutParams

        // Get a handle for the ad view parent.
        val adViewParentLayout = adView.parent as RelativeLayout

        // Remove the AdView.
        adViewParentLayout.removeView(adView)

        // Create a new AdView.  This is necessary because the size can change when the device is rotated.
        adView = AdView(context)

        // Set the ad unit ID.
        adView.adUnitId = adUnitId

        //  Set the view ID.
        adView.id = R.id.adview

        // Set the layout parameters.
        adView.layoutParams = adViewLayoutParameters

        // Add the new ad view to the parent layout.
        adViewParentLayout.addView(adView)

        // Get a handle for the display.  Once the minimum API >= 30, this should be changed to `context.getDisplay()`.
        @Suppress("DEPRECATION") val display = activity.windowManager.defaultDisplay

        // Initialize a display metrics.
        val displayMetrics = DisplayMetrics()

        // Get the display metrics from the display.  Once the minimum APO >= 30, this should be replaced with `WindowMetrics.getBounds()` and `Configuration.densityDpi`.
        @Suppress("DEPRECATION")
        display.getMetrics(displayMetrics)

        // Get the width pixels and the density.
        val widthPixels = displayMetrics.widthPixels.toFloat()
        val density = displayMetrics.density

        // Calculate the ad width.
        val adWidth = (widthPixels / density).toInt()

        // Get the ad size.
        val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)

        // Set the ad size on the adView.
        adView.adSize = adSize

        // Create an ad settings bundle.
        val adSettingsBundle = Bundle()

        // Only request non-personalized ads.  <https://developers.google.com/ad-manager/mobile-ads-sdk/android/eu-consent#forward_consent_to_the_google_mobile_ads_sdk>
        adSettingsBundle.putString("npa", "1")

        // Build the ad request.
        val adRequest = AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, adSettingsBundle).build()

        // Make it so.
        adView.loadAd(adRequest)
    }

    // The `@JvmStatic` notation can be removed once all the code has migrated to Kotlin.
    // This method exists here for the sake of consistency with the following two methods.
    @JvmStatic
    fun hideAd(view: View) {
        // Cast the generic view to an AdView.
        val adView = view as AdView

        // Hide the ad.
        adView.visibility = View.GONE
    }

    // The `@JvmStatic` notation can be removed once all the code has migrated to Kotlin.
    // This method exists here so that the main WebView activity doesn't need to import `com.google.android.gms.ads.AdView`.
    @JvmStatic
    fun pauseAd(view: View) {
        // Cast The generic view to an AdView.
        val adView = view as AdView

        // Pause the AdView.
        adView.pause()
    }

    // The `@JvmStatic` notation can be removed once all the code has migrated to Kotlin.
    // This method exists here so that the main WebView activity doesn't need to import `com.google.android.gms.ads.AdView`.
    @JvmStatic
    fun resumeAd(view: View) {
        // Cast the generic view to an AdView.
        val adView = view as AdView

        // Resume the AdView.
        adView.resume()
    }
}