
/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2018-2019, 2021-2023, 2025-2026 Soren Stoutner <soren@stoutner.com>
 *
 * This file is part of Privacy Browser Android <https://www.stoutner.com/privacy-browser-android/>.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.stoutner.privacybrowser.helpers

import android.util.Log
import com.stoutner.privacybrowser.dataclasses.FilterListDataClass
import com.stoutner.privacybrowser.dataclasses.FilterListEntryDataClass
import com.stoutner.privacybrowser.dataclasses.FilterOptionDisposition
import com.stoutner.privacybrowser.dataclasses.MatchedUrlType
import com.stoutner.privacybrowser.dataclasses.RequestDataClass
import com.stoutner.privacybrowser.dataclasses.RequestDisposition
import com.stoutner.privacybrowser.dataclasses.Sublist

import java.util.regex.Pattern


// The public filter list data classes.
var easyListDataClass = FilterListDataClass()
var easyPrivacyDataClass = FilterListDataClass()
var fanboysAnnoyanceDataClass: FilterListDataClass? = null  // This is nullable so that the app can check to see if it has been restarted.
var ultraListDataClass = FilterListDataClass()
var ultraPrivacyDataClass = FilterListDataClass()

class CheckFilterListHelper {
    fun checkFilterList(requestDataClass: RequestDataClass, filterListDataClass: FilterListDataClass): Boolean {
        // Create a continue checking tracker.  If the tracker changes to false, all further checking of the request will be bypassed.
        var continueChecking = true

        // Check the main allow list.
        for (filterListEntryDataClass in filterListDataClass.mainAllowList) {
            // Check the applied entry against the request URL.
            continueChecking = checkAppliedEntry(requestDataClass, MatchedUrlType.Url, filterListEntryDataClass)

            // Check the applied entry against the request URL with separators.
            if (continueChecking)
                continueChecking = checkAppliedEntry(requestDataClass, MatchedUrlType.UrlWithSeparators, filterListEntryDataClass)

            // Exit the loop if checking should no longer be continued.
            if (!continueChecking)
                break
        }

        // Check the initial domain allow list unless a match has already been found.
        if (continueChecking) {
            // Check the initial domain allow list.
            for (filterListEntryDataClass in filterListDataClass.initialDomainAllowList) {
                // Check the applied entry against the truncated URL.
                continueChecking = checkAppliedEntry(requestDataClass, MatchedUrlType.TruncatedUrl, filterListEntryDataClass)

                //Check the applied entry against the truncated URL with separators.
                if (continueChecking)
                    continueChecking = checkAppliedEntry(requestDataClass, MatchedUrlType.TruncatedUrlWithSeparators, filterListEntryDataClass)

                // Exit the loop if checking should no longer be continued.
                if (!continueChecking)
                    break
            }
        }

        // Check the regular expression allow list unless a match has already been found.
        if (continueChecking) {
            // Check the regular expression allow list.
            for (filterListEntryDataClass in filterListDataClass.regularExpressionAllowList) {
                // Check the applied entries.
                continueChecking = checkRegularExpression(requestDataClass, filterListEntryDataClass)

                // Exit the loop if checking should no longer be continued.
                if (!continueChecking)
                    break
            }
        }

        // Check the main block list unless a match has already been found.
        if (continueChecking) {
            // Check the main block list.
            for (filterListEntryDataClass in filterListDataClass.mainBlockList) {
                // Check the applied entry against the request URL.
                continueChecking = checkAppliedEntry(requestDataClass, MatchedUrlType.Url, filterListEntryDataClass)

                // Check the applied entry against the request URL with separators.
                if (continueChecking)
                    continueChecking = checkAppliedEntry(requestDataClass, MatchedUrlType.UrlWithSeparators, filterListEntryDataClass)

                // Exit the loop if checking should no longer be continued.
                if (!continueChecking)
                    break
            }
        }

        // Check the initial domain block list unless a match has already been found.
        if (continueChecking) {
            // Check the initial domain block list.
            for (filterListEntryDataClass in filterListDataClass.initialDomainBlockList) {
                // Check the applied entry against the truncated URL.
                continueChecking = checkAppliedEntry(requestDataClass, MatchedUrlType.TruncatedUrl, filterListEntryDataClass)

                // Check the applied entry against the truncated URL with separators.
                if (continueChecking)
                    continueChecking = checkAppliedEntry(requestDataClass, MatchedUrlType.TruncatedUrlWithSeparators, filterListEntryDataClass)

                // Exit the loop if checking should no longer be continued.
                if (!continueChecking)
                    break
            }
        }

        // Check the regular expression block list unless a match has already been found.
        if (continueChecking) {
            // Check the regular expression block list.
            for (filterListEntryDataClass in filterListDataClass.regularExpressionBlockList) {
                // Check the applied entries.
                continueChecking = checkRegularExpression(requestDataClass, filterListEntryDataClass)

                // Exit the loop if checking should no longer be continued.
                if (!continueChecking)
                    break
            }
        }

        // Return the continue checking status.
        return continueChecking
    }

    private fun checkAppliedEntry(requestDataClass : RequestDataClass, matchedUrlType : MatchedUrlType, filterListEntryDataClass : FilterListEntryDataClass) : Boolean {
        // Populate the URL string according to the matched URL type.
        var urlString = when (matchedUrlType) {
            MatchedUrlType.Url -> requestDataClass.requestUrlString
            MatchedUrlType.UrlWithSeparators -> requestDataClass.requestUrlWithSeparatorsString
            MatchedUrlType.TruncatedUrl -> requestDataClass.truncatedRequestUrlString
            MatchedUrlType.TruncatedUrlWithSeparators -> requestDataClass.truncatedRequestUrlWithSeparatorsString
        }

        // Check the entries according to the number.
        if (filterListEntryDataClass.singleAppliedEntry) {  // There is a single entry.
            // Process the initial and final matches.
            if (filterListEntryDataClass.initialMatch && filterListEntryDataClass.finalMatch) {  // This is both an initial and final match.
                // Check the URL against the applied entry.
                if (urlString == filterListEntryDataClass.appliedEntryList[0]) {
                    // Set the matched URL type.
                    requestDataClass.matchedUrlType = matchedUrlType

                    // Check the domain status.
                    return checkDomain(requestDataClass, filterListEntryDataClass)
                }
            } else if (filterListEntryDataClass.initialMatch) {  // This is an initial match.
                // Check the URL against the applied entry.
                if (urlString.startsWith(filterListEntryDataClass.appliedEntryList[0])) {
                    // Set the matched URL type.
                    requestDataClass.matchedUrlType = matchedUrlType

                    // Check the domain status.
                    return checkDomain(requestDataClass, filterListEntryDataClass)
                }
            } else if (filterListEntryDataClass.finalMatch) {  // This is a final match.
                // Check the URL against the applied entry.
                if (urlString.endsWith(filterListEntryDataClass.appliedEntryList[0])) {
                    // Set the matched URL type.
                    requestDataClass.matchedUrlType = matchedUrlType

                    // Check the domain status.
                    return checkDomain(requestDataClass, filterListEntryDataClass)
                }
            } else {  // There is no initial or final matching.
                // Check if the URL string contains the applied entry.
                if (urlString.contains(filterListEntryDataClass.appliedEntryList[0])) {
                    // Set the matches URL type.
                    requestDataClass.matchedUrlType = matchedUrlType

                    return checkDomain(requestDataClass, filterListEntryDataClass)
                }
            }
        } else {  // There are multiple entries.
            // Create a URL matches flag.
            var urlMatches = true

            // Process initial and final matches.
            if (filterListEntryDataClass.initialMatch && filterListEntryDataClass.finalMatch) {  // This is both an initial and final match.
                // Check the URL.
                if (urlString.startsWith(filterListEntryDataClass.appliedEntryList[0])) {
                    // Get the number of characters to remove from the front of the URL string.
                    val charactersToRemove = filterListEntryDataClass.appliedEntryList[0].length

                    // Remove the entry from the front of the modified URL string.
                    urlString = urlString.substring(charactersToRemove)

                    // Get the entry locations.
                    val penultimateEntryNumber = (filterListEntryDataClass.sizeOfAppliedEntryList - 1)
                    val ultimateEntryIndex: Int = penultimateEntryNumber

                    // Check all the middle entries.
                    for (i in 0 until penultimateEntryNumber) {
                        // Get the applied entry index, which will be `-1` if it doesn't exist.
                        val appliedEntryIndex = urlString.indexOf(filterListEntryDataClass.appliedEntryList[i])

                        // Check if the entry was found.
                        if (appliedEntryIndex >= 0) {  // The entry is contained in the URL string.
                            // Get the number of characters to remove from teh from of the URL string.
                            val charactersToRemove = (appliedEntryIndex + filterListEntryDataClass.appliedEntryList[i].length)

                            // Remove the entry from the fron of the modified URL string.
                            urlString = urlString.substring(charactersToRemove)
                        } else {  // The entry is not contained in the URL string.
                            // Mark the URL matches flag as false.
                            urlMatches = false
                        }
                    }

                    // Check the final entry if the URL still matches.
                    if (urlMatches) {
                        // Check if the URL string ends with the last applied entry.
                        if (urlString.endsWith(filterListEntryDataClass.appliedEntryList[ultimateEntryIndex])) {
                            // Set the matched URL type.
                            requestDataClass.matchedUrlType = matchedUrlType

                            // Check the domain status.
                            return checkDomain(requestDataClass, filterListEntryDataClass)
                        }
                    }
                }
            } else if (filterListEntryDataClass.initialMatch) {  // This is an initial match.
                // Check the URL.
                if (urlString.startsWith(filterListEntryDataClass.appliedEntryList[0])) {
                    // Get the number of characters to remove from the front of the URL string.
                    val charactersToRemove = filterListEntryDataClass.appliedEntryList[0].length

                    // Remove the entry from the front of the modified URL string.
                    urlString = urlString.substring(charactersToRemove)

                    // Check the rest of the entries.
                    for (i in 1 until filterListEntryDataClass.sizeOfAppliedEntryList) {
                        // Get the applied entry index, which will be `-1` if it doesn't exist.
                        val appliedEntryIndex = urlString.indexOf(filterListEntryDataClass.appliedEntryList[i])

                        // Check if the entry was found.
                        if (appliedEntryIndex >= 0) {  // The entry is contained in the URL string.
                            // Get the number of characters to remove from the front of the URL string.
                            val charactersToRemove = appliedEntryIndex + filterListEntryDataClass.appliedEntryList[i].length

                            // Remove the entry from the front of the modified URL string.
                            urlString = urlString.substring(charactersToRemove)
                        } else {  // The entry is not contained in the URL string.
                            // Mark the URL matches flag as false.
                            urlMatches = false
                        }
                    }

                    // Check the domain status if the URL still matches.
                    if (urlMatches) {
                        // Set the matched URL type.
                        requestDataClass.matchedUrlType = matchedUrlType

                        // Check the domain status.
                        return checkDomain(requestDataClass, filterListEntryDataClass)
                    }
                }
            } else if (filterListEntryDataClass.finalMatch) {  // This is a final match.
                // Get the entry locations.
                val penultimateEntryNumber= (filterListEntryDataClass.sizeOfAppliedEntryList - 1)
                val ultimateEntryIndex: Int = penultimateEntryNumber

                // Check all the entries except the last one.
                for (i in 0 until penultimateEntryNumber - 1) {
                    // Get the applied entry index, which will be `-1` if it doesn't exist.
                    val appliedEntryIndex =urlString.indexOf(filterListEntryDataClass.appliedEntryList[i])

                    // Check if the entry was found.
                    if (appliedEntryIndex >= 0) {  // The entry is contained in the URL string.
                        // Get the number of characters to remove from teh front of the URL string.
                        val charactersToRemove = (appliedEntryIndex + filterListEntryDataClass.appliedEntryList[i].length)

                        // Remove the entry from the front of the modified URL string.
                        urlString = urlString.substring(charactersToRemove)
                    } else {  // The entry is not contained in the URL string.
                        // Mark the URL matches flag as false.
                        urlMatches = false
                    }
                }

                // Check the final entry if the URL still matches.
                if (urlMatches) {
                    // Check if the URL string ends with the last applied entry.
                    if (urlString.endsWith(filterListEntryDataClass.appliedEntryList[ultimateEntryIndex])) {
                        // Set the matched URL type.
                        requestDataClass.matchedUrlType = matchedUrlType

                        // Check the domain status.
                        return checkDomain(requestDataClass, filterListEntryDataClass)
                    }
                }
            } else {  // There is no initial or final matching.
                // Check the URL.
                for (i in 0 until filterListEntryDataClass.sizeOfAppliedEntryList) {
                    // Get the applied entry index, which will be `-1` if it doesn't exist.
                    val appliedEntryIndex = urlString.indexOf(filterListEntryDataClass.appliedEntryList[i])

                    // Check if the entry was found.
                    if (appliedEntryIndex >= 0) {  // The entry is contained in the URL string.
                        // Get the number of characters to remove from the front of the URL string.
                        val charactersToRemove = (appliedEntryIndex + filterListEntryDataClass.appliedEntryList[i].length)

                        // Remove the entry from the front of the modified URL string.
                        urlString = urlString.substring(charactersToRemove)
                    } else {  // The entry is not contained in the URL string.
                        // Mark the URL matches flag as false.
                        urlMatches = false
                    }
                }

                // Check the domain status if the URL still matches.
                if (urlMatches) {
                    // Set the matched URL type.
                    requestDataClass.matchedUrlType = matchedUrlType

                    // Check the domain status.
                    return checkDomain(requestDataClass, filterListEntryDataClass)
                }
            }
        }

        // The applied entry doesn't match.  Return `true` to continue processing the URL request.
        return true
    }

    private fun checkRegularExpression(requestDataClass : RequestDataClass, filterListEntryDataClass : FilterListEntryDataClass) : Boolean {
        // Create an applied entry regular expression pattern.
        val appliedEntryRegularExpressionPattern = Pattern.compile(filterListEntryDataClass.appliedEntryList[0])

        // Check if the regular expression matches the URL string.
        val regularExpressionMatcher = appliedEntryRegularExpressionPattern.matcher(requestDataClass.requestUrlString)

        // Check the domain status if the regular expression matches.
        return if (regularExpressionMatcher.matches())
            checkDomain(requestDataClass, filterListEntryDataClass)
        else // If the regular expression doesn't match, return `true` to continue processing the URL request.
            true
    }

    private fun checkDomain(requestDataClass : RequestDataClass, filterListEntryDataClass : FilterListEntryDataClass) : Boolean {
        // Check the domain status.
        if (filterListEntryDataClass.domain == FilterOptionDisposition.Null) {  // Ignore the domain status.
            // Check the third party status.
            return checkThirdParty(requestDataClass, filterListEntryDataClass)
        } else if (filterListEntryDataClass.domain == FilterOptionDisposition.Apply) {  // Block the enumerated domains.
            // Check each domain.
            for (blockedDomain in filterListEntryDataClass.domainList) {
                // Check if the request came from a blocked domain.
                if (requestDataClass.firstPartyHostString.endsWith(blockedDomain)) {
                    // Check the third-party status.
                    return checkThirdParty(requestDataClass, filterListEntryDataClass)
                }
            }
        } else if (filterListEntryDataClass.domain == FilterOptionDisposition.Override) {  // Block domains that are not overridden.
            // Create a block domain flag.
            var blockDomain = true

            // Check each overridden domain.
            for (overriddenDomain in filterListEntryDataClass.domainList) {
                // Check if the request came from an overridden domain.
                if (requestDataClass.firstPartyHostString.endsWith(overriddenDomain)) {
                    // Don't block the domain.
                    blockDomain = false
                }
            }

            // Continue checking if the domain is blocked.
            if (blockDomain) {
                // Check the third-party status.
                return checkThirdParty(requestDataClass, filterListEntryDataClass)
            }
        }

        // There is a domain specified that doesn't match this request.  Return `true` to continue processing the URL request.
        return true
    }

    private fun checkThirdParty(requestDataClass : RequestDataClass, filterListEntryDataClass : FilterListEntryDataClass) : Boolean {
        // Check the third-party status.
        return if ((filterListEntryDataClass.thirdParty == FilterOptionDisposition.Null) ||  // Ignore the third-party status.
            ((filterListEntryDataClass.thirdParty == FilterOptionDisposition.Apply) && requestDataClass.isThirdPartyRequest) ||  // Block third-party requests.
            ((filterListEntryDataClass.thirdParty == FilterOptionDisposition.Override) && !requestDataClass.isThirdPartyRequest)) // Block first-party requests.
        {
            // Process the request.
            processRequest(requestDataClass, filterListEntryDataClass)
        } else { // The third-party option specified doesn't match this request.  Return `true` to continue processing the URL request.
            true
        }
    }

    private fun processRequest(requestDataClass : RequestDataClass, filterListEntryDataClass : FilterListEntryDataClass) : Boolean {
        // Populate the filter list and sublist.
        requestDataClass.filterList = filterListEntryDataClass.filterList
        requestDataClass.sublist = filterListEntryDataClass.sublist

        Log.i("FilterList", "Filter List:  ${filterListEntryDataClass.filterList}")

        // Set the request disposition.
        when (requestDataClass.sublist) {
            Sublist.MainAllowList -> requestDataClass.disposition = RequestDisposition.Allowed
            Sublist.InitialDomainAllowList -> requestDataClass.disposition = RequestDisposition.Allowed
            Sublist.RegularExpressionAllowList -> requestDataClass.disposition = RequestDisposition.Allowed
            Sublist.MainBlockList -> requestDataClass.disposition = RequestDisposition.Blocked
            Sublist.InitialDomainBlockList -> requestDataClass.disposition = RequestDisposition.Blocked
            Sublist.RegularExpressionBlockList -> requestDataClass.disposition = RequestDisposition.Blocked
        }

        // Populate the filter list entry data class items.
        requestDataClass.filterListOriginalEntryString = filterListEntryDataClass.originalEntryString
        requestDataClass.filterListOriginalFilterOptionsString = filterListEntryDataClass.originalFilterOptionsString
        requestDataClass.filterListAppliedEntryList = filterListEntryDataClass.appliedEntryList
        requestDataClass.filterListAppliedFilterOptionsList = filterListEntryDataClass.appliedFilterOptionsList
        requestDataClass.filterListDomainList = filterListEntryDataClass.domainList
        requestDataClass.filterListFinalMatch = filterListEntryDataClass.finalMatch
        requestDataClass.filterListInitialMatch = filterListEntryDataClass.initialMatch
        requestDataClass.filterListSingleAppliedEntry = filterListEntryDataClass.singleAppliedEntry
        requestDataClass.filterListSizeOfAppliedEntryList = filterListEntryDataClass.sizeOfAppliedEntryList
        requestDataClass.filterListDomain = filterListEntryDataClass.domain
        requestDataClass.filterListThirdParty = filterListEntryDataClass.thirdParty

        // Log the disposition.
        //Log.i("Request processed", "${requestDataClass.requestUrlString} - Filter list entry:  ${requestDataClass.filterListAppliedEntryList}")

        // Returning `false` stops all processing of the request.
        return false
    }
}
