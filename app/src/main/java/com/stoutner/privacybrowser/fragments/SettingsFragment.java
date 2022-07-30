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

package com.stoutner.privacybrowser.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import com.stoutner.privacybrowser.R;
import com.stoutner.privacybrowser.activities.MainWebViewActivity;
import com.stoutner.privacybrowser.helpers.ProxyHelper;

import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat {
    // Declare the class variables.
    private int currentThemeStatus;
    private String defaultUserAgent;
    private ArrayAdapter<CharSequence> userAgentNamesArray;
    private String[] translatedUserAgentNamesArray;
    private String[] userAgentDataArray;
    private String[] appThemeEntriesStringArray;
    private String[] appThemeEntryValuesStringArray;
    private String[] webViewThemeEntriesStringArray;
    private String[] webViewThemeEntryValuesStringArray;
    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    // Declare the class views.
    private Preference javaScriptPreference;
    private Preference cookiesPreference;
    private Preference domStoragePreference;
    private Preference formDataPreference;  // The form data preference can be removed once the minimum API >= 26.
    private Preference userAgentPreference;
    private Preference customUserAgentPreference;
    private Preference xRequestedWithHeaderPreference;
    private Preference incognitoModePreference;
    private Preference allowScreenshotsPreference;
    private Preference easyListPreference;
    private Preference easyPrivacyPreference;
    private Preference fanboyAnnoyanceListPreference;
    private Preference fanboySocialBlockingListPreference;
    private Preference ultraListPreference;
    private Preference ultraPrivacyPreference;
    private Preference blockAllThirdPartyRequestsPreference;
    private Preference trackingQueriesPreference;
    private Preference ampRedirectsPreference;
    private Preference searchPreference;
    private Preference searchCustomURLPreference;
    private Preference proxyPreference;
    private Preference proxyCustomUrlPreference;
    private Preference fullScreenBrowsingModePreference;
    private Preference hideAppBarPreference;
    private Preference clearEverythingPreference;
    private Preference clearCookiesPreference;
    private Preference clearDomStoragePreference;
    private Preference clearFormDataPreference;  // The clear form data preference can be removed once the minimum API >= 26.
    private Preference clearLogcatPreference;
    private Preference clearCachePreference;
    private Preference homepagePreference;
    private Preference fontSizePreference;
    private Preference openIntentsInNewTabPreference;
    private Preference swipeToRefreshPreference;
    private Preference downloadWithExternalAppPreference;
    private Preference scrollAppBarPreference;
    private Preference bottomAppBarPreference;
    private Preference displayAdditionalAppBarIconsPreference;
    private Preference appThemePreference;
    private Preference webViewThemePreference;
    private Preference wideViewportPreference;
    private Preference displayWebpageImagesPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from the XML file.
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // Get a handle for the activity.
        Activity activity = getActivity();

        // Remove the lint warning below that `getApplicationContext()` might produce a null pointer exception.
        assert activity != null;

        // Get a handle for the resources.
        Resources resources = getResources();

        // Get the current theme status.
        currentThemeStatus = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        // Get a handle for the shared preferences.
        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();

        // Remove the incorrect warning below that the shared preferences might be null.
        assert sharedPreferences != null;

        // Get handles for the preferences.
        javaScriptPreference = findPreference("javascript");
        cookiesPreference = findPreference(getString(R.string.cookies_key));
        domStoragePreference = findPreference("dom_storage");
        formDataPreference = findPreference("save_form_data");  // The form data preference can be removed once the minimum API >= 26.
        userAgentPreference = findPreference("user_agent");
        customUserAgentPreference = findPreference("custom_user_agent");
        xRequestedWithHeaderPreference = findPreference(getString(R.string.x_requested_with_header_key));
        incognitoModePreference = findPreference("incognito_mode");
        allowScreenshotsPreference = findPreference(getString(R.string.allow_screenshots_key));
        easyListPreference = findPreference("easylist");
        easyPrivacyPreference = findPreference("easyprivacy");
        fanboyAnnoyanceListPreference = findPreference("fanboys_annoyance_list");
        fanboySocialBlockingListPreference = findPreference("fanboys_social_blocking_list");
        ultraListPreference = findPreference("ultralist");
        ultraPrivacyPreference = findPreference("ultraprivacy");
        blockAllThirdPartyRequestsPreference = findPreference("block_all_third_party_requests");
        trackingQueriesPreference = findPreference(getString(R.string.tracking_queries_key));
        ampRedirectsPreference = findPreference(getString(R.string.amp_redirects_key));
        searchPreference = findPreference("search");
        searchCustomURLPreference = findPreference("search_custom_url");
        proxyPreference = findPreference("proxy");
        proxyCustomUrlPreference = findPreference(getString(R.string.proxy_custom_url_key));
        fullScreenBrowsingModePreference = findPreference("full_screen_browsing_mode");
        hideAppBarPreference = findPreference("hide_app_bar");
        clearEverythingPreference = findPreference("clear_everything");
        clearCookiesPreference = findPreference("clear_cookies");
        clearDomStoragePreference = findPreference("clear_dom_storage");
        clearFormDataPreference = findPreference("clear_form_data");  // The clear form data preference can be removed once the minimum API >= 26.
        clearLogcatPreference = findPreference(getString(R.string.clear_logcat_key));
        clearCachePreference = findPreference("clear_cache");
        homepagePreference = findPreference("homepage");
        fontSizePreference = findPreference("font_size");
        openIntentsInNewTabPreference = findPreference("open_intents_in_new_tab");
        swipeToRefreshPreference = findPreference("swipe_to_refresh");
        downloadWithExternalAppPreference = findPreference(getString(R.string.download_with_external_app_key));
        scrollAppBarPreference = findPreference(getString(R.string.scroll_app_bar_key));
        bottomAppBarPreference = findPreference(getString(R.string.bottom_app_bar_key));
        displayAdditionalAppBarIconsPreference = findPreference(getString(R.string.display_additional_app_bar_icons_key));
        appThemePreference = findPreference("app_theme");
        webViewThemePreference = findPreference("webview_theme");
        wideViewportPreference = findPreference("wide_viewport");
        displayWebpageImagesPreference = findPreference("display_webpage_images");

        // Remove the lint warnings below that the preferences might be null.
        assert javaScriptPreference != null;
        assert cookiesPreference != null;
        assert domStoragePreference != null;
        assert formDataPreference != null;
        assert userAgentPreference != null;
        assert customUserAgentPreference != null;
        assert xRequestedWithHeaderPreference != null;
        assert incognitoModePreference != null;
        assert allowScreenshotsPreference != null;
        assert easyListPreference != null;
        assert easyPrivacyPreference != null;
        assert fanboyAnnoyanceListPreference != null;
        assert fanboySocialBlockingListPreference != null;
        assert ultraListPreference != null;
        assert ultraPrivacyPreference != null;
        assert blockAllThirdPartyRequestsPreference != null;
        assert trackingQueriesPreference != null;
        assert ampRedirectsPreference != null;
        assert searchPreference != null;
        assert searchCustomURLPreference != null;
        assert proxyPreference != null;
        assert proxyCustomUrlPreference != null;
        assert fullScreenBrowsingModePreference != null;
        assert hideAppBarPreference != null;
        assert clearEverythingPreference != null;
        assert clearCookiesPreference != null;
        assert clearDomStoragePreference != null;
        assert clearFormDataPreference != null;
        assert clearLogcatPreference != null;
        assert clearCachePreference != null;
        assert homepagePreference != null;
        assert fontSizePreference != null;
        assert openIntentsInNewTabPreference != null;
        assert swipeToRefreshPreference != null;
        assert downloadWithExternalAppPreference != null;
        assert scrollAppBarPreference != null;
        assert bottomAppBarPreference != null;
        assert displayAdditionalAppBarIconsPreference != null;
        assert appThemePreference != null;
        assert webViewThemePreference != null;
        assert wideViewportPreference != null;
        assert displayWebpageImagesPreference != null;

        // Set the preference dependencies.
        hideAppBarPreference.setDependency("full_screen_browsing_mode");
        domStoragePreference.setDependency("javascript");

        // Get strings from the preferences.
        String userAgentName = sharedPreferences.getString("user_agent", getString(R.string.user_agent_default_value));
        String searchString = sharedPreferences.getString("search", getString(R.string.search_default_value));
        String proxyString = sharedPreferences.getString("proxy", getString(R.string.proxy_default_value));

        // Get booleans that are used in multiple places from the preferences.
        boolean javaScriptEnabled = sharedPreferences.getBoolean("javascript", false);
        boolean fanboyAnnoyanceListEnabled = sharedPreferences.getBoolean("fanboys_annoyance_list", true);
        boolean fanboySocialBlockingEnabled = sharedPreferences.getBoolean("fanboys_social_blocking_list", true);
        boolean fullScreenBrowsingMode = sharedPreferences.getBoolean("full_screen_browsing_mode", false);
        boolean clearEverything = sharedPreferences.getBoolean("clear_everything", true);

        // Remove the form data preferences if the API is >= 26 as they no longer do anything.
        if (Build.VERSION.SDK_INT >= 26) {
            // Get handles for the categories.
            PreferenceCategory privacyCategory = findPreference("privacy");
            PreferenceCategory clearAndExitCategory = findPreference("clear_and_exit");

            // Remove the incorrect lint warnings below that the preference categories might be null.
            assert privacyCategory != null;
            assert clearAndExitCategory != null;

            // Remove the form data preferences.
            privacyCategory.removePreference(formDataPreference);
            clearAndExitCategory.removePreference(clearFormDataPreference);
        }

        // Only enable Fanboy's social blocking list preference if Fanboy's annoyance list is disabled.
        fanboySocialBlockingListPreference.setEnabled(!fanboyAnnoyanceListEnabled);


        // Inflate a WebView to get the default user agent.
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // `@SuppressLint("InflateParams")` removes the warning about using `null` as the `ViewGroup`, which in this case makes sense because the `bare_webview` will not be displayed.
        @SuppressLint("InflateParams") View bareWebViewLayout = inflater.inflate(R.layout.bare_webview, null, false);

        // Get a handle for a bare WebView.
        WebView bareWebView = bareWebViewLayout.findViewById(R.id.bare_webview);

        // Get the default user agent.
        defaultUserAgent = bareWebView.getSettings().getUserAgentString();

        // Get the user agent arrays.
        userAgentNamesArray = ArrayAdapter.createFromResource(requireContext(), R.array.user_agent_names, R.layout.spinner_item);
        translatedUserAgentNamesArray = resources.getStringArray(R.array.translated_user_agent_names);
        userAgentDataArray = resources.getStringArray(R.array.user_agent_data);

        // Get the array position of the user agent name.
        int userAgentArrayPosition = userAgentNamesArray.getPosition(userAgentName);

        // Populate the user agent summary.
        switch (userAgentArrayPosition) {
            case MainWebViewActivity.UNRECOGNIZED_USER_AGENT:  // The user agent name is not on the canonical list.
                // This is probably because it was set in an older version of Privacy Browser before the switch to persistent user agent names.  Use the current user agent entry name as the summary.
                userAgentPreference.setSummary(userAgentName);
                break;

            case MainWebViewActivity.SETTINGS_WEBVIEW_DEFAULT_USER_AGENT:
                // Get the user agent text from the webview (which changes based on the version of Android and WebView installed).
                userAgentPreference.setSummary(translatedUserAgentNamesArray[userAgentArrayPosition] + ":\n" + defaultUserAgent);
                break;

            case MainWebViewActivity.SETTINGS_CUSTOM_USER_AGENT:
                // Set the summary text.
                userAgentPreference.setSummary(R.string.custom_user_agent);
                break;

            default:
                // Get the user agent summary from the user agent data array.
                userAgentPreference.setSummary(translatedUserAgentNamesArray[userAgentArrayPosition] + ":\n" + userAgentDataArray[userAgentArrayPosition]);
        }

        // Set the summary text for the custom user agent preference.
        customUserAgentPreference.setSummary(sharedPreferences.getString("custom_user_agent", getString(R.string.custom_user_agent_default_value)));

        // Only enable the custom user agent preference if the user agent is set to `Custom`.
        customUserAgentPreference.setEnabled(Objects.equals(userAgentPreference.getSummary(), getString(R.string.custom_user_agent)));

        // Set the search URL as the summary text for the search preference when the preference screen is loaded.
        if (searchString.equals("Custom URL")) {
            // Use R.string.custom_url, which will be translated, instead of the array value, which will not.
            searchPreference.setSummary(R.string.custom_url);
        } else {
            // Set the array value as the summary text.
            searchPreference.setSummary(searchString);
        }

        // Set the summary text for the search custom URL (the default is `""`).
        searchCustomURLPreference.setSummary(sharedPreferences.getString("search_custom_url", getString(R.string.search_custom_url_default_value)));

        // Only enable the search custom URL preference if the search is set to `Custom URL`.
        searchCustomURLPreference.setEnabled(searchString.equals("Custom URL"));


        // Set the summary text for the proxy preference when the preference screen is loaded.
        switch (proxyString) {
            case ProxyHelper.NONE:
                proxyPreference.setSummary(getString(R.string.no_proxy_enabled));
                break;

            case ProxyHelper.TOR:
                proxyPreference.setSummary(getString(R.string.tor_enabled));
                break;

            case ProxyHelper.I2P:
                proxyPreference.setSummary(getString(R.string.i2p_enabled));
                break;

            case ProxyHelper.CUSTOM:
                proxyPreference.setSummary(getString(R.string.custom_proxy));
                break;
        }

        // Set the summary text for the custom proxy URL.
        proxyCustomUrlPreference.setSummary(sharedPreferences.getString(getString(R.string.proxy_custom_url_key), getString(R.string.proxy_custom_url_default_value)));

        // Only enable the custom proxy URL if a custom proxy is selected.
        proxyCustomUrlPreference.setEnabled(proxyString.equals(ProxyHelper.CUSTOM));


        // Set the status of the clear and exit preferences.
        clearCookiesPreference.setEnabled(!clearEverything);
        clearDomStoragePreference.setEnabled(!clearEverything);
        clearFormDataPreference.setEnabled(!clearEverything);  // The form data line can be removed once the minimum API is >= 26.
        clearLogcatPreference.setEnabled(!clearEverything);
        clearCachePreference.setEnabled(!clearEverything);


        // Set the homepage URL as the summary text for the homepage preference.
        homepagePreference.setSummary(sharedPreferences.getString("homepage", getString(R.string.homepage_default_value)));


        // Set the font size as the summary text for the preference.
        fontSizePreference.setSummary(sharedPreferences.getString("font_size", getString(R.string.font_size_default_value)) + "%");


        // Get the app theme string arrays.
        appThemeEntriesStringArray = resources.getStringArray(R.array.app_theme_entries);
        appThemeEntryValuesStringArray = resources.getStringArray(R.array.app_theme_entry_values);

        // Get the current app theme.
        String currentAppTheme = sharedPreferences.getString("app_theme", getString(R.string.app_theme_default_value));

        // Define an app theme entry number.
        int appThemeEntryNumber;

        // Get the app theme entry number that matches the current app theme.  A switch statement cannot be used because the theme entry values string array is not a compile time constant.
        if (currentAppTheme.equals(appThemeEntryValuesStringArray[1])) {  // The light theme is selected.
            // Store the app theme entry number.
            appThemeEntryNumber = 1;
        } else if (currentAppTheme.equals(appThemeEntryValuesStringArray[2])) {  // The dark theme is selected.
            // Store the app theme entry number.
            appThemeEntryNumber = 2;
        } else {  // The system default theme is selected.
            // Store the app theme entry number.
            appThemeEntryNumber = 0;
        }

        // Set the current theme as the summary text for the preference.
        appThemePreference.setSummary(appThemeEntriesStringArray[appThemeEntryNumber]);


        // Get the WebView theme string arrays.
        webViewThemeEntriesStringArray = resources.getStringArray(R.array.webview_theme_entries);
        webViewThemeEntryValuesStringArray = resources.getStringArray(R.array.webview_theme_entry_values);

        // Get the current WebView theme.
        String currentWebViewTheme = sharedPreferences.getString("webview_theme", getString(R.string.webview_theme_default_value));

        // Define a WebView theme entry number.
        int webViewThemeEntryNumber;

        // Get the WebView theme entry number that matches the current WebView theme.  A switch statement cannot be used because the WebView theme entry values string array is not a compile time constant.
        if (currentWebViewTheme.equals(webViewThemeEntryValuesStringArray[1])) {  // The light theme is selected.
            // Store the WebView theme entry number.
            webViewThemeEntryNumber = 1;
        } else if (currentWebViewTheme.equals(webViewThemeEntryValuesStringArray[2])) {  // The dark theme is selected.
            // Store the WebView theme entry number.
            webViewThemeEntryNumber = 2;
        } else {  // The system default theme is selected.
            // Store the WebView theme entry number.
            webViewThemeEntryNumber = 0;
        }

        // Set the current theme as the summary text for the preference.
        webViewThemePreference.setSummary(webViewThemeEntriesStringArray[webViewThemeEntryNumber]);


        // Set the JavaScript icon.
        if (javaScriptEnabled) {
            javaScriptPreference.setIcon(R.drawable.javascript_enabled);
        } else {
            javaScriptPreference.setIcon(R.drawable.privacy_mode);
        }

        // Set the cookies icon.
        if (sharedPreferences.getBoolean(getString(R.string.cookies_key), false)) {
            cookiesPreference.setIcon(R.drawable.cookies_enabled);
        } else {
            cookiesPreference.setIcon(R.drawable.cookies_disabled);
        }

        // Set the DOM storage icon.
        if (javaScriptEnabled) {  // The preference is enabled.
            if (sharedPreferences.getBoolean("dom_storage", false)) {  // DOM storage is enabled.
                domStoragePreference.setIcon(R.drawable.dom_storage_enabled);
            } else {  // DOM storage is disabled.
                domStoragePreference.setIcon(R.drawable.dom_storage_disabled);
            }
        } else {  // The preference is disabled.  The icon should be ghosted.
            domStoragePreference.setIcon(R.drawable.dom_storage_ghosted);
        }

        // Set the save form data icon if API < 26.  Save form data has no effect on API >= 26.
        if (Build.VERSION.SDK_INT < 26) {
            if (sharedPreferences.getBoolean("save_form_data", false))
                formDataPreference.setIcon(R.drawable.form_data_enabled);
            else
                formDataPreference.setIcon(R.drawable.form_data_disabled);
        }

        // Set the custom user agent icon.
        if (customUserAgentPreference.isEnabled())
            customUserAgentPreference.setIcon(R.drawable.custom_user_agent_enabled);
        else
            customUserAgentPreference.setIcon(R.drawable.custom_user_agent_ghosted);

        // Set the X-Requested With header icon.
        if (sharedPreferences.getBoolean(getString(R.string.x_requested_with_header_key), true))
            xRequestedWithHeaderPreference.setIcon(R.drawable.x_requested_with_header_enabled);
        else
            xRequestedWithHeaderPreference.setIcon(R.drawable.x_requested_with_header_disabled);

        // Set the incognito mode icon.
        if (sharedPreferences.getBoolean("incognito_mode", false))
            incognitoModePreference.setIcon(R.drawable.incognito_mode_enabled);
        else
            incognitoModePreference.setIcon(R.drawable.incognito_mode_disabled);

        // Set the allow screenshots icon.
        if (sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false))
            allowScreenshotsPreference.setIcon(R.drawable.allow_screenshots_enabled);
        else
            allowScreenshotsPreference.setIcon(R.drawable.allow_screenshots_disabled);

        // Set the EasyList icon.
        if (sharedPreferences.getBoolean("easylist", true))
            easyListPreference.setIcon(R.drawable.block_ads_enabled);
        else
            easyListPreference.setIcon(R.drawable.block_ads_disabled);

        // Set the EasyPrivacy icon.
        if (sharedPreferences.getBoolean("easyprivacy", true))
            easyPrivacyPreference.setIcon(R.drawable.block_tracking_enabled);
        else
            easyPrivacyPreference.setIcon(R.drawable.block_tracking_disabled);

        // Set the Fanboy lists icons.
        if (fanboyAnnoyanceListEnabled) {
            // Set the Fanboy annoyance list icon.
            fanboyAnnoyanceListPreference.setIcon(R.drawable.social_media_enabled);

            // Set the Fanboy social blocking list icon.
            fanboySocialBlockingListPreference.setIcon(R.drawable.social_media_ghosted);
        } else {
            // Set the Fanboy annoyance list icon.
            fanboyAnnoyanceListPreference.setIcon(R.drawable.social_media_disabled);

            // Set the Fanboy social blocking list icon.
            if (fanboySocialBlockingEnabled) {
                fanboySocialBlockingListPreference.setIcon(R.drawable.social_media_enabled);
            } else {
                fanboySocialBlockingListPreference.setIcon(R.drawable.social_media_disabled);
            }
        }

        // Set the UltraList icon.
        if (sharedPreferences.getBoolean("ultralist", true)){
            ultraListPreference.setIcon(R.drawable.block_ads_enabled);
        } else {
            ultraListPreference.setIcon(R.drawable.block_ads_disabled);
        }

        // Set the UltraPrivacy icon.
        if (sharedPreferences.getBoolean("ultraprivacy", true)) {
            ultraPrivacyPreference.setIcon(R.drawable.block_tracking_enabled);
        } else {
            ultraPrivacyPreference.setIcon(R.drawable.block_tracking_disabled);
        }

        // Set the block all third-party requests icon.
        if (sharedPreferences.getBoolean("block_all_third_party_requests", false)) {
            blockAllThirdPartyRequestsPreference.setIcon(R.drawable.block_all_third_party_requests_enabled);
        } else {
            blockAllThirdPartyRequestsPreference.setIcon(R.drawable.block_all_third_party_requests_disabled);
        }

        // Set the Tracking Queries icon.
        if (sharedPreferences.getBoolean(getString(R.string.tracking_queries_key), true)) {
            trackingQueriesPreference.setIcon(R.drawable.modify_url_enabled);
        } else {
            trackingQueriesPreference.setIcon(R.drawable.modify_url_disabled);
        }

        // Set the AMP Redirects icon.
        if (sharedPreferences.getBoolean(getString(R.string.amp_redirects_key), true)) {
            ampRedirectsPreference.setIcon(R.drawable.modify_url_enabled);
        } else {
            ampRedirectsPreference.setIcon(R.drawable.modify_url_disabled);
        }

        // Set the search custom URL icon.
        if (searchCustomURLPreference.isEnabled()) {
            searchCustomURLPreference.setIcon(R.drawable.search_custom_enabled);
        } else {
            searchCustomURLPreference.setIcon(R.drawable.search_custom_ghosted);
        }

        // Set the Proxy icons according to the theme and status.
        if (proxyString.equals(ProxyHelper.NONE)) {  // Proxying is disabled.
            // Set the main proxy icon to be disabled.
            proxyPreference.setIcon(R.drawable.proxy_disabled);

            // Set the custom proxy URL icon to be ghosted.
            proxyCustomUrlPreference.setIcon(R.drawable.proxy_ghosted);
        } else {  // Proxying is enabled.
            // Set the main proxy icon to be enabled.
            proxyPreference.setIcon(R.drawable.proxy_enabled);

            // Set the custom proxy URL icon according to its status.
            if (proxyCustomUrlPreference.isEnabled()) {
                proxyCustomUrlPreference.setIcon(R.drawable.proxy_enabled);
            } else {
                proxyCustomUrlPreference.setIcon(R.drawable.proxy_ghosted);
            }
        }

        // Set the full screen browsing mode icons.
        if (fullScreenBrowsingMode) {  // Full screen browsing mode is enabled.
            // Set the full screen browsing mode preference icon.
            fullScreenBrowsingModePreference.setIcon(R.drawable.full_screen_enabled);

            // Set the hide app bar icon.
            if (sharedPreferences.getBoolean("hide_app_bar", true)) {
                hideAppBarPreference.setIcon(R.drawable.app_bar_enabled);
            } else {
                hideAppBarPreference.setIcon(R.drawable.app_bar_disabled);
            }
        } else {  // Full screen browsing mode is disabled.
            // Set the icons.
            fullScreenBrowsingModePreference.setIcon(R.drawable.full_screen_disabled);
            hideAppBarPreference.setIcon(R.drawable.app_bar_ghosted);
        }

        // Set the clear everything preference icon.
        if (clearEverything) {
            clearEverythingPreference.setIcon(R.drawable.clear_everything_enabled);
        } else {
            clearEverythingPreference.setIcon(R.drawable.clear_everything_disabled);
        }

        // Set the clear cookies preference icon.
        if (clearEverything || sharedPreferences.getBoolean("clear_cookies", true)) {
            clearCookiesPreference.setIcon(R.drawable.clear_cookies_enabled);
        } else {
            clearCookiesPreference.setIcon(R.drawable.clear_cookies_disabled);
        }

        // Set the clear DOM storage preference icon.
        if (clearEverything || sharedPreferences.getBoolean("clear_dom_storage", true)) {
            clearDomStoragePreference.setIcon(R.drawable.clear_dom_storage_enabled);
        } else {
            clearDomStoragePreference.setIcon(R.drawable.clear_dom_storage_disabled);
        }

        // Set the clear form data preference icon if the API < 26.  It has no effect on newer versions of Android.
        if (Build.VERSION.SDK_INT < 26) {
            if (clearEverything || sharedPreferences.getBoolean("clear_form_data", true)) {
                clearFormDataPreference.setIcon(R.drawable.clear_form_data_enabled);
            } else {
                clearFormDataPreference.setIcon(R.drawable.clear_form_data_disabled);
            }
        }

        // Set the clear logcat preference icon.
        if (clearEverything || sharedPreferences.getBoolean(getString(R.string.clear_logcat_key), true)) {
            clearLogcatPreference.setIcon(R.drawable.clear_logcat_enabled);
        } else {
            clearLogcatPreference.setIcon(R.drawable.clear_logcat_disabled);
        }

        // Set the clear cache preference icon.
        if (clearEverything || sharedPreferences.getBoolean("clear_cache", true)) {
            clearCachePreference.setIcon(R.drawable.clear_cache_enabled);
        } else {
            clearCachePreference.setIcon(R.drawable.clear_cache_disabled);
        }

        // Set the open intents in new tab preference icon.
        if (sharedPreferences.getBoolean("open_intents_in_new_tab", true)) {
            openIntentsInNewTabPreference.setIcon(R.drawable.tab_enabled);
        } else {
            openIntentsInNewTabPreference.setIcon(R.drawable.tab_disabled);
        }

        // Set the swipe to refresh preference icon.
        if (sharedPreferences.getBoolean("swipe_to_refresh", true))
            swipeToRefreshPreference.setIcon(R.drawable.refresh_enabled);
        else
            swipeToRefreshPreference.setIcon(R.drawable.refresh_disabled);

        // Set the download with external app preference icon.
        if (sharedPreferences.getBoolean(getString(R.string.download_with_external_app_key), false))
            downloadWithExternalAppPreference.setIcon(R.drawable.download_with_external_app_enabled);
        else
            downloadWithExternalAppPreference.setIcon(R.drawable.download_with_external_app_disabled);

        // Set the scroll app bar preference icon.
        if (sharedPreferences.getBoolean(getString(R.string.scroll_app_bar_key), true))
            scrollAppBarPreference.setIcon(R.drawable.app_bar_enabled);
        else
            scrollAppBarPreference.setIcon(R.drawable.app_bar_disabled);

        // Set the bottom app bar preference icon.
        if (sharedPreferences.getBoolean(getString(R.string.bottom_app_bar_key), false))
            bottomAppBarPreference.setIcon(R.drawable.bottom_app_bar_enabled);
        else
            bottomAppBarPreference.setIcon(R.drawable.bottom_app_bar_disabled);

        // Set the display additional app bar icons preference icon.
        if (sharedPreferences.getBoolean(getString(R.string.display_additional_app_bar_icons_key), false))
            displayAdditionalAppBarIconsPreference.setIcon(R.drawable.more_enabled);
        else
            displayAdditionalAppBarIconsPreference.setIcon(R.drawable.more_disabled);

        // Set the WebView theme preference icon.
        switch (webViewThemeEntryNumber) {
            case 0:  // The system default WebView theme is selected.
                // Set the icon according to the app theme.
                if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {
                    webViewThemePreference.setIcon(R.drawable.webview_light_theme);
                } else {
                    webViewThemePreference.setIcon(R.drawable.webview_dark_theme);
                }
                break;

            case 1:  // The light WebView theme is selected.
                // Set the icon.
                webViewThemePreference.setIcon(R.drawable.webview_light_theme);
                break;

            case 2:  // The dark WebView theme is selected.
                // Set the icon.
                webViewThemePreference.setIcon(R.drawable.webview_dark_theme);
                break;
        }

        // Set the wide viewport preference icon.
        if (sharedPreferences.getBoolean("wide_viewport", true)) {
            wideViewportPreference.setIcon(R.drawable.wide_viewport_enabled);
        } else {
            wideViewportPreference.setIcon(R.drawable.wide_viewport_disabled);
        }

        // Set the display webpage images preference icon.
        if (sharedPreferences.getBoolean("display_webpage_images", true)) {
            displayWebpageImagesPreference.setIcon(R.drawable.images_enabled);
        } else {
            displayWebpageImagesPreference.setIcon(R.drawable.images_disabled);
        }
    }

    // The listener should be unregistered when the app is paused.
    @Override
    public void onPause() {
        // Run the default commands.
        super.onPause();

        // Get a handle for the shared preferences.
        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();

        // Remove the incorrect lint warning below that the shared preferences might be null.
        assert sharedPreferences != null;

        // Unregister the shared preference listener.
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    // The listener should be re-registered when the app is resumed.
    @Override
    public void onResume() {
        // Run the default commands.
        super.onResume();

        // Get a new shared preference change listener.
        sharedPreferenceChangeListener = getSharedPreferenceChangeListener(requireContext());

        // Get a handle for the shared preferences.
        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();

        // Remove the incorrect lint warning below that the shared preferences might be null.
        assert sharedPreferences != null;

        // Re-register the shared preference listener.
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    // The context must be passed to the shared preference change listener or else any calls to the system `getString()` will crash if the app has been restarted.
    // This can be removed at some future point, perhaps after the switch to PreferenceScreenCompat.  It isn't an issue in Privacy Cell.
    private SharedPreferences.OnSharedPreferenceChangeListener getSharedPreferenceChangeListener(Context context) {
        // Return the shared preference change listener.
        return (SharedPreferences sharedPreferences, String key) -> {
            switch (key) {
                case "javascript":
                    // Update the icons and the DOM storage preference status.
                    if (sharedPreferences.getBoolean("javascript", false)) {  // The JavaScript preference is enabled.
                        // Update the icon for the JavaScript preference.
                        javaScriptPreference.setIcon(R.drawable.javascript_enabled);

                        // Update the status of the DOM storage preference.
                        domStoragePreference.setEnabled(true);

                        // Update the icon for the DOM storage preference.
                        if (sharedPreferences.getBoolean("dom_storage", false)) {
                            domStoragePreference.setIcon(R.drawable.dom_storage_enabled);
                        } else {
                            domStoragePreference.setIcon(R.drawable.dom_storage_disabled);
                        }
                    } else {  // The JavaScript preference is disabled.
                        // Update the icon for the JavaScript preference.
                        javaScriptPreference.setIcon(R.drawable.privacy_mode);

                        // Update the status of the DOM storage preference.
                        domStoragePreference.setEnabled(false);

                        // Set the icon for DOM storage preference to be ghosted.
                        domStoragePreference.setIcon(R.drawable.dom_storage_ghosted);
                    }
                    break;

                case "cookies":
                    // Update the icon.
                    if (sharedPreferences.getBoolean(context.getString(R.string.cookies_key), false)) {
                        cookiesPreference.setIcon(R.drawable.cookies_enabled);
                    } else {
                        cookiesPreference.setIcon(R.drawable.cookies_disabled);
                    }
                    break;

                case "dom_storage":
                    // Update the icon.
                    if (sharedPreferences.getBoolean("dom_storage", false)) {
                        domStoragePreference.setIcon(R.drawable.dom_storage_enabled);
                    } else {
                        domStoragePreference.setIcon(R.drawable.dom_storage_disabled);
                    }
                    break;

                // Save form data can be removed once the minimum API >= 26.
                case "save_form_data":
                    // Update the icon.
                    if (sharedPreferences.getBoolean("save_form_data", false)) {
                        formDataPreference.setIcon(R.drawable.form_data_enabled);
                    } else {
                        formDataPreference.setIcon(R.drawable.form_data_disabled);
                    }
                    break;

                case "user_agent":
                    // Get the new user agent name.
                    String newUserAgentName = sharedPreferences.getString("user_agent", context.getString(R.string.user_agent_default_value));

                    // Get the array position for the new user agent name.
                    int newUserAgentArrayPosition = userAgentNamesArray.getPosition(newUserAgentName);

                    // Get the translated new user agent name.
                    String translatedNewUserAgentName = translatedUserAgentNamesArray[newUserAgentArrayPosition];

                    // Populate the user agent summary.
                    switch (newUserAgentArrayPosition) {
                        case MainWebViewActivity.SETTINGS_WEBVIEW_DEFAULT_USER_AGENT:
                            // Get the user agent text from the webview (which changes based on the version of Android and WebView installed).
                            userAgentPreference.setSummary(translatedNewUserAgentName + ":\n" + defaultUserAgent);

                            // Disable the custom user agent preference.
                            customUserAgentPreference.setEnabled(false);

                            // Set the custom user agent preference icon.
                            customUserAgentPreference.setIcon(R.drawable.custom_user_agent_ghosted);
                            break;

                        case MainWebViewActivity.SETTINGS_CUSTOM_USER_AGENT:
                            // Set the summary text.
                            userAgentPreference.setSummary(R.string.custom_user_agent);

                            // Enable the custom user agent preference.
                            customUserAgentPreference.setEnabled(true);

                            // Set the custom user agent preference icon.
                            customUserAgentPreference.setIcon(R.drawable.custom_user_agent_enabled);
                            break;

                        default:
                            // Get the user agent summary from the user agent data array.
                            userAgentPreference.setSummary(translatedNewUserAgentName + ":\n" + userAgentDataArray[newUserAgentArrayPosition]);

                            // Disable the custom user agent preference.
                            customUserAgentPreference.setEnabled(false);

                            // Set the custom user agent preference icon.
                            customUserAgentPreference.setIcon(R.drawable.custom_user_agent_ghosted);
                    }
                    break;

                case "custom_user_agent":
                    // Set the new custom user agent as the summary text for the preference.
                    customUserAgentPreference.setSummary(sharedPreferences.getString("custom_user_agent", context.getString(R.string.custom_user_agent_default_value)));
                    break;

                case "x_requested_with_header":
                    // Update the icon.
                    if (sharedPreferences.getBoolean(context.getString(R.string.x_requested_with_header_key), true))
                        xRequestedWithHeaderPreference.setIcon(R.drawable.x_requested_with_header_enabled);
                    else
                        xRequestedWithHeaderPreference.setIcon(R.drawable.x_requested_with_header_disabled);

                    // Restart Privacy Browser.
                    restartPrivacyBrowser();
                    break;

                case "incognito_mode":
                    // Update the icon.
                    if (sharedPreferences.getBoolean("incognito_mode", false))
                        incognitoModePreference.setIcon(R.drawable.incognito_mode_enabled);
                    else
                        incognitoModePreference.setIcon(R.drawable.incognito_mode_disabled);
                    break;

                case "allow_screenshots":
                    // Update the icon.
                    if (sharedPreferences.getBoolean(context.getString(R.string.allow_screenshots_key), false))
                        allowScreenshotsPreference.setIcon(R.drawable.allow_screenshots_enabled);
                    else
                        allowScreenshotsPreference.setIcon(R.drawable.allow_screenshots_disabled);

                    // Restart Privacy Browser.
                    restartPrivacyBrowser();
                    break;

                case "easylist":
                    // Update the icon.
                    if (sharedPreferences.getBoolean("easylist", true))
                        easyListPreference.setIcon(R.drawable.block_ads_enabled);
                    else
                        easyListPreference.setIcon(R.drawable.block_ads_disabled);
                    break;

                case "easyprivacy":
                    // Update the icon.
                    if (sharedPreferences.getBoolean("easyprivacy", true))
                        easyPrivacyPreference.setIcon(R.drawable.block_tracking_enabled);
                    else
                        easyPrivacyPreference.setIcon(R.drawable.block_tracking_disabled);
                    break;

                case "fanboys_annoyance_list":
                    boolean currentFanboyAnnoyanceList = sharedPreferences.getBoolean("fanboys_annoyance_list", true);
                    boolean currentFanboySocialBlockingList = sharedPreferences.getBoolean("fanboys_social_blocking_list", true);

                    // Update the Fanboy icons.
                    if (currentFanboyAnnoyanceList) {  // Fanboy's annoyance list is enabled.
                        // Update the Fanboy's annoyance list icon.
                        fanboyAnnoyanceListPreference.setIcon(R.drawable.social_media_enabled);

                        // Update the Fanboy's social blocking list icon.
                        fanboySocialBlockingListPreference.setIcon(R.drawable.social_media_ghosted);
                    } else {  // Fanboy's annoyance list is disabled.
                        // Update the Fanboy's annoyance list icon.
                        fanboyAnnoyanceListPreference.setIcon(R.drawable.social_media_disabled);

                        // Update the Fanboy's social blocking list icon.
                        if (currentFanboySocialBlockingList) {
                            fanboySocialBlockingListPreference.setIcon(R.drawable.social_media_enabled);
                        } else {
                            fanboySocialBlockingListPreference.setIcon(R.drawable.social_media_disabled);
                        }
                    }

                    // Only enable Fanboy's social blocking list preference if Fanboy's annoyance list preference is disabled.
                    fanboySocialBlockingListPreference.setEnabled(!currentFanboyAnnoyanceList);
                    break;

                case "fanboys_social_blocking_list":
                    // Update the icon.
                    if (sharedPreferences.getBoolean("fanboys_social_blocking_list", true)) {
                        fanboySocialBlockingListPreference.setIcon(R.drawable.social_media_enabled);
                    } else {
                        fanboySocialBlockingListPreference.setIcon(R.drawable.social_media_disabled);
                    }
                    break;

                case "ultralist":
                    // Update the icon.
                    if (sharedPreferences.getBoolean("ultralist", true)) {
                        ultraListPreference.setIcon(R.drawable.block_ads_enabled);
                    } else {
                        ultraListPreference.setIcon(R.drawable.block_ads_disabled);
                    }
                    break;

                case "ultraprivacy":
                    // Update the icon.
                    if (sharedPreferences.getBoolean("ultraprivacy", true)) {
                        ultraPrivacyPreference.setIcon(R.drawable.block_tracking_enabled);
                    } else {
                        ultraPrivacyPreference.setIcon(R.drawable.block_tracking_disabled);
                    }
                    break;

                case "block_all_third_party_requests":
                    // Update the icon.
                    if (sharedPreferences.getBoolean("block_all_third_party_requests", false)) {
                        blockAllThirdPartyRequestsPreference.setIcon(R.drawable.block_all_third_party_requests_enabled);
                    } else {
                        blockAllThirdPartyRequestsPreference.setIcon(R.drawable.block_all_third_party_requests_disabled);
                    }
                    break;

                case "tracking_queries":
                    // Update the icon.
                    if (sharedPreferences.getBoolean(context.getString(R.string.tracking_queries_key), true)) {
                        trackingQueriesPreference.setIcon(R.drawable.modify_url_enabled);
                    } else {
                        trackingQueriesPreference.setIcon(R.drawable.modify_url_disabled);
                    }
                    break;

                case "amp_redirects":
                    // Update the icon.
                    if (sharedPreferences.getBoolean(context.getString(R.string.amp_redirects_key), true)) {
                        ampRedirectsPreference.setIcon(R.drawable.modify_url_enabled);
                    } else {
                        ampRedirectsPreference.setIcon(R.drawable.modify_url_disabled);
                    }
                    break;

                case "search":
                    // Store the new search string.
                    String newSearchString = sharedPreferences.getString("search", context.getString(R.string.search_default_value));

                    // Update the search and search custom URL preferences.
                    if (newSearchString.equals("Custom URL")) {  // `Custom URL` is selected.
                        // Set the summary text to `R.string.custom_url`, which is translated.
                        searchPreference.setSummary(R.string.custom_url);

                        // Enable the search custom URL preference.
                        searchCustomURLPreference.setEnabled(true);

                        // Set the search custom URL preference icon.
                        searchCustomURLPreference.setIcon(R.drawable.search_custom_enabled);
                    } else {  // `Custom URL` is not selected.
                        // Set the summary text to `newSearchString`.
                        searchPreference.setSummary(newSearchString);

                        // Disable `searchCustomURLPreference`.
                        searchCustomURLPreference.setEnabled(false);

                        // Set the search custom URL preference icon.
                        searchCustomURLPreference.setIcon(R.drawable.search_custom_ghosted);
                    }
                    break;

                case "search_custom_url":
                    // Set the new search custom URL as the summary text for the preference.
                    searchCustomURLPreference.setSummary(sharedPreferences.getString("search_custom_url", context.getString(R.string.search_custom_url_default_value)));
                    break;

                case "proxy":
                    // Get current proxy string.
                    String currentProxyString = sharedPreferences.getString("proxy", context.getString(R.string.proxy_default_value));

                    // Update the summary text for the proxy preference.
                    switch (currentProxyString) {
                        case ProxyHelper.NONE:
                            proxyPreference.setSummary(context.getString(R.string.no_proxy_enabled));
                            break;

                        case ProxyHelper.TOR:
                            proxyPreference.setSummary(context.getString(R.string.tor_enabled));
                            break;

                        case ProxyHelper.I2P:
                            proxyPreference.setSummary(context.getString(R.string.i2p_enabled));
                            break;

                        case ProxyHelper.CUSTOM:
                            proxyPreference.setSummary(context.getString(R.string.custom_proxy));
                            break;
                    }

                    // Update the status of the custom URL preference.
                    proxyCustomUrlPreference.setEnabled(currentProxyString.equals(ProxyHelper.CUSTOM));

                    // Update the icons.
                    if (currentProxyString.equals(ProxyHelper.NONE)) {  // Proxying is disabled.
                        // Set the main proxy icon to be disabled
                        proxyPreference.setIcon(R.drawable.proxy_disabled);

                        // Set the custom proxy URL icon to be ghosted.
                        proxyCustomUrlPreference.setIcon(R.drawable.proxy_ghosted);
                    } else {  // Proxying is enabled.
                        // Set the main proxy icon to be enabled.
                        proxyPreference.setIcon(R.drawable.proxy_enabled);

                        /// Set the custom proxy URL icon according to its status.
                        if (proxyCustomUrlPreference.isEnabled()) {
                            proxyCustomUrlPreference.setIcon(R.drawable.proxy_enabled);
                        } else {
                            proxyCustomUrlPreference.setIcon(R.drawable.proxy_ghosted);
                        }
                    }
                    break;

                case "proxy_custom_url":
                    // Set the summary text for the proxy custom URL.
                    proxyCustomUrlPreference.setSummary(sharedPreferences.getString(context.getString(R.string.proxy_custom_url_key), context.getString(R.string.proxy_custom_url_default_value)));
                    break;

                case "full_screen_browsing_mode":
                    if (sharedPreferences.getBoolean("full_screen_browsing_mode", false)) {  // Full screen browsing is enabled.
                        // Set the full screen browsing mode preference icon.
                        fullScreenBrowsingModePreference.setIcon(R.drawable.full_screen_enabled);

                        // Set the hide app bar preference icon.
                        if (sharedPreferences.getBoolean("hide_app_bar", true)) {
                            hideAppBarPreference.setIcon(R.drawable.app_bar_enabled);
                        } else {
                            hideAppBarPreference.setIcon(R.drawable.app_bar_disabled);
                        }
                    } else {  // Full screen browsing is disabled.
                        // Update the icons.
                        fullScreenBrowsingModePreference.setIcon(R.drawable.full_screen_disabled);
                        hideAppBarPreference.setIcon(R.drawable.app_bar_ghosted);
                    }
                    break;

                case "hide_app_bar":
                    // Update the icon.
                    if (sharedPreferences.getBoolean("hide_app_bar", true)) {
                        hideAppBarPreference.setIcon(R.drawable.app_bar_enabled);
                    } else {  // Hide app bar is disabled.
                        // Set the icon according to the theme.
                        hideAppBarPreference.setIcon(R.drawable.app_bar_disabled);
                    }
                    break;

                case "clear_everything":
                    // Store the new clear everything status
                    boolean newClearEverythingBoolean = sharedPreferences.getBoolean("clear_everything", true);

                    // Update the status of the clear and exit preferences.
                    clearCookiesPreference.setEnabled(!newClearEverythingBoolean);
                    clearDomStoragePreference.setEnabled(!newClearEverythingBoolean);
                    clearFormDataPreference.setEnabled(!newClearEverythingBoolean);  // This line can be removed once the minimum API >= 26.
                    clearLogcatPreference.setEnabled(!newClearEverythingBoolean);
                    clearCachePreference.setEnabled(!newClearEverythingBoolean);

                    // Update the clear everything preference icon.
                    if (newClearEverythingBoolean) {
                        clearEverythingPreference.setIcon(R.drawable.clear_everything_enabled);
                    } else {
                        clearEverythingPreference.setIcon(R.drawable.clear_everything_disabled);
                    }

                    // Update the clear cookies preference icon.
                    if (newClearEverythingBoolean || sharedPreferences.getBoolean("clear_cookies", true)) {
                        clearCookiesPreference.setIcon(R.drawable.clear_cookies_enabled);
                    } else {
                        clearCookiesPreference.setIcon(R.drawable.clear_cookies_disabled);
                    }

                    // Update the clear dom storage preference icon.
                    if (newClearEverythingBoolean || sharedPreferences.getBoolean("clear_dom_storage", true)) {
                        clearDomStoragePreference.setIcon(R.drawable.clear_dom_storage_enabled);
                    } else {
                        clearDomStoragePreference.setIcon(R.drawable.clear_dom_storage_disabled);
                    }

                    // Update the clear form data preference icon if the API < 26.
                    if (Build.VERSION.SDK_INT < 26) {
                        if (newClearEverythingBoolean || sharedPreferences.getBoolean("clear_form_data", true)) {
                            clearFormDataPreference.setIcon(R.drawable.clear_form_data_enabled);
                        } else {
                            clearFormDataPreference.setIcon(R.drawable.clear_form_data_disabled);
                        }
                    }

                    // Update the clear logcat preference icon.
                    if (newClearEverythingBoolean || sharedPreferences.getBoolean(context.getString(R.string.clear_logcat_key), true)) {
                        clearLogcatPreference.setIcon(R.drawable.clear_logcat_enabled);
                    } else {
                        clearLogcatPreference.setIcon(R.drawable.clear_cache_disabled);
                    }

                    // Update the clear cache preference icon.
                    if (newClearEverythingBoolean || sharedPreferences.getBoolean("clear_cache", true)) {
                        clearCachePreference.setIcon(R.drawable.clear_cache_enabled);
                    } else {
                        clearCachePreference.setIcon(R.drawable.clear_cache_disabled);
                    }
                    break;

                case "clear_cookies":
                    // Update the icon.
                    if (sharedPreferences.getBoolean("clear_cookies", true)) {
                        clearCookiesPreference.setIcon(R.drawable.clear_cookies_enabled);
                    } else {
                        clearCookiesPreference.setIcon(R.drawable.clear_cookies_disabled);
                    }
                    break;

                case "clear_dom_storage":
                    // Update the icon.
                    if (sharedPreferences.getBoolean("clear_dom_storage", true)) {
                        clearDomStoragePreference.setIcon(R.drawable.clear_dom_storage_enabled);
                    } else {
                        clearDomStoragePreference.setIcon(R.drawable.clear_dom_storage_disabled);
                    }
                    break;

                // This section can be removed once the minimum API >= 26.
                case "clear_form_data":
                    // Update the icon.
                    if (sharedPreferences.getBoolean("clear_form_data", true)) {
                        clearFormDataPreference.setIcon(R.drawable.clear_form_data_enabled);
                    } else {
                        clearFormDataPreference.setIcon(R.drawable.clear_form_data_disabled);
                    }
                    break;

                case "clear_logcat":
                    // Update the icon.
                    if (sharedPreferences.getBoolean(context.getString(R.string.clear_logcat_key), true)) {
                        clearLogcatPreference.setIcon(R.drawable.clear_logcat_enabled);
                    } else {
                        clearLogcatPreference.setIcon(R.drawable.clear_logcat_disabled);
                    }
                    break;

                case "clear_cache":
                    // Update the icon.
                    if (sharedPreferences.getBoolean("clear_cache", true)) {
                        clearCachePreference.setIcon(R.drawable.clear_cache_enabled);
                    } else {
                        clearCachePreference.setIcon(R.drawable.clear_cache_disabled);
                    }
                    break;

                case "homepage":
                    // Set the new homepage URL as the summary text for the Homepage preference.
                    homepagePreference.setSummary(sharedPreferences.getString("homepage", context.getString(R.string.homepage_default_value)));
                    break;

                case "font_size":
                    // Update the font size summary text.
                    fontSizePreference.setSummary(sharedPreferences.getString("font_size", context.getString(R.string.font_size_default_value)) + "%");
                    break;

                case "open_intents_in_new_tab":
                    // Update the icon.
                    if (sharedPreferences.getBoolean("open_intents_in_new_tab", true))
                        openIntentsInNewTabPreference.setIcon(R.drawable.tab_enabled);
                    else
                        openIntentsInNewTabPreference.setIcon(R.drawable.tab_disabled);
                    break;

                case "swipe_to_refresh":
                    // Update the icon.
                    if (sharedPreferences.getBoolean("swipe_to_refresh", true))
                        swipeToRefreshPreference.setIcon(R.drawable.refresh_enabled);
                    else
                        swipeToRefreshPreference.setIcon(R.drawable.refresh_disabled);
                    break;

                case "download_with_external_app":
                    // Update the icon.
                    if (sharedPreferences.getBoolean(context.getString(R.string.download_with_external_app_key), false))
                        downloadWithExternalAppPreference.setIcon(R.drawable.download_with_external_app_enabled);
                    else
                        downloadWithExternalAppPreference.setIcon(R.drawable.download_with_external_app_disabled);
                    break;

                case "scroll_app_bar":
                    // Update the icon.
                    if (sharedPreferences.getBoolean(context.getString(R.string.scroll_app_bar_key), true))
                        scrollAppBarPreference.setIcon(R.drawable.app_bar_enabled);
                    else
                        scrollAppBarPreference.setIcon(R.drawable.app_bar_disabled);

                    break;

                case "bottom_app_bar":
                    // Update the icon.
                    if (sharedPreferences.getBoolean(context.getString(R.string.bottom_app_bar_key), false))
                        bottomAppBarPreference.setIcon(R.drawable.bottom_app_bar_enabled);
                    else
                        bottomAppBarPreference.setIcon(R.drawable.bottom_app_bar_disabled);

                    // Restart Privacy Browser.
                    restartPrivacyBrowser();
                    break;

                case "display_additional_app_bar_icons":
                    // Update the icon.
                    if (sharedPreferences.getBoolean(context.getString(R.string.display_additional_app_bar_icons_key), false))
                        displayAdditionalAppBarIconsPreference.setIcon(R.drawable.more_enabled);
                    else
                        displayAdditionalAppBarIconsPreference.setIcon(R.drawable.more_disabled);
                    break;

                case "app_theme":
                    // Get the new theme.
                    String newAppTheme = sharedPreferences.getString("app_theme", context.getString(R.string.app_theme_default_value));

                    // Update the system according to the new theme.  A switch statement cannot be used because the theme entry values string array is not a compile-time constant.
                    if (newAppTheme.equals(appThemeEntryValuesStringArray[1])) {  // The light theme is selected.
                        // Update the theme preference summary text.
                        appThemePreference.setSummary(appThemeEntriesStringArray[1]);

                        // Apply the new theme.
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    } else if (newAppTheme.equals(appThemeEntryValuesStringArray[2])) {  // The dark theme is selected.
                        // Update the theme preference summary text.
                        appThemePreference.setSummary(appThemeEntriesStringArray[2]);

                        // Apply the new theme.
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    } else {  // The system default theme is selected.
                        // Update the theme preference summary text.
                        appThemePreference.setSummary(appThemeEntriesStringArray[0]);

                        // Apply the new theme.
                        if (Build.VERSION.SDK_INT >= 28) {  // The system default theme is supported.
                            // Follow the system default theme.
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        } else {// The system default theme is not supported.
                            // Follow the battery saver mode.
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                        }
                    }

                    // Update the current theme status.
                    currentThemeStatus = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                    break;

                case "webview_theme":
                    // Get the new WebView theme.
                    String newWebViewTheme = sharedPreferences.getString("webview_theme", context.getString(R.string.webview_theme_default_value));

                    // Define a new WebView theme entry number.
                    int newWebViewThemeEntryNumber;

                    // Get the webView theme entry number that matches the new WebView theme.  A switch statement cannot be used because the theme entry values string array is not a compile time constant.
                    if (newWebViewTheme.equals(webViewThemeEntriesStringArray[1])) {  // The light theme is selected.
                        // Store the new WebView theme entry number.
                        newWebViewThemeEntryNumber = 1;
                    } else if (newWebViewTheme.equals(webViewThemeEntryValuesStringArray[2])) {  // The dark theme is selected.
                        // Store the WebView theme entry number.
                        newWebViewThemeEntryNumber = 2;
                    } else {  // The system default theme is selected.
                        // Store the WebView theme entry number.
                        newWebViewThemeEntryNumber = 0;
                    }

                    // Update the icon.
                    switch (newWebViewThemeEntryNumber) {
                        case 0:  // The system default WebView theme is selected.
                            // Set the icon.
                            if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {
                                webViewThemePreference.setIcon(R.drawable.webview_light_theme);
                            } else {
                                webViewThemePreference.setIcon(R.drawable.webview_dark_theme);
                            }
                            break;

                        case 1:  // The light theme is selected.
                            // Set the icon.
                            webViewThemePreference.setIcon(R.drawable.webview_light_theme);
                            break;

                        case 2:  // The dark theme is selected.
                            // Set the icon.
                            webViewThemePreference.setIcon(R.drawable.webview_dark_theme);
                            break;
                    }

                    // Set the current theme as the summary text for the preference.
                    webViewThemePreference.setSummary(webViewThemeEntriesStringArray[newWebViewThemeEntryNumber]);
                    break;

                case "wide_viewport":
                    // Update the icon.
                    if (sharedPreferences.getBoolean("wide_viewport", true)) {
                        wideViewportPreference.setIcon(R.drawable.wide_viewport_enabled);
                    } else {
                        wideViewportPreference.setIcon(R.drawable.wide_viewport_disabled);
                    }
                    break;

                case "display_webpage_images":
                    // Update the icon.
                    if (sharedPreferences.getBoolean("display_webpage_images", true)) {
                        displayWebpageImagesPreference.setIcon(R.drawable.images_enabled);
                    } else {
                        displayWebpageImagesPreference.setIcon(R.drawable.images_disabled);
                    }
                    break;
            }
        };
    }

    private void restartPrivacyBrowser() {
        // Create an intent to restart Privacy Browser.
        Intent restartIntent = requireActivity().getParentActivityIntent();

        // Assert that the intent is not null to remove the lint error below.
        assert restartIntent != null;

        // `Intent.FLAG_ACTIVITY_CLEAR_TASK` removes all activities from the stack.  It requires `Intent.FLAG_ACTIVITY_NEW_TASK`.
        restartIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Create a handler to restart the activity.
        Handler restartHandler = new Handler(Looper.getMainLooper());

        // Create a runnable to restart the activity.
        Runnable restartRunnable = () -> {
            // Restart the activity.
            startActivity(restartIntent);

            // Kill this instance of Privacy Browser.  Otherwise, the app exhibits sporadic behavior after the restart.
            System.exit(0);
        };

        // Restart the activity after 400 milliseconds, so that the app has enough time to save the change to the preference.
        restartHandler.postDelayed(restartRunnable, 400);
    }
}
