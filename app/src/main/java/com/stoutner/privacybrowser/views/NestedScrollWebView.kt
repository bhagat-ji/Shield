/*
 * Copyright © 2019-2022 Soren Stoutner <soren@stoutner.com>.
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

package com.stoutner.privacybrowser.views

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.HttpAuthHandler
import android.webkit.SslErrorHandler
import android.webkit.WebView

import androidx.core.content.ContextCompat
import androidx.core.view.NestedScrollingChild2
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.activities.MainWebViewActivity

import java.util.Collections
import java.util.Date

import kotlin.collections.ArrayList

import kotlin.jvm.JvmOverloads

// Define the saved state constants.
private const val DOMAIN_SETTINGS_APPLIED = "domain_settings_applied"
private const val DOMAIN_SETTINGS_DATABASE_ID = "domain_settings_database_id"
private const val CURRENT_DOMAIN_NAME = "current_domain_name"
private const val CURRENT_URl = "current_url"
private const val ACCEPT_COOKIES = "accept_cookies"
private const val EASYLIST_ENABLED = "easylist_enabled"
private const val EASYPRIVACY_ENABLED = "easyprivacy_enabled"
private const val FANBOYS_ANNOYANCE_LIST_ENABLED = "fanboys_annoyance_list_enabled"
private const val FANBOYS_SOCIAL_BLOCKING_LIST_ENABLED = "fanboys_social_blocking_list_enabled"
private const val ULTRALIST_ENABLED = "ultralist_enabled"
private const val ULTRAPRIVACY_ENABLED = "ultraprivacy_enabled"
private const val BLOCK_ALL_THIRD_PARTY_REQUESTS = "block_all_third_party_requests"
private const val HAS_PINNED_SSL_CERTIFICATE = "has_pinned_ssl_certificate"
private const val PINNED_SSL_ISSUED_TO_CNAME = "pinned_ssl_issued_to_cname"
private const val PINNED_SSL_ISSUED_TO_ONAME = "pinned_ssl_issued_to_oname"
private const val PINNED_SSL_ISSUED_TO_UNAME = "pinned_ssl_issued_to_uname"
private const val PINNED_SSL_ISSUED_BY_CNAME = "pinned_ssl_issued_by_cname"
private const val PINNED_SSL_ISSUED_BY_ONAME = "pinned_ssl_issued_by_oname"
private const val PINNED_SSL_ISSUED_BY_UNAME = "pinned_ssl_issued_by_uname"
private const val PINNED_SSL_START_DATE = "pinned_ssl_start_date"
private const val PINNED_SSL_END_DATE = "pinned_ssl_end_date"
private const val PINNED_IP_ADDRESSES = "pinned_ip_addresses"
private const val IGNORE_PINNED_DOMAIN_INFORMATION = "ignore_pinned_domain_information"
private const val SWIPE_TO_REFRESH = "swipe_to_refresh"
private const val JAVASCRIPT_ENABLED = "javascript_enabled"
private const val DOM_STORAGE_ENABLED = "dom_storage_enabled"
private const val USER_AGENT = "user_agent"
private const val WIDE_VIEWPORT = "wide_viewport"
private const val FONT_SIZE = "font_size"

// NestedScrollWebView extends WebView to handle nested scrolls (scrolling the app bar off the screen).  It also stores extra information about the state of the WebView used by Privacy Browser.
class NestedScrollWebView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defaultStyle: Int = android.R.attr.webViewStyle) : WebView(context, attributeSet, defaultStyle),
    NestedScrollingChild2 {
    companion object {
        // Define the public companion object constants.  These can be moved to public class constants once the entire project has migrated to Kotlin.
        const val BLOCKED_REQUESTS = 0
        const val EASYLIST = 1
        const val EASYPRIVACY = 2
        const val FANBOYS_ANNOYANCE_LIST = 3
        const val FANBOYS_SOCIAL_BLOCKING_LIST = 4
        const val ULTRALIST = 5
        const val ULTRAPRIVACY = 6
        const val THIRD_PARTY_REQUESTS = 7
    }

    // Define the public variables.
    var acceptCookies = false
    var blockAllThirdPartyRequests = false
    var currentDomainName = ""
    var currentIpAddresses = ""
    var currentUrl = ""
    var domainSettingsApplied = false
    var domainSettingsDatabaseId = 0
    var easyListEnabled = true
    var easyPrivacyEnabled = true
    var fanboysAnnoyanceListEnabled = true
    var fanboysSocialBlockingListEnabled = true
    var httpAuthHandler: HttpAuthHandler? = null
    var ignorePinnedDomainInformation = false
    var pinnedIpAddresses = ""
    var sslErrorHandler: SslErrorHandler? = null
    var swipeToRefresh = false
    var ultraListEnabled = true
    var ultraPrivacyEnabled = true
    var waitingForProxyUrlString = ""
    var webViewFragmentId: Long = 0


    // Define the private variables.
    private val nestedScrollingChildHelper: NestedScrollingChildHelper = NestedScrollingChildHelper(this)
    private lateinit var favoriteOrDefaultIcon: Bitmap
    private var previousYPosition = 0  // The previous Y position needs to be tracked between motion events.
    private var hasPinnedSslCertificate = false
    private var pinnedSslIssuedToCName = ""
    private var pinnedSslIssuedToOName = ""
    private var pinnedSslIssuedToUName = ""
    private var pinnedSslIssuedByCName = ""
    private var pinnedSslIssuedByOName = ""
    private var pinnedSslIssuedByUName = ""
    private var pinnedSslStartDate = Date(0)
    private var pinnedSslEndDate = Date(0)
    private val resourceRequests = Collections.synchronizedList(ArrayList<Array<String>>())  // Using a synchronized list makes adding resource requests thread safe.
    private var blockedRequests = 0
    private var easyListBlockedRequests = 0
    private var easyPrivacyBlockedRequests = 0
    private var fanboysAnnoyanceListBlockedRequests = 0
    private var fanboysSocialBlockingListBlockedRequests = 0
    private var ultraListBlockedRequests = 0
    private var ultraPrivacyBlockedRequests = 0
    private var thirdPartyBlockedRequests = 0
    private var xRequestedWithHeader = mutableMapOf<String, String>()

    init {
        // Enable nested scrolling by default.
        nestedScrollingChildHelper.isNestedScrollingEnabled = true

        // Initialize the favorite icon.
        initializeFavoriteIcon()
    }


    // Favorite or default icon.
    fun initializeFavoriteIcon() {
        // Get the default favorite icon drawable.  `ContextCompat` must be used until API >= 21.
        val favoriteIconDrawable = ContextCompat.getDrawable(context, R.drawable.world)

        // Cast the favorite icon drawable to a bitmap drawable.
        val favoriteIconBitmapDrawable = (favoriteIconDrawable as BitmapDrawable?)!!

        // Store the default icon bitmap.
        favoriteOrDefaultIcon = favoriteIconBitmapDrawable.bitmap
    }

    fun setFavoriteOrDefaultIcon(icon: Bitmap) {
        // Scale the favorite icon bitmap down if it is larger than 256 x 256.  Filtering uses bilinear interpolation.
        favoriteOrDefaultIcon = if (icon.height > 256 || icon.width > 256) {
            Bitmap.createScaledBitmap(icon, 256, 256, true)
        } else {
            // Store the icon as presented.
            icon
        }
    }

    fun getFavoriteOrDefaultIcon(): Bitmap {
        // Return the favorite or default icon.  This is the only way to return a non-nullable variable while retaining the custom initialization and setter functions above.
        return favoriteOrDefaultIcon
    }


    // Reset the handlers.
    fun resetSslErrorHandler() {
        // Reset the current SSL error handler.
        sslErrorHandler = null
    }

    fun resetHttpAuthHandler() {
        // Reset the current HTTP authentication handler.
        httpAuthHandler = null
    }


    // Pinned SSL certificates.
    fun hasPinnedSslCertificate(): Boolean {
        // Return the status of the pinned SSL certificate.
        return hasPinnedSslCertificate
    }

    fun setPinnedSslCertificate(issuedToCName: String, issuedToOName: String, issuedToUName: String, issuedByCName: String, issuedByOName: String, issuedByUName: String, startDate: Date, endDate: Date) {
        // Store the pinned SSL certificate information.
        pinnedSslIssuedToCName = issuedToCName
        pinnedSslIssuedToOName = issuedToOName
        pinnedSslIssuedToUName = issuedToUName
        pinnedSslIssuedByCName = issuedByCName
        pinnedSslIssuedByOName = issuedByOName
        pinnedSslIssuedByUName = issuedByUName
        pinnedSslStartDate = startDate
        pinnedSslEndDate = endDate

        // Set the pinned SSL certificate tracker.
        hasPinnedSslCertificate = true
    }

    fun getPinnedSslCertificate(): Pair<Array<String>, Array<Date>> {
        // Create the SSL certificate string array.
        val sslCertificateStringArray = arrayOf(pinnedSslIssuedToCName, pinnedSslIssuedToOName, pinnedSslIssuedToUName, pinnedSslIssuedByCName, pinnedSslIssuedByOName, pinnedSslIssuedByUName)

        // Create the SSL certificate date array.
        val sslCertificateDateArray = arrayOf(pinnedSslStartDate, pinnedSslEndDate)

        // Return the pinned SSL certificate pair.
        return Pair(sslCertificateStringArray, sslCertificateDateArray)
    }

    fun clearPinnedSslCertificate() {
        // Clear the pinned SSL certificate.
        pinnedSslIssuedToCName = ""
        pinnedSslIssuedToOName = ""
        pinnedSslIssuedToUName = ""
        pinnedSslIssuedByCName = ""
        pinnedSslIssuedByOName = ""
        pinnedSslIssuedByUName = ""
        pinnedSslStartDate = Date(0)
        pinnedSslEndDate = Date(0)

        // Clear the pinned SSL certificate tracker.
        hasPinnedSslCertificate = false
    }


    // Resource requests.
    fun addResourceRequest(resourceRequest: Array<String>) {
        // Add the resource request to the list.
        resourceRequests.add(resourceRequest)
    }

    fun getResourceRequests(): List<Array<String>> {
        // Return the list of resource requests as an array list.
        return resourceRequests
    }

    fun clearResourceRequests() {
        // Clear the resource requests.
        resourceRequests.clear()
    }


    // Resource request counters.
    fun incrementRequestsCount(blocklist: Int) {
        // Increment the count of the indicated blocklist.
        when (blocklist) {
            BLOCKED_REQUESTS -> blockedRequests++
            EASYLIST -> easyListBlockedRequests++
            EASYPRIVACY -> easyPrivacyBlockedRequests++
            FANBOYS_ANNOYANCE_LIST -> fanboysAnnoyanceListBlockedRequests++
            FANBOYS_SOCIAL_BLOCKING_LIST -> fanboysSocialBlockingListBlockedRequests++
            ULTRALIST -> ultraListBlockedRequests++
            ULTRAPRIVACY -> ultraPrivacyBlockedRequests++
            THIRD_PARTY_REQUESTS -> thirdPartyBlockedRequests++
        }
    }

    fun getRequestsCount(blocklist: Int): Int {
        // Return the count of the indicated blocklist.
        return when (blocklist) {
            BLOCKED_REQUESTS -> blockedRequests
            EASYLIST -> easyListBlockedRequests
            EASYPRIVACY -> easyPrivacyBlockedRequests
            FANBOYS_ANNOYANCE_LIST -> fanboysAnnoyanceListBlockedRequests
            FANBOYS_SOCIAL_BLOCKING_LIST -> fanboysSocialBlockingListBlockedRequests
            ULTRALIST -> ultraListBlockedRequests
            ULTRAPRIVACY -> ultraPrivacyBlockedRequests
            THIRD_PARTY_REQUESTS -> thirdPartyBlockedRequests
            else -> 0 // Return 0.  This should never be called, but it is required by the return when statement.
        }
    }

    fun resetRequestsCounters() {
        // Reset all the resource request counters.
        blockedRequests = 0
        easyListBlockedRequests = 0
        easyPrivacyBlockedRequests = 0
        fanboysAnnoyanceListBlockedRequests = 0
        fanboysSocialBlockingListBlockedRequests = 0
        ultraListBlockedRequests = 0
        ultraPrivacyBlockedRequests = 0
        thirdPartyBlockedRequests = 0
    }


    // X-Requested-With header.
    fun getXRequestedWithHeader() : MutableMap<String, String> {
        // Return the X-Requested-With header.
        return xRequestedWithHeader
    }

    fun setXRequestedWithHeader() {
        // Set the X-Requested-With header to use a null value.
        if (xRequestedWithHeader.isEmpty())
            xRequestedWithHeader["X-Requested-With"] = ""
    }

    fun resetXRequestedWithHeader() {
        // Clear the map, which resets the X-Requested-With header to use the default value of the application ID (com.stoutner.privacybrowser.standard).
        xRequestedWithHeader.clear()
    }


    // Publicly expose the scroll ranges.
    fun getHorizontalScrollRange(): Int {
        // Return the horizontal scroll range.
        return computeHorizontalScrollRange()
    }

    fun getVerticalScrollRange(): Int {
        // Return the vertical scroll range.
        return computeVerticalScrollRange()
    }

    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        // Run the default commands.
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)

        // Display the bottom app bar if it has been hidden and the WebView was over-scrolled at the top of the screen.
        if ((MainWebViewActivity.appBarLayout.translationY != 0f) && (scrollY == 0) && clampedY) {
            // Animate the bottom app bar onto the screen.
            val objectAnimator = ObjectAnimator.ofFloat(MainWebViewActivity.appBarLayout, "translationY", 0f)

            // Make it so.
            objectAnimator.start()
        }
    }

    // Handle touches.
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        // Run the commands for the given motion event action.
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                // Start nested scrolling along the vertical axis.  `ViewCompat` must be used until the minimum API >= 21.
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)

                // Save the current Y position.  Action down will not be called again until a new motion starts.
                previousYPosition = motionEvent.y.toInt()
            }
            MotionEvent.ACTION_MOVE -> {
                // Get the current Y position.
                val currentYMotionPosition = motionEvent.y.toInt()

                // Calculate the pre-scroll delta Y.
                val preScrollDeltaY = previousYPosition - currentYMotionPosition

                // Initialize a variable to track how much of the scroll is consumed.
                val consumedScroll = IntArray(2)

                // Initialize a variable to track the offset in the window.
                val offsetInWindow = IntArray(2)

                // Get the WebView Y position.
                val webViewYPosition = scrollY

                // Set the scroll delta Y to initially be the same as the pre-scroll delta Y.
                var scrollDeltaY = preScrollDeltaY

                // Dispatch the nested pre-school.  This scrolls the app bar if it needs it.  `offsetInWindow` will be returned with an updated value.
                if (dispatchNestedPreScroll(0, preScrollDeltaY, consumedScroll, offsetInWindow)) {
                    // Update the scroll delta Y if some of it was consumed.
                    scrollDeltaY = preScrollDeltaY - consumedScroll[1]
                }

                // Check to see if the WebView is at the top and and the scroll action is downward.
                if (webViewYPosition == 0 && scrollDeltaY < 0) {  // Swipe to refresh is being engaged.
                    // Stop the nested scroll so that swipe to refresh has complete control.  This way releasing the scroll to refresh circle doesn't scroll the WebView at the same time.
                    stopNestedScroll()
                } else {  // Swipe to refresh is not being engaged.
                    // Start the nested scroll so that the app bar can scroll off the screen.
                    startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)

                    // Dispatch the nested scroll.  This scrolls the WebView.  The delta Y unconsumed normally controls the swipe refresh layout, but that is handled with the `if` statement above.
                    dispatchNestedScroll(0, scrollDeltaY, 0, 0, offsetInWindow)

                    // Store the current Y position for use in the next action move.
                    previousYPosition -= scrollDeltaY
                }
            }
            else -> stopNestedScroll()  // Stop nested scrolling.
        }

        // Perform a click.  This is required by the Android accessibility guidelines.
        performClick()

        // Run the default commands and return the result.
        return super.onTouchEvent(motionEvent)
    }


    // Save and restore state.
    fun saveNestedScrollWebViewState(): Bundle {
        // Create a saved state bundle.
        val savedState = Bundle()

        // Populate the saved state bundle.
        savedState.putBoolean(DOMAIN_SETTINGS_APPLIED, domainSettingsApplied)
        savedState.putInt(DOMAIN_SETTINGS_DATABASE_ID, domainSettingsDatabaseId)
        savedState.putString(CURRENT_DOMAIN_NAME, currentDomainName)
        savedState.putString(CURRENT_URl, currentUrl)
        savedState.putBoolean(ACCEPT_COOKIES, acceptCookies)
        savedState.putBoolean(EASYLIST_ENABLED, easyListEnabled)
        savedState.putBoolean(EASYPRIVACY_ENABLED, easyPrivacyEnabled)
        savedState.putBoolean(FANBOYS_ANNOYANCE_LIST_ENABLED, fanboysAnnoyanceListEnabled)
        savedState.putBoolean(FANBOYS_SOCIAL_BLOCKING_LIST_ENABLED, fanboysSocialBlockingListEnabled)
        savedState.putBoolean(ULTRALIST_ENABLED, ultraListEnabled)
        savedState.putBoolean(ULTRAPRIVACY_ENABLED, ultraPrivacyEnabled)
        savedState.putBoolean(BLOCK_ALL_THIRD_PARTY_REQUESTS, blockAllThirdPartyRequests)
        savedState.putBoolean(HAS_PINNED_SSL_CERTIFICATE, hasPinnedSslCertificate)
        savedState.putString(PINNED_SSL_ISSUED_TO_CNAME, pinnedSslIssuedToCName)
        savedState.putString(PINNED_SSL_ISSUED_TO_ONAME, pinnedSslIssuedToOName)
        savedState.putString(PINNED_SSL_ISSUED_TO_UNAME, pinnedSslIssuedToUName)
        savedState.putString(PINNED_SSL_ISSUED_BY_CNAME, pinnedSslIssuedByCName)
        savedState.putString(PINNED_SSL_ISSUED_BY_ONAME, pinnedSslIssuedByOName)
        savedState.putString(PINNED_SSL_ISSUED_BY_UNAME, pinnedSslIssuedByUName)
        savedState.putLong(PINNED_SSL_START_DATE, pinnedSslStartDate.time)
        savedState.putLong(PINNED_SSL_END_DATE, pinnedSslEndDate.time)
        savedState.putString(PINNED_IP_ADDRESSES, pinnedIpAddresses)
        savedState.putBoolean(IGNORE_PINNED_DOMAIN_INFORMATION, ignorePinnedDomainInformation)
        savedState.putBoolean(SWIPE_TO_REFRESH, swipeToRefresh)
        savedState.putBoolean(JAVASCRIPT_ENABLED, this.settings.javaScriptEnabled)
        savedState.putBoolean(DOM_STORAGE_ENABLED, this.settings.domStorageEnabled)
        savedState.putString(USER_AGENT, this.settings.userAgentString)
        savedState.putBoolean(WIDE_VIEWPORT, this.settings.useWideViewPort)
        savedState.putInt(FONT_SIZE, this.settings.textZoom)

        // Return the saved state bundle.
        return savedState
    }

    fun restoreNestedScrollWebViewState(savedState: Bundle) {
        // Restore the class variables.
        domainSettingsApplied = savedState.getBoolean(DOMAIN_SETTINGS_APPLIED)
        domainSettingsDatabaseId = savedState.getInt(DOMAIN_SETTINGS_DATABASE_ID)
        currentDomainName = savedState.getString(CURRENT_DOMAIN_NAME)!!
        currentUrl = savedState.getString(CURRENT_URl)!!
        acceptCookies = savedState.getBoolean(ACCEPT_COOKIES)
        easyListEnabled = savedState.getBoolean(EASYLIST_ENABLED)
        easyPrivacyEnabled = savedState.getBoolean(EASYPRIVACY_ENABLED)
        fanboysAnnoyanceListEnabled = savedState.getBoolean(FANBOYS_ANNOYANCE_LIST_ENABLED)
        fanboysSocialBlockingListEnabled = savedState.getBoolean(FANBOYS_SOCIAL_BLOCKING_LIST_ENABLED)
        ultraListEnabled = savedState.getBoolean(ULTRALIST_ENABLED)
        ultraPrivacyEnabled = savedState.getBoolean(ULTRAPRIVACY_ENABLED)
        blockAllThirdPartyRequests = savedState.getBoolean(BLOCK_ALL_THIRD_PARTY_REQUESTS)
        hasPinnedSslCertificate = savedState.getBoolean(HAS_PINNED_SSL_CERTIFICATE)
        pinnedSslIssuedToCName = savedState.getString(PINNED_SSL_ISSUED_TO_CNAME)!!
        pinnedSslIssuedToOName = savedState.getString(PINNED_SSL_ISSUED_TO_ONAME)!!
        pinnedSslIssuedToUName = savedState.getString(PINNED_SSL_ISSUED_TO_UNAME)!!
        pinnedSslIssuedByCName = savedState.getString(PINNED_SSL_ISSUED_BY_CNAME)!!
        pinnedSslIssuedByOName = savedState.getString(PINNED_SSL_ISSUED_BY_ONAME)!!
        pinnedSslIssuedByUName = savedState.getString(PINNED_SSL_ISSUED_BY_UNAME)!!
        pinnedSslStartDate = Date(savedState.getLong(PINNED_SSL_START_DATE))
        pinnedSslEndDate = Date(savedState.getLong(PINNED_SSL_END_DATE))
        pinnedIpAddresses = savedState.getString(PINNED_IP_ADDRESSES)!!
        ignorePinnedDomainInformation = savedState.getBoolean(IGNORE_PINNED_DOMAIN_INFORMATION)
        swipeToRefresh = savedState.getBoolean(SWIPE_TO_REFRESH)
        this.settings.javaScriptEnabled = savedState.getBoolean(JAVASCRIPT_ENABLED)
        this.settings.domStorageEnabled = savedState.getBoolean(DOM_STORAGE_ENABLED)
        this.settings.userAgentString = savedState.getString(USER_AGENT)
        this.settings.useWideViewPort = savedState.getBoolean(WIDE_VIEWPORT)
        this.settings.textZoom = savedState.getInt(FONT_SIZE)
    }


    // Method from NestedScrollingChild.
    override fun setNestedScrollingEnabled(status: Boolean) {
        // Set the status of the nested scrolling.
        nestedScrollingChildHelper.isNestedScrollingEnabled = status
    }

    // Method from NestedScrollingChild.
    override fun isNestedScrollingEnabled(): Boolean {
        // Return the status of nested scrolling.
        return nestedScrollingChildHelper.isNestedScrollingEnabled
    }

    // Method from NestedScrollingChild.
    override fun startNestedScroll(axes: Int): Boolean {
        // Start a nested scroll along the indicated axes.
        return nestedScrollingChildHelper.startNestedScroll(axes)
    }

    // Method from NestedScrollingChild2.
    override fun startNestedScroll(axes: Int, type: Int): Boolean {
        // Start a nested scroll along the indicated axes for the given type of input which caused the scroll event.
        return nestedScrollingChildHelper.startNestedScroll(axes, type)
    }

    // Method from NestedScrollingChild.
    override fun stopNestedScroll() {
        // Stop the nested scroll.
        nestedScrollingChildHelper.stopNestedScroll()
    }

    // Method from NestedScrollingChild2.
    override fun stopNestedScroll(type: Int) {
        // Stop the nested scroll of the given type of input which caused the scroll event.
        nestedScrollingChildHelper.stopNestedScroll(type)
    }

    // Method from NestedScrollingChild.
    override fun hasNestedScrollingParent(): Boolean {
        // Return the status of the nested scrolling parent.
        return nestedScrollingChildHelper.hasNestedScrollingParent()
    }

    // Method from NestedScrollingChild2.
    override fun hasNestedScrollingParent(type: Int): Boolean {
        // return the status of the nested scrolling parent for the given type of input which caused the scroll event.
        return nestedScrollingChildHelper.hasNestedScrollingParent(type)
    }

    // Method from NestedScrollingChild.
    override fun dispatchNestedPreScroll(deltaX: Int, deltaY: Int, consumed: IntArray?, offsetInWindow: IntArray?): Boolean {
        // Dispatch a nested pre-scroll with the specified deltas, which lets a parent to consume some of the scroll if desired.
        return nestedScrollingChildHelper.dispatchNestedPreScroll(deltaX, deltaY, consumed, offsetInWindow)
    }

    // Method from NestedScrollingChild2.
    override fun dispatchNestedPreScroll(deltaX: Int, deltaY: Int, consumed: IntArray?, offsetInWindow: IntArray?, type: Int): Boolean {
        // Dispatch a nested pre-scroll with the specified deltas for the given type of input which caused the scroll event, which lets a parent to consume some of the scroll if desired.
        return nestedScrollingChildHelper.dispatchNestedPreScroll(deltaX, deltaY, consumed, offsetInWindow, type)
    }

    // Method from NestedScrollingChild.
    override fun dispatchNestedScroll(deltaXConsumed: Int, deltaYConsumed: Int, deltaXUnconsumed: Int, deltaYUnconsumed: Int, offsetInWindow: IntArray?): Boolean {
        // Dispatch a nested scroll with the specified deltas.
        return nestedScrollingChildHelper.dispatchNestedScroll(deltaXConsumed, deltaYConsumed, deltaXUnconsumed, deltaYUnconsumed, offsetInWindow)
    }

    // Method from NestedScrollingChild2.
    override fun dispatchNestedScroll(deltaXConsumed: Int, deltaYConsumed: Int, deltaXUnconsumed: Int, deltaYUnconsumed: Int, offsetInWindow: IntArray?, type: Int): Boolean {
        // Dispatch a nested scroll with the specified deltas for the given type of input which caused the scroll event.
        return nestedScrollingChildHelper.dispatchNestedScroll(deltaXConsumed, deltaYConsumed, deltaXUnconsumed, deltaYUnconsumed, offsetInWindow, type)
    }

    // Method from NestedScrollingChild.
    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        // Dispatch a nested pre-fling with the specified velocity, which lets a parent consume the fling if desired.
        return nestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY)
    }

    // Method from NestedScrollingChild.
    override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
        // Dispatch a nested fling with the specified velocity.
        return nestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed)
    }
}