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

package com.stoutner.privacybrowser.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.WindowManager
import android.widget.TextView

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.activities.MainWebViewActivity
import com.stoutner.privacybrowser.views.NestedScrollWebView

import java.text.DateFormat
import java.util.Calendar

// Define the class constants.
private const val WEBVIEW_FRAGMENT_ID = "webview_fragment_id"

class ViewSslCertificateDialog : DialogFragment() {
    companion object {
        // `@JvmStatic` will no longer be required once all the code has transitioned to Kotlin.
        @JvmStatic
        fun displayDialog(webViewFragmentId: Long): ViewSslCertificateDialog {
            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the WebView fragment ID in the bundle.
            argumentsBundle.putLong(WEBVIEW_FRAGMENT_ID, webViewFragmentId)

            // Create a new instance of the view SSL certificate dialog.
            val viewSslCertificateDialog = ViewSslCertificateDialog()

            // Add the bundle to the new dialog.
            viewSslCertificateDialog.arguments = argumentsBundle

            // Return the new dialog.
            return viewSslCertificateDialog
        }
    }

    // `@SuppressLint("InflateParams")` removes the warning about using `null` as the parent view group when inflating the alert dialog.
    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get the current position of this WebView fragment.
        val webViewPosition = MainWebViewActivity.webViewPagerAdapter.getPositionForId(requireArguments().getLong(WEBVIEW_FRAGMENT_ID))

        // Get the WebView tab fragment.
        val webViewTabFragment = MainWebViewActivity.webViewPagerAdapter.getPageFragment(webViewPosition)

        // Get the fragment view.
        val fragmentView = webViewTabFragment.requireView()

        // Get a handle for the current nested scroll WebView.
        val nestedScrollWebView: NestedScrollWebView = fragmentView.findViewById(R.id.nestedscroll_webview)

        // Use a builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.PrivacyBrowserAlertDialog)

        // Create a drawable version of the favorite icon.
        val favoriteIconDrawable: Drawable = BitmapDrawable(resources, nestedScrollWebView.favoriteOrDefaultIcon)

        // Set the icon.
        dialogBuilder.setIcon(favoriteIconDrawable)

        // Set the close button listener.  Using `null` as the listener closes the dialog without doing anything else.
        dialogBuilder.setNegativeButton(R.string.close, null)

        // Get the SSL certificate.
        val sslCertificate = nestedScrollWebView.certificate

        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        // Get the screenshot preference.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)

        // Check to see if the website is encrypted.
        if (sslCertificate == null) {  // The website is not encrypted.
            // Set the title.
            dialogBuilder.setTitle(R.string.unencrypted_website)

            // Set the Layout.  The parent view is `null` because it will be assigned by the alert dialog.
            dialogBuilder.setView(layoutInflater.inflate(R.layout.unencrypted_website_dialog, null))

            // Create an alert dialog from the builder.
            val alertDialog = dialogBuilder.create()

            // Disable screenshots if not allowed.
            if (!allowScreenshots) {
                // Disable screenshots.
                alertDialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            // Return the alert dialog.
            return alertDialog
        } else {  // The website is encrypted.
            // Set the title.
            dialogBuilder.setTitle(R.string.ssl_certificate)

            // Set the layout.  The parent view is `null` because it will be assigned by the alert dialog.
            dialogBuilder.setView(layoutInflater.inflate(R.layout.view_ssl_certificate_dialog, null))

            // Create an alert dialog from the builder.
            val alertDialog = dialogBuilder.create()

            // Disable screenshots if not allowed.
            if (!allowScreenshots) {
                // Disable screenshots.
                alertDialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            // The alert dialog must be shown before items in the layout can be modified.
            alertDialog.show()

            // Get handles for the text views.
            val domainTextView = alertDialog.findViewById<TextView>(R.id.domain)!!
            val ipAddressesTextView = alertDialog.findViewById<TextView>(R.id.ip_addresses)!!
            val issuedToCNameTextView = alertDialog.findViewById<TextView>(R.id.issued_to_cname)!!
            val issuedToONameTextView = alertDialog.findViewById<TextView>(R.id.issued_to_oname)!!
            val issuedToUNameTextView = alertDialog.findViewById<TextView>(R.id.issued_to_uname)!!
            val issuedByCNameTextView = alertDialog.findViewById<TextView>(R.id.issued_by_cname)!!
            val issuedByONameTextView = alertDialog.findViewById<TextView>(R.id.issued_by_oname)!!
            val issuedByUNameTextView = alertDialog.findViewById<TextView>(R.id.issued_by_uname)!!
            val startDateTextView = alertDialog.findViewById<TextView>(R.id.start_date)!!
            val endDateTextView = alertDialog.findViewById<TextView>(R.id.end_date)!!

            // Setup the labels.
            val domainLabel = getString(R.string.domain_label) + "  "
            val ipAddressesLabel = getString(R.string.ip_addresses) + "  "
            val cNameLabel = getString(R.string.common_name) + "  "
            val oNameLabel = getString(R.string.organization) + "  "
            val uNameLabel = getString(R.string.organizational_unit) + "  "
            val startDateLabel = getString(R.string.start_date) + "  "
            val endDateLabel = getString(R.string.end_date) + "  "

            // Convert the URL to a URI.
            val uri = Uri.parse(nestedScrollWebView.url)

            // Extract the domain name from the URI.
            val domainString = uri.host

            // Get the strings from the SSL certificate.
            val issuedToCName = sslCertificate.issuedTo.cName
            val issuedToOName = sslCertificate.issuedTo.oName
            val issuedToUName = sslCertificate.issuedTo.uName
            val issuedByCName = sslCertificate.issuedBy.cName
            val issuedByOName = sslCertificate.issuedBy.oName
            val issuedByUName = sslCertificate.issuedBy.uName
            val startDate = sslCertificate.validNotBeforeDate
            val endDate = sslCertificate.validNotAfterDate

            // Create spannable string builders for each text view that needs multiple colors of text.
            val domainStringBuilder = SpannableStringBuilder(domainLabel + domainString)
            val ipAddressesStringBuilder = SpannableStringBuilder(ipAddressesLabel + nestedScrollWebView.currentIpAddresses)
            val issuedToCNameStringBuilder = SpannableStringBuilder(cNameLabel + issuedToCName)
            val issuedToONameStringBuilder = SpannableStringBuilder(oNameLabel + issuedToOName)
            val issuedToUNameStringBuilder = SpannableStringBuilder(uNameLabel + issuedToUName)
            val issuedByCNameStringBuilder = SpannableStringBuilder(cNameLabel + issuedByCName)
            val issuedByONameStringBuilder = SpannableStringBuilder(oNameLabel + issuedByOName)
            val issuedByUNameStringBuilder = SpannableStringBuilder(uNameLabel + issuedByUName)
            val startDateStringBuilder = SpannableStringBuilder(startDateLabel + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG).format(startDate))
            val endDateStringBuilder = SpannableStringBuilder(endDateLabel + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG).format(endDate))

            // Define the color spans.
            val blueColorSpan: ForegroundColorSpan
            val redColorSpan: ForegroundColorSpan

            // Get the current theme status.
            val currentThemeStatus = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

            // Set the color spans according to the theme.  The deprecated `getColor()` must be used until the minimum API >= 23.
            if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {
                @Suppress("DEPRECATION")
                blueColorSpan = ForegroundColorSpan(resources.getColor(R.color.blue_700))
                @Suppress("DEPRECATION")
                redColorSpan = ForegroundColorSpan(resources.getColor(R.color.red_a700))
            } else {
                @Suppress("DEPRECATION")
                blueColorSpan = ForegroundColorSpan(resources.getColor(R.color.violet_700))
                @Suppress("DEPRECATION")
                redColorSpan = ForegroundColorSpan(resources.getColor(R.color.red_900))
            }

            // Format the domain string and issued to CName colors.
            if (domainString == issuedToCName) {  // The domain and issued to CName match.
                // Set the strings to be blue.
                domainStringBuilder.setSpan(blueColorSpan, domainLabel.length, domainStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                issuedToCNameStringBuilder.setSpan(blueColorSpan, cNameLabel.length, issuedToCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            } else if (issuedToCName.startsWith("*.")) {  // The issued to CName begins with a wildcard.
                // Remove the initial `*.`.
                val baseCertificateDomain = issuedToCName.substring(2)

                // Setup a copy of the domain string to test subdomains.
                var domainStringSubdomain = domainString!!

                // Define a domain names match variable.
                var domainNamesMatch = false

                // Check all the subdomains against the base certificate domain.
                while (!domainNamesMatch && domainStringSubdomain.contains(".")) {  // Stop checking if we know that the domain names match or if we run out of subdomains.
                    // Test the subdomain against the base certificate domain.
                    if (domainStringSubdomain == baseCertificateDomain) {
                        domainNamesMatch = true
                    }

                    // Strip out the lowest subdomain.
                    domainStringSubdomain = domainStringSubdomain.substring(domainStringSubdomain.indexOf(".") + 1)
                }

                // Format the domain and issued to CName.
                if (domainNamesMatch) {  // The domain is a subdomain of the wildcard certificate.
                    // Set the strings to be blue.
                    domainStringBuilder.setSpan(blueColorSpan, domainLabel.length, domainStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                    issuedToCNameStringBuilder.setSpan(blueColorSpan, cNameLabel.length, issuedToCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                } else {  // The domain is not a subdomain of the wildcard certificate.
                    // Set the string to be red.
                    domainStringBuilder.setSpan(redColorSpan, domainLabel.length, domainStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                    issuedToCNameStringBuilder.setSpan(redColorSpan, cNameLabel.length, issuedToCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                }
            } else {  // The strings do not match and issued to CName does not begin with a wildcard.
                // Set the strings to be red.
                domainStringBuilder.setSpan(redColorSpan, domainLabel.length, domainStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                issuedToCNameStringBuilder.setSpan(redColorSpan, cNameLabel.length, issuedToCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            }

            // Set the IP addresses, issued to, and issued by spans to display the certificate information in blue.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
            ipAddressesStringBuilder.setSpan(blueColorSpan, ipAddressesLabel.length, ipAddressesStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            issuedToONameStringBuilder.setSpan(blueColorSpan, oNameLabel.length, issuedToONameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            issuedToUNameStringBuilder.setSpan(blueColorSpan, uNameLabel.length, issuedToUNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            issuedByCNameStringBuilder.setSpan(blueColorSpan, cNameLabel.length, issuedByCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            issuedByONameStringBuilder.setSpan(blueColorSpan, oNameLabel.length, issuedByONameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            issuedByUNameStringBuilder.setSpan(blueColorSpan, uNameLabel.length, issuedByUNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

            // Get the current date.
            val currentDate = Calendar.getInstance().time

            //  Format the start date color.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
            if (startDate.after(currentDate)) {  // The certificate start date is in the future.
                startDateStringBuilder.setSpan(redColorSpan, startDateLabel.length, startDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            } else {  // The certificate start date is in the past.
                startDateStringBuilder.setSpan(blueColorSpan, startDateLabel.length, startDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            }

            // Format the end date color.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
            if (endDate.before(currentDate)) {  // The certificate end date is in the past.
                endDateStringBuilder.setSpan(redColorSpan, endDateLabel.length, endDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            } else {  // The certificate end date is in the future.
                endDateStringBuilder.setSpan(blueColorSpan, endDateLabel.length, endDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            }

            // Display the strings.
            domainTextView.text = domainStringBuilder
            ipAddressesTextView.text = ipAddressesStringBuilder
            issuedToCNameTextView.text = issuedToCNameStringBuilder
            issuedToONameTextView.text = issuedToONameStringBuilder
            issuedToUNameTextView.text = issuedToUNameStringBuilder
            issuedByCNameTextView.text = issuedByCNameStringBuilder
            issuedByONameTextView.text = issuedByONameStringBuilder
            issuedByUNameTextView.text = issuedByUNameStringBuilder
            startDateTextView.text = startDateStringBuilder
            endDateTextView.text = endDateStringBuilder

            // Return the alert dialog.
            return alertDialog
        }
    }
}