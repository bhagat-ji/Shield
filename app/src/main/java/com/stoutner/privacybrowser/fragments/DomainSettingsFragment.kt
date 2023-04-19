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

package com.stoutner.privacybrowser.fragments

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView

import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.activities.DOMAINS_CUSTOM_USER_AGENT
import com.stoutner.privacybrowser.activities.DOMAINS_SYSTEM_DEFAULT_USER_AGENT
import com.stoutner.privacybrowser.activities.DOMAINS_WEBVIEW_DEFAULT_USER_AGENT
import com.stoutner.privacybrowser.activities.SETTINGS_CUSTOM_USER_AGENT
import com.stoutner.privacybrowser.activities.SETTINGS_WEBVIEW_DEFAULT_USER_AGENT
import com.stoutner.privacybrowser.activities.UNRECOGNIZED_USER_AGENT
import com.stoutner.privacybrowser.activities.DomainsActivity
import com.stoutner.privacybrowser.helpers.DomainsDatabaseHelper

import java.lang.IndexOutOfBoundsException
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

class DomainSettingsFragment : Fragment() {
    // Define the class variables.
    private var scrollY = 0

    companion object {
        // Define the public constants.
        const val DATABASE_ID = "database_id"
        const val SCROLL_Y = "scroll_y"

        // Define the public variables.  `databaseId` is public so it can be accessed from `DomainsActivity`.
        var databaseId = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Run the default commands.
        super.onCreate(savedInstanceState)

        // Store the arguments in class variables.
        databaseId = requireArguments().getInt(DATABASE_ID)
        scrollY = requireArguments().getInt(SCROLL_Y)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout.  The fragment will take care of attaching the root automatically.
        val domainSettingsView = inflater.inflate(R.layout.domain_settings_fragment, container, false)

        // Get the current theme status.
        val currentThemeStatus = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        // Get a handle for the shared preference.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Store the default settings.
        val defaultUserAgentName = sharedPreferences.getString(getString(R.string.user_agent_key), getString(R.string.user_agent_default_value))
        val defaultCustomUserAgentString = sharedPreferences.getString(getString(R.string.custom_user_agent_key), getString(R.string.custom_user_agent_default_value))
        val defaultFontSizeString = sharedPreferences.getString(getString(R.string.font_size_key), getString(R.string.font_size_default_value))
        val defaultSwipeToRefresh = sharedPreferences.getBoolean(getString(R.string.swipe_to_refresh_key), true)
        val defaultWebViewTheme = sharedPreferences.getString(getString(R.string.webview_theme_key), getString(R.string.webview_theme_default_value))
        val defaultWideViewport = sharedPreferences.getBoolean(getString(R.string.wide_viewport_key), true)
        val defaultDisplayWebpageImages = sharedPreferences.getBoolean(getString(R.string.display_webpage_images_key), true)

        // Get handles for the views.
        val domainSettingsScrollView = domainSettingsView.findViewById<ScrollView>(R.id.domain_settings_scrollview)
        val domainNameEditText = domainSettingsView.findViewById<EditText>(R.id.domain_settings_name_edittext)
        val javaScriptImageView = domainSettingsView.findViewById<ImageView>(R.id.javascript_imageview)
        val javaScriptSwitch = domainSettingsView.findViewById<SwitchCompat>(R.id.javascript_switch)
        val cookiesImageView = domainSettingsView.findViewById<ImageView>(R.id.cookies_imageview)
        val cookiesSwitch = domainSettingsView.findViewById<SwitchCompat>(R.id.cookies_switch)
        val domStorageImageView = domainSettingsView.findViewById<ImageView>(R.id.dom_storage_imageview)
        val domStorageSwitch = domainSettingsView.findViewById<SwitchCompat>(R.id.dom_storage_switch)
        val formDataImageView = domainSettingsView.findViewById<ImageView>(R.id.form_data_imageview) // The form data views can be remove once the minimum API >= 26.
        val formDataSwitch = domainSettingsView.findViewById<SwitchCompat>(R.id.form_data_switch) // The form data views can be remove once the minimum API >= 26.
        val easyListImageView = domainSettingsView.findViewById<ImageView>(R.id.easylist_imageview)
        val easyListSwitch = domainSettingsView.findViewById<SwitchCompat>(R.id.easylist_switch)
        val easyPrivacyImageView = domainSettingsView.findViewById<ImageView>(R.id.easyprivacy_imageview)
        val easyPrivacySwitch = domainSettingsView.findViewById<SwitchCompat>(R.id.easyprivacy_switch)
        val fanboysAnnoyanceListImageView = domainSettingsView.findViewById<ImageView>(R.id.fanboys_annoyance_list_imageview)
        val fanboysAnnoyanceListSwitch = domainSettingsView.findViewById<SwitchCompat>(R.id.fanboys_annoyance_list_switch)
        val fanboysSocialBlockingListImageView = domainSettingsView.findViewById<ImageView>(R.id.fanboys_social_blocking_list_imageview)
        val fanboysSocialBlockingListSwitch = domainSettingsView.findViewById<SwitchCompat>(R.id.fanboys_social_blocking_list_switch)
        val ultraListImageView = domainSettingsView.findViewById<ImageView>(R.id.ultralist_imageview)
        val ultraListSwitch = domainSettingsView.findViewById<SwitchCompat>(R.id.ultralist_switch)
        val ultraPrivacyImageView = domainSettingsView.findViewById<ImageView>(R.id.ultraprivacy_imageview)
        val ultraPrivacySwitch = domainSettingsView.findViewById<SwitchCompat>(R.id.ultraprivacy_switch)
        val blockAllThirdPartyRequestsImageView = domainSettingsView.findViewById<ImageView>(R.id.block_all_third_party_requests_imageview)
        val blockAllThirdPartyRequestsSwitch = domainSettingsView.findViewById<SwitchCompat>(R.id.block_all_third_party_requests_switch)
        val userAgentLinearLayout = domainSettingsView.findViewById<LinearLayout>(R.id.user_agent_linear_layout)
        val userAgentSpinner = domainSettingsView.findViewById<Spinner>(R.id.user_agent_spinner)
        val userAgentTextView = domainSettingsView.findViewById<TextView>(R.id.user_agent_textview)
        val customUserAgentEditText = domainSettingsView.findViewById<EditText>(R.id.custom_user_agent_edittext)
        val fontSizeSpinner = domainSettingsView.findViewById<Spinner>(R.id.font_size_spinner)
        val defaultFontSizeTextView = domainSettingsView.findViewById<TextView>(R.id.default_font_size_textview)
        val customFontSizeEditText = domainSettingsView.findViewById<EditText>(R.id.custom_font_size_edittext)
        val swipeToRefreshImageView = domainSettingsView.findViewById<ImageView>(R.id.swipe_to_refresh_imageview)
        val swipeToRefreshSpinner = domainSettingsView.findViewById<Spinner>(R.id.swipe_to_refresh_spinner)
        val swipeToRefreshTextView = domainSettingsView.findViewById<TextView>(R.id.swipe_to_refresh_textview)
        val webViewThemeLinearLayout = domainSettingsView.findViewById<LinearLayout>(R.id.webview_theme_linearlayout)
        val webViewThemeImageView = domainSettingsView.findViewById<ImageView>(R.id.webview_theme_imageview)
        val webViewThemeSpinner = domainSettingsView.findViewById<Spinner>(R.id.webview_theme_spinner)
        val webViewThemeTextView = domainSettingsView.findViewById<TextView>(R.id.webview_theme_textview)
        val wideViewportImageView = domainSettingsView.findViewById<ImageView>(R.id.wide_viewport_imageview)
        val wideViewportSpinner = domainSettingsView.findViewById<Spinner>(R.id.wide_viewport_spinner)
        val wideViewportTextView = domainSettingsView.findViewById<TextView>(R.id.wide_viewport_textview)
        val displayWebpageImagesImageView = domainSettingsView.findViewById<ImageView>(R.id.display_webpage_images_imageview)
        val displayWebpageImagesSpinner = domainSettingsView.findViewById<Spinner>(R.id.display_webpage_images_spinner)
        val displayImagesTextView = domainSettingsView.findViewById<TextView>(R.id.display_webpage_images_textview)
        val pinnedSslCertificateImageView = domainSettingsView.findViewById<ImageView>(R.id.pinned_ssl_certificate_imageview)
        val pinnedSslCertificateSwitch = domainSettingsView.findViewById<SwitchCompat>(R.id.pinned_ssl_certificate_switch)
        val savedSslCardView = domainSettingsView.findViewById<CardView>(R.id.saved_ssl_certificate_cardview)
        val savedSslCertificateLinearLayout = domainSettingsView.findViewById<LinearLayout>(R.id.saved_ssl_certificate_linearlayout)
        val savedSslCertificateRadioButton = domainSettingsView.findViewById<RadioButton>(R.id.saved_ssl_certificate_radiobutton)
        val savedSslIssuedToCNameTextView = domainSettingsView.findViewById<TextView>(R.id.saved_ssl_certificate_issued_to_cname)
        val savedSslIssuedToONameTextView = domainSettingsView.findViewById<TextView>(R.id.saved_ssl_certificate_issued_to_oname)
        val savedSslIssuedToUNameTextView = domainSettingsView.findViewById<TextView>(R.id.saved_ssl_certificate_issued_to_uname)
        val savedSslIssuedByCNameTextView = domainSettingsView.findViewById<TextView>(R.id.saved_ssl_certificate_issued_by_cname)
        val savedSslIssuedByONameTextView = domainSettingsView.findViewById<TextView>(R.id.saved_ssl_certificate_issued_by_oname)
        val savedSslIssuedByUNameTextView = domainSettingsView.findViewById<TextView>(R.id.saved_ssl_certificate_issued_by_uname)
        val savedSslStartDateTextView = domainSettingsView.findViewById<TextView>(R.id.saved_ssl_certificate_start_date)
        val savedSslEndDateTextView = domainSettingsView.findViewById<TextView>(R.id.saved_ssl_certificate_end_date)
        val currentSslCardView = domainSettingsView.findViewById<CardView>(R.id.current_website_certificate_cardview)
        val currentWebsiteCertificateLinearLayout = domainSettingsView.findViewById<LinearLayout>(R.id.current_website_certificate_linearlayout)
        val currentWebsiteCertificateRadioButton = domainSettingsView.findViewById<RadioButton>(R.id.current_website_certificate_radiobutton)
        val currentSslIssuedToCNameTextView = domainSettingsView.findViewById<TextView>(R.id.current_website_certificate_issued_to_cname)
        val currentSslIssuedToONameTextView = domainSettingsView.findViewById<TextView>(R.id.current_website_certificate_issued_to_oname)
        val currentSslIssuedToUNameTextView = domainSettingsView.findViewById<TextView>(R.id.current_website_certificate_issued_to_uname)
        val currentSslIssuedByCNameTextView = domainSettingsView.findViewById<TextView>(R.id.current_website_certificate_issued_by_cname)
        val currentSslIssuedByONameTextView = domainSettingsView.findViewById<TextView>(R.id.current_website_certificate_issued_by_oname)
        val currentSslIssuedByUNameTextView = domainSettingsView.findViewById<TextView>(R.id.current_website_certificate_issued_by_uname)
        val currentSslStartDateTextView = domainSettingsView.findViewById<TextView>(R.id.current_website_certificate_start_date)
        val currentSslEndDateTextView = domainSettingsView.findViewById<TextView>(R.id.current_website_certificate_end_date)
        val noCurrentWebsiteCertificateTextView = domainSettingsView.findViewById<TextView>(R.id.no_current_website_certificate)
        val pinnedIpAddressesImageView = domainSettingsView.findViewById<ImageView>(R.id.pinned_ip_addresses_imageview)
        val pinnedIpAddressesSwitch = domainSettingsView.findViewById<SwitchCompat>(R.id.pinned_ip_addresses_switch)
        val savedIpAddressesCardView = domainSettingsView.findViewById<CardView>(R.id.saved_ip_addresses_cardview)
        val savedIpAddressesLinearLayout = domainSettingsView.findViewById<LinearLayout>(R.id.saved_ip_addresses_linearlayout)
        val savedIpAddressesRadioButton = domainSettingsView.findViewById<RadioButton>(R.id.saved_ip_addresses_radiobutton)
        val savedIpAddressesTextView = domainSettingsView.findViewById<TextView>(R.id.saved_ip_addresses_textview)
        val currentIpAddressesCardView = domainSettingsView.findViewById<CardView>(R.id.current_ip_addresses_cardview)
        val currentIpAddressesLinearLayout = domainSettingsView.findViewById<LinearLayout>(R.id.current_ip_addresses_linearlayout)
        val currentIpAddressesRadioButton = domainSettingsView.findViewById<RadioButton>(R.id.current_ip_addresses_radiobutton)
        val currentIpAddressesTextView = domainSettingsView.findViewById<TextView>(R.id.current_ip_addresses_textview)

        // Hide the WebView theme linear layout if the API < 29.
        if (Build.VERSION.SDK_INT < 29)
            webViewThemeLinearLayout.visibility = View.GONE

        // Setup the pinned labels.
        val cNameLabel = getString(R.string.common_name)
        val oNameLabel = getString(R.string.organization)
        val uNameLabel = getString(R.string.organizational_unit)
        val startDateLabel = getString(R.string.start_date)
        val endDateLabel = getString(R.string.end_date)

        // Initialize the database handler.
        val domainsDatabaseHelper = DomainsDatabaseHelper(requireContext())

        // Get the database cursor for this ID.
        val domainCursor = domainsDatabaseHelper.getCursorForId(databaseId)

        // Move to the first row.
        domainCursor.moveToFirst()

        // Save the cursor entries as variables.
        val domainNameString = domainCursor.getString(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.DOMAIN_NAME))
        val javaScriptInt = domainCursor.getInt(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_JAVASCRIPT))
        val cookiesInt = domainCursor.getInt(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.COOKIES))
        val domStorageInt = domainCursor.getInt(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_DOM_STORAGE))
        val formDataInt = domainCursor.getInt(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_FORM_DATA)) // Form data can be remove once the minimum API >= 26.
        val easyListInt = domainCursor.getInt(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_EASYLIST))
        val easyPrivacyInt = domainCursor.getInt(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_EASYPRIVACY))
        val fanboysAnnoyanceListInt = domainCursor.getInt(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_FANBOYS_ANNOYANCE_LIST))
        val fanboysSocialBlockingListInt = domainCursor.getInt(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_FANBOYS_SOCIAL_BLOCKING_LIST))
        val ultraListInt = domainCursor.getInt(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ULTRALIST))
        val ultraPrivacyInt = domainCursor.getInt(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.ENABLE_ULTRAPRIVACY))
        val blockAllThirdPartyRequestsInt = domainCursor.getInt(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.BLOCK_ALL_THIRD_PARTY_REQUESTS))
        val currentUserAgentName = domainCursor.getString(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.USER_AGENT))
        val fontSizeInt = domainCursor.getInt(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.FONT_SIZE))
        val swipeToRefreshInt = domainCursor.getInt(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SWIPE_TO_REFRESH))
        val webViewThemeInt = domainCursor.getInt(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.WEBVIEW_THEME))
        val wideViewportInt = domainCursor.getInt(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.WIDE_VIEWPORT))
        val displayImagesInt = domainCursor.getInt(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.DISPLAY_IMAGES))
        val pinnedSslCertificateInt = domainCursor.getInt(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.PINNED_SSL_CERTIFICATE))
        val savedSslIssuedToCNameString = domainCursor.getString(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_TO_COMMON_NAME))
        val savedSslIssuedToONameString = domainCursor.getString(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_TO_ORGANIZATION))
        val savedSslIssuedToUNameString = domainCursor.getString(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_TO_ORGANIZATIONAL_UNIT))
        val savedSslIssuedByCNameString = domainCursor.getString(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_BY_COMMON_NAME))
        val savedSslIssuedByONameString = domainCursor.getString(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_BY_ORGANIZATION))
        val savedSslIssuedByUNameString = domainCursor.getString(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_ISSUED_BY_ORGANIZATIONAL_UNIT))
        val pinnedIpAddressesInt = domainCursor.getInt(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.PINNED_IP_ADDRESSES))
        val savedIpAddresses = domainCursor.getString(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.IP_ADDRESSES))

        // Get the SSL dates from the database.
        val savedSslStartDateLong = domainCursor.getLong(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_START_DATE))
        val savedSslEndDateLong = domainCursor.getLong(domainCursor.getColumnIndexOrThrow(DomainsDatabaseHelper.SSL_END_DATE))

        // Initialize the saved SSL certificate date variables.
        var savedSslStartDate: Date? = null
        var savedSslEndDate: Date? = null

        // Only get the saved SSL certificate dates from the cursor if they are not set to `0`.
        if (savedSslStartDateLong != 0L)
            savedSslStartDate = Date(savedSslStartDateLong)
        if (savedSslEndDateLong != 0L)
            savedSslEndDate = Date(savedSslEndDateLong)

        // Create array adapters for the spinners.
        val translatedUserAgentArrayAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.translated_domain_settings_user_agent_names, R.layout.spinner_item)
        val fontSizeArrayAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.font_size_array, R.layout.spinner_item)
        val swipeToRefreshArrayAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.swipe_to_refresh_array, R.layout.spinner_item)
        val webViewThemeArrayAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.webview_theme_array, R.layout.spinner_item)
        val wideViewportArrayAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.wide_viewport_array, R.layout.spinner_item)
        val displayImagesArrayAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.display_webpage_images_array, R.layout.spinner_item)

        // Set the drop down view resource on the spinners.
        translatedUserAgentArrayAdapter.setDropDownViewResource(R.layout.domain_settings_spinner_dropdown_items)
        fontSizeArrayAdapter.setDropDownViewResource(R.layout.domain_settings_spinner_dropdown_items)
        swipeToRefreshArrayAdapter.setDropDownViewResource(R.layout.domain_settings_spinner_dropdown_items)
        webViewThemeArrayAdapter.setDropDownViewResource(R.layout.domain_settings_spinner_dropdown_items)
        wideViewportArrayAdapter.setDropDownViewResource(R.layout.domain_settings_spinner_dropdown_items)
        displayImagesArrayAdapter.setDropDownViewResource(R.layout.domain_settings_spinner_dropdown_items)

        // Set the array adapters for the spinners.
        userAgentSpinner.adapter = translatedUserAgentArrayAdapter
        fontSizeSpinner.adapter = fontSizeArrayAdapter
        swipeToRefreshSpinner.adapter = swipeToRefreshArrayAdapter
        webViewThemeSpinner.adapter = webViewThemeArrayAdapter
        wideViewportSpinner.adapter = wideViewportArrayAdapter
        displayWebpageImagesSpinner.adapter = displayImagesArrayAdapter

        // Create a spannable string builder for each TextView that needs multiple colors of text.
        val savedSslIssuedToCNameStringBuilder = SpannableStringBuilder(cNameLabel + savedSslIssuedToCNameString)
        val savedSslIssuedToONameStringBuilder = SpannableStringBuilder(oNameLabel + savedSslIssuedToONameString)
        val savedSslIssuedToUNameStringBuilder = SpannableStringBuilder(uNameLabel + savedSslIssuedToUNameString)
        val savedSslIssuedByCNameStringBuilder = SpannableStringBuilder(cNameLabel + savedSslIssuedByCNameString)
        val savedSslIssuedByONameStringBuilder = SpannableStringBuilder(oNameLabel + savedSslIssuedByONameString)
        val savedSslIssuedByUNameStringBuilder = SpannableStringBuilder(uNameLabel + savedSslIssuedByUNameString)

        // Create the date spannable string builders.
        val savedSslStartDateStringBuilder: SpannableStringBuilder = if (savedSslStartDate == null)
            SpannableStringBuilder(startDateLabel)
        else
            SpannableStringBuilder(startDateLabel + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG).format(savedSslStartDate))

        val savedSslEndDateStringBuilder: SpannableStringBuilder = if (savedSslEndDate == null)
            SpannableStringBuilder(endDateLabel)
        else
            SpannableStringBuilder(endDateLabel + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG).format(savedSslEndDate))

        // Create the color spans.
        val blueColorSpan = ForegroundColorSpan(requireContext().getColor(R.color.alt_blue_text))
        val redColorSpan = ForegroundColorSpan(requireContext().getColor(R.color.red_text))

        // Set the domain name from the the database cursor.
        domainNameEditText.setText(domainNameString)

        // Update the certificates' Common Name color when the domain name text changes.
        domainNameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // Do nothing.
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // Do nothing.
            }

            override fun afterTextChanged(s: Editable) {
                // Get the new domain name.
                val newDomainName = domainNameEditText.text.toString()

                // Check the saved SSL certificate against the new domain name.
                val savedSslMatchesNewDomainName = checkDomainNameAgainstCertificate(newDomainName, savedSslIssuedToCNameString)

                // Create a spannable string builder for the saved certificate's Common Name.
                val savedSslCNameStringBuilder = SpannableStringBuilder(cNameLabel + savedSslIssuedToCNameString)

                // Format the saved certificate's Common Name color.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
                if (savedSslMatchesNewDomainName) {
                    savedSslCNameStringBuilder.setSpan(blueColorSpan, cNameLabel.length, savedSslCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                } else {
                    savedSslCNameStringBuilder.setSpan(redColorSpan, cNameLabel.length, savedSslCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                }

                // Update the saved SSL issued to CName text view.
                savedSslIssuedToCNameTextView.text = savedSslCNameStringBuilder

                // Update the current website certificate if it exists.
                if (DomainsActivity.sslIssuedToCName != null) {
                    // Check the current website certificate against the new domain name.
                    val currentSslMatchesNewDomainName = checkDomainNameAgainstCertificate(newDomainName, DomainsActivity.sslIssuedToCName)

                    // Create a spannable string builder for the current website certificate's Common Name.
                    val currentSslCNameStringBuilder = SpannableStringBuilder(cNameLabel + DomainsActivity.sslIssuedToCName)

                    // Format the current certificate Common Name color.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
                    if (currentSslMatchesNewDomainName) {
                        currentSslCNameStringBuilder.setSpan(blueColorSpan, cNameLabel.length, currentSslCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                    } else {
                        currentSslCNameStringBuilder.setSpan(redColorSpan, cNameLabel.length, currentSslCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                    }

                    // Update the current SSL issued to CName text view.
                    currentSslIssuedToCNameTextView.text = currentSslCNameStringBuilder
                }
            }
        })

        // Set the switch positions.
        javaScriptSwitch.isChecked = (javaScriptInt == 1)
        cookiesSwitch.isChecked = (cookiesInt == 1)
        domStorageSwitch.isChecked = (domStorageInt == 1)
        formDataSwitch.isChecked = (formDataInt == 1)  // Form data can be removed once the minimum API >= 26.
        easyListSwitch.isChecked = (easyListInt == 1)
        easyPrivacySwitch.isChecked = (easyPrivacyInt == 1)
        fanboysAnnoyanceListSwitch.isChecked = (fanboysAnnoyanceListInt == 1)
        fanboysSocialBlockingListSwitch.isChecked = (fanboysSocialBlockingListInt == 1)
        ultraListSwitch.isChecked = (ultraListInt == 1)
        ultraPrivacySwitch.isChecked = (ultraPrivacyInt == 1)
        blockAllThirdPartyRequestsSwitch.isChecked = (blockAllThirdPartyRequestsInt == 1)
        pinnedSslCertificateSwitch.isChecked = (pinnedSslCertificateInt == 1)
        pinnedIpAddressesSwitch.isChecked = (pinnedIpAddressesInt == 1)

        // Set the switch icon colors.
        cookiesImageView.isSelected = (cookiesInt == 1)
        domStorageImageView.isSelected = (domStorageInt == 1)
        formDataImageView.isSelected = (formDataInt == 1)  // Form data can be removed once the minimum API >= 26.
        easyListImageView.isSelected = (easyListInt == 1)
        easyPrivacyImageView.isSelected = (easyPrivacyInt == 1)
        fanboysAnnoyanceListImageView.isSelected = (fanboysAnnoyanceListInt == 1)
        fanboysSocialBlockingListImageView.isSelected = (fanboysSocialBlockingListInt == 1)
        ultraListImageView.isSelected = (ultraListInt == 1)
        ultraPrivacyImageView.isSelected = (ultraPrivacyInt == 1)
        blockAllThirdPartyRequestsImageView.isSelected = (blockAllThirdPartyRequestsInt == 1)
        pinnedSslCertificateImageView.isSelected = (pinnedSslCertificateInt == 1)
        pinnedIpAddressesImageView.isSelected = (pinnedIpAddressesInt == 1)

        // Set the JavaScript icon.
        if (javaScriptInt == 1)
            javaScriptImageView.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.javascript_enabled, null))
        else
            javaScriptImageView.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.privacy_mode, null))

        // Set the DOM storage switch status based on the JavaScript status.
        domStorageSwitch.isEnabled = (javaScriptInt == 1)

        // Set the DOM storage icon ghosted status based on the JavaScript status.
        domStorageImageView.isEnabled = (javaScriptInt == 1)

        // Set the form data visibility.  Form data can be removed once the minimum API >= 26.
        if (Build.VERSION.SDK_INT >= 26) {
            // Hide the form data image view and switch.
            formDataImageView.visibility = View.GONE
            formDataSwitch.visibility = View.GONE
        }

        // Set Fanboy's Social Blocking List switch status based on the Annoyance List status.
        fanboysSocialBlockingListSwitch.isEnabled = (fanboysAnnoyanceListInt == 0)

        // Set the Social Blocking List icon ghosted status based on the Annoyance List status.
        fanboysSocialBlockingListImageView.isEnabled = (fanboysAnnoyanceListInt == 0)

        // Open the spinners when the text view is clicked.
        userAgentTextView.setOnClickListener { userAgentSpinner.performClick() }
        defaultFontSizeTextView.setOnClickListener { fontSizeSpinner.performClick() }
        swipeToRefreshTextView.setOnClickListener { swipeToRefreshSpinner.performClick() }
        webViewThemeTextView.setOnClickListener { webViewThemeSpinner.performClick() }
        wideViewportTextView.setOnClickListener { wideViewportSpinner.performClick() }
        displayImagesTextView.setOnClickListener { displayWebpageImagesSpinner.performClick() }

        // Inflated a WebView to get the default user agent.
        // `@SuppressLint("InflateParams")` removes the warning about using `null` as the `ViewGroup`, which in this case makes sense because the bare WebView should not be displayed on the screen.
        @SuppressLint("InflateParams") val bareWebViewLayout = inflater.inflate(R.layout.bare_webview, null, false)
        val bareWebView = bareWebViewLayout.findViewById<WebView>(R.id.bare_webview)
        val webViewDefaultUserAgentString = bareWebView.settings.userAgentString

        // Get a handle for the user agent array adapter.  This array does not contain the `System default` entry.
        val userAgentNamesArray = ArrayAdapter.createFromResource(requireContext(), R.array.user_agent_names, R.layout.spinner_item)

        // Get the positions of the user agent and the default user agent.
        val userAgentArrayPosition = userAgentNamesArray.getPosition(currentUserAgentName)
        val defaultUserAgentArrayPosition = userAgentNamesArray.getPosition(defaultUserAgentName)

        // Get a handle for the user agent data array.  This array does not contain the `System default` entry.
        val userAgentDataArray = resources.getStringArray(R.array.user_agent_data)

        // Set the user agent text.
        if (currentUserAgentName == getString(R.string.system_default_user_agent)) {  // Use the system default user agent.
            // Set the user agent according to the system default.
            when (defaultUserAgentArrayPosition) {
                // This is probably because it was set in an older version of Privacy Browser before the switch to persistent user agent names.
                UNRECOGNIZED_USER_AGENT -> userAgentTextView.text = defaultUserAgentName

                // Display the WebView default user agent.
                SETTINGS_WEBVIEW_DEFAULT_USER_AGENT -> userAgentTextView.text = webViewDefaultUserAgentString

                // Display the custom user agent.
                SETTINGS_CUSTOM_USER_AGENT -> userAgentTextView.text = defaultCustomUserAgentString

                // Get the user agent string from the user agent data array.
                else -> userAgentTextView.text = userAgentDataArray[defaultUserAgentArrayPosition]
            }

            // Set the background color to be transparent.
            userAgentLinearLayout.setBackgroundColor(getColor(requireContext(), R.color.transparent))
        } else if (userAgentArrayPosition == UNRECOGNIZED_USER_AGENT || currentUserAgentName == getString(R.string.custom_user_agent)) {
            // A custom user agent is stored in the current user agent name.  The second check is necessary in case the user did not change the default custom text.
            // Set the user agent spinner to `Custom user agent`.
            userAgentSpinner.setSelection(DOMAINS_CUSTOM_USER_AGENT)

            // Hide the user agent text view.
            userAgentTextView.visibility = View.GONE

            // Show the custom user agent edit text and set the current user agent name as the text.
            customUserAgentEditText.visibility = View.VISIBLE
            customUserAgentEditText.setText(currentUserAgentName)

            // Set the background color to be blue.
            userAgentLinearLayout.setBackgroundColor(getColor(requireContext(), R.color.blue_background))
        } else {  // The user agent name contains one of the canonical user agents.
            // Set the user agent spinner selection.  The spinner has one more entry at the beginning than the user agent data array, so the position must be incremented.
            userAgentSpinner.setSelection(userAgentArrayPosition + 1)

            // Show the user agent text view.
            userAgentTextView.visibility = View.VISIBLE

            // Hide the custom user agent edit text.
            customUserAgentEditText.visibility = View.GONE

            // Set the user agent text.
            if (userAgentArrayPosition == DOMAINS_WEBVIEW_DEFAULT_USER_AGENT) {  // The WebView default user agent is selected.
                // Display the WebView default user agent.
                userAgentTextView.text = webViewDefaultUserAgentString
            } else {  // A user agent besides the default is selected.
                // Get the user agent string from the user agent data array.  The spinner has one more entry at the beginning than the user agent data array, so the position must be incremented.
                userAgentTextView.text = userAgentDataArray[userAgentArrayPosition + 1]
            }

            // Set the background color to be blue.
            userAgentLinearLayout.setBackgroundColor(getColor(requireContext(), R.color.blue_background))
        }


        // Display the font size settings.
        if (fontSizeInt == 0) {  // `0` is the code for system default font size.
            // Set the font size to the system default.
            fontSizeSpinner.setSelection(0)

            // Show the default font size text view.
            defaultFontSizeTextView.visibility = View.VISIBLE

            // Hide the custom font size edit text.
            customFontSizeEditText.visibility = View.GONE

            // Set the default font size as the text of the custom font size edit text.  This way, if the user switches to custom it will already be populated.
            customFontSizeEditText.setText(defaultFontSizeString)
        } else {  // A custom font size is selected.
            // Set the spinner to the custom font size.
            fontSizeSpinner.setSelection(1)

            // Hide the default font size text view.
            defaultFontSizeTextView.visibility = View.GONE

            // Show the custom font size edit text.
            customFontSizeEditText.visibility = View.GONE

            // Set the custom font size.
            customFontSizeEditText.setText(fontSizeInt.toString())
        }

        // Initialize the default font size percentage string.
        val defaultFontSizePercentageString = "$defaultFontSizeString%"

        // Set the default font size text in the text view.
        defaultFontSizeTextView.text = defaultFontSizePercentageString

        // Select the swipe-to-refresh selection in the spinner.
        swipeToRefreshSpinner.setSelection(swipeToRefreshInt)

        // Set the swipe-to-refresh text.
        if (defaultSwipeToRefresh)
            swipeToRefreshTextView.text = swipeToRefreshArrayAdapter.getItem(DomainsDatabaseHelper.ENABLED)
        else
            swipeToRefreshTextView.text = swipeToRefreshArrayAdapter.getItem(DomainsDatabaseHelper.DISABLED)

        // Set the swipe-to-refresh icon and text view settings.
        when (swipeToRefreshInt) {
            DomainsDatabaseHelper.SYSTEM_DEFAULT -> {
                // Set the icon color.
                swipeToRefreshImageView.isSelected = defaultSwipeToRefresh

                // Show the swipe-to-refresh text view.
                swipeToRefreshTextView.visibility = View.VISIBLE
            }

            DomainsDatabaseHelper.ENABLED -> {
                // Set the icon color.
                swipeToRefreshImageView.isSelected = true

                // Hide the swipe-to-refresh text view.
                swipeToRefreshTextView.visibility = View.GONE
            }

            DomainsDatabaseHelper.DISABLED -> {
                // Set the icon color.
                swipeToRefreshImageView.isSelected = false

                // Hide the swipe-to-refresh text view.
                swipeToRefreshTextView.visibility = View.GONE
            }
        }

        // Get the WebView theme string arrays.
        val webViewThemeStringArray = resources.getStringArray(R.array.webview_theme_array)
        val webViewThemeEntryValuesStringArray = resources.getStringArray(R.array.webview_theme_entry_values)

        // Get the WebView theme entry number that matches the current WebView theme.
        val appWebViewThemeEntryNumber = when (defaultWebViewTheme) {
            webViewThemeEntryValuesStringArray[1] -> { 1 }  // The light theme is selected.
            webViewThemeEntryValuesStringArray[2] -> { 2 }  // The dark theme is selected.
            else -> { 0 }  // The system default theme is selected.
        }

        // Select the WebView theme in the spinner.
        webViewThemeSpinner.setSelection(webViewThemeInt)

        // Set the WebView theme text.
        if (appWebViewThemeEntryNumber == DomainsDatabaseHelper.SYSTEM_DEFAULT) {  // The app WebView theme is system default.
            // Set the text according to the current UI theme.
            if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO)
                webViewThemeTextView.text = webViewThemeStringArray[DomainsDatabaseHelper.LIGHT_THEME]
            else
                webViewThemeTextView.text = webViewThemeStringArray[DomainsDatabaseHelper.DARK_THEME]
        } else {  // The app WebView theme is not system default.
            // Set the text according to the app WebView theme.
            webViewThemeTextView.text = webViewThemeStringArray[appWebViewThemeEntryNumber]
        }

        // Set the WebView theme icon and text visibility.
        when (webViewThemeInt) {
            DomainsDatabaseHelper.SYSTEM_DEFAULT -> {
                // Set the icon color.
                when (appWebViewThemeEntryNumber) {
                    DomainsDatabaseHelper.SYSTEM_DEFAULT -> webViewThemeImageView.isSelected = (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO)
                    DomainsDatabaseHelper.LIGHT_THEME -> webViewThemeImageView.isSelected = true
                    DomainsDatabaseHelper.DARK_THEME -> webViewThemeImageView.isSelected = false
                }

                // Show the WebView theme text view.
                webViewThemeTextView.visibility = View.VISIBLE
            }

            DomainsDatabaseHelper.LIGHT_THEME -> {
                // Set the icon color.
                webViewThemeImageView.isSelected = true

                // Hide the WebView theme text view.
                webViewThemeTextView.visibility = View.GONE
            }

            DomainsDatabaseHelper.DARK_THEME -> {
                // Set the icon color.
                webViewThemeImageView.isSelected = false

                // Hide the WebView theme text view.
                webViewThemeTextView.visibility = View.GONE
            }
        }

        // Select the wide viewport in the spinner.
        wideViewportSpinner.setSelection(wideViewportInt)

        // Set the default wide viewport text.
        if (defaultWideViewport)
            wideViewportTextView.text = wideViewportArrayAdapter.getItem(DomainsDatabaseHelper.ENABLED)
        else wideViewportTextView.text = wideViewportArrayAdapter.getItem(DomainsDatabaseHelper.DISABLED)

        // Set the wide viewport icon and text view settings.
        when (wideViewportInt) {
            DomainsDatabaseHelper.SYSTEM_DEFAULT -> {
                // Set the icon color.
                wideViewportImageView.isSelected = defaultWideViewport

                // Show the wide viewport text view.
                wideViewportTextView.visibility = View.VISIBLE
            }

            DomainsDatabaseHelper.ENABLED -> {
                // Set the icon color.
                wideViewportImageView.isSelected = true

                // Hide the wide viewport text view.
                wideViewportTextView.visibility = View.GONE
            }

            DomainsDatabaseHelper.DISABLED -> {
                // Set the icon color.
                wideViewportImageView.isSelected = false

                // Hide the wide viewport text view.
                wideViewportTextView.visibility = View.GONE
            }
        }

        // Display the website images mode in the spinner.
        displayWebpageImagesSpinner.setSelection(displayImagesInt)

        // Set the default display images text.
        if (defaultDisplayWebpageImages)
            displayImagesTextView.text = displayImagesArrayAdapter.getItem(DomainsDatabaseHelper.ENABLED)
        else
            displayImagesTextView.text = displayImagesArrayAdapter.getItem(DomainsDatabaseHelper.DISABLED)

        // Set the display website images icon and text view settings.
        when (displayImagesInt) {
            DomainsDatabaseHelper.SYSTEM_DEFAULT -> {
                // Set the icon color.
                displayWebpageImagesImageView.isSelected = defaultDisplayWebpageImages

                // Show the display images text view.
                displayImagesTextView.visibility = View.VISIBLE
            }

            DomainsDatabaseHelper.ENABLED -> {
                // Set the icon color.
                displayWebpageImagesImageView.isSelected = true

                // Hide the display images text view.
                displayImagesTextView.visibility = View.GONE
            }

            DomainsDatabaseHelper.DISABLED -> {
                // Set the icon color.
                displayWebpageImagesImageView.isSelected = false

                // Hide the display images text view.
                displayImagesTextView.visibility = View.GONE
            }
        }

        // Store the current date.
        val currentDate = Calendar.getInstance().time

        // Setup the string builders to display the general certificate information in blue.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
        savedSslIssuedToONameStringBuilder.setSpan(blueColorSpan, oNameLabel.length, savedSslIssuedToONameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        savedSslIssuedToUNameStringBuilder.setSpan(blueColorSpan, uNameLabel.length, savedSslIssuedToUNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        savedSslIssuedByCNameStringBuilder.setSpan(blueColorSpan, cNameLabel.length, savedSslIssuedByCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        savedSslIssuedByONameStringBuilder.setSpan(blueColorSpan, oNameLabel.length, savedSslIssuedByONameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        savedSslIssuedByUNameStringBuilder.setSpan(blueColorSpan, uNameLabel.length, savedSslIssuedByUNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

        // Check the certificate Common Name against the domain name.
        val savedSslCommonNameMatchesDomainName = checkDomainNameAgainstCertificate(domainNameString, savedSslIssuedToCNameString)

        // Format the issued to Common Name color.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
        if (savedSslCommonNameMatchesDomainName)
            savedSslIssuedToCNameStringBuilder.setSpan(blueColorSpan, cNameLabel.length, savedSslIssuedToCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        else
            savedSslIssuedToCNameStringBuilder.setSpan(redColorSpan, cNameLabel.length, savedSslIssuedToCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

        //  Format the start date color.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
        if (savedSslStartDate != null && savedSslStartDate.after(currentDate))  // The certificate start date is in the future.
            savedSslStartDateStringBuilder.setSpan(redColorSpan, startDateLabel.length, savedSslStartDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        else  // The certificate start date is in the past.
            savedSslStartDateStringBuilder.setSpan(blueColorSpan, startDateLabel.length, savedSslStartDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

        // Format the end date color.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
        if (savedSslEndDate != null && savedSslEndDate.before(currentDate))  // The certificate end date is in the past.
            savedSslEndDateStringBuilder.setSpan(redColorSpan, endDateLabel.length, savedSslEndDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        else  // The certificate end date is in the future.
            savedSslEndDateStringBuilder.setSpan(blueColorSpan, endDateLabel.length, savedSslEndDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

        // Display the saved website SSL certificate strings.
        savedSslIssuedToCNameTextView.text = savedSslIssuedToCNameStringBuilder
        savedSslIssuedToONameTextView.text = savedSslIssuedToONameStringBuilder
        savedSslIssuedToUNameTextView.text = savedSslIssuedToUNameStringBuilder
        savedSslIssuedByCNameTextView.text = savedSslIssuedByCNameStringBuilder
        savedSslIssuedByONameTextView.text = savedSslIssuedByONameStringBuilder
        savedSslIssuedByUNameTextView.text = savedSslIssuedByUNameStringBuilder
        savedSslStartDateTextView.text = savedSslStartDateStringBuilder
        savedSslEndDateTextView.text = savedSslEndDateStringBuilder

        // Populate the current website SSL certificate if there is one.
        if (DomainsActivity.sslIssuedToCName != null) {
            // Get dates from the raw long values.
            val currentSslStartDate = Date(DomainsActivity.sslStartDateLong)
            val currentSslEndDate = Date(DomainsActivity.sslEndDateLong)

            // Create a spannable string builder for each text view that needs multiple colors of text.
            val currentSslIssuedToCNameStringBuilder = SpannableStringBuilder(cNameLabel + DomainsActivity.sslIssuedToCName)
            val currentSslIssuedToONameStringBuilder = SpannableStringBuilder(oNameLabel + DomainsActivity.sslIssuedToOName)
            val currentSslIssuedToUNameStringBuilder = SpannableStringBuilder(uNameLabel + DomainsActivity.sslIssuedToUName)
            val currentSslIssuedByCNameStringBuilder = SpannableStringBuilder(cNameLabel + DomainsActivity.sslIssuedByCName)
            val currentSslIssuedByONameStringBuilder = SpannableStringBuilder(oNameLabel + DomainsActivity.sslIssuedByOName)
            val currentSslIssuedByUNameStringBuilder = SpannableStringBuilder(uNameLabel + DomainsActivity.sslIssuedByUName)
            val currentSslStartDateStringBuilder = SpannableStringBuilder(startDateLabel + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG).format(currentSslStartDate))
            val currentSslEndDateStringBuilder = SpannableStringBuilder(endDateLabel + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG).format(currentSslEndDate))

            // Setup the string builders to display the general certificate information in blue.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
            currentSslIssuedToONameStringBuilder.setSpan(blueColorSpan, oNameLabel.length, currentSslIssuedToONameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            currentSslIssuedToUNameStringBuilder.setSpan(blueColorSpan, uNameLabel.length, currentSslIssuedToUNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            currentSslIssuedByCNameStringBuilder.setSpan(blueColorSpan, cNameLabel.length, currentSslIssuedByCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            currentSslIssuedByONameStringBuilder.setSpan(blueColorSpan, oNameLabel.length, currentSslIssuedByONameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            currentSslIssuedByUNameStringBuilder.setSpan(blueColorSpan, uNameLabel.length, currentSslIssuedByUNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

            // Check the certificate Common Name against the domain name.
            val currentSslCommonNameMatchesDomainName = checkDomainNameAgainstCertificate(domainNameString, DomainsActivity.sslIssuedToCName)

            // Format the issued to Common Name color.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
            if (currentSslCommonNameMatchesDomainName)
                currentSslIssuedToCNameStringBuilder.setSpan(blueColorSpan, cNameLabel.length, currentSslIssuedToCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            else
                currentSslIssuedToCNameStringBuilder.setSpan(redColorSpan, cNameLabel.length, currentSslIssuedToCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

            //  Format the start date color.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
            if (currentSslStartDate.after(currentDate))  // The certificate start date is in the future.
                currentSslStartDateStringBuilder.setSpan(redColorSpan, startDateLabel.length, currentSslStartDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            else  // The certificate start date is in the past.
                currentSslStartDateStringBuilder.setSpan(blueColorSpan, startDateLabel.length, currentSslStartDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

            // Format the end date color.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
            if (currentSslEndDate.before(currentDate))  // The certificate end date is in the past.
                currentSslEndDateStringBuilder.setSpan(redColorSpan, endDateLabel.length, currentSslEndDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            else  // The certificate end date is in the future.
                currentSslEndDateStringBuilder.setSpan(blueColorSpan, endDateLabel.length, currentSslEndDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

            // Display the current website SSL certificate strings.
            currentSslIssuedToCNameTextView.text = currentSslIssuedToCNameStringBuilder
            currentSslIssuedToONameTextView.text = currentSslIssuedToONameStringBuilder
            currentSslIssuedToUNameTextView.text = currentSslIssuedToUNameStringBuilder
            currentSslIssuedByCNameTextView.text = currentSslIssuedByCNameStringBuilder
            currentSslIssuedByONameTextView.text = currentSslIssuedByONameStringBuilder
            currentSslIssuedByUNameTextView.text = currentSslIssuedByUNameStringBuilder
            currentSslStartDateTextView.text = currentSslStartDateStringBuilder
            currentSslEndDateTextView.text = currentSslEndDateStringBuilder
        }

        // Set the initial display status of the SSL certificates card views.
        if (pinnedSslCertificateSwitch.isChecked) {  // An SSL certificate is pinned.
            // Set the visibility of the saved SSL certificate.
            if (savedSslIssuedToCNameString == null)
                savedSslCardView.visibility = View.GONE
            else
                savedSslCardView.visibility = View.VISIBLE

            // Set the visibility of the current website SSL certificate.
            if (DomainsActivity.sslIssuedToCName == null) {  // There is no current SSL certificate.
                // Hide the SSL certificate.
                currentSslCardView.visibility = View.GONE

                // Show the instruction.
                noCurrentWebsiteCertificateTextView.visibility = View.VISIBLE
            } else {  // There is a current SSL certificate.
                // Show the SSL certificate.
                currentSslCardView.visibility = View.VISIBLE

                // Hide the instruction.
                noCurrentWebsiteCertificateTextView.visibility = View.GONE
            }

            // Set the status of the radio buttons and the card view backgrounds.
            if (savedSslCardView.visibility == View.VISIBLE) {  // The saved SSL certificate is displayed.
                // Check the saved SSL certificate radio button.
                savedSslCertificateRadioButton.isChecked = true

                // Uncheck the current website SSL certificate radio button.
                currentWebsiteCertificateRadioButton.isChecked = false

                // Darken the background of the current website SSL certificate linear layout.
                currentWebsiteCertificateLinearLayout.setBackgroundResource(R.color.translucent_background)
            } else if (currentSslCardView.visibility == View.VISIBLE) {  // The saved SSL certificate is hidden but the current website SSL certificate is visible.
                // Check the current website SSL certificate radio button.
                currentWebsiteCertificateRadioButton.isChecked = true

                // Uncheck the saved SSL certificate radio button.
                savedSslCertificateRadioButton.isChecked = false
            } else {  // Neither SSL certificate is visible.
                // Uncheck both radio buttons.
                savedSslCertificateRadioButton.isChecked = false
                currentWebsiteCertificateRadioButton.isChecked = false
            }
        } else {  // An SSL certificate is not pinned.
            // Hide the SSl certificates and instructions.
            savedSslCardView.visibility = View.GONE
            currentSslCardView.visibility = View.GONE
            noCurrentWebsiteCertificateTextView.visibility = View.GONE

            // Uncheck the radio buttons.
            savedSslCertificateRadioButton.isChecked = false
            currentWebsiteCertificateRadioButton.isChecked = false
        }

        // Populate the saved and current IP addresses.
        savedIpAddressesTextView.text = savedIpAddresses
        currentIpAddressesTextView.text = DomainsActivity.currentIpAddresses

        // Set the initial display status of the IP addresses card views.
        if (pinnedIpAddressesSwitch.isChecked) {  // IP addresses are pinned.
            // Set the visibility of the saved IP addresses.
            if (savedIpAddresses == null)  // There are no saved IP addresses.
                savedIpAddressesCardView.visibility = View.GONE
            else  // There are saved IP addresses.
                savedIpAddressesCardView.visibility = View.VISIBLE

            // Set the visibility of the current IP addresses.
            currentIpAddressesCardView.visibility = View.VISIBLE

            // Set the status of the radio buttons and the card view backgrounds.
            if (savedIpAddressesCardView.visibility == View.VISIBLE) {  // The saved IP addresses are displayed.
                // Check the saved IP addresses radio button.
                savedIpAddressesRadioButton.isChecked = true

                // Uncheck the current IP addresses radio button.
                currentIpAddressesRadioButton.isChecked = false

                // Darken the background of the current IP addresses linear layout.
                currentWebsiteCertificateLinearLayout.setBackgroundResource(R.color.translucent_background)
            } else {  // The saved IP addresses are hidden.
                // Check the current IP addresses radio button.
                currentIpAddressesRadioButton.isChecked = true

                // Uncheck the saved IP addresses radio button.
                savedIpAddressesRadioButton.isChecked = false
            }
        } else {  // IP addresses are not pinned.
            // Hide the IP addresses card views.
            savedIpAddressesCardView.visibility = View.GONE
            currentIpAddressesCardView.visibility = View.GONE

            // Uncheck the radio buttons.
            savedIpAddressesRadioButton.isChecked = false
            currentIpAddressesRadioButton.isChecked = false
        }


        // Set the JavaScript switch listener.
        javaScriptSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            // Update the JavaScript icon.
            if (isChecked)
                javaScriptImageView.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.javascript_enabled, null))
            else
                javaScriptImageView.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.privacy_mode, null))

            // Set the DOM storage switch status.
            domStorageSwitch.isEnabled = isChecked

            // Set the DOM storage ghosted icon status.
            domStorageImageView.isEnabled = isChecked
        }

        // Set the cookies switch listener.
        cookiesSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            // Update the icon color.
            cookiesImageView.isSelected = isChecked
        }

        // Set the DOM Storage switch listener.
        domStorageSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            // Update the icon color.
            domStorageImageView.isSelected = isChecked
        }

        // Set the form data switch listener.  It can be removed once the minimum API >= 26.
        if (Build.VERSION.SDK_INT < 26) {
            formDataSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                // Update the icon color.
                formDataImageView.isSelected = isChecked
            }
        }

        // Set the EasyList switch listener.
        easyListSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            // Update the icon color.
            easyListImageView.isSelected = isChecked
        }

        // Set the EasyPrivacy switch listener.
        easyPrivacySwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            // Update the icon color.
            easyPrivacyImageView.isSelected = isChecked
        }

        // Set the Fanboy's Annoyance List switch listener.
        fanboysAnnoyanceListSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            // Update the icon color.
            fanboysAnnoyanceListImageView.isSelected = isChecked

            // Set Fanboy's Social Blocking List switch position.
            fanboysSocialBlockingListSwitch.isEnabled = !isChecked

            // Set the Social Blocking List icon ghosted status.
            fanboysSocialBlockingListImageView.isEnabled = !isChecked
        }

        // Set the Fanboy's Social Blocking List switch listener.
        fanboysSocialBlockingListSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            // Update the icon color.
            fanboysSocialBlockingListImageView.isSelected = isChecked
        }

        // Set the UltraList switch listener.
        ultraListSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            // Update the icon color.
            ultraListImageView.isSelected = isChecked
        }

        // Set the UltraPrivacy switch listener.
        ultraPrivacySwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            // Update the icon color.
            ultraPrivacyImageView.isSelected = isChecked
        }

        // Set the block all third-party requests switch listener.
        blockAllThirdPartyRequestsSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            // Update the icon color.
            blockAllThirdPartyRequestsImageView.isSelected = isChecked
        }

        // Set the user agent spinner listener.
        userAgentSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Set the new user agent.
                when (position) {
                    DOMAINS_SYSTEM_DEFAULT_USER_AGENT -> {
                        // Show the user agent text view.
                        userAgentTextView.visibility = View.VISIBLE

                        // Hide the custom user agent edit text.
                        customUserAgentEditText.visibility = View.GONE

                        // Set the user text.
                        when (defaultUserAgentArrayPosition) {
                            // This is probably because it was set in an older version of Privacy Browser before the switch to persistent user agent names.
                            UNRECOGNIZED_USER_AGENT -> userAgentTextView.text = defaultUserAgentName

                            // Display the `WebView` default user agent.
                            SETTINGS_WEBVIEW_DEFAULT_USER_AGENT -> userAgentTextView.text = webViewDefaultUserAgentString

                            // Display the custom user agent.
                            SETTINGS_CUSTOM_USER_AGENT -> userAgentTextView.text = defaultCustomUserAgentString

                            // Get the user agent string from the user agent data array.
                            else -> userAgentTextView.text = userAgentDataArray[defaultUserAgentArrayPosition]
                        }

                        // Set the background color to be transparent.
                        userAgentLinearLayout.setBackgroundColor(getColor(requireContext(), R.color.transparent))
                    }

                    DOMAINS_WEBVIEW_DEFAULT_USER_AGENT -> {
                        // Show the user agent text view.
                        userAgentTextView.visibility = View.VISIBLE

                        // Set the user agent text.
                        userAgentTextView.text = webViewDefaultUserAgentString

                        // Hide the custom user agent EditTex.
                        customUserAgentEditText.visibility = View.GONE

                        // Set the background color to be blue.
                        userAgentLinearLayout.setBackgroundColor(getColor(requireContext(), R.color.blue_background))
                    }

                    DOMAINS_CUSTOM_USER_AGENT -> {
                        // Hide the user agent TextView.
                        userAgentTextView.visibility = View.GONE

                        // Show the custom user agent edit text.
                        customUserAgentEditText.visibility = View.VISIBLE

                        // Set the current user agent name as the text.
                        customUserAgentEditText.setText(currentUserAgentName)

                        // Set the background color to be blue.
                        userAgentLinearLayout.setBackgroundColor(getColor(requireContext(), R.color.blue_background))
                    }

                    else -> {
                        // Show the user agent text view.
                        userAgentTextView.visibility = View.VISIBLE

                        // Set the text from the user agent data array, which has one less entry than the spinner, so the position must be decremented.
                        userAgentTextView.text = userAgentDataArray[position - 1]

                        // Hide the custom user agent edit text.
                        customUserAgentEditText.visibility = View.GONE

                        // Set the background color to be blue.
                        userAgentLinearLayout.setBackgroundColor(getColor(requireContext(), R.color.blue_background))
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing.
            }
        }

        // Set the font size spinner listener.
        fontSizeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Update the font size display options.
                if (position == 0) {  // The system default font size has been selected.
                    // Show the default font size text view.
                    defaultFontSizeTextView.visibility = View.VISIBLE

                    // Hide the custom font size edit text.
                    customFontSizeEditText.visibility = View.GONE
                } else {  // A custom font size has been selected.
                    // Hide the default font size text view.
                    defaultFontSizeTextView.visibility = View.GONE

                    // Show the custom font size edit text.
                    customFontSizeEditText.visibility = View.VISIBLE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing.
            }
        }

        // Set the swipe-to-refresh spinner listener.
        swipeToRefreshSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Update the icon and the visibility of the text view.
                when (position) {
                    DomainsDatabaseHelper.SYSTEM_DEFAULT -> {
                        // Set the icon color.
                        swipeToRefreshImageView.isSelected = defaultSwipeToRefresh

                        // Show the swipe-to-refresh text view.
                        swipeToRefreshTextView.visibility = View.VISIBLE
                    }

                    DomainsDatabaseHelper.ENABLED -> {
                        // Set the icon color.
                        swipeToRefreshImageView.isSelected = true

                        // Hide the swipe-to-refresh text view.
                        swipeToRefreshTextView.visibility = View.GONE
                    }

                    DomainsDatabaseHelper.DISABLED -> {
                        // Set the icon color.
                        swipeToRefreshImageView.isSelected = false

                        // Hide the swipe-to-refresh text view.
                        swipeToRefreshTextView.visibility = View.GONE
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing.
            }
        }

        // Set the WebView theme spinner listener.
        webViewThemeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Update the icon and the visibility of the WebView theme text view.
                when (position) {
                    DomainsDatabaseHelper.SYSTEM_DEFAULT -> {
                        // Set the icon color.
                        when (appWebViewThemeEntryNumber) {
                            DomainsDatabaseHelper.SYSTEM_DEFAULT -> webViewThemeImageView.isSelected = (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO)
                            DomainsDatabaseHelper.LIGHT_THEME -> webViewThemeImageView.isSelected = true
                            DomainsDatabaseHelper.DARK_THEME -> webViewThemeImageView.isSelected = false
                        }

                        // Show the WebView theme text view.
                        webViewThemeTextView.visibility = View.VISIBLE
                    }

                    DomainsDatabaseHelper.LIGHT_THEME -> {
                        // Set the icon color.
                        webViewThemeImageView.isSelected = true

                        // Hide the WebView theme text view.
                        webViewThemeTextView.visibility = View.GONE
                    }

                    DomainsDatabaseHelper.DARK_THEME -> {
                        // Set the icon color.
                        webViewThemeImageView.isSelected = false

                        // Hide the WebView theme text view.
                        webViewThemeTextView.visibility = View.GONE
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing.
            }
        }

        // Set the wide viewport spinner listener.
        wideViewportSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Update the icon and the visibility of the wide viewport text view.
                when (position) {
                    DomainsDatabaseHelper.SYSTEM_DEFAULT -> {
                        // Set the icon color.
                        wideViewportImageView.isSelected = defaultWideViewport

                        // Show the wide viewport text view.
                        wideViewportTextView.visibility = View.VISIBLE
                    }

                    DomainsDatabaseHelper.ENABLED -> {
                        // Set the icon color.
                        wideViewportImageView.isSelected = true

                        // Hide the wide viewport text view.
                        wideViewportTextView.visibility = View.GONE
                    }

                    DomainsDatabaseHelper.DISABLED -> {
                        // Set the icon color.
                        wideViewportImageView.isSelected = false

                        // Hid ethe wide viewport text view.
                        wideViewportTextView.visibility = View.GONE
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing.
            }
        }

        // Set the display webpage images spinner listener.
        displayWebpageImagesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Update the icon and the visibility of the display images text view.
                when (position) {
                    DomainsDatabaseHelper.SYSTEM_DEFAULT -> {
                        // Set the icon color.
                        displayWebpageImagesImageView.isSelected = defaultDisplayWebpageImages

                        // Show the display images text view.
                        displayImagesTextView.visibility = View.VISIBLE
                    }

                    DomainsDatabaseHelper.ENABLED -> {
                        // Set the icon color.
                        displayWebpageImagesImageView.isSelected = true

                        // Hide the display images text view.
                        displayImagesTextView.visibility = View.GONE
                    }

                    DomainsDatabaseHelper.DISABLED -> {
                        // Set the icon color.
                        displayWebpageImagesImageView.isSelected = false

                        // Hide the display images text view.
                        displayImagesTextView.visibility = View.GONE
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing.
            }
        }

        // Set the pinned SSL certificate switch listener.
        pinnedSslCertificateSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            // Update the icon color.
            pinnedSslCertificateImageView.isSelected = isChecked

            // Update the views.
            if (isChecked) {  // SSL certificate pinning is enabled.
                // Update the visibility of the saved SSL certificate.
                if (savedSslIssuedToCNameString == null)
                    savedSslCardView.visibility = View.GONE
                else
                    savedSslCardView.visibility = View.VISIBLE

                // Update the visibility of the current website SSL certificate.
                if (DomainsActivity.sslIssuedToCName == null) {
                    // Hide the SSL certificate.
                    currentSslCardView.visibility = View.GONE

                    // Show the instruction.
                    noCurrentWebsiteCertificateTextView.visibility = View.VISIBLE
                } else {
                    // Show the SSL certificate.
                    currentSslCardView.visibility = View.VISIBLE

                    // Hide the instruction.
                    noCurrentWebsiteCertificateTextView.visibility = View.GONE
                }

                // Set the status of the radio buttons.
                if (savedSslCardView.visibility == View.VISIBLE) {  // The saved SSL certificate is displayed.
                    // Check the saved SSL certificate radio button.
                    savedSslCertificateRadioButton.isChecked = true

                    // Uncheck the current website SSL certificate radio button.
                    currentWebsiteCertificateRadioButton.isChecked = false

                    // Set the background of the saved SSL certificate linear layout to be transparent.
                    savedSslCertificateLinearLayout.setBackgroundResource(R.color.transparent)

                    // Darken the background of the current website SSL certificate linear layout according to the theme.
                    currentWebsiteCertificateLinearLayout.setBackgroundResource(R.color.translucent_background)

                    // Scroll to the current website SSL certificate card.
                    savedSslCardView.parent.requestChildFocus(savedSslCardView, savedSslCardView)
                } else if (currentSslCardView.visibility == View.VISIBLE) {  // The saved SSL certificate is hidden but the current website SSL certificate is visible.
                    // Check the current website SSL certificate radio button.
                    currentWebsiteCertificateRadioButton.isChecked = true

                    // Uncheck the saved SSL certificate radio button.
                    savedSslCertificateRadioButton.isChecked = false

                    // Set the background of the current website SSL certificate linear layout to be transparent.
                    currentWebsiteCertificateLinearLayout.setBackgroundResource(R.color.transparent)

                    // Darken the background of the saved SSL certificate linear layout according to the theme.
                    savedSslCertificateLinearLayout.setBackgroundResource(R.color.translucent_background)

                    // Scroll to the current website SSL certificate card.
                    currentSslCardView.parent.requestChildFocus(currentSslCardView, currentSslCardView)
                } else {  // Neither SSL certificate is visible.
                    // Uncheck both radio buttons.
                    savedSslCertificateRadioButton.isChecked = false
                    currentWebsiteCertificateRadioButton.isChecked = false

                    // Scroll to the current website SSL certificate card.
                    noCurrentWebsiteCertificateTextView.parent.requestChildFocus(noCurrentWebsiteCertificateTextView, noCurrentWebsiteCertificateTextView)
                }
            } else {  // SSL certificate pinning is disabled.
                // Hide the SSl certificates and instructions.
                savedSslCardView.visibility = View.GONE
                currentSslCardView.visibility = View.GONE
                noCurrentWebsiteCertificateTextView.visibility = View.GONE

                // Uncheck the radio buttons.
                savedSslCertificateRadioButton.isChecked = false
                currentWebsiteCertificateRadioButton.isChecked = false
            }
        }

        // Set the saved SSL card view listener.
        savedSslCardView.setOnClickListener {
            // Check the saved SSL certificate radio button.
            savedSslCertificateRadioButton.isChecked = true

            // Uncheck the current website SSL certificate radio button.
            currentWebsiteCertificateRadioButton.isChecked = false

            // Set the background of the saved SSL certificate linear layout to be transparent.
            savedSslCertificateLinearLayout.setBackgroundResource(R.color.transparent)

            // Darken the background of the current website SSL certificate linear layout.
            currentWebsiteCertificateLinearLayout.setBackgroundResource(R.color.translucent_background)
        }

        // Set the saved SSL certificate radio button listener.
        savedSslCertificateRadioButton.setOnClickListener {
            // Check the saved SSL certificate radio button.
            savedSslCertificateRadioButton.isChecked = true

            // Uncheck the current website SSL certificate radio button.
            currentWebsiteCertificateRadioButton.isChecked = false

            // Set the background of the saved SSL certificate linear layout to be transparent.
            savedSslCertificateLinearLayout.setBackgroundResource(R.color.transparent)

            // Darken the background of the current website SSL certificate linear layout.
            currentWebsiteCertificateLinearLayout.setBackgroundResource(R.color.translucent_background)
        }

        // Set the current SSL card view listener.
        currentSslCardView.setOnClickListener {
            // Check the current website SSL certificate radio button.
            currentWebsiteCertificateRadioButton.isChecked = true

            // Uncheck the saved SSL certificate radio button.
            savedSslCertificateRadioButton.isChecked = false

            // Set the background of the current website SSL certificate linear layout to be transparent.
            currentWebsiteCertificateLinearLayout.setBackgroundResource(R.color.transparent)

            // Darken the background of the saved SSL certificate linear layout.
            savedSslCertificateLinearLayout.setBackgroundResource(R.color.translucent_background)
        }

        // Set the current website certificate radio button listener.
        currentWebsiteCertificateRadioButton.setOnClickListener {
            // Check the current website SSL certificate radio button.
            currentWebsiteCertificateRadioButton.isChecked = true

            // Uncheck the saved SSL certificate radio button.
            savedSslCertificateRadioButton.isChecked = false

            // Set the background of the current website SSL certificate linear layout to be transparent.
            currentWebsiteCertificateLinearLayout.setBackgroundResource(R.color.transparent)

            // Darken the background of the saved SSL certificate linear layout.
            savedSslCertificateLinearLayout.setBackgroundResource(R.color.translucent_background)
        }

        // Set the pinned IP addresses switch listener.
        pinnedIpAddressesSwitch.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            // Update the icon color.
            pinnedIpAddressesImageView.isSelected = isChecked

            // Update the views.
            if (isChecked) {  // IP addresses pinning is enabled.
                // Update the visibility of the saved IP addresses card view.
                if (savedIpAddresses == null)
                    savedIpAddressesCardView.visibility = View.GONE
                else
                    savedIpAddressesCardView.visibility = View.VISIBLE

                // Show the current IP addresses card view.
                currentIpAddressesCardView.visibility = View.VISIBLE

                // Set the status of the radio buttons.
                if (savedIpAddressesCardView.visibility == View.VISIBLE) {  // The saved IP addresses are visible.
                    // Check the saved IP addresses radio button.
                    savedIpAddressesRadioButton.isChecked = true

                    // Uncheck the current IP addresses radio button.
                    currentIpAddressesRadioButton.isChecked = false

                    // Set the background of the saved IP addresses linear layout to be transparent.
                    savedSslCertificateLinearLayout.setBackgroundResource(R.color.transparent)

                    // Darken the background of the current IP addresses linear layout.
                    currentIpAddressesLinearLayout.setBackgroundResource(R.color.translucent_background)
                } else {  // The saved IP addresses are not visible.
                    // Check the current IP addresses radio button.
                    currentIpAddressesRadioButton.isChecked = true

                    // Uncheck the saved IP addresses radio button.
                    savedIpAddressesRadioButton.isChecked = false

                    // Set the background of the current IP addresses linear layout to be transparent.
                    currentIpAddressesLinearLayout.setBackgroundResource(R.color.transparent)

                    // Darken the background of the saved IP addresses linear layout.
                    savedIpAddressesLinearLayout.setBackgroundResource(R.color.translucent_background)
                }

                // Scroll to the bottom of the card views.
                currentIpAddressesCardView.parent.requestChildFocus(currentIpAddressesCardView, currentIpAddressesCardView)
            } else {  // IP addresses pinning is disabled.
                // Hide the IP addresses card views.
                savedIpAddressesCardView.visibility = View.GONE
                currentIpAddressesCardView.visibility = View.GONE

                // Uncheck the radio buttons.
                savedIpAddressesRadioButton.isChecked = false
                currentIpAddressesRadioButton.isChecked = false
            }
        }

        // Set the saved IP addresses card view listener.
        savedIpAddressesCardView.setOnClickListener {
            // Check the saved IP addresses radio button.
            savedIpAddressesRadioButton.isChecked = true

            // Uncheck the current website IP addresses radio button.
            currentIpAddressesRadioButton.isChecked = false

            // Set the background of the saved IP addresses linear layout to be transparent.
            savedIpAddressesLinearLayout.setBackgroundResource(R.color.transparent)

            // Darken the background of the current IP addresses linear layout.
                currentIpAddressesLinearLayout.setBackgroundResource(R.color.translucent_background)
        }

        // Set the saved IP addresses radio button listener.
        savedIpAddressesRadioButton.setOnClickListener {
            // Check the saved IP addresses radio button.
            savedIpAddressesRadioButton.isChecked = true

            // Uncheck the current website IP addresses radio button.
            currentIpAddressesRadioButton.isChecked = false

            // Set the background of the saved IP addresses linear layout to be transparent.
            savedIpAddressesLinearLayout.setBackgroundResource(R.color.transparent)

            // Darken the background of the current IP addresses linear layout.
            currentIpAddressesLinearLayout.setBackgroundResource(R.color.translucent_background)
        }

        // Set the current IP addresses card view listener.
        currentIpAddressesCardView.setOnClickListener {
            // Check the current IP addresses radio button.
            currentIpAddressesRadioButton.isChecked = true

            // Uncheck the saved IP addresses radio button.
            savedIpAddressesRadioButton.isChecked = false

            // Set the background of the current IP addresses linear layout to be transparent.
            currentIpAddressesLinearLayout.setBackgroundResource(R.color.transparent)

            // Darken the background of the saved IP addresses linear layout.
            savedIpAddressesLinearLayout.setBackgroundResource(R.color.translucent_background)
        }

        // Set the current IP addresses radio button listener.
        currentIpAddressesRadioButton.setOnClickListener {
            // Check the current IP addresses radio button.
            currentIpAddressesRadioButton.isChecked = true

            // Uncheck the saved IP addresses radio button.
            savedIpAddressesRadioButton.isChecked = false

            // Set the background of the current IP addresses linear layout to be transparent.
            currentIpAddressesLinearLayout.setBackgroundResource(R.color.transparent)

            // Darken the background of the saved IP addresses linear layout.
            savedIpAddressesLinearLayout.setBackgroundResource(R.color.translucent_background)
        }

        // Set the scroll Y.
        domainSettingsScrollView.post { domainSettingsScrollView.scrollY = scrollY }

        // Return the domain settings view.
        return domainSettingsView
    }

    private fun checkDomainNameAgainstCertificate(domainName: String?, certificateCommonName: String?): Boolean {
        // Initialize the domain names match tracker.
        var domainNamesMatch = false

        // Check various wildcard permutations if the domain name and the certificate Common Name are not empty.
        if ((domainName != null) && (certificateCommonName != null)) {
            // Check if the domains match.
            if (domainName == certificateCommonName)
                domainNamesMatch = true

            // If the domain name starts with a wildcard, check the base domain against all the subdomains of the certificate Common Name.
            if (!domainNamesMatch && domainName.startsWith("*.") && domainName.length > 2) {
                // Remove the initial `*.`.
                val baseDomainName = domainName.substring(2)

                // Create a copy of the certificate Common Name to test subdomains.
                var certificateCommonNameSubdomain: String = certificateCommonName

                // Check all the subdomains in the certificate Common Name subdomain against the base domain name.
                while (!domainNamesMatch && certificateCommonNameSubdomain.contains(".")) {  // Stop checking if the domain names match or if there are no more dots.
                    // Test the certificate Common Name subdomain against the base domain name.
                    if (certificateCommonNameSubdomain == baseDomainName)
                        domainNamesMatch = true

                    // Strip out the lowest subdomain of the certificate Common Name subdomain.
                    certificateCommonNameSubdomain = try {
                        certificateCommonNameSubdomain.substring(certificateCommonNameSubdomain.indexOf(".") + 1)
                    } catch (e: IndexOutOfBoundsException) {  // The certificate Common Name subdomain ends with a dot.
                        ""
                    }
                }
            }

            // If the certificate Common Name starts with a wildcard, check the base common name against all the subdomains of the domain name.
            if (!domainNamesMatch && certificateCommonName.startsWith("*.") && certificateCommonName.length > 2) {
                // Remove the initial `*.`.
                val baseCertificateCommonName = certificateCommonName.substring(2)

                // Setup a copy of domain name to test subdomains.
                var domainNameSubdomain: String = domainName

                // Check all the subdomains in the domain name subdomain against the base certificate Common Name.
                while (!domainNamesMatch && domainNameSubdomain.contains(".") && domainNameSubdomain.length > 2) {
                    // Test the domain name subdomain  against the base certificate Common Name.
                    if (domainNameSubdomain == baseCertificateCommonName)
                        domainNamesMatch = true

                    // Strip out the lowest subdomain of the domain name subdomain.
                    domainNameSubdomain = try {
                        domainNameSubdomain.substring(domainNameSubdomain.indexOf(".") + 1)
                    } catch (e: IndexOutOfBoundsException) {  // `domainNameSubdomain` ends with a dot.
                        ""
                    }
                }
            }

            // If both names start with a wildcard, check if the root of one contains the root of the other.
            if (!domainNamesMatch && domainName.startsWith("*.") && domainName.length > 2 && certificateCommonName.startsWith("*.") && certificateCommonName.length > 2) {
                // Remove the wildcards.
                val rootDomainName = domainName.substring(2)
                val rootCertificateCommonName = certificateCommonName.substring(2)

                // Check if one name ends with the contents of the other.  If so, there will be overlap in the their wildcard subdomains.
                if (rootDomainName.endsWith(rootCertificateCommonName) || rootCertificateCommonName.endsWith(rootDomainName))
                    domainNamesMatch = true
            }
        }

        return domainNamesMatch
    }
}
