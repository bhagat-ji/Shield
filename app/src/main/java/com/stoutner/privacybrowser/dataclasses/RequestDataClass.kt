/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2025-2026 Soren Stoutner <soren@stoutner.com>
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

package com.stoutner.privacybrowser.dataclasses

import android.os.Parcelable

import kotlinx.parcelize.Parcelize

enum class MatchedUrlType(val int: Int) {
    Url (0),
    TruncatedUrl (1),
    UrlWithSeparators (2),
    TruncatedUrlWithSeparators (3)
}

enum class RequestDisposition(val int: Int) {
    Default (0),
    Allowed (1),
    Blocked (2),
    ThirdParty (3)
}

enum class Sublist(val int: Int) {
    MainAllowList (0),
    InitialDomainAllowList (1),
    RegularExpressionAllowList (2),
    MainBlockList (3),
    InitialDomainBlockList (4),
    RegularExpressionBlockList (5)
}

@Parcelize
class RequestDataClass (
    // The booleans.
    var isThirdPartyRequest: Boolean = false,

    // The strings.
    var firstPartyHostString: String = "",
    var webPageUrlString: String = "",
    var requestUrlString: String = "",
    var requestUrlWithSeparatorsString: String = "",
    var truncatedRequestUrlString: String = "",
    var truncatedRequestUrlWithSeparatorsString: String = "",

    // The enums.
    var disposition: RequestDisposition = RequestDisposition.Default,
    var filterList: FilterList = FilterList.UltraPrivacy,
    var matchedUrlType: MatchedUrlType = MatchedUrlType.Url,
    var sublist: Sublist = Sublist.MainAllowList,

    // The filter list entry data class strings.
    var filterListOriginalEntryString: String = "",
    var filterListOriginalFilterOptionsString: String = "",

    // The filter list entry data class string lists.
    var filterListAppliedEntryList: List<String> = listOf(),
    var filterListAppliedFilterOptionsList: List<String> = listOf(),
    var filterListDomainList: List<String> = listOf(),

    // The filter list entry data class booleans.
    var filterListFinalMatch: Boolean = false,
    var filterListInitialMatch: Boolean = false,
    var filterListSingleAppliedEntry: Boolean = false,

    // The filter list entry data class ints.
    var filterListSizeOfAppliedEntryList: Int = 0,

    // The filter list entry data class filter options.
    var filterListDomain: FilterOptionDisposition = FilterOptionDisposition.Null,
    var filterListThirdParty: FilterOptionDisposition = FilterOptionDisposition.Null
) : Parcelable
