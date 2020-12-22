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

package com.stoutner.privacybrowser.helpers;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import com.stoutner.privacybrowser.R;
import com.stoutner.privacybrowser.dialogs.AdConsentDialog;

public class AdHelper {
    private static boolean initialized;

    public static void initializeAds(View view, Context applicationContext, Activity activity, FragmentManager fragmentManager, String adUnitId) {
        if (!initialized) {  // This is the first run.
            // Initialize mobile ads.
            MobileAds.initialize(applicationContext);

            // Initialize the bookmarks database helper.  The `0` specifies a database version, but that is ignored and set instead using a constant in `AdConsentDatabaseHelper`.
            AdConsentDatabaseHelper adConsentDatabaseHelper = new AdConsentDatabaseHelper(applicationContext, null, null, 0);

            // Check to see if consent has been granted.
            boolean adConsentGranted = adConsentDatabaseHelper.isGranted();

            // Display the ad consent dialog if needed.
            if (!adConsentGranted) {  // Ad consent has not been granted.
                // Instantiate the ad consent dialog.
                DialogFragment adConsentDialogFragment = new AdConsentDialog();

                // Display the ad consent dialog.
                adConsentDialogFragment.show(fragmentManager, "Ad Consent");
            } else {  // Ad consent has been granted.
                // Load an ad.
                loadAd(view, applicationContext, activity, adUnitId);
            }

            // Set the initialized variable to true so this section doesn't run again.
            initialized = true;
        } else {  // Ads have previously been initialized.
            // Load an ad.
            loadAd(view, applicationContext, activity, adUnitId);
        }
    }

    public static void loadAd(View view, Context applicationContext, Activity activity, String adUnitId) {
        // Cast the generic view to an AdView.
        AdView adView = (AdView) view;

        // Save the layout parameters.  They are used when programatically recreating the ad below.
        RelativeLayout.LayoutParams adViewLayoutParameters = (RelativeLayout.LayoutParams) adView.getLayoutParams();

        // Get a handle for the ad view parent.
        RelativeLayout adViewParentLayout = (RelativeLayout) adView.getParent();

        // Remove the AdView.
        adViewParentLayout.removeView(adView);

        // Create a new AdView.  This is necessary because the size can change when the device is rotated.
        adView = new AdView(applicationContext);

        // Set the ad unit ID.
        adView.setAdUnitId(adUnitId);

        //  Set the view ID.
        adView.setId(R.id.adview);

        // Set the layout parameters.
        adView.setLayoutParams(adViewLayoutParameters);

        // Add the new ad view to the parent layout.
        adViewParentLayout.addView(adView);

        // Get a handle for the display.
        Display display = activity.getWindowManager().getDefaultDisplay();

        // Initialize a display metrics.
        DisplayMetrics displayMetrics = new DisplayMetrics();

        // Get the display metrics from the display.
        display.getMetrics(displayMetrics);

        // Get the width pixels and the density.
        float widthPixels = displayMetrics.widthPixels;
        float density = displayMetrics.density;

        // Calculate the ad width.
        int adWidth = (int) (widthPixels / density);

        // Get the ad size.  This line should be enabled once Firebase Ads is updated to 20.0.0.
        AdSize adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(applicationContext, adWidth);

        // Set the ad size on the adView.
        adView.setAdSize(adSize);

        // Create an ad settings bundle.
        Bundle adSettingsBundle = new Bundle();

        // Only request non-personalized ads.  https://developers.google.com/ad-manager/mobile-ads-sdk/android/eu-consent#forward_consent_to_the_google_mobile_ads_sdk
        adSettingsBundle.putString("npa", "1");

        // Build the ad request.
        AdRequest adRequest = new AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter.class, adSettingsBundle).build();

        // Make it so.
        adView.loadAd(adRequest);
    }

    // This method exists here for the sake of consistency with the following two methods.
    public static void hideAd(View view) {
        // Cast the generic view to an AdView.
        AdView adView = (AdView) view;

        // Hide the ad.
        adView.setVisibility(View.GONE);
    }

    // This method exists here so that the main WebView activity doesn't need to import `com.google.android.gms.ads.AdView`.
    public static void pauseAd(View view) {
        // Cast The generic view to an AdView.
        AdView adView = (AdView) view;

        // Pause the AdView.
        adView.pause();
    }

    // This method exists here so that the main WebView activity doesn't need to import `com.google.android.gms.ads.AdView`.
    public static void resumeAd(View view) {
        // Cast the generic view to an AdView.
        AdView adView = (AdView) view;

        // Resume the AdView.
        adView.resume();
    }
}