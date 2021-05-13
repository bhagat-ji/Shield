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
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Configuration
import android.net.Uri
import android.net.http.SslError
import android.os.AsyncTask
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

import java.lang.ref.WeakReference
import java.net.InetAddress
import java.net.UnknownHostException
import java.text.DateFormat

// Define the class constants.
private const val PRIMARY_ERROR_INT = "primary_error_int"
private const val URL_WITH_ERRORS = "url_with_errors"
private const val ISSUED_TO_CNAME = "issued_to_cname"
private const val ISSUED_TO_ONAME = "issued_to_oname"
private const val ISSUED_TO_UNAME = "issued_to_uname"
private const val ISSUED_BY_CNAME = "issued_by_cname"
private const val ISSUED_BY_ONAME = "issued_by_oname"
private const val ISSUED_BY_UNAME = "issued_by_uname"
private const val START_DATE = "start_date"
private const val END_DATE = "end_date"
private const val WEBVIEW_FRAGMENT_ID = "webview_fragment_id"

class SslCertificateErrorDialog : DialogFragment() {
    companion object {
        // `@JvmStatic` will no longer be required once all the code has transitioned to Kotlin.
        @JvmStatic
        fun displayDialog(sslError: SslError, webViewFragmentId: Long): SslCertificateErrorDialog {
            // Get the various components of the SSL error message.
            val primaryErrorInt = sslError.primaryError
            val urlWithErrors = sslError.url
            val sslCertificate = sslError.certificate
            val issuedToCName = sslCertificate.issuedTo.cName
            val issuedToOName = sslCertificate.issuedTo.oName
            val issuedToUName = sslCertificate.issuedTo.uName
            val issuedByCName = sslCertificate.issuedBy.cName
            val issuedByOName = sslCertificate.issuedBy.oName
            val issuedByUName = sslCertificate.issuedBy.uName
            val startDate = sslCertificate.validNotBeforeDate
            val endDate = sslCertificate.validNotAfterDate

            // Create an arguments bundle.
            val argumentsBundle = Bundle()

            // Store the SSL error message components in the bundle.
            argumentsBundle.putInt(PRIMARY_ERROR_INT, primaryErrorInt)
            argumentsBundle.putString(URL_WITH_ERRORS, urlWithErrors)
            argumentsBundle.putString(ISSUED_TO_CNAME, issuedToCName)
            argumentsBundle.putString(ISSUED_TO_ONAME, issuedToOName)
            argumentsBundle.putString(ISSUED_TO_UNAME, issuedToUName)
            argumentsBundle.putString(ISSUED_BY_CNAME, issuedByCName)
            argumentsBundle.putString(ISSUED_BY_ONAME, issuedByOName)
            argumentsBundle.putString(ISSUED_BY_UNAME, issuedByUName)
            argumentsBundle.putString(START_DATE, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG).format(startDate))
            argumentsBundle.putString(END_DATE, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG).format(endDate))
            argumentsBundle.putLong(WEBVIEW_FRAGMENT_ID, webViewFragmentId)

            // Create a new instance of the SSL certificate error dialog.
            val thisSslCertificateErrorDialog = SslCertificateErrorDialog()

            // Add the arguments bundle to the new dialog.
            thisSslCertificateErrorDialog.arguments = argumentsBundle

            // Return the new dialog.
            return thisSslCertificateErrorDialog
        }
    }

    // `@SuppressLint("InflateParams")` removes the warning about using `null` as the parent view group when inflating the alert dialog.
    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get the variables from the bundle.
        val primaryErrorInt = requireArguments().getInt(PRIMARY_ERROR_INT)
        val urlWithErrors = requireArguments().getString(URL_WITH_ERRORS)
        val issuedToCName = requireArguments().getString(ISSUED_TO_CNAME)
        val issuedToOName = requireArguments().getString(ISSUED_TO_ONAME)
        val issuedToUName = requireArguments().getString(ISSUED_TO_UNAME)
        val issuedByCName = requireArguments().getString(ISSUED_BY_CNAME)
        val issuedByOName = requireArguments().getString(ISSUED_BY_ONAME)
        val issuedByUName = requireArguments().getString(ISSUED_BY_UNAME)
        val startDate = requireArguments().getString(START_DATE)
        val endDate = requireArguments().getString(END_DATE)
        val webViewFragmentId = requireArguments().getLong(WEBVIEW_FRAGMENT_ID)

        // Get the current position of this WebView fragment.
        val webViewPosition = MainWebViewActivity.webViewPagerAdapter.getPositionForId(webViewFragmentId)

        // Get the WebView tab fragment.
        val webViewTabFragment = MainWebViewActivity.webViewPagerAdapter.getPageFragment(webViewPosition)

        // Get the fragment view.
        val fragmentView = webViewTabFragment.requireView()

        // Get a handle for the current WebView.
        val nestedScrollWebView: NestedScrollWebView = fragmentView.findViewById(R.id.nestedscroll_webview)

        // Get a handle for the SSL error handler.
        val sslErrorHandler = nestedScrollWebView.sslErrorHandler

        // Use an alert dialog builder to create the alert dialog.
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.PrivacyBrowserAlertDialog)

        // Get the current theme status.
        val currentThemeStatus = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        // Set the icon according to the theme.
        dialogBuilder.setIconAttribute(R.attr.sslCertificateBlueIcon)

        // Set the title.
        dialogBuilder.setTitle(R.string.ssl_certificate_error)

        // Set the view.  The parent view is null because it will be assigned by the alert dialog.
        dialogBuilder.setView(layoutInflater.inflate(R.layout.ssl_certificate_error, null))

        // Set the cancel button listener.
        dialogBuilder.setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int ->
            // Check to make sure the SSL error handler is not null.  This might happen if multiple dialogs are displayed at once.
            if (sslErrorHandler != null) {
                // Cancel the request.
                sslErrorHandler.cancel()

                // Reset the SSL error handler.
                nestedScrollWebView.resetSslErrorHandler()
            }
        }

        // Set the proceed button listener.
        dialogBuilder.setPositiveButton(R.string.proceed) { _: DialogInterface?, _: Int ->
            // Check to make sure the SSL error handler is not null.  This might happen if multiple dialogs are displayed at once.
            if (sslErrorHandler != null) {
                // Proceed to the website.
                sslErrorHandler.proceed()

                // Reset the SSL error handler.
                nestedScrollWebView.resetSslErrorHandler()
            }
        }

        // Create an alert dialog from the builder.
        val alertDialog = dialogBuilder.create()

        // Get a handle for the shared preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        // Get the screenshot preference.
        val allowScreenshots = sharedPreferences.getBoolean(getString(R.string.allow_screenshots_key), false)

        // Disable screenshots if not allowed.
        if (!allowScreenshots) {
            // Disable screenshots.
            alertDialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // Get a URI for the URL with errors.
        val uriWithErrors = Uri.parse(urlWithErrors)

        // Get the IP addresses for the URI.
        GetIpAddresses(requireActivity(), alertDialog).execute(uriWithErrors.host)

        // The alert dialog must be shown before the contents can be modified.
        alertDialog.show()

        // Get handles for the views.
        val primaryErrorTextView = alertDialog.findViewById<TextView>(R.id.primary_error)!!
        val urlTextView = alertDialog.findViewById<TextView>(R.id.url)!!
        val issuedToCNameTextView = alertDialog.findViewById<TextView>(R.id.issued_to_cname)!!
        val issuedToONameTextView = alertDialog.findViewById<TextView>(R.id.issued_to_oname)!!
        val issuedToUNameTextView = alertDialog.findViewById<TextView>(R.id.issued_to_uname)!!
        val issuedByTextView = alertDialog.findViewById<TextView>(R.id.issued_by_textview)!!
        val issuedByCNameTextView = alertDialog.findViewById<TextView>(R.id.issued_by_cname)!!
        val issuedByONameTextView = alertDialog.findViewById<TextView>(R.id.issued_by_oname)!!
        val issuedByUNameTextView = alertDialog.findViewById<TextView>(R.id.issued_by_uname)!!
        val validDatesTextView = alertDialog.findViewById<TextView>(R.id.valid_dates_textview)!!
        val startDateTextView = alertDialog.findViewById<TextView>(R.id.start_date)!!
        val endDateTextView = alertDialog.findViewById<TextView>(R.id.end_date)!!

        // Setup the common strings.
        val urlLabel = getString(R.string.url_label) + "  "
        val cNameLabel = getString(R.string.common_name) + "  "
        val oNameLabel = getString(R.string.organization) + "  "
        val uNameLabel = getString(R.string.organizational_unit) + "  "
        val startDateLabel = getString(R.string.start_date) + "  "
        val endDateLabel = getString(R.string.end_date) + "  "

        // Create a spannable string builder for each text view that needs multiple colors of text.
        val urlStringBuilder = SpannableStringBuilder(urlLabel + urlWithErrors)
        val issuedToCNameStringBuilder = SpannableStringBuilder(cNameLabel + issuedToCName)
        val issuedToONameStringBuilder = SpannableStringBuilder(oNameLabel + issuedToOName)
        val issuedToUNameStringBuilder = SpannableStringBuilder(uNameLabel + issuedToUName)
        val issuedByCNameStringBuilder = SpannableStringBuilder(cNameLabel + issuedByCName)
        val issuedByONameStringBuilder = SpannableStringBuilder(oNameLabel + issuedByOName)
        val issuedByUNameStringBuilder = SpannableStringBuilder(uNameLabel + issuedByUName)
        val startDateStringBuilder = SpannableStringBuilder(startDateLabel + startDate)
        val endDateStringBuilder = SpannableStringBuilder(endDateLabel + endDate)

        // Define the color spans.
        val blueColorSpan: ForegroundColorSpan
        val redColorSpan: ForegroundColorSpan

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

        // Setup the spans to display the certificate information in blue.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
        urlStringBuilder.setSpan(blueColorSpan, urlLabel.length, urlStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        issuedToCNameStringBuilder.setSpan(blueColorSpan, cNameLabel.length, issuedToCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        issuedToONameStringBuilder.setSpan(blueColorSpan, oNameLabel.length, issuedToONameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        issuedToUNameStringBuilder.setSpan(blueColorSpan, uNameLabel.length, issuedToUNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        issuedByCNameStringBuilder.setSpan(blueColorSpan, cNameLabel.length, issuedByCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        issuedByONameStringBuilder.setSpan(blueColorSpan, oNameLabel.length, issuedByONameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        issuedByUNameStringBuilder.setSpan(blueColorSpan, uNameLabel.length, issuedByUNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        startDateStringBuilder.setSpan(blueColorSpan, startDateLabel.length, startDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        endDateStringBuilder.setSpan(blueColorSpan, endDateLabel.length, endDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

        // Define the primary error string.
        var primaryErrorString = ""

        // Highlight the primary error in red and store it in the primary error string.
        when (primaryErrorInt) {
            SslError.SSL_IDMISMATCH -> {
                // Change the URL span colors to red.
                urlStringBuilder.setSpan(redColorSpan, urlLabel.length, urlStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                issuedToCNameStringBuilder.setSpan(redColorSpan, cNameLabel.length, issuedToCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

                // Store the primary error string.
                primaryErrorString = getString(R.string.cn_mismatch)
            }

            SslError.SSL_UNTRUSTED -> {
                // Change the issued by text view text to red.  The deprecated `getColor()` must be used until the minimum API >= 23.
                if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {
                    @Suppress("DEPRECATION")
                    issuedByTextView.setTextColor(resources.getColor(R.color.red_a700))
                } else {
                    @Suppress("DEPRECATION")
                    issuedByTextView.setTextColor(resources.getColor(R.color.red_900))
                }

                // Change the issued by span color to red.
                issuedByCNameStringBuilder.setSpan(redColorSpan, cNameLabel.length, issuedByCNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                issuedByONameStringBuilder.setSpan(redColorSpan, oNameLabel.length, issuedByONameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                issuedByUNameStringBuilder.setSpan(redColorSpan, uNameLabel.length, issuedByUNameStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

                // Store the primary error string.
                primaryErrorString = getString(R.string.untrusted)
            }

            SslError.SSL_DATE_INVALID -> {
                // Change the valid dates text view text to red.  The deprecated `getColor()` must be used until the minimum API >= 23.
                if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {
                    @Suppress("DEPRECATION")
                    validDatesTextView.setTextColor(resources.getColor(R.color.red_a700))
                } else {
                    @Suppress("DEPRECATION")
                    validDatesTextView.setTextColor(resources.getColor(R.color.red_900))
                }

                // Change the date span colors to red.
                startDateStringBuilder.setSpan(redColorSpan, startDateLabel.length, startDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
                endDateStringBuilder.setSpan(redColorSpan, endDateLabel.length, endDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

                // Store the primary error string.
                primaryErrorString = getString(R.string.invalid_date)
            }

            SslError.SSL_NOTYETVALID -> {
                // Change the start date span color to red.
                startDateStringBuilder.setSpan(redColorSpan, startDateLabel.length, startDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

                // Store the primary error string.
                primaryErrorString = getString(R.string.future_certificate)
            }

            SslError.SSL_EXPIRED -> {
                // Change the end date span color to red.
                endDateStringBuilder.setSpan(redColorSpan, endDateLabel.length, endDateStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

                // Store the primary error string.
                primaryErrorString = getString(R.string.expired_certificate)
            }

            SslError.SSL_INVALID ->
                // Store the primary error string.
                primaryErrorString = getString(R.string.invalid_certificate)
        }

        // Display the strings.
        primaryErrorTextView.text = primaryErrorString
        urlTextView.text = urlStringBuilder
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

    // This must run asynchronously because it involves a network request.  `String` declares the parameters.  `Void` does not declare progress units.  `SpannableStringBuilder` contains the results.
    private class GetIpAddresses constructor(activity: Activity, alertDialog: AlertDialog) : AsyncTask<String, Void?, SpannableStringBuilder>() {
        // Define the weak references.
        private val activityWeakReference: WeakReference<Activity> = WeakReference(activity)
        private val alertDialogWeakReference: WeakReference<AlertDialog> = WeakReference(alertDialog)

        override fun doInBackground(vararg domainName: String): SpannableStringBuilder {
            // Get handles for the activity and the alert dialog.
            val activity = activityWeakReference.get()
            val alertDialog = alertDialogWeakReference.get()

            // Abort if the activity or the dialog is gone.
            if (activity == null || activity.isFinishing || alertDialog == null) {
                return SpannableStringBuilder()
            }

            // Initialize an IP address string builder.
            val ipAddresses = StringBuilder()

            // Get an array with the IP addresses for the host.
            try {
                // Get an array with all the IP addresses for the domain.
                val inetAddressesArray = InetAddress.getAllByName(domainName[0])

                // Add each IP address to the string builder.
                for (inetAddress in inetAddressesArray) {
                    // Check to see if this is not the first IP address.
                    if (ipAddresses.isNotEmpty()) {
                        // Add a line break to the string builder first.
                        ipAddresses.append("\n")
                    }

                    // Add the IP Address to the string builder.
                    ipAddresses.append(inetAddress.hostAddress)
                }
            } catch (exception: UnknownHostException) {
                // Do nothing.
            }

            // Set the label.
            val ipAddressesLabel = activity.getString(R.string.ip_addresses) + "  "

            // Create a spannable string builder.
            val ipAddressesStringBuilder = SpannableStringBuilder(ipAddressesLabel + ipAddresses)

            // Create a blue foreground color span.
            val blueColorSpan: ForegroundColorSpan

            // Get the current theme status.
            val currentThemeStatus = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

            // Set the blue color span according to the theme.  The deprecated `getColor()` must be used until the minimum API >= 23.
            blueColorSpan = if (currentThemeStatus == Configuration.UI_MODE_NIGHT_NO) {
                @Suppress("DEPRECATION")
                ForegroundColorSpan(activity.resources.getColor(R.color.blue_700))
            } else {
                @Suppress("DEPRECATION")
                ForegroundColorSpan(activity.resources.getColor(R.color.violet_500))
            }

            // Set the string builder to display the certificate information in blue.  `SPAN_INCLUSIVE_INCLUSIVE` allows the span to grow in either direction.
            ipAddressesStringBuilder.setSpan(blueColorSpan, ipAddressesLabel.length, ipAddressesStringBuilder.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

            // Return the formatted string.
            return ipAddressesStringBuilder
        }

        // `onPostExecute()` operates on the UI thread.
        override fun onPostExecute(ipAddresses: SpannableStringBuilder) {
            // Get handles for the activity and the alert dialog.
            val activity = activityWeakReference.get()
            val alertDialog = alertDialogWeakReference.get()

            // Abort if the activity or the alert dialog is gone.
            if (activity == null || activity.isFinishing || alertDialog == null) {
                return
            }

            // Get a handle for the IP addresses text view.
            val ipAddressesTextView = alertDialog.findViewById<TextView>(R.id.ip_addresses)!!

            // Populate the IP addresses text view.
            ipAddressesTextView.text = ipAddresses
        }
    }
}