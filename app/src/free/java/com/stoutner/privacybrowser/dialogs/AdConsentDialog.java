/*
 * Copyright © 2018-2020 Soren Stoutner <soren@stoutner.com>.
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

package com.stoutner.privacybrowser.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.stoutner.privacybrowser.R;
import com.stoutner.privacybrowser.helpers.AdConsentDatabaseHelper;
import com.stoutner.privacybrowser.helpers.AdHelper;

public class AdConsentDialog extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use a builder to create the alert dialog.
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext(), R.style.PrivacyBrowserAlertDialog);

        // Get the current theme status.
        int currentThemeStatus = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        // Set the icon according to the theme.
        if (currentThemeStatus == Configuration.UI_MODE_NIGHT_YES) {
            dialogBuilder.setIcon(R.drawable.block_ads_enabled_night);
        } else {
            dialogBuilder.setIcon(R.drawable.block_ads_enabled_day);
        }

        // Remove the incorrect lint warning below that `getApplicationContext()` might be null.
        assert getActivity() != null;

        // Initialize the bookmarks database helper.  The `0` specifies a database version, but that is ignored and set instead using a constant in `AdConsentDatabaseHelper`.
        // `getContext()` can be used instead of `getActivity.getApplicationContext()` when the minimum API >= 23.
        AdConsentDatabaseHelper adConsentDatabaseHelper = new AdConsentDatabaseHelper(getActivity().getApplicationContext(), null, null, 0);

        // Set the title.
        dialogBuilder.setTitle(R.string.ad_consent);

        // Set the text.
        dialogBuilder.setMessage(R.string.ad_consent_text);

        // Configure the close button.
        dialogBuilder.setNegativeButton(R.string.close_browser, (DialogInterface dialog, int which) -> {
            // Update the ad consent database.
            adConsentDatabaseHelper.updateAdConsent(false);

            // Close the browser.  `finishAndRemoveTask` also removes Privacy Browser from the recent app list.
            if (Build.VERSION.SDK_INT >= 21) {
                getActivity().finishAndRemoveTask();
            } else {
                getActivity().finish();
            }

            // Remove the terminated program from RAM.  The status code is `0`.
            System.exit(0);
        });

        // Configure the accept button.
        dialogBuilder.setPositiveButton(R.string.accept_ads, (DialogInterface dialog, int which) -> {
            // Update the ad consent database.
            adConsentDatabaseHelper.updateAdConsent(true);

            // Load an ad.  `getContext()` can be used instead of `getActivity.getApplicationContext()` once the minimum API >= 23.
            AdHelper.loadAd(getActivity().findViewById(R.id.adview), getActivity().getApplicationContext(), getActivity(), getString(R.string.ad_unit_id));
        });

        // Create an alert dialog from the alert dialog builder.
        AlertDialog alertDialog = dialogBuilder.create();

        // Get a handle for the shared preferences.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        // Get the screenshot preference.
        boolean allowScreenshots = sharedPreferences.getBoolean("allow_screenshots", false);

        // Disable screenshots if not allowed.
        if (!allowScreenshots) {
            // Remove the warning below that `getWindow()` might be null.
            assert alertDialog.getWindow() != null;

            // Disable screenshots.
            alertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        // Return the alert dialog.
        return alertDialog;
    }

    // Close Privacy Browser Free if the dialog is cancelled without selecting a button (by tapping on the background).
    @Override
    public void onCancel(@NonNull DialogInterface dialogInterface) {
        // Remove the incorrect lint warning below that `getApplicationContext()` might be null.
        assert getActivity() != null;

        // Initialize the bookmarks database helper.  The `0` specifies a database version, but that is ignored and set instead using a constant in `AdConsentDatabaseHelper`.
        // `getContext()` can be used instead of `getActivity.getApplicationContext()` when the minimum API >= 23.
        AdConsentDatabaseHelper adConsentDatabaseHelper = new AdConsentDatabaseHelper(getActivity().getApplicationContext(), null, null, 0);

        // Update the ad consent database.
        adConsentDatabaseHelper.updateAdConsent(false);

        // Close the browser.  `finishAndRemoveTask()` also removes Privacy Browser from the recent app list.
        if (Build.VERSION.SDK_INT >= 21) {
            getActivity().finishAndRemoveTask();
        } else {
            getActivity().finish();
        }

        // Remove the terminated program from RAM.  The status code is `0`.
        System.exit(0);
    }
}