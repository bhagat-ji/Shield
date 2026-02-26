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

enum class FilterList(val int: Int) {
    UltraPrivacy (0),
    UltraList (1),
    EasyPrivacy (2),
    EasyList (3),
    FanboysAnnoyanceList (4),
    ThirdPartyRequests (5)
}

data class FilterListDataClass (
    // The strings.
    var titleString: String = "",
    var versionString: String = "",

    // The enums.
    var filterList: FilterList = FilterList.UltraPrivacy,

    // The filter lists.
    val mainAllowList: MutableList<FilterListEntryDataClass> = mutableListOf(),
    val initialDomainAllowList: MutableList<FilterListEntryDataClass> = mutableListOf(),
    val regularExpressionAllowList: MutableList<FilterListEntryDataClass> = mutableListOf(),
    val mainBlockList: MutableList<FilterListEntryDataClass> = mutableListOf(),
    val initialDomainBlockList: MutableList<FilterListEntryDataClass> = mutableListOf(),
    val regularExpressionBlockList: MutableList<FilterListEntryDataClass> = mutableListOf()
)
