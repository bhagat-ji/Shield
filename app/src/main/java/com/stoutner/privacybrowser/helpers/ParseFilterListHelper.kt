/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2018-2019, 2021-2026 Soren Stoutner <soren@stoutner.com>
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

import android.content.res.AssetManager

import com.stoutner.privacybrowser.dataclasses.FilterList
import com.stoutner.privacybrowser.dataclasses.FilterListDataClass
import com.stoutner.privacybrowser.dataclasses.FilterListEntryDataClass
import com.stoutner.privacybrowser.dataclasses.FilterOptionDisposition
import com.stoutner.privacybrowser.dataclasses.Sublist

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class ParseFilterListHelper {
    fun parseFilterList(assetManager: AssetManager, filterListFilePath: String, filterList: FilterList): FilterListDataClass {
        // Create the filter list data class.
        val filterListDataClass = FilterListDataClass()

        // Parse the filter list.  The `try` is required by input stream reader.
        try {
            // Load the filter list into a buffered reader.
            val bufferedReader = BufferedReader(InputStreamReader(assetManager.open(filterListFilePath)))

            // Populate the filter list enum.
            filterListDataClass.filterList = filterList

            // Create the temporary filter list entry string.
            var filterListEntryString: String

            // Parse each line of the filter list.
            bufferedReader.forEachLine {
                // Get the filter list entry.
                filterListEntryString = it

                // Create a filter list entry data class.
                var filterListEntryDataClass = FilterListEntryDataClass()

                // Store the filter list name in the entry data class.
                filterListEntryDataClass.filterList = filterList

                // Store the original filter list entry.
                filterListEntryDataClass.originalEntryString = filterListEntryString

                // Parse the entry.
                if (filterListEntryString.isBlank()) {  // Ignore blank lines.
                    // Do nothing.

                    // Log the dropping of the line.
                    //Log.i("FilterLists", "$filterListEntryString  NOT added from  $filterListName  (empty line).")
                } else if (filterListEntryString.startsWith('[')) {  // The line starts with `[`, which is the file format.
                    // Do nothing.

                    // Log the dropping of the line.
                    //Log.i("FilterLists", "$filterListEntryString  NOT added from  $filterListName  (file format).");
                } else if (filterListEntryString.contains("##") ||
                    filterListEntryString.contains("#?#") ||
                    filterListEntryString.contains("#@#") ||
                    filterListEntryString.contains("#$#")
                ) {  // The line contains unimplemented content filtering.
                    // Do nothing.

                    // Log the dropping of the line.
                    //Log.i("FilterLists", "$filterListEntryString  NOT added from  $filterListName  (content filtering).");
                } else if (filterListEntryString.startsWith('!')) {  // The line starts with `!`, which are comments.
                    if (filterListEntryString.startsWith("! Title:")) {  // The line contains the title.
                        // Add the title to the filter list data class.
                        filterListDataClass.titleString = filterListEntryString.substring(9)

                        // Log the addition of the filter list title.
                        //Log.i("FilterLists", "Filter list title:  ${filterListDataClass.titleString}  ADDED from  $filterListName.")
                    } else if (filterListEntryString.startsWith("! Version:")) {  // The line contains the version.
                        // Add the version to the filter list data class.
                        filterListDataClass.versionString = filterListEntryString.substring(11)

                        // Log the addition of the filter list title.
                        //Log.i("FilterLists", "Filter list version:  ${filterListDataClass.versionString}  ADDED from  $filterListName.")
                    } else {
                        // Else do nothing.

                        // Log the dropping of the line.
                        //Log.i("FilterLists", "$filterListEntryString  NOT added from  $filterListName  (comment).");
                    }
                } else {  // Process the entry.
                    // Get the index of the last dollar sign.
                    val indexOfLastDollarSign = filterListEntryString.lastIndexOf('$')

                    // Process the filter options if they exist.
                    if (indexOfLastDollarSign > -1) {
                        // Store the original filter options string.
                        filterListEntryDataClass.originalFilterOptionsString = filterListEntryString.substring(indexOfLastDollarSign + 1)

                        // Store the entry without the filter options as the filter list string.
                        filterListEntryString = filterListEntryString.take(indexOfLastDollarSign)

                        // Split the options list.
                        val originalFilterOptionsList = filterListEntryDataClass.originalFilterOptionsString.split(',')

                        // Populate the applied filter options list.
                        for (filterOptionString in originalFilterOptionsList) {
                            // Only add filter options that are handled by Privacy Browser Android.  <https://help.adblockplus.org/hc/en-us/articles/360062733293-How-to-write-filters>
                            // Currently these are only `domain` and `third-party`.
                            if (filterOptionString.contains("domain=") ||
                                filterOptionString.contains("third-party")
                            ) {
                                // Add the filter option to the applied filter options list.
                                filterListEntryDataClass.appliedFilterOptionsList.add(filterOptionString)
                            }
                        }

                        // Populate the filter option entries.
                        for (filterOptionString in filterListEntryDataClass.appliedFilterOptionsList) {
                            // Parse the filter options.
                            if (filterOptionString.startsWith("domain=")) {  // Domain.
                                // Remove `domain=` from the filter option.
                                val modifiedFilterOptionString = filterOptionString.substring(7)

                                // Set the disposition according to the domain type.
                                if (modifiedFilterOptionString.startsWith('~')) { // Override domains.
                                    // Remove the `~` from each domain.
                                    modifiedFilterOptionString.replace("~", "")

                                    // Populate the domain filter disposition.
                                    filterListEntryDataClass.domain = FilterOptionDisposition.Override
                                } else {  // Standard domains.
                                    // Populate the domain filter disposition.
                                    filterListEntryDataClass.domain = FilterOptionDisposition.Apply
                                }

                                // Store the domain list.
                                filterListEntryDataClass.domainList = modifiedFilterOptionString.split('|')
                            } else if (filterOptionString == "third-party") {  // Third-party.
                                // Populate the third-party filter disposition.
                                filterListEntryDataClass.thirdParty = FilterOptionDisposition.Apply
                            } else if (filterOptionString == "~third-party") {  // Third-party override.
                                // Populate the third-party filter disposition.
                                filterListEntryDataClass.thirdParty = FilterOptionDisposition.Override
                            }
                        }
                    }  // Finished processing the filter options.

                    // Process the base entry.
                    if (filterListEntryDataClass.originalFilterOptionsString.isNotEmpty() &&
                    (filterListEntryDataClass.domain == FilterOptionDisposition.Null) &&
                    (filterListEntryDataClass.thirdParty == FilterOptionDisposition.Null)) {  // There were filter options, but they have all been removed because they don't apply to Privacy Browser.
                        // Ignore these entries as they will have unintended consequences.

                        // Log the dropping of the entry.
                        //Log.i("FilterLists", "Unsupported filter options:  ${filterListEntryDataClass.originalEntryString}  NOT added from  $filterListName.")
                    } else if (filterListEntryString.isEmpty()) {  // There are no applied entries.  This should check for the presence of request options in the future when they are supported in Privacy Browser Android.
                        // Ignore these entries as they will block all requests generally or for a specified domain.  Typically these are left over after removing `csp=` filter options.

                        // Log the dropping of the entry.
                        //Log.i("FilterLists", "Dropped because nothing left is applied:  ${filterListEntryDataClass.originalEntryString}  NOT added from  $filterListName.")
                    } else if (filterListEntryString.startsWith("@@")) {  // Process an allow list entry.
                        // Remove the initial `@@`.
                        filterListEntryString = filterListEntryString.substring(2)

                        if (filterListEntryString.startsWith("||")) {  // Process an initial domain allow list entry.
                            // Remove the initial `||`.
                            filterListEntryString = filterListEntryString.substring(2)

                            // Set the initial flag match.
                            filterListEntryDataClass.initialMatch = true

                            // Prepare the filter list string.
                            filterListEntryDataClass = prepareFilterListString(filterListEntryString, filterListEntryDataClass)

                            // Store the sublist.
                            filterListEntryDataClass.sublist = Sublist.InitialDomainAllowList

                            // Add the entry data class to the initial domain allow list.
                            filterListDataClass.initialDomainAllowList.add(filterListEntryDataClass)

                            // Log the addition to the filter list.
                            //Log.i("FilterLists", "${filterListEntryDataClass.originalEntryString} added to Initial Domain Allow List from $filterListName.")
                        } else if (filterListEntryString.contains("\\")) {  // Process a regular expression allow list entry.
                            // Set the regular expression as the applied entry list.
                            filterListEntryDataClass.appliedEntryList = listOf(filterListEntryString)

                            // Store the sublist.
                            filterListEntryDataClass.sublist = Sublist.RegularExpressionBlockList

                            // Add the entry data class to the regular expression allow list.
                            filterListDataClass.regularExpressionAllowList.add(filterListEntryDataClass)

                            // Log the addition to the filter list.
                            //Log.i("FilterLists", "${filterListEntryDataClass.originalEntryString} added to Regular Expression Allow List from $filterListName.")
                        } else {  // Process a main allow list entry.
                            // Prepare the filter list string.
                            filterListEntryDataClass = prepareFilterListString(filterListEntryString, filterListEntryDataClass)

                            // Store the sublist.
                            filterListEntryDataClass.sublist = Sublist.MainAllowList

                            // Add the entry data class to the main allow list.
                            filterListDataClass.mainAllowList.add(filterListEntryDataClass)

                            // Log the addition to the filter list.
                            //Log.i("FilterLists", "${filterListEntryDataClass.originalEntryString} added to Main Allow List from $filterListName.")
                        }
                    } else if (filterListEntryString.startsWith("||")) {  // Process an initial domain block list entry.
                        // Remove the initial `||`.
                        filterListEntryString = filterListEntryString.substring(2)

                        // Set the initial match flag.
                        filterListEntryDataClass.initialMatch = true

                        // Prepare the filter list string.
                        filterListEntryDataClass = prepareFilterListString(filterListEntryString, filterListEntryDataClass)

                        // Store the sublist.
                        filterListEntryDataClass.sublist = Sublist.InitialDomainBlockList

                        // Add the entry data class to the initial domain block list.
                        filterListDataClass.initialDomainBlockList.add(filterListEntryDataClass)

                        // Log the addition to the filter list.
                        //Log.i("FilterLists", "${filterListEntryDataClass.originalEntryString} added to Initial Domain Block List from $filterListName.")
                    } else if (filterListEntryString.contains("\\")) {  // Process a regular expression block list entry.
                        // Set the regular expression as the applied entry list.
                        filterListEntryDataClass.appliedEntryList = listOf(filterListEntryString)

                        // Store the sublist.
                        filterListEntryDataClass.sublist = Sublist.RegularExpressionBlockList

                        // Add the entry data clas to the regular expression allow list.
                        filterListDataClass.regularExpressionBlockList.add(filterListEntryDataClass)

                        // Log the addition ot the filter list.
                        //Log.i("FilterLists", "${filterListEntryDataClass.originalEntryString} added to Regular Expression Block List from $filterListName.")
                    } else {  // Process a main block list entry.
                        // Prepare the filter list string.
                        filterListEntryDataClass = prepareFilterListString(filterListEntryString, filterListEntryDataClass)

                        // Store the sublist.
                        filterListEntryDataClass.sublist = Sublist.MainBlockList

                        // Add the entry to the main block list.
                        filterListDataClass.mainBlockList.add(filterListEntryDataClass)

                        // Log the addition to the filter list.
                        //Log.i("FilterLists", "${filterListEntryDataClass.originalEntryString} added to Main Block List from $filterListName.")
                    }
                }
            }  // Finished processing the filter list.

            // Close the buffered reader.
            bufferedReader.close()
        } catch (_: IOException) {
            // Do nothing if the filter list cannot be read.
        }

        // Return the filter list data class.
        return filterListDataClass
    }

    private fun prepareFilterListString(filterListEntryString: String, filterListEntryDataClass: FilterListEntryDataClass): FilterListEntryDataClass {
        // Create a modified filter list entry string, as the variable passed in cannot be edited.
        var modifiedFilterListEntryString = filterListEntryString

        // Check if this is an initial match.
        if (modifiedFilterListEntryString.startsWith('|')) {
            // Strip the initial `|`.
            modifiedFilterListEntryString = modifiedFilterListEntryString.substring(1)

            // Set the initial match flag.
            filterListEntryDataClass.initialMatch = true
        }

        // Check if this is a final match.
        if (modifiedFilterListEntryString.endsWith('|')) {
            // Strip the final `|`.
            modifiedFilterListEntryString = modifiedFilterListEntryString.dropLast(1)

            // Set the final match flag.
            filterListEntryDataClass.finalMatch = true
        }

        // Remove the initial asterisk if it exists.
        if (modifiedFilterListEntryString.startsWith('*'))
            modifiedFilterListEntryString = modifiedFilterListEntryString.substring(1)

        // Remove the final asterisk if it exists.
        if (modifiedFilterListEntryString.endsWith('*'))
            modifiedFilterListEntryString = modifiedFilterListEntryString.dropLast(1)

        // Split the filter list entry string and set it as the applied entry list.
        filterListEntryDataClass.appliedEntryList = modifiedFilterListEntryString.split('*')

        // Store the size of the applied entry list.
        filterListEntryDataClass.sizeOfAppliedEntryList = filterListEntryDataClass.appliedEntryList.size

        // Determine if this is a single applied entry (including an empty entry, which, amazingly, are calculated as having a size of 1).
        filterListEntryDataClass.singleAppliedEntry = (filterListEntryDataClass.sizeOfAppliedEntryList == 1)

        // Return the filter list entry data class.
        return filterListEntryDataClass
    }
}
