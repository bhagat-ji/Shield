/*
 * Copyright © 2017-2020 Soren Stoutner <soren@stoutner.com>.
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

package com.stoutner.privacybrowser.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.preference.PreferenceManager;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.stoutner.privacybrowser.R;
import com.stoutner.privacybrowser.dialogs.AboutViewSourceDialog;
import com.stoutner.privacybrowser.helpers.ProxyHelper;
import com.stoutner.privacybrowser.viewmodelfactories.WebViewSourceFactory;
import com.stoutner.privacybrowser.viewmodels.WebViewSource;

import java.net.Proxy;
import java.util.Locale;

public class ViewSourceActivity extends AppCompatActivity {
    // `activity` is used in `onCreate()` and `goBack()`.
    private Activity activity;

    // The color spans are used in `onCreate()` and `highlightUrlText()`.
    private ForegroundColorSpan redColorSpan;
    private ForegroundColorSpan initialGrayColorSpan;
    private ForegroundColorSpan finalGrayColorSpan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Get a handle for the shared preferences.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // Get the screenshot preference.
        boolean allowScreenshots = sharedPreferences.getBoolean("allow_screenshots", false);

        // Disable screenshots if not allowed.
        if (!allowScreenshots) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        // Set the theme.
        setTheme(R.style.PrivacyBrowser);

        // Run the default commands.
        super.onCreate(savedInstanceState);

        // Get the launching intent
        Intent intent = getIntent();

        // Get the information from the intent.
        String userAgent = intent.getStringExtra("user_agent");
        String currentUrl = intent.getStringExtra("current_url");

        // Remove the incorrect lint warning below that the user agent might be null.
        assert userAgent != null;

        // Store a handle for the current activity.
        activity = this;

        // Set the content view.
        setContentView(R.layout.view_source_coordinatorlayout);

        // Get a handle for the toolbar.
        Toolbar toolbar = findViewById(R.id.view_source_toolbar);

        // Set the support action bar.
        setSupportActionBar(toolbar);

        // Get a handle for the action bar.
        final ActionBar actionBar = getSupportActionBar();

        // Remove the incorrect lint warning that the action bar might be null.
        assert actionBar != null;

        // Add the custom layout to the action bar.
        actionBar.setCustomView(R.layout.view_source_app_bar);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

        // Get handles for the views.
        EditText urlEditText = findViewById(R.id.url_edittext);
        TextView requestHeadersTextView = findViewById(R.id.request_headers);
        TextView responseMessageTextView = findViewById(R.id.response_message);
        TextView responseHeadersTextView = findViewById(R.id.response_headers);
        TextView responseBodyTextView = findViewById(R.id.response_body);
        ProgressBar progressBar = findViewById(R.id.progress_bar);
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.view_source_swiperefreshlayout);

        // Populate the URL text box.
        urlEditText.setText(currentUrl);

        // Initialize the gray foreground color spans for highlighting the URLs.  The deprecated `getResources()` must be used until API >= 23.
        initialGrayColorSpan = new ForegroundColorSpan(getResources().getColor(R.color.gray_500));
        finalGrayColorSpan = new ForegroundColorSpan(getResources().getColor(R.color.gray_500));

        // Get the current theme status.
        int currentThemeStatus = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        // Set the red color span according to the theme.
        if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {
            redColorSpan = new ForegroundColorSpan(getResources().getColor(R.color.red_a700));
        } else {
            redColorSpan = new ForegroundColorSpan(getResources().getColor(R.color.red_900));
        }

        // Apply text highlighting to the URL.
        highlightUrlText();

        // Get a handle for the input method manager, which is used to hide the keyboard.
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        // Remove the lint warning that the input method manager might be null.
        assert inputMethodManager != null;

        // Remove the formatting from the URL when the user is editing the text.
        urlEditText.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if (hasFocus) {  // The user is editing `urlTextBox`.
                // Remove the highlighting.
                urlEditText.getText().removeSpan(redColorSpan);
                urlEditText.getText().removeSpan(initialGrayColorSpan);
                urlEditText.getText().removeSpan(finalGrayColorSpan);
            } else {  // The user has stopped editing `urlTextBox`.
                // Hide the soft keyboard.
                inputMethodManager.hideSoftInputFromWindow(urlEditText.getWindowToken(), 0);

                // Move to the beginning of the string.
                urlEditText.setSelection(0);

                // Reapply the highlighting.
                highlightUrlText();
            }
        });

        // Set the refresh color scheme according to the theme.
        if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {
            swipeRefreshLayout.setColorSchemeResources(R.color.blue_700);
        } else {
            swipeRefreshLayout.setColorSchemeResources(R.color.violet_500);
        }

        // Initialize a color background typed value.
        TypedValue colorBackgroundTypedValue = new TypedValue();

        // Get the color background from the theme.
        getTheme().resolveAttribute(android.R.attr.colorBackground, colorBackgroundTypedValue, true);

        // Get the color background int from the typed value.
        int colorBackgroundInt = colorBackgroundTypedValue.data;

        // Set the swipe refresh background color.
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(colorBackgroundInt);

        // Get the Do Not Track status.
        boolean doNotTrack = sharedPreferences.getBoolean("do_not_track", false);

        // Instantiate a locale string.
        String localeString;

        // Populate the locale string.
        if (Build.VERSION.SDK_INT >= 24) {  // SDK >= 24 has a list of locales.
            // Get the list of locales.
            LocaleList localeList = getResources().getConfiguration().getLocales();

            // Initialize a string builder to extract the locales from the list.
            StringBuilder localesStringBuilder = new StringBuilder();

            // Initialize a `q` value, which is used by `WebView` to indicate the order of importance of the languages.
            int q = 10;

            // Populate the string builder with the contents of the locales list.
            for (int i = 0; i < localeList.size(); i++) {
                // Append a comma if there is already an item in the string builder.
                if (i > 0) {
                    localesStringBuilder.append(",");
                }

                // Get the locale from the list.
                Locale locale = localeList.get(i);

                // Add the locale to the string.  `locale` by default displays as `en_US`, but WebView uses the `en-US` format.
                localesStringBuilder.append(locale.getLanguage());
                localesStringBuilder.append("-");
                localesStringBuilder.append(locale.getCountry());

                // If not the first locale, append `;q=0.x`, which drops by .1 for each removal from the main locale until q=0.1.
                if (q < 10) {
                    localesStringBuilder.append(";q=0.");
                    localesStringBuilder.append(q);
                }

                // Decrement `q` if it is greater than 1.
                if (q > 1) {
                    q--;
                }

                // Add a second entry for the language only portion of the locale.
                localesStringBuilder.append(",");
                localesStringBuilder.append(locale.getLanguage());

                // Append `1;q=0.x`, which drops by .1 for each removal form the main locale until q=0.1.
                localesStringBuilder.append(";q=0.");
                localesStringBuilder.append(q);

                // Decrement `q` if it is greater than 1.
                if (q > 1) {
                    q--;
                }
            }

            // Store the populated string builder in the locale string.
            localeString = localesStringBuilder.toString();
        } else {  // SDK < 24 only has a primary locale.
            // Store the locale in the locale string.
            localeString = Locale.getDefault().toString();
        }

        // Instantiate the proxy helper.
        ProxyHelper proxyHelper = new ProxyHelper();

        // Get the current proxy.
        Proxy proxy = proxyHelper.getCurrentProxy(this);

        // Make the progress bar visible.
        progressBar.setVisibility(View.VISIBLE);

        // Set the progress bar to be indeterminate.
        progressBar.setIndeterminate(true);

        // Instantiate the WebView source factory.
        ViewModelProvider.Factory webViewSourceFactory = new WebViewSourceFactory(currentUrl, userAgent, doNotTrack, localeString, proxy, MainWebViewActivity.executorService);

        // Instantiate the WebView source view model class.
        final WebViewSource webViewSource = new ViewModelProvider(this, webViewSourceFactory).get(WebViewSource.class);

        // Create a source observer.
        webViewSource.observeSource().observe(this, sourceStringArray -> {
            // Populate the text views.  This can take a long time, and freezes the user interface, if the response body is particularly large.
            requestHeadersTextView.setText(sourceStringArray[0]);
            responseMessageTextView.setText(sourceStringArray[1]);
            responseHeadersTextView.setText(sourceStringArray[2]);
            responseBodyTextView.setText(sourceStringArray[3]);

            // Hide the progress bar.
            progressBar.setIndeterminate(false);
            progressBar.setVisibility(View.GONE);

            //Stop the swipe to refresh indicator if it is running
            swipeRefreshLayout.setRefreshing(false);
        });

        // Create an error observer.
        webViewSource.observeErrors().observe(this, errorString -> {
            // Display an error snackbar if the string is not `""`.
            if (!errorString.equals("")) {
                Snackbar.make(swipeRefreshLayout, errorString, Snackbar.LENGTH_LONG).show();
            }
        });

        // Implement swipe to refresh.
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Make the progress bar visible.
            progressBar.setVisibility(View.VISIBLE);

            // Set the progress bar to be indeterminate.
            progressBar.setIndeterminate(true);

            // Get the URL.
            String urlString = urlEditText.getText().toString();

            // Get the updated source.
            webViewSource.updateSource(urlString);
        });

        // Set the go button on the keyboard to request new source data.
        urlEditText.setOnKeyListener((View v, int keyCode, KeyEvent event) -> {
            // Request new source data if the enter key was pressed.
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                // Hide the soft keyboard.
                inputMethodManager.hideSoftInputFromWindow(urlEditText.getWindowToken(), 0);

                // Remove the focus from the URL box.
                urlEditText.clearFocus();

                // Make the progress bar visible.
                progressBar.setVisibility(View.VISIBLE);

                // Set the progress bar to be indeterminate.
                progressBar.setIndeterminate(true);

                // Get the URL.
                String urlString = urlEditText.getText().toString();

                // Get the updated source.
                webViewSource.updateSource(urlString);

                // Consume the key press.
                return true;
            } else {
                // Do not consume the key press.
                return false;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu.  This adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.view_source_options_menu, menu);

        // Display the menu.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        // Get a handle for the about alert dialog.
        DialogFragment aboutDialogFragment = new AboutViewSourceDialog();

        // Show the about alert dialog.
        aboutDialogFragment.show(getSupportFragmentManager(), getString(R.string.about));

        // Consume the event.
        return true;
    }

    public void goBack(View view) {
        // Go home.
        NavUtils.navigateUpFromSameTask(activity);
    }

    private void highlightUrlText() {
        // Get a handle for the URL EditText.
        EditText urlEditText = findViewById(R.id.url_edittext);

        // Get the URL string.
        String urlString = urlEditText.getText().toString();

        // Highlight the URL according to the protocol.
        if (urlString.startsWith("file://")) {  // This is a file URL.
            // De-emphasize only the protocol.
            urlEditText.getText().setSpan(initialGrayColorSpan, 0, 7, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        } else if (urlString.startsWith("content://")) {
            // De-emphasize only the protocol.
            urlEditText.getText().setSpan(initialGrayColorSpan, 0, 10, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        } else {  // This is a web URL.
            // Get the index of the `/` immediately after the domain name.
            int endOfDomainName = urlString.indexOf("/", (urlString.indexOf("//") + 2));

            // Create a base URL string.
            String baseUrl;

            // Get the base URL.
            if (endOfDomainName > 0) {  // There is at least one character after the base URL.
                // Get the base URL.
                baseUrl = urlString.substring(0, endOfDomainName);
            } else {  // There are no characters after the base URL.
                // Set the base URL to be the entire URL string.
                baseUrl = urlString;
            }

            // Get the index of the last `.` in the domain.
            int lastDotIndex = baseUrl.lastIndexOf(".");

            // Get the index of the penultimate `.` in the domain.
            int penultimateDotIndex = baseUrl.lastIndexOf(".", lastDotIndex - 1);

            // Markup the beginning of the URL.
            if (urlString.startsWith("http://")) {  // Highlight the protocol of connections that are not encrypted.
                urlEditText.getText().setSpan(redColorSpan, 0, 7, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                // De-emphasize subdomains.
                if (penultimateDotIndex > 0) {  // There is more than one subdomain in the domain name.
                    urlEditText.getText().setSpan(initialGrayColorSpan, 7, penultimateDotIndex + 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                }
            } else if (urlString.startsWith("https://")) {  // De-emphasize the protocol of connections that are encrypted.
                if (penultimateDotIndex > 0) {  // There is more than one subdomain in the domain name.
                    // De-emphasize the protocol and the additional subdomains.
                    urlEditText.getText().setSpan(initialGrayColorSpan, 0, penultimateDotIndex + 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                } else {  // There is only one subdomain in the domain name.
                    // De-emphasize only the protocol.
                    urlEditText.getText().setSpan(initialGrayColorSpan, 0, 8, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                }
            }

            // De-emphasize the text after the domain name.
            if (endOfDomainName > 0) {
                urlEditText.getText().setSpan(finalGrayColorSpan, endOfDomainName, urlString.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            }
        }
    }
}