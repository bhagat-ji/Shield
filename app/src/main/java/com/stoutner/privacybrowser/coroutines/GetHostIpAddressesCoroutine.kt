/*
 * Copyright 2019,2021-2022 Soren Stoutner <soren@stoutner.com>.
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

package com.stoutner.privacybrowser.coroutines

import androidx.fragment.app.FragmentManager
import com.stoutner.privacybrowser.helpers.CheckPinnedMismatchHelper
import com.stoutner.privacybrowser.views.NestedScrollWebView

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.lang.StringBuilder
import java.net.InetAddress
import java.net.UnknownHostException

object GetHostIpAddressesCoroutine {
    @JvmStatic
    fun getAddresses(domainName: String, nestedScrollWebView: NestedScrollWebView, supportFragmentManager: FragmentManager, pinnedMismatchString: String) {
        // Get the IP addresses using a coroutine.
        CoroutineScope(Dispatchers.Main).launch {
            // Get the IP addresses on the IO thread.
            withContext(Dispatchers.IO) {
                // Get an array with the IP addresses for the host.
                try {
                    // Initialize an IP address string builder.
                    val ipAddresses = StringBuilder()

                    // Get an array with all the IP addresses for the domain.
                    val inetAddressesArray = InetAddress.getAllByName(domainName)

                    // Add each IP address to the string builder.
                    for (inetAddress in inetAddressesArray) {
                        // Add a line break to the string builder if this is not the first IP address.
                        if (ipAddresses.isNotEmpty()) {
                            ipAddresses.append("\n")
                        }

                        // Add the IP address to the string builder.
                        ipAddresses.append(inetAddress.hostAddress)
                    }

                    // Store the IP addresses.
                    nestedScrollWebView.currentIpAddresses = ipAddresses.toString()

                    // Checked for pinned mismatches if there is pinned information and it is not ignored.  This must be done on the UI thread because checking the pinned mismatch interacts with the WebView.
                    withContext(Dispatchers.Main) {
                        if ((nestedScrollWebView.hasPinnedSslCertificate() || nestedScrollWebView.pinnedIpAddresses.isNotEmpty()) && !nestedScrollWebView.ignorePinnedDomainInformation) {
                            CheckPinnedMismatchHelper.checkPinnedMismatch(nestedScrollWebView, supportFragmentManager, pinnedMismatchString)
                        }
                    }
                } catch (exception: UnknownHostException) {
                    // Do nothing.
                }
            }
        }
    }
}
