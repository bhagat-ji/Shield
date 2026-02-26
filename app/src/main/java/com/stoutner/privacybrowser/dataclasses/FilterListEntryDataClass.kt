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

enum class FilterOptionDisposition(val int: Int) {
    Null (0),
    Apply (1),
    Override (2)
}

data class FilterListEntryDataClass (
    // The strings.
    var originalEntryString: String = "",
    var originalFilterOptionsString: String = "",

    // The string lists.
    var appliedEntryList: List<String> = listOf(),
    val appliedFilterOptionsList: MutableList<String> = mutableListOf(),
    var domainList: List<String> = listOf(),

    // The booleans.
    var finalMatch: Boolean = false,
    var initialMatch: Boolean = false,
    var singleAppliedEntry: Boolean = false,

    // The ints.
    var sizeOfAppliedEntryList: Int = 0,

    // The enums.
    var domain: FilterOptionDisposition = FilterOptionDisposition.Null,
    var filterList: FilterList = FilterList.UltraPrivacy,
    var sublist: Sublist = Sublist.MainAllowList,
    var thirdParty: FilterOptionDisposition = FilterOptionDisposition.Null
)
