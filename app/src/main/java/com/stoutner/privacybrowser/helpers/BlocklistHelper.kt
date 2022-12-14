/*
 * Copyright 2018-2019,2021-2022 Soren Stoutner <soren@stoutner.com>.
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

package com.stoutner.privacybrowser.helpers

import android.content.res.AssetManager

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.ArrayList
import java.util.regex.Pattern

class BlocklistHelper {
    companion object {
        // Describe the schema of the string array in each entry of the resource requests array list.
        const val REQUEST_DISPOSITION = 0
        const val REQUEST_URL = 1
        const val REQUEST_BLOCKLIST = 2
        const val REQUEST_SUBLIST = 3
        const val REQUEST_BLOCKLIST_ENTRIES = 4
        const val REQUEST_BLOCKLIST_ORIGINAL_ENTRY = 5

        // The request disposition options.
        const val REQUEST_DEFAULT = "0"
        const val REQUEST_ALLOWED = "1"
        const val REQUEST_THIRD_PARTY = "2"
        const val REQUEST_BLOCKED = "3"

        // The whitelists.
        const val MAIN_WHITELIST = "1"
        const val FINAL_WHITELIST = "2"
        const val DOMAIN_WHITELIST = "3"
        const val DOMAIN_INITIAL_WHITELIST = "4"
        const val DOMAIN_FINAL_WHITELIST = "5"
        const val THIRD_PARTY_WHITELIST = "6"
        const val THIRD_PARTY_DOMAIN_WHITELIST = "7"
        const val THIRD_PARTY_DOMAIN_INITIAL_WHITELIST = "8"

        // The blacklists.
        const val MAIN_BLACKLIST = "9"
        const val INITIAL_BLACKLIST = "10"
        const val FINAL_BLACKLIST = "11"
        const val DOMAIN_BLACKLIST = "12"
        const val DOMAIN_INITIAL_BLACKLIST = "13"
        const val DOMAIN_FINAL_BLACKLIST = "14"
        const val DOMAIN_REGULAR_EXPRESSION_BLACKLIST = "15"
        const val THIRD_PARTY_BLACKLIST = "16"
        const val THIRD_PARTY_INITIAL_BLACKLIST = "17"
        const val THIRD_PARTY_DOMAIN_BLACKLIST = "18"
        const val THIRD_PARTY_DOMAIN_INITIAL_BLACKLIST = "19"
        const val THIRD_PARTY_REGULAR_EXPRESSION_BLACKLIST = "20"
        const val THIRD_PARTY_DOMAIN_REGULAR_EXPRESSION_BLACKLIST = "21"
        const val REGULAR_EXPRESSION_BLACKLIST = "22"
    }

    fun parseBlocklist(assetManager: AssetManager, blocklistName: String): ArrayList<List<Array<String>>> {
        // Initialize the header list.
        val headers: MutableList<Array<String>> = ArrayList()  // 0.

        // Initialize the whitelists.
        val mainWhitelist: MutableList<Array<String>> = ArrayList()  // 1.
        val finalWhitelist: MutableList<Array<String>> = ArrayList()  // 2.
        val domainWhitelist: MutableList<Array<String>> = ArrayList()  // 3.
        val domainInitialWhitelist: MutableList<Array<String>> = ArrayList()  // 4.
        val domainFinalWhitelist: MutableList<Array<String>> = ArrayList()  // 5.
        val thirdPartyWhitelist: MutableList<Array<String>> = ArrayList()  // 6.
        val thirdPartyDomainWhitelist: MutableList<Array<String>> = ArrayList()  // 7.
        val thirdPartyDomainInitialWhitelist: MutableList<Array<String>> = ArrayList()  // 8.

        // Initialize the blacklists.
        val mainBlacklist: MutableList<Array<String>> = ArrayList()  // 9.
        val initialBlacklist: MutableList<Array<String>> = ArrayList()  // 10.
        val finalBlacklist: MutableList<Array<String>> = ArrayList()  // 11.
        val domainBlacklist: MutableList<Array<String>> = ArrayList()  // 12.
        val domainInitialBlacklist: MutableList<Array<String>> = ArrayList()  // 13.
        val domainFinalBlacklist: MutableList<Array<String>> = ArrayList()  // 14.
        val domainRegularExpressionBlacklist: MutableList<Array<String>> = ArrayList()  // 15.
        val thirdPartyBlacklist: MutableList<Array<String>> = ArrayList()  // 16.
        val thirdPartyInitialBlacklist: MutableList<Array<String>> = ArrayList()  // 17.
        val thirdPartyDomainBlacklist: MutableList<Array<String>> = ArrayList()  // 18.
        val thirdPartyDomainInitialBlacklist: MutableList<Array<String>> = ArrayList()  // 19.
        val regularExpressionBlacklist: MutableList<Array<String>> = ArrayList()  // 20.
        val thirdPartyRegularExpressionBlacklist: MutableList<Array<String>> = ArrayList()  // 21.
        val thirdPartyDomainRegularExpressionBlacklist: MutableList<Array<String>> = ArrayList()  // 22.

        // Populate the blocklists.  The `try` is required by input stream reader.
        try {
            // Load the blocklist into a buffered reader.
            val bufferedReader = BufferedReader(InputStreamReader(assetManager.open(blocklistName)))

            // Create strings for storing the block list entries.
            var blocklistEntry: String
            var originalBlocklistEntry: String

            // Parse the block list.
            bufferedReader.forEachLine {
                // Store the original block list entry.
                originalBlocklistEntry = it

                // Remove any `^` from the block list entry.  Privacy Browser does not process them in the interest of efficiency.
                blocklistEntry = it.replace("^", "")

                // Parse the entry.
                if (blocklistEntry.contains("##") || blocklistEntry.contains("#?#") || blocklistEntry.contains("#@#") || blocklistEntry.startsWith("[")) {
                    // Entries that contain `##`, `#?#`, and `#@#` are for hiding elements in the main page's HTML.  Entries that start with `[` describe the AdBlock compatibility level.
                    // Do nothing.  Privacy Browser does not currently use these entries.

                    //Log.i("Blocklists", "Not added: " + blocklistEntry);
                } else if (blocklistEntry.contains("\$csp=script-src")) {  // Ignore entries that contain `$csp=script-src`.
                    // Do nothing.  It is uncertain what this directive is even supposed to mean, and it is blocking entire websites like androidcentral.com.  https://redmine.stoutner.com/issues/306.

                    //Log.i("Blocklists", headers.get(1)[0] + " not added: " + originalBlocklistEntry);
                } else if (blocklistEntry.contains("\$websocket") || blocklistEntry.contains("\$third-party,websocket") || blocklistEntry.contains("\$script,websocket")) {
                    // Ignore entries with `websocket`.
                    // Do nothing.  Privacy Browser does not differentiate between websocket requests and other requests and these entries cause a lot of false positives.

                    //Log.i("Blocklists", headers.get(1)[0] + " not added: " + originalBlocklistEntry);
                } else if (blocklistEntry.startsWith("!")) {  //  Comment entries.
                    if (blocklistEntry.startsWith("! Version:")) {
                        // Get the list version number.
                        val listVersion = arrayOf(blocklistEntry.substring(11))

                        // Store the list version in the headers list.
                        headers.add(listVersion)
                    }

                    if (blocklistEntry.startsWith("! Title:")) {
                        // Get the list title.
                        val listTitle = arrayOf(blocklistEntry.substring(9))

                        // Store the list title in the headers list.
                        headers.add(listTitle)
                    }

                    //Log.i("Blocklists", "Not added: " + blocklistEntry);
                } else if (blocklistEntry.startsWith("@@")) {  // Entries that begin with `@@` are whitelists.
                    // Remove the `@@`
                    blocklistEntry = blocklistEntry.substring(2)

                    // Strip out any initial `||`.  Privacy Browser doesn't differentiate items that only match against the end of the domain name.
                    if (blocklistEntry.startsWith("||")) {
                        blocklistEntry = blocklistEntry.substring(2)
                    }

                    if (blocklistEntry.contains("$")) {  // Filter entries.
                        if (blocklistEntry.contains("~third-party")) {  // Ignore entries that contain `~third-party`.
                            // Do nothing.

                            //Log.i("Blocklists", headers.get(1)[0] + " not added: " + originalBlocklistEntry);
                        } else if (blocklistEntry.contains("third-party")) {  // Third-party whitelist entries.
                            if (blocklistEntry.contains("domain=")) {  // Third-party domain whitelist entries.
                                // Parse the entry.
                                var entry = blocklistEntry.substring(0, blocklistEntry.indexOf("$"))
                                val filters = blocklistEntry.substring(blocklistEntry.indexOf("$") + 1)
                                var domains = filters.substring(filters.indexOf("domain=") + 7)

                                if (domains.contains("~")) {  // It is uncertain what a `~` domain means inside an `@@` entry.
                                    // Do Nothing

                                    //Log.i("Blocklists", headers.get(1)[0] + " not added: " + originalBlocklistEntry);
                                } else if (blocklistEntry.startsWith("|")) {  // Third-party domain initial whitelist entries.
                                    // Strip out the initial `|`.
                                    entry = entry.substring(1)

                                    if (entry == "http://" || entry == "https://") {  // Ignore generic entries.
                                        // Do nothing.  These entries are designed for filter options that Privacy Browser does not use.

                                        //Log.i("Blocklists", headers.get(1)[0] + " not added: " + originalBlocklistEntry);
                                    } else {  // Process third-party domain initial whitelist entries.
                                        // Process each domain.
                                        do {
                                            // Create a string to keep track of the current domain.
                                            var domain: String

                                            if (domains.contains("|")) {  // There is more than one domain in the list.
                                                // Get the first domain from the list.
                                                domain = domains.substring(0, domains.indexOf("|"))

                                                // Remove the first domain from the list.
                                                domains = domains.substring(domains.indexOf("|") + 1)
                                            } else {  // There is only one domain in the list.
                                                domain = domains
                                            }

                                            if (entry.contains("*")) {  // Process a third-party domain initial whitelist double entry.
                                                // Get the index of the wildcard.
                                                val wildcardIndex = entry.indexOf("*")

                                                // Split the entry into components.
                                                val firstEntry = entry.substring(0, wildcardIndex)
                                                val secondEntry = entry.substring(wildcardIndex + 1)

                                                // Create an entry string array.
                                                val domainDoubleEntry = arrayOf(domain, firstEntry, secondEntry, originalBlocklistEntry)

                                                // Add the entry to the whitelist.
                                                thirdPartyDomainInitialWhitelist.add(domainDoubleEntry)

                                                //Log.i("Blocklists", headers.get(1)[0] + " third-party domain initial whitelist added: " + domain + " , " + firstEntry + " , " + secondEntry +
                                                //        "  -  " + originalBlocklistEntry);
                                            } else {  // Process a third-party domain initial whitelist single entry.
                                                // Create a domain entry string array.
                                                val domainEntry = arrayOf(domain, entry, originalBlocklistEntry)

                                                // Add the entry to the third party domain initial whitelist.
                                                thirdPartyDomainInitialWhitelist.add(domainEntry)

                                                //Log.i("Blocklists", headers.get(1)[0] + " third-party domain initial whitelist added: " + domain + " , " + entry + "  -  " + originalBlocklistEntry);
                                            }
                                        } while (domains.contains("|"))
                                    }
                                } else {  // Third-party domain entries.
                                    // Process each domain.
                                    do {
                                        // Create a string to keep track of the current domain.
                                        var domain: String

                                        if (domains.contains("|")) {  // three is more than one domain in the list.
                                            // Get the first domain from the list.
                                            domain = domains.substring(0, domains.indexOf("|"))

                                            // Remove the first domain from the list.
                                            domains = domains.substring(domains.indexOf("|") + 1)
                                        } else {  // There is only one domain in the list.
                                            domain = domains
                                        }

                                        // Remove any trailing `*` from the entry.
                                        if (entry.endsWith("*")) {
                                            entry = entry.substring(0, entry.length - 1)
                                        }

                                        if (entry.contains("*")) {  // Process a third-party domain double entry.
                                            // Get the index of the wildcard.
                                            val wildcardIndex = entry.indexOf("*")

                                            // Split the entry into components.
                                            val firstEntry = entry.substring(0, wildcardIndex)
                                            val secondEntry = entry.substring(wildcardIndex + 1)

                                            // Create an entry string array.
                                            val domainDoubleEntry = arrayOf(domain, firstEntry, secondEntry, originalBlocklistEntry)

                                            // Add the entry to the whitelist.
                                            thirdPartyDomainWhitelist.add(domainDoubleEntry)

                                            //Log.i("Blocklists", headers.get(1)[0] + " third-party domain whitelist added: " + domain + " , " + firstEntry + " , " + secondEntry + "  -  " +
                                            //        originalBlocklistEntry);
                                        } else {  // Process a third-party domain single entry.
                                            // Create an entry string array.
                                            val domainEntry = arrayOf(domain, entry, originalBlocklistEntry)

                                            // Add the entry to the whitelist.
                                            thirdPartyDomainWhitelist.add(domainEntry)

                                            //Log.i("Blocklists", headers.get(1)[0] + " third-party domain whitelist added: " + domain + " , " + entry + "  -  " + originalBlocklistEntry);
                                        }
                                    } while (domains.contains("|"))
                                }
                            } else {  // Process third-party whitelist entries.
                                // Parse the entry
                                val entry = blocklistEntry.substring(0, blocklistEntry.indexOf("$"))

                                if (entry.contains("*")) {  // There are two or more entries.
                                    // Get the index of the wildcard.
                                    val wildcardIndex = entry.indexOf("*")

                                    // Split the entry into components.
                                    val firstEntry = entry.substring(0, wildcardIndex)
                                    val secondEntry = entry.substring(wildcardIndex + 1)

                                    if (secondEntry.contains("*")) {  // There are three or more entries.
                                        // Get the index of the wildcard.
                                        val secondWildcardIndex = secondEntry.indexOf("*")

                                        // Split the entry into components.
                                        val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                        val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                        if (thirdEntry.contains("*")) {  // There are four or more entries.
                                            // Get the index of the wildcard.
                                            val thirdWildcardIndex = thirdEntry.indexOf("*")

                                            // Split the entry into components.
                                            val realThirdEntry = thirdEntry.substring(0, thirdWildcardIndex)
                                            val fourthEntry = thirdEntry.substring(thirdWildcardIndex + 1)

                                            if (fourthEntry.contains("*")) {  // Process a third-party whitelist quintuple entry.
                                                // Get the index of the wildcard.
                                                val fourthWildcardIndex = fourthEntry.indexOf("*")

                                                // Split the entry into components.
                                                val realFourthEntry = fourthEntry.substring(0, fourthWildcardIndex)
                                                val fifthEntry = fourthEntry.substring(fourthWildcardIndex + 1)

                                                // Create an entry string array.
                                                val quintupleEntry = arrayOf(firstEntry, realSecondEntry, realThirdEntry, realFourthEntry, fifthEntry, originalBlocklistEntry)

                                                // Add the entry to the whitelist.
                                                thirdPartyWhitelist.add(quintupleEntry)

                                                //Log.i("Blocklists", headers.get(1)[0] + " third-party whitelist added: " + firstEntry + " , " + realSecondEntry + " , " + realThirdEntry + " , " +
                                                //        realFourthEntry + " , " + fifthEntry + "  -  " + originalBlocklistEntry);
                                            } else {  // Process a third-party whitelist quadruple entry.
                                                // Create an entry string array.
                                                val quadrupleEntry = arrayOf(firstEntry, realSecondEntry, realThirdEntry, fourthEntry, originalBlocklistEntry)

                                                // Add the entry to the whitelist.
                                                thirdPartyWhitelist.add(quadrupleEntry)

                                                //Log.i("Blocklists", headers.get(1)[0] + " third-party whitelist added: " + firstEntry + " , " + realSecondEntry + " , " + realThirdEntry + " , " +
                                                //        fourthEntry + "  -  " + originalBlocklistEntry);
                                            }
                                        } else {  // Process a third-party whitelist triple entry.
                                            // Create an entry string array.
                                            val tripleEntry = arrayOf(firstEntry, realSecondEntry, thirdEntry, originalBlocklistEntry)

                                            // Add the entry to the whitelist.
                                            thirdPartyWhitelist.add(tripleEntry)

                                            //Log.i("Blocklists", headers.get(1)[0] + " third-party whitelist added: " + firstEntry + " , " + realSecondEntry + " , " + thirdEntry + "  -  " +
                                            //        originalBlocklistEntry);
                                        }
                                    } else {  // Process a third-party whitelist double entry.
                                        // Create an entry string array.
                                        val doubleEntry = arrayOf(firstEntry, secondEntry, originalBlocklistEntry)

                                        // Add the entry to the whitelist.
                                        thirdPartyWhitelist.add(doubleEntry)

                                        //Log.i("Blocklists", headers.get(1)[0] + " third-party whitelist added: " + firstEntry + " , " + secondEntry + "  -  " + originalBlocklistEntry);
                                    }
                                } else {  // Process a third-party whitelist single entry.
                                    // Create an entry string array.
                                    val singleEntry = arrayOf(entry, originalBlocklistEntry)

                                    // Add the entry to the whitelist.
                                    thirdPartyWhitelist.add(singleEntry)

                                    //Log.i("Blocklists", headers.get(1)[0] + " third-party domain whitelist added: " + entry + "  -  " + originalBlocklistEntry);
                                }
                            }
                        } else if (blocklistEntry.contains("domain=")) {  // Process domain whitelist entries.
                            // Parse the entry
                            var entry = blocklistEntry.substring(0, blocklistEntry.indexOf("$"))
                            val filters = blocklistEntry.substring(blocklistEntry.indexOf("$") + 1)
                            var domains = filters.substring(filters.indexOf("domain=") + 7)

                            if (entry.startsWith("|")) {  // Initial domain whitelist entries.
                                // Strip the initial `|`.
                                entry = entry.substring(1)

                                if (entry == "http://" || entry == "https://") {  // Ignore generic entries.
                                    // Do nothing.  These entries are designed for filter options that Privacy Browser does not use.

                                    //Log.i("Blocklists", headers.get(1)[0] + " not added: " + originalBlocklistEntry);
                                } else {  // Initial domain whitelist entry.
                                    // Process each domain.
                                    do {
                                        // Create a string to keep track of the current domain.
                                        var domain: String

                                        if (domains.contains("|")) {  // There is more than one domain in the list.
                                            // Get the first domain from the list.
                                            domain = domains.substring(0, domains.indexOf("|"))

                                            // Remove the first domain from the list.
                                            domains = domains.substring(domains.indexOf("|") + 1)
                                        } else {  // There is only one domain in the list.
                                            domain = domains
                                        }

                                        if (entry.contains("*")) {  // There are two or more entries.
                                            // Get the index of the wildcard.
                                            val wildcardIndex = entry.indexOf("*")

                                            // Split the entry into components.
                                            val firstEntry = entry.substring(0, wildcardIndex)
                                            val secondEntry = entry.substring(wildcardIndex + 1)

                                            if (secondEntry.contains("*")) {  // Process a domain initial triple entry.
                                                // Get the index of the wildcard.
                                                val secondWildcardIndex = secondEntry.indexOf("*")

                                                // Split the entry into components.
                                                val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                                val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                                // Create an entry string array.
                                                val domainTripleEntry = arrayOf(domain, firstEntry, realSecondEntry, thirdEntry, originalBlocklistEntry)

                                                // Add the entry to the whitelist.
                                                domainInitialWhitelist.add(domainTripleEntry)

                                                //Log.i("Blocklists", headers.get(1)[0] + " domain initial whitelist entry added: " + domain + " , " + firstEntry + " , " + realSecondEntry + " , " +
                                                //        thirdEntry + "  -  " + originalBlocklistEntry);
                                            } else {  // Process a domain initial double entry.
                                                // Create an entry string array.
                                                val domainDoubleEntry = arrayOf(domain, firstEntry, secondEntry, originalBlocklistEntry)

                                                // Add the entry to the whitelist.
                                                domainInitialWhitelist.add(domainDoubleEntry)

                                                //Log.i("Blocklists", headers.get(1)[0] + " domain initial whitelist entry added: " + domain + " , " + firstEntry + " , " + secondEntry + "  -  " +
                                                //        originalBlocklistEntry);
                                            }
                                        } else {  // Process a domain initial single entry.
                                            // Create an entry string array.
                                            val domainEntry = arrayOf(domain, entry, originalBlocklistEntry)

                                            // Add the entry to the whitelist.
                                            domainInitialWhitelist.add(domainEntry)

                                            //Log.i("Blocklists", headers.get(1)[0] + " domain initial whitelist entry added: " + domain + " , " + entry + "  -  " + originalBlocklistEntry);
                                        }
                                    } while (domains.contains("|"))
                                }
                            } else if (entry.endsWith("|")) {  // Final domain whitelist entries.
                                // Strip the `|` from the end of the entry.
                                entry = entry.substring(0, entry.length - 1)

                                // Process each domain.
                                do {
                                    // Create a string to keep track of the current domain.
                                    var domain: String

                                    if (domains.contains("|")) {  // There is more than one domain in the list.
                                        // Get the first domain from the list.
                                        domain = domains.substring(0, domains.indexOf("|"))

                                        // Remove the first domain from the list.
                                        domains = domains.substring(domains.indexOf("|") + 1)
                                    } else {  // There is only one domain in the list.
                                        domain = domains
                                    }

                                    if (entry.contains("*")) {  // Process a domain final whitelist double entry.
                                        // Get the index of the wildcard.
                                        val wildcardIndex = entry.indexOf("*")

                                        // Split the entry into components.
                                        val firstEntry = entry.substring(0, wildcardIndex)
                                        val secondEntry = entry.substring(wildcardIndex + 1)

                                        // Create an entry string array.
                                        val domainDoubleEntry = arrayOf(domain, firstEntry, secondEntry, originalBlocklistEntry)

                                        // Add the entry to the whitelist.
                                        domainFinalWhitelist.add(domainDoubleEntry)

                                        //Log.i("Blocklists", headers.get(1)[0] + " domain final whitelist added: " + domain + " , " + firstEntry + " , " + secondEntry + "  -  " +
                                        //        originalBlocklistEntry);
                                    } else {  // Process a domain final whitelist single entry.
                                        // create an entry string array.
                                        val domainEntry = arrayOf(domain, entry, originalBlocklistEntry)

                                        // Add the entry to the whitelist.
                                        domainFinalWhitelist.add(domainEntry)

                                        //Log.i("Blocklists", headers.get(1)[0] + " domain final whitelist added: " + domain + " , " + entry + "  -  " + originalBlocklistEntry);
                                    }
                                } while (domains.contains("|"))
                            } else {  // Standard domain whitelist entries with filters.
                                if (domains.contains("~")) {  // It is uncertain what a `~` domain means inside an `@@` entry.
                                    // Do Nothing

                                    //Log.i("Blocklists", headers.get(1)[0] + " not added: " + originalBlocklistEntry);
                                } else {
                                    // Process each domain.
                                    do {
                                        // Create a string to keep track of the current domain.
                                        var domain: String

                                        if (domains.contains("|")) {  // There is more than one domain in the list.
                                            // Get the first domain from the list.
                                            domain = domains.substring(0, domains.indexOf("|"))

                                            // Remove the first domain from the list.
                                            domains = domains.substring(domains.indexOf("|") + 1)
                                        } else {  // There is only one domain in the list.
                                            domain = domains
                                        }

                                        if (entry.contains("*")) {  // There are two or more entries.
                                            // Get the index of the wildcard.
                                            val wildcardIndex = entry.indexOf("*")

                                            // Split the entry into components.
                                            val firstEntry = entry.substring(0, wildcardIndex)
                                            val secondEntry = entry.substring(wildcardIndex + 1)

                                            if (secondEntry.contains("*")) {  // There are three or more entries.
                                                // Get the index of the wildcard.
                                                val secondWildcardIndex = secondEntry.indexOf("*")

                                                // Split the entry into components.
                                                val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                                val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                                if (thirdEntry.contains("*")) {  // Process a domain whitelist quadruple entry.
                                                    // Get the index of the wildcard.
                                                    val thirdWildcardIndex = thirdEntry.indexOf("*")

                                                    // Split the entry into components.
                                                    val realThirdEntry = thirdEntry.substring(0, thirdWildcardIndex)
                                                    val fourthEntry = thirdEntry.substring(thirdWildcardIndex + 1)

                                                    // Create an entry string array.
                                                    val domainQuadrupleEntry = arrayOf(domain, firstEntry, realSecondEntry, realThirdEntry, fourthEntry, originalBlocklistEntry)

                                                    // Add the entry to the whitelist.
                                                    domainWhitelist.add(domainQuadrupleEntry)

                                                    //Log.i("Blocklists", headers.get(1)[0] + " domain whitelist added : " + domain + " , " + firstEntry + " , " + realSecondEntry + " , " +
                                                    //        realThirdEntry + " , " + fourthEntry + "  -  " + originalBlocklistEntry);
                                                } else {  // Process a domain whitelist triple entry.
                                                    // Create an entry string array.
                                                    val domainTripleEntry = arrayOf(domain, firstEntry, realSecondEntry, thirdEntry, originalBlocklistEntry)

                                                    // Add the entry to the whitelist.
                                                    domainWhitelist.add(domainTripleEntry)

                                                    //Log.i("Blocklists", headers.get(1)[0] + " domain whitelist added : " + domain + " , " + firstEntry + " , " + realSecondEntry + " , " +
                                                    //        thirdEntry + "  -  " + originalBlocklistEntry);
                                                }
                                            } else {  // Process a domain whitelist double entry.
                                                // Create an entry string array.
                                                val domainDoubleEntry = arrayOf(domain, firstEntry, secondEntry, originalBlocklistEntry)

                                                // Add the entry to the whitelist.
                                                domainWhitelist.add(domainDoubleEntry)

                                                //Log.i("Blocklists", headers.get(1)[0] + " domain whitelist added : " + domain + " , " + firstEntry + " , " + secondEntry + "  -  " +
                                                //        originalBlocklistEntry);
                                            }
                                        } else {  // Process a domain whitelist single entry.
                                            // Create an entry string array.
                                            val domainEntry = arrayOf(domain, entry, originalBlocklistEntry)

                                            // Add the entry to the whitelist.
                                            domainWhitelist.add(domainEntry)

                                            //Log.i("Blocklists", headers.get(1)[0] + " domain whitelist added : " + domain + " , " + entry + "  -  " + originalBlocklistEntry);
                                        }
                                    } while (domains.contains("|"))
                                }
                            }
                        }  // Ignore all other filter entries.
                    } else if (blocklistEntry.endsWith("|")) {  // Final whitelist entries.
                        // Remove the final `|` from the entry.
                        val entry = blocklistEntry.substring(0, blocklistEntry.length - 1)

                        if (entry.contains("*")) {  // Process a final whitelist double entry
                            // Get the index of the wildcard.
                            val wildcardIndex = entry.indexOf("*")

                            // split the entry into components.
                            val firstEntry = entry.substring(0, wildcardIndex)
                            val secondEntry = entry.substring(wildcardIndex + 1)

                            // Create an entry string array.
                            val doubleEntry = arrayOf(firstEntry, secondEntry, originalBlocklistEntry)

                            // Add the entry to the whitelist.
                            finalWhitelist.add(doubleEntry)

                            //Log.i("Blocklists", headers.get(1)[0] + " final whitelist added: " + firstEntry + " , " + secondEntry + "  -  " + originalBlocklistEntry);
                        } else {  // Process a final whitelist single entry.
                            // Create an entry string array.
                            val singleEntry = arrayOf(entry, originalBlocklistEntry)

                            // Add the entry to the whitelist.
                            finalWhitelist.add(singleEntry)

                            //Log.i("Blocklists", headers.get(1)[0] + " final whitelist added: " + entry + "  -  " + originalBlocklistEntry);
                        }
                    } else {  // Main whitelist entries.
                        if (blocklistEntry.contains("*")) {  // There are two or more entries.
                            // Get the index of the wildcard.
                            val wildcardIndex = blocklistEntry.indexOf("*")

                            // Split the entry into components.
                            val firstEntry = blocklistEntry.substring(0, wildcardIndex)
                            val secondEntry = blocklistEntry.substring(wildcardIndex + 1)

                            if (secondEntry.contains("*")) {  // Process a main whitelist triple entry.
                                // Get the index of the wildcard.
                                val secondWildcardIndex = secondEntry.indexOf("*")

                                // Split the entry into components.
                                val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                // Create an entry string array.
                                val tripleEntry = arrayOf(firstEntry, realSecondEntry, thirdEntry, originalBlocklistEntry)

                                // Add the entry to the whitelist.
                                mainWhitelist.add(tripleEntry)

                                //Log.i("Blocklists", headers.get(1)[0] + " main whitelist added: " + firstEntry + " , " + realSecondEntry + " , " + thirdEntry + "  -  " + originalBlocklistEntry);
                            } else {  // Process a main whitelist double entry.
                                // Create an entry string array.
                                val doubleEntry = arrayOf(firstEntry, secondEntry, originalBlocklistEntry)

                                // Add the entry to the whitelist.
                                mainWhitelist.add(doubleEntry)

                                //Log.i("Blocklists", headers.get(1)[0] + " main whitelist added: " + firstEntry + " , " + secondEntry + "  -  " + originalBlocklistEntry);
                            }
                        } else {  // Process a main whitelist single entry.
                            // Create an entry string array.
                            val singleEntry = arrayOf(blocklistEntry, originalBlocklistEntry)

                            // Add the entry to the whitelist.
                            mainWhitelist.add(singleEntry)

                            //Log.i("Blocklists", headers.get(1)[0] + " main whitelist added: " + blocklistEntry + "  -  " + originalBlocklistEntry);
                        }
                    }
                } else if (blocklistEntry.endsWith("|")) {  // Final blacklist entries.
                    // Strip out the final "|"
                    var entry = blocklistEntry.substring(0, blocklistEntry.length - 1)

                    // Strip out any initial `||`.  They are redundant in this case because the blocklist entry is being matched against the end of the URL.
                    if (entry.startsWith("||")) {
                        entry = entry.substring(2)
                    }

                    if (entry.contains("*")) {  // Process a final blacklist double entry.
                        // Get the index of the wildcard.
                        val wildcardIndex = entry.indexOf("*")

                        // Split the entry into components.
                        val firstEntry = entry.substring(0, wildcardIndex)
                        val secondEntry = entry.substring(wildcardIndex + 1)

                        // Create an entry string array.
                        val doubleEntry = arrayOf(firstEntry, secondEntry, originalBlocklistEntry)

                        // Add the entry to the blacklist.
                        finalBlacklist.add(doubleEntry)

                        //Log.i("Blocklists", headers.get(1)[0] + " final blacklist added: " + firstEntry + " , " + secondEntry + "  -  " + originalBlocklistEntry);
                    } else {  // Process a final blacklist single entry.
                        // create an entry string array.
                        val singleEntry = arrayOf(entry, originalBlocklistEntry)

                        // Add the entry to the blacklist.
                        finalBlacklist.add(singleEntry)

                        //Log.i("Blocklists", headers.get(1)[0] + " final blacklist added: " + entry + "  -  " + originalBlocklistEntry);
                    }
                } else if (blocklistEntry.contains("$")) {  // Entries with filter options.
                    // Strip out any initial `||`.  These will be treated like any other entry.
                    if (blocklistEntry.startsWith("||")) {
                        blocklistEntry = blocklistEntry.substring(2)
                    }

                    if (blocklistEntry.contains("third-party")) {  // Third-party entries.
                        if (blocklistEntry.contains("~third-party")) {  // Third-party filter whitelist entries.
                            // Do not process these whitelist entries.  They are designed to combine with block filters that Privacy Browser doesn't use, like `subdocument` and `xmlhttprequest`.

                            //Log.i("Blocklists", headers.get(1)[0] + " not added: " + originalBlocklistEntry);
                        } else if (blocklistEntry.contains("domain=")) {  // Third-party domain entries.
                            if (blocklistEntry.startsWith("|")) {  // Third-party domain initial entries.
                                // Strip the initial `|`.
                                blocklistEntry = blocklistEntry.substring(1)

                                // Parse the entry
                                val entry = blocklistEntry.substring(0, blocklistEntry.indexOf("$"))
                                val filters = blocklistEntry.substring(blocklistEntry.indexOf("$") + 1)
                                var domains = filters.substring(filters.indexOf("domain=") + 7)

                                if (entry == "http:" || entry == "https:" || entry == "http://" || entry == "https://") {  // Ignore generic entries.
                                    // Do nothing.  These entries will almost entirely disable the website.
                                    // Often the original entry blocks filter options like `$script`, which Privacy Browser does not differentiate.

                                    //Log.i("Blocklists", headers.get(1)[0] + " not added: " + originalBlocklistEntry);
                                } else {  // Third-party domain initial entries.
                                    // Process each domain.
                                    do {
                                        // Create a string to keep track of the current domain.
                                        var domain: String

                                        if (domains.contains("|")) {  // There is more than one domain in the list.
                                            // Get the first domain from the list.
                                            domain = domains.substring(0, domains.indexOf("|"))

                                            // Remove the first domain from the list.
                                            domains = domains.substring(domains.indexOf("|") + 1)
                                        } else {  // There is only one domain in the list.
                                            domain = domains
                                        }

                                        if (entry.contains("*")) {  // Three are two or more entries.
                                            // Get the index of the wildcard.
                                            val wildcardIndex = entry.indexOf("*")

                                            // Split the entry into components.
                                            val firstEntry = entry.substring(0, wildcardIndex)
                                            val secondEntry = entry.substring(wildcardIndex + 1)

                                            if (secondEntry.contains("*")) {  // Process a third-party domain initial blacklist triple entry.
                                                // Get the index of the wildcard.
                                                val secondWildcardIndex = secondEntry.indexOf("*")

                                                // Split the entry into components.
                                                val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                                val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                                // Create an entry string array.
                                                val tripleDomainEntry = arrayOf(domain, firstEntry, realSecondEntry, thirdEntry, originalBlocklistEntry)

                                                // Add the entry to the blacklist.
                                                thirdPartyDomainInitialBlacklist.add(tripleDomainEntry)

                                                //Log.i("Blocklists", headers.get(1)[0] + " third-party domain initial blacklist added: " + domain + " , " + firstEntry + " , " + realSecondEntry +
                                                //        " , " + thirdEntry + "  -  " + originalBlocklistEntry);
                                            } else {  // Process a third-party domain initial blacklist double entry.
                                                // Create an entry string array.
                                                val doubleDomainEntry = arrayOf(domain, firstEntry, secondEntry, originalBlocklistEntry)

                                                // Add the entry to the blacklist.
                                                thirdPartyDomainInitialBlacklist.add(doubleDomainEntry)

                                                //Log.i("Blocklists", headers.get(1)[0] + " third-party domain initial blacklist added: " + domain + " , " + firstEntry + " , " + secondEntry +
                                                //        "  -  " + originalBlocklistEntry);
                                            }
                                        } else {  // Process a third-party domain initial blacklist single entry.
                                            // Create an entry string array.
                                            val singleEntry = arrayOf(domain, entry, originalBlocklistEntry)

                                            // Add the entry to the blacklist.
                                            thirdPartyDomainInitialBlacklist.add(singleEntry)

                                            //Log.i("Blocklists", headers.get(1)[0] + " third-party domain initial blacklist added: " + domain + " , " + entry + "  -  " + originalBlocklistEntry);
                                        }
                                    } while (domains.contains("|"))
                                }
                            } else if (blocklistEntry.contains("\\")) {  // Process a third-party domain blacklist regular expression.
                                // Parse the entry.  At least one regular expression in this entry contains `$`, so the parser uses `/$`.
                                val entry = blocklistEntry.substring(0, blocklistEntry.indexOf("/$") + 1)
                                val filters = blocklistEntry.substring(blocklistEntry.indexOf("/$") + 2)
                                var domains = filters.substring(filters.indexOf("domain=") + 7)

                                // Process each domain.
                                do {
                                    // Create a string to keep track of the current domain.
                                    var domain: String

                                    if (domains.contains("|")) {  // There is more than one domain in the list.
                                        // Get the first domain from the list.
                                        domain = domains.substring(0, domains.indexOf("|"))

                                        // Remove the first domain from the list.
                                        domains = domains.substring(domains.indexOf("|") + 1)
                                    } else {  // There is only one domain in the list.
                                        domain = domains
                                    }

                                    // Create an entry string array.
                                    val domainEntry = arrayOf(domain, entry, originalBlocklistEntry)

                                    // Add the entry to the blacklist.
                                    thirdPartyDomainRegularExpressionBlacklist.add(domainEntry)

                                    //Log.i("Blocklists", headers.get(1)[0] + " third-party domain regular expression blacklist added: " + domain + " , " + entry + "  -  " + originalBlocklistEntry);
                                } while (domains.contains("|"))
                            } else {  // Third-party domain entries.
                                // Parse the entry
                                var entry = blocklistEntry.substring(0, blocklistEntry.indexOf("$"))
                                val filters = blocklistEntry.substring(blocklistEntry.indexOf("$") + 1)
                                var domains = filters.substring(filters.indexOf("domain=") + 7)

                                // Strip any trailing "*" from the entry.
                                if (entry.endsWith("*")) {
                                    entry = entry.substring(0, entry.length - 1)
                                }

                                // Track if any third-party whitelist filters are applied.
                                var whitelistDomain = false

                                // Process each domain.
                                do {
                                    // Create a string to keep track of the current domain.
                                    var domain: String

                                    if (domains.contains("|")) {  // There is more than one domain in the list.
                                        // Get the first domain from the list.
                                        domain = domains.substring(0, domains.indexOf("|"))

                                        // Remove the first domain from the list.
                                        domains = domains.substring(domains.indexOf("|") + 1)
                                    } else {  // The is only one domain in the list.
                                        domain = domains
                                    }

                                    // Differentiate between blocklist domains and whitelist domains.
                                    if (domain.startsWith("~")) {  // Whitelist third-party domain entry.
                                        // Strip the initial `~`.
                                        domain = domain.substring(1)

                                        // Set the whitelist domain flag.
                                        whitelistDomain = true

                                        if (entry.contains("*")) {  // Process a third-party domain whitelist double entry.
                                            // Get the index of the wildcard.
                                            val wildcardIndex = entry.indexOf("*")

                                            // Split the entry into components.
                                            val firstEntry = entry.substring(0, wildcardIndex)
                                            val secondEntry = entry.substring(wildcardIndex + 1)

                                            // Create an entry string array.
                                            val domainDoubleEntry = arrayOf(domain, firstEntry, secondEntry, originalBlocklistEntry)

                                            // Add the entry to the whitelist.
                                            thirdPartyDomainWhitelist.add(domainDoubleEntry)

                                            //Log.i("Blocklists", headers.get(1)[0] + " third-party domain whitelist added: " + domain + " , " + firstEntry + " , " + secondEntry + "  -  " +
                                            //        originalBlocklistEntry);
                                        } else {  // Process a third-party domain whitelist single entry.
                                            // Create an entry string array.
                                            val domainEntry = arrayOf(domain, entry, originalBlocklistEntry)

                                            // Add the entry to the whitelist.
                                            thirdPartyDomainWhitelist.add(domainEntry)

                                            //Log.i("Blocklists", headers.get(1)[0] + " third-party domain whitelist added: " + domain + " , " + entry + "  -  " + originalBlocklistEntry);
                                        }
                                    } else {  // Third-party domain blacklist entries.
                                        if (entry.contains("*")) {  // Process a third-party domain blacklist double entry.
                                            // Get the index of the wildcard.
                                            val wildcardIndex = entry.indexOf("*")

                                            // Split the entry into components.
                                            val firstEntry = entry.substring(0, wildcardIndex)
                                            val secondEntry = entry.substring(wildcardIndex + 1)

                                            // Create an entry string array.
                                            val domainDoubleEntry = arrayOf(domain, firstEntry, secondEntry, originalBlocklistEntry)

                                            // Add the entry to the blacklist
                                            thirdPartyDomainBlacklist.add(domainDoubleEntry)

                                            //Log.i("Blocklists", headers.get(1)[0] + " third-party domain blacklist added: " + domain + " , " + firstEntry + " , " + secondEntry + "  -  " +
                                            //        originalBlocklistEntry);
                                        } else {  // Process a third-party domain blacklist single entry.
                                            // Create an entry string array.
                                            val domainEntry = arrayOf(domain, entry, originalBlocklistEntry)

                                            // Add the entry to the blacklist.
                                            thirdPartyDomainBlacklist.add(domainEntry)

                                            //Log.i("Blocklists", headers.get(1)[0] + " third-party domain blocklist added: " + domain + " , " + entry + "  -  " + originalBlocklistEntry);
                                        }
                                    }
                                } while (domains.contains("|"))

                                // Add a third-party blacklist entry if a whitelist domain was processed.
                                if (whitelistDomain) {
                                    if (entry.contains("*")) {  // Process a third-party blacklist double entry.
                                        // Get the index of the wildcard.
                                        val wildcardIndex = entry.indexOf("*")

                                        // Split the entry into components.
                                        val firstEntry = entry.substring(0, wildcardIndex)
                                        val secondEntry = entry.substring(wildcardIndex + 1)

                                        // Create an entry string array.
                                        val doubleEntry = arrayOf(firstEntry, secondEntry, originalBlocklistEntry)

                                        // Add the entry to the blacklist.
                                        thirdPartyBlacklist.add(doubleEntry)

                                        //Log.i("Blocklists", headers.get(1)[0] + " third-party blacklist added: " + firstEntry + " , " + secondEntry + "  -  " + originalBlocklistEntry);
                                    } else {  // Process a third-party blacklist single entry.
                                        // Create an entry string array.
                                        val singleEntry = arrayOf(entry, originalBlocklistEntry)

                                        // Add an entry to the blacklist.
                                        thirdPartyBlacklist.add(singleEntry)

                                        //Log.i("Blocklists", headers.get(1)[0] + " third-party blacklist added: " + entry + "  -  " + originalBlocklistEntry);
                                    }
                                }
                            }
                        } else if (blocklistEntry.startsWith("|")) {  // Third-party initial blacklist entries.
                            // Strip the initial `|`.
                            blocklistEntry = blocklistEntry.substring(1)

                            // Get the entry.
                            val entry = blocklistEntry.substring(0, blocklistEntry.indexOf("$"))
                            if (entry.contains("*")) {  // Process a third-party initial blacklist double entry.
                                // Get the index of the wildcard.
                                val wildcardIndex = entry.indexOf("*")

                                // Split the entry into components.
                                val firstEntry = entry.substring(0, wildcardIndex)
                                val secondEntry = entry.substring(wildcardIndex + 1)

                                // Create an entry string array.
                                val thirdPartyDoubleEntry = arrayOf(firstEntry, secondEntry, originalBlocklistEntry)

                                // Add the entry to the blacklist.
                                thirdPartyInitialBlacklist.add(thirdPartyDoubleEntry)

                                //Log.i("Blocklists", headers.get(1)[0] + " third-party initial blacklist added: " + firstEntry + " , " + secondEntry + "  -  " + originalBlocklistEntry);
                            } else {  // Process a third-party initial blacklist single entry.
                                // Create an entry string array.
                                val singleEntry = arrayOf(entry, originalBlocklistEntry)

                                // Add the entry to the blacklist.
                                thirdPartyInitialBlacklist.add(singleEntry)

                                //Log.i("Blocklists", headers.get(1)[0] + " third-party initial blacklist added: " + entry + "  -  " + originalBlocklistEntry);
                            }
                        } else if (blocklistEntry.contains("\\")) {  // Process a regular expression blacklist entry.
                            // Prepare a string to hold the entry.

                            // Get the entry.
                            val entry: String = if (blocklistEntry.contains("$/$")) {  // The first `$` is part of the regular expression.
                                blocklistEntry.substring(0, blocklistEntry.indexOf("$/$") + 2)
                            } else {  // The only `$` indicates the filter options.
                                blocklistEntry.substring(0, blocklistEntry.indexOf("$"))
                            }

                            // Create an entry string array.
                            val singleEntry = arrayOf(entry, originalBlocklistEntry)

                            // Add the entry to the blacklist.
                            thirdPartyRegularExpressionBlacklist.add(singleEntry)

                            //Log.i("Blocklists", headers.get(1)[0] + " third-party regular expression blacklist added: " + entry + "  -  " + originalBlocklistEntry);
                        } else if (blocklistEntry.contains("*")) {  // Third-party and regular expression blacklist entries.
                            // Get the entry.
                            var entry = blocklistEntry.substring(0, blocklistEntry.indexOf("$"))

                            if (entry.endsWith("*")) {  // Process a third-party blacklist single entry.
                                // Strip the final `*`.
                                entry = entry.substring(0, entry.length - 1)

                                // Create an entry string array.
                                val singleEntry = arrayOf(entry, originalBlocklistEntry)

                                // Add the entry to the blacklist.
                                thirdPartyBlacklist.add(singleEntry)

                                //Log.i("Blocklists", headers.get(1)[0] + " third party blacklist added: " + entry + "  -  " + originalBlocklistEntry);
                            } else {  // There are two or more entries.
                                // Get the index of the wildcard.
                                val wildcardIndex = entry.indexOf("*")

                                // Split the entry into components.
                                val firstEntry = entry.substring(0, wildcardIndex)
                                val secondEntry = entry.substring(wildcardIndex + 1)

                                if (secondEntry.contains("*")) {  // There are three or more entries.
                                    // Get the index of the wildcard.
                                    val secondWildcardIndex = secondEntry.indexOf("*")

                                    // Split the entry into components.
                                    val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                    val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                    if (thirdEntry.contains("*")) {  // Process a third-party blacklist quadruple entry.
                                        // Get the index of the wildcard.
                                        val thirdWildcardIndex = thirdEntry.indexOf("*")

                                        // Split the entry into components.
                                        val realThirdEntry = thirdEntry.substring(0, thirdWildcardIndex)
                                        val fourthEntry = thirdEntry.substring(thirdWildcardIndex + 1)

                                        // Create an entry string array.
                                        val quadrupleEntry = arrayOf(firstEntry, realSecondEntry, realThirdEntry, fourthEntry, originalBlocklistEntry)

                                        // Add the entry to the blacklist.
                                        thirdPartyBlacklist.add(quadrupleEntry)

                                        //Log.i("Blocklists", headers.get(1)[0] + " third-party blacklist added: " + firstEntry + " , " + realSecondEntry + " , " + realThirdEntry + " , " +
                                        //        fourthEntry + "  -  " + originalBlocklistEntry);
                                    } else {  // Process a third-party blacklist triple entry.
                                        // Create an entry string array.
                                        val tripleEntry = arrayOf(firstEntry, realSecondEntry, thirdEntry, originalBlocklistEntry)

                                        // Add the entry to the blacklist.
                                        thirdPartyBlacklist.add(tripleEntry)

                                        //Log.i("Blocklists", headers.get(1)[0] + " third-party blacklist added: " + firstEntry + " , " + realSecondEntry + " , " + thirdEntry + "  -  " +
                                        //        originalBlocklistEntry);
                                    }
                                } else {  // Process a third-party blacklist double entry.
                                    // Create an entry string array.
                                    val doubleEntry = arrayOf(firstEntry, secondEntry, originalBlocklistEntry)

                                    // Add the entry to the blacklist.
                                    thirdPartyBlacklist.add(doubleEntry)

                                    //Log.i("Blocklists", headers.get(1)[0] + " third-party blacklist added: " + firstEntry + " , " + secondEntry + "  -  " + originalBlocklistEntry);
                                }
                            }
                        } else {  // Process a third party blacklist single entry.
                            // Get the entry.
                            val entry = blocklistEntry.substring(0, blocklistEntry.indexOf("$"))

                            // Create an entry string array.
                            val singleEntry = arrayOf(entry, originalBlocklistEntry)

                            // Add the entry to the blacklist.
                            thirdPartyBlacklist.add(singleEntry)

                            //Log.i("Blocklists", headers.get(1)[0] + " third party blacklist added: " + entry + "  -  " + originalBlocklistEntry);
                        }
                    } else if (blocklistEntry.substring(blocklistEntry.indexOf("$")).contains("domain=")) {  // Domain entries.
                        if (blocklistEntry.contains("~")) {  // Domain whitelist entries.
                            // Separate the filters.
                            var entry = blocklistEntry.substring(0, blocklistEntry.indexOf("$"))
                            val filters = blocklistEntry.substring(blocklistEntry.indexOf("$") + 1)
                            var domains = filters.substring(filters.indexOf("domain=") + 7)

                            // Strip any final `*` from the entry.  They are redundant.
                            if (entry.endsWith("*")) {
                                entry = entry.substring(0, entry.length - 1)
                            }

                            // Process each domain.
                            do {
                                // Create a string to keep track of the current domain.
                                var domain: String

                                if (domains.contains("|")) {  // There is more than one domain in the list.
                                    // Get the first domain from the list.
                                    domain = domains.substring(0, domains.indexOf("|"))

                                    // Remove the first domain from the list.
                                    domains = domains.substring(domains.indexOf("|") + 1)
                                } else {  // There is only one domain in the list.
                                    domain = domains
                                }

                                // Strip the initial `~`.
                                domain = domain.substring(1)
                                if (entry.contains("*")) {  // There are two or more entries.
                                    // Get the index of the wildcard.
                                    val wildcardIndex = entry.indexOf("*")

                                    // Split the entry into components.
                                    val firstEntry = entry.substring(0, wildcardIndex)
                                    val secondEntry = entry.substring(wildcardIndex + 1)

                                    if (secondEntry.contains("*")) {  // Process a domain whitelist triple entry.
                                        // Get the index of the wildcard.
                                        val secondWildcardIndex = secondEntry.indexOf("*")

                                        // Split the entry into components.
                                        val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                        val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                        // Create an entry string array.
                                        val domainTripleEntry = arrayOf(domain, firstEntry, realSecondEntry, thirdEntry, originalBlocklistEntry)

                                        // Add the entry to the whitelist.
                                        domainWhitelist.add(domainTripleEntry)

                                        //Log.i("Blocklists", headers.get(1)[0] + " domain whitelist added: " + domain + " , " + firstEntry + " , " + realSecondEntry + " , " + thirdEntry +
                                        //        "  -  " + originalBlocklistEntry);
                                    } else {  // Process a domain whitelist double entry.
                                        // Create an entry string array.
                                        val domainDoubleEntry = arrayOf(domain, firstEntry, secondEntry, originalBlocklistEntry)

                                        // Add the entry to the whitelist.
                                        domainWhitelist.add(domainDoubleEntry)

                                        //Log.i("Blocklists", headers.get(1)[0] + " domain whitelist added: " + domain + " , " + firstEntry + " , " + secondEntry + "  -  " + originalBlocklistEntry);
                                    }
                                } else {  // Process a domain whitelist single entry.
                                    // Create an entry string array.
                                    val domainEntry = arrayOf(domain, entry, originalBlocklistEntry)

                                    // Add the entry to the whitelist.
                                    domainWhitelist.add(domainEntry)

                                    //Log.i("Blocklists", headers.get(1)[0] + " domain whitelist added: " + domain + " , " + entry + "  -  " + originalBlocklistEntry);
                                }
                            } while (domains.contains("|"))
                        } else {  // Domain blacklist entries.
                            // Separate the filters.
                            val entry = blocklistEntry.substring(0, blocklistEntry.indexOf("$"))
                            val filters = blocklistEntry.substring(blocklistEntry.indexOf("$") + 1)
                            var domains = filters.substring(filters.indexOf("domain=") + 7)

                            // Only process the item if the entry is not null.  For example, some lines begin with `$websocket`, which create a null entry.
                            if (entry != "") {
                                // Process each domain.
                                do {
                                    // Create a string to keep track of the current domain.
                                    var domain: String

                                    if (domains.contains("|")) {  // There is more than one domain in the list.
                                        // Get the first domain from the list.
                                        domain = domains.substring(0, domains.indexOf("|"))

                                        // Remove the first domain from the list.
                                        domains = domains.substring(domains.indexOf("|") + 1)
                                    } else {  // There is only one domain in the list.
                                        domain = domains
                                    }

                                    if (entry.startsWith("|")) {  // Domain initial blacklist entries.
                                        // Remove the initial `|`;
                                        val entryBase = entry.substring(1)

                                        if (entryBase == "http://" || entryBase == "https://") {
                                            // Do nothing.  These entries will entirely block the website.
                                            // Often the original entry blocks `$script` but Privacy Browser does not currently differentiate between scripts and other entries.

                                            //Log.i("Blocklists", headers.get(1)[0] + " not added: " + originalBlocklistEntry);
                                        } else {  // Process a domain initial blacklist entry
                                            // Create an entry string array.
                                            val domainEntry = arrayOf(domain, entryBase, originalBlocklistEntry)

                                            // Add the entry to the blacklist.
                                            domainInitialBlacklist.add(domainEntry)

                                            //Log.i("Blocklists", headers.get(1)[0] + " domain initial blacklist added: " + domain + " , " + entryBase + "  -  " + originalBlocklistEntry);
                                        }
                                    } else if (entry.endsWith("|")) {  // Domain final blacklist entries.
                                        // Remove the final `|`.
                                        val entryBase = entry.substring(0, entry.length - 1)

                                        if (entryBase.contains("*")) {  // Process a domain final blacklist double entry.
                                            // Get the index of the wildcard.
                                            val wildcardIndex = entry.indexOf("*")

                                            // Split the entry into components.
                                            val firstEntry = entryBase.substring(0, wildcardIndex)
                                            val secondEntry = entryBase.substring(wildcardIndex + 1)

                                            // Create an entry string array.
                                            val domainDoubleEntry = arrayOf(domain, firstEntry, secondEntry, originalBlocklistEntry)

                                            // Add the entry to the blacklist.
                                            domainFinalBlacklist.add(domainDoubleEntry)

                                            //Log.i("Blocklists", headers.get(1)[0] + " domain final blacklist added: " + domain + " , " + firstEntry + " , " + secondEntry + "  -  " +
                                            //        originalBlocklistEntry);
                                        } else {  // Process a domain final blacklist single entry.
                                            // Create an entry string array.
                                            val domainEntry = arrayOf(domain, entryBase, originalBlocklistEntry)

                                            // Add the entry to the blacklist.
                                            domainFinalBlacklist.add(domainEntry)

                                            //Log.i("Blocklists", headers.get(1)[0] + " domain final blacklist added: " + domain + " , " + entryBase + "  -  " + originalBlocklistEntry);
                                        }
                                    } else if (entry.contains("\\")) {  // Process a domain regular expression blacklist entry.
                                        // Create an entry string array.
                                        val domainEntry = arrayOf(domain, entry, originalBlocklistEntry)

                                        // Add the entry to the blacklist.
                                        domainRegularExpressionBlacklist.add(domainEntry)

                                        //Log.i("Blocklists", headers.get(1)[0] + " domain regular expression blacklist added: " + domain + " , " + entry + "  -  " + originalBlocklistEntry);
                                    } else if (entry.contains("*")) {  // There are two or more entries.
                                        // Get the index of the wildcard.
                                        val wildcardIndex = entry.indexOf("*")

                                        // Split the entry into components.
                                        val firstEntry = entry.substring(0, wildcardIndex)
                                        val secondEntry = entry.substring(wildcardIndex + 1)

                                        if (secondEntry.contains("*")) {  // Process a domain blacklist triple entry.
                                            // Get the index of the wildcard.
                                            val secondWildcardIndex = secondEntry.indexOf("*")

                                            // Split the entry into components.
                                            val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                            val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                            // Create an entry string array.
                                            val domainTripleEntry = arrayOf(domain, firstEntry, realSecondEntry, thirdEntry, originalBlocklistEntry)

                                            // Add the entry to the blacklist.
                                            domainBlacklist.add(domainTripleEntry)

                                            //Log.i("Blocklists", headers.get(1)[0] + " domain blacklist added: " + domain + " , " + firstEntry + " , " + realSecondEntry + " , " + thirdEntry +
                                            //        "  -  " + originalBlocklistEntry);
                                        } else {  // Process a domain blacklist double entry.
                                            // Create an entry string array.
                                            val domainDoubleEntry = arrayOf(domain, firstEntry, secondEntry, originalBlocklistEntry)

                                            // Add the entry to the blacklist.
                                            domainBlacklist.add(domainDoubleEntry)

                                            //Log.i("Blocklists", headers.get(1)[0] + " domain blacklist added: " + domain + " , " + firstEntry + " , " + secondEntry + "  -  " +
                                            //        originalBlocklistEntry);
                                        }
                                    } else {  // Process a domain blacklist single entry.
                                        // Create an entry string array.
                                        val domainEntry = arrayOf(domain, entry, originalBlocklistEntry)

                                        // Add the entry to the blacklist.
                                        domainBlacklist.add(domainEntry)

                                        //Log.i("Blocklists", headers.get(1)[0] + " domain blacklist added: " + domain + " , " + entry + "  -  " + originalBlocklistEntry);
                                    }
                                } while (domains.contains("|"))
                            }
                        }
                    } else if (blocklistEntry.contains("~")) {  // Whitelist entries.  Privacy Browser does not differentiate against these filter options, so they are just generally whitelisted.
                        // Remove the filter options.
                        blocklistEntry = blocklistEntry.substring(0, blocklistEntry.indexOf("$"))

                        // Strip any trailing `*`.
                        if (blocklistEntry.endsWith("*")) {
                            blocklistEntry = blocklistEntry.substring(0, blocklistEntry.length - 1)
                        }

                        if (blocklistEntry.contains("*")) {  // Process a whitelist double entry.
                            // Get the index of the wildcard.
                            val wildcardIndex = blocklistEntry.indexOf("*")

                            // Split the entry into components.
                            val firstEntry = blocklistEntry.substring(0, wildcardIndex)
                            val secondEntry = blocklistEntry.substring(wildcardIndex + 1)

                            // Create an entry string array.
                            val doubleEntry = arrayOf(firstEntry, secondEntry, originalBlocklistEntry)

                            // Add the entry to the whitelist.
                            mainWhitelist.add(doubleEntry)

                            //Log.i("Blocklists", headers.get(1)[0] + " main whitelist added: " + firstEntry + " , " + secondEntry + "  -  " + originalBlocklistEntry);
                        } else {  // Process a whitelist single entry.
                            // Create an entry string array.
                            val singleEntry = arrayOf(blocklistEntry, originalBlocklistEntry)

                            // Add the entry to the whitelist.
                            mainWhitelist.add(singleEntry)

                            //Log.i("Blocklists", headers.get(1)[0] + " main whitelist added: " + blocklistEntry + "  -  + " + originalBlocklistEntry);
                        }
                    } else if (blocklistEntry.contains("\\")) {  // Process a regular expression blacklist entry.
                        // Remove the filter options.
                        blocklistEntry = blocklistEntry.substring(0, blocklistEntry.indexOf("$"))

                        // Create an entry string array.
                        val singleEntry = arrayOf(blocklistEntry, originalBlocklistEntry)

                        // Add the entry to the blacklist.
                        regularExpressionBlacklist.add(singleEntry)

                        //Log.i("Blocklists", headers.get(1)[0] + " regular expression blacklist added: " + blocklistEntry + "  -  " + originalBlocklistEntry);
                    } else {  // Blacklist entries.
                        // Remove the filter options.
                        if (!blocklistEntry.contains("\$file")) {  // EasyPrivacy contains an entry with `$file` that does not have filter options.
                            blocklistEntry = blocklistEntry.substring(0, blocklistEntry.indexOf("$"))
                        }

                        // Strip any trailing `*`.  These are redundant.
                        if (blocklistEntry.endsWith("*")) {
                            blocklistEntry = blocklistEntry.substring(0, blocklistEntry.length - 1)
                        }

                        if (blocklistEntry.startsWith("|")) {  // Initial blacklist entries.
                            // Strip the initial `|`.
                            val entry = blocklistEntry.substring(1)

                            if (entry.contains("*")) {  // Process an initial blacklist double entry.
                                // Get the index of the wildcard.
                                val wildcardIndex = entry.indexOf("*")

                                // Split the entry into components.
                                val firstEntry = entry.substring(0, wildcardIndex)
                                val secondEntry = entry.substring(wildcardIndex + 1)

                                // Create an entry string array.
                                val doubleEntry = arrayOf(firstEntry, secondEntry, originalBlocklistEntry)

                                // Add the entry to the blacklist.
                                initialBlacklist.add(doubleEntry)

                                //Log.i("Blocklists", headers.get(1)[0] + " initial blacklist added: " + firstEntry + " , " + secondEntry + "  -  " + originalBlocklistEntry);
                            } else {  // Process an initial blacklist single entry.
                                // Create an entry string array.
                                val singleEntry = arrayOf(entry, originalBlocklistEntry)

                                // Add the entry to the blacklist.
                                initialBlacklist.add(singleEntry)

                                //Log.i("Blocklists", headers.get(1)[0] + " initial blacklist added: " + entry + "  -  " + originalBlocklistEntry);
                            }
                        } else if (blocklistEntry.endsWith("|")) {  // Final blacklist entries.
                            // Ignore entries with `object` filters.  They can block entire websites and don't have any meaning in the context of Privacy Browser.
                            if (!originalBlocklistEntry.contains("\$object")) {
                                // Strip the final `|`.
                                val entry = blocklistEntry.substring(0, blocklistEntry.length - 1)

                                if (entry.contains("*")) {  // There are two or more entries.
                                    // Get the index of the wildcard.
                                    val wildcardIndex = entry.indexOf("*")

                                    // Split the entry into components.
                                    val firstEntry = entry.substring(0, wildcardIndex)
                                    val secondEntry = entry.substring(wildcardIndex + 1)

                                    if (secondEntry.contains("*")) {  // Process a final blacklist triple entry.
                                        // Get the index of the wildcard.
                                        val secondWildcardIndex = secondEntry.indexOf("*")

                                        // Split the entry into components.
                                        val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                        val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                        // Create an entry string array.
                                        val tripleEntry = arrayOf(firstEntry, realSecondEntry, thirdEntry, originalBlocklistEntry)

                                        // Add the entry to the blacklist.
                                        finalBlacklist.add(tripleEntry)

                                        //Log.i("Blocklists", headers.get(1)[0] + " final blacklist added: " + firstEntry + " , " + realSecondEntry + " , " + thirdEntry + "  -  " +
                                        //        originalBlocklistEntry);
                                    } else {  // Process a final blacklist double entry.
                                        // Create an entry string array.
                                        val doubleEntry = arrayOf(firstEntry, secondEntry, originalBlocklistEntry)

                                        // Add the entry to the blacklist.
                                        finalBlacklist.add(doubleEntry)

                                        //Log.i("Blocklists", headers.get(1)[0] + " final blacklist added: " + firstEntry + " , " + secondEntry + "  -  " + originalBlocklistEntry);
                                    }
                                } else {  // Process a final blacklist single entry.
                                    // Create an entry sting array.
                                    val singleEntry = arrayOf(entry, originalBlocklistEntry)

                                    // Add the entry to the blacklist.
                                    finalBlacklist.add(singleEntry)

                                    //Log.i("Blocklists", headers.get(1)[0] + " final blacklist added: " + entry + "  -  " + originalBlocklistEntry);
                                }
                            }
                        } else if (blocklistEntry.contains("*")) {  // There are two or more entries.
                            // Get the index of the wildcard.
                            val wildcardIndex = blocklistEntry.indexOf("*")

                            // Split the entry into components.
                            val firstEntry = blocklistEntry.substring(0, wildcardIndex)
                            val secondEntry = blocklistEntry.substring(wildcardIndex + 1)

                            if (secondEntry.contains("*")) {  // Process a main blacklist triple entry.
                                // Get the index of the wildcard.
                                val secondWildcardIndex = secondEntry.indexOf("*")

                                // Split the entry into components.
                                val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                // Create an entry string array.
                                val tripleEntry = arrayOf(firstEntry, realSecondEntry, thirdEntry, originalBlocklistEntry)

                                // Add the entry to the blacklist.
                                mainBlacklist.add(tripleEntry)

                                //Log.i("Blocklists", headers.get(1)[0] + " main blacklist added: " + firstEntry + " , " + realSecondEntry + " , " + thirdEntry + "  -  " + originalBlocklistEntry);
                            } else {  // Process a main blacklist double entry.
                                // Create an entry string array.
                                val doubleEntry = arrayOf(firstEntry, secondEntry, originalBlocklistEntry)

                                // Add the entry to the blacklist.
                                mainBlacklist.add(doubleEntry)

                                //Log.i("Blocklists", headers.get(1)[0] + " main blacklist added: " + firstEntry + " , " + secondEntry + "  -  " + originalBlocklistEntry);
                            }
                        } else {  // Process a main blacklist single entry.
                            // Create an entry string array.
                            val singleEntry = arrayOf(blocklistEntry, originalBlocklistEntry)

                            // Add the entry to the blacklist.
                            mainBlacklist.add(singleEntry)

                            //Log.i("Blocklists", headers.get(1)[0] + " main blacklist added: " + blocklistEntry + "  -  " + originalBlocklistEntry);
                        }
                    }
                } else {  // Main blacklist entries
                    // Strip out any initial `||`.  These will be treated like any other entry.
                    if (blocklistEntry.startsWith("||")) {
                        blocklistEntry = blocklistEntry.substring(2)
                    }

                    // Strip out any initial `*`.
                    if (blocklistEntry.startsWith("*")) {
                        blocklistEntry = blocklistEntry.substring(1)
                    }

                    // Strip out any trailing `*`.
                    if (blocklistEntry.endsWith("*")) {
                        blocklistEntry = blocklistEntry.substring(0, blocklistEntry.length - 1)
                    }

                    if (blocklistEntry.startsWith("|")) {  // Initial blacklist entries.
                        // Strip the initial `|`.
                        val entry = blocklistEntry.substring(1)

                        if (entry.contains("*")) {  // Process an initial blacklist double entry.
                            // Get the index of the wildcard.
                            val wildcardIndex = entry.indexOf("*")

                            // Split the entry into components.
                            val firstEntry = entry.substring(0, wildcardIndex)
                            val secondEntry = entry.substring(wildcardIndex + 1)

                            // Create an entry string array.
                            val doubleEntry = arrayOf(firstEntry, secondEntry, originalBlocklistEntry)

                            // Add the entry to the blacklist.
                            initialBlacklist.add(doubleEntry)

                            //Log.i("Blocklists", headers.get(1)[0] + " initial blacklist added: " + firstEntry + " , " + secondEntry + "  -  " + originalBlocklistEntry);
                        } else {  // Process an initial blacklist single entry.
                            // Create an entry string array.
                            val singleEntry = arrayOf(entry, originalBlocklistEntry)

                            // Add the entry to the blacklist.
                            initialBlacklist.add(singleEntry)

                            //Log.i("Blocklists", headers.get(1)[0] + " initial blacklist added: " + entry + "  -  " + originalBlocklistEntry);
                        }
                    } else if (blocklistEntry.endsWith("|")) {  // Final blacklist entries.
                        // Strip the final `|`.
                        val entry = blocklistEntry.substring(0, blocklistEntry.length - 1)

                        if (entry.contains("*")) {  // There are two or more entries.
                            // Get the index of the wildcard.
                            val wildcardIndex = entry.indexOf("*")

                            // Split the entry into components.
                            val firstEntry = entry.substring(0, wildcardIndex)
                            val secondEntry = entry.substring(wildcardIndex + 1)

                            if (secondEntry.contains("*")) {  // Process a final blacklist triple entry.
                                // Get the index of the wildcard.
                                val secondWildcardIndex = secondEntry.indexOf("*")

                                // Split the entry into components.
                                val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                // Create an entry string array.
                                val tripleEntry = arrayOf(firstEntry, realSecondEntry, thirdEntry, originalBlocklistEntry)

                                // Add the entry to the blacklist.
                                finalBlacklist.add(tripleEntry)

                                //Log.i("Blocklists", headers.get(1)[0] + " final blacklist added: " + firstEntry + " , " + realSecondEntry + " , " + thirdEntry + "  -  " + originalBlocklistEntry);
                            } else {  // Process a final blacklist double entry.
                                // Create an entry string array.
                                val doubleEntry = arrayOf(firstEntry, secondEntry, originalBlocklistEntry)

                                // Add the entry to the blacklist.
                                finalBlacklist.add(doubleEntry)

                                //Log.i("Blocklists", headers.get(1)[0] + " final blacklist added: " + firstEntry + " , " + secondEntry + "  -  " + originalBlocklistEntry);
                            }
                        } else {  // Process a final blacklist single entry.
                            // Create an entry string array.
                            val singleEntry = arrayOf(entry, originalBlocklistEntry)

                            // Add the entry to the blacklist.
                            finalBlacklist.add(singleEntry)

                            //Log.i("Blocklists", headers.get(1)[0] + " final blacklist added: " + entry + "  -  " + originalBlocklistEntry);
                        }
                    } else {  // Main blacklist entries.
                        if (blocklistEntry.contains("*")) {  // There are two or more entries.
                            // Get the index of the wildcard.
                            val wildcardIndex = blocklistEntry.indexOf("*")

                            // Split the entry into components.
                            val firstEntry = blocklistEntry.substring(0, wildcardIndex)
                            val secondEntry = blocklistEntry.substring(wildcardIndex + 1)

                            if (secondEntry.contains("*")) {  // There are three or more entries.
                                // Get the index of the wildcard.
                                val secondWildcardIndex = secondEntry.indexOf("*")

                                // Split the entry into components.
                                val realSecondEntry = secondEntry.substring(0, secondWildcardIndex)
                                val thirdEntry = secondEntry.substring(secondWildcardIndex + 1)

                                if (thirdEntry.contains("*")) {  // There are four or more entries.
                                    // Get the index of the wildcard.
                                    val thirdWildcardIndex = thirdEntry.indexOf("*")

                                    // Split the entry into components.
                                    val realThirdEntry = thirdEntry.substring(0, thirdWildcardIndex)
                                    val fourthEntry = thirdEntry.substring(thirdWildcardIndex + 1)

                                    if (fourthEntry.contains("*")) {  // Process a main blacklist quintuple entry.
                                        // Get the index of the wildcard.
                                        val fourthWildcardIndex = fourthEntry.indexOf("*")

                                        // Split the entry into components.
                                        val realFourthEntry = fourthEntry.substring(0, fourthWildcardIndex)
                                        val fifthEntry = fourthEntry.substring(fourthWildcardIndex + 1)

                                        // Create an entry string array.
                                        val quintupleEntry = arrayOf(firstEntry, realSecondEntry, realThirdEntry, realFourthEntry, fifthEntry, originalBlocklistEntry)

                                        // Add the entry to the blacklist.
                                        mainBlacklist.add(quintupleEntry)

                                        //Log.i("Blocklists", headers.get(1)[0] + " main blacklist added: " + firstEntry + " , " + realSecondEntry + " , " + realThirdEntry + " , " + realFourthEntry + " , " +
                                        //      fifthEntry + "  -  " + originalBlocklistEntry);
                                    } else {  // Process a main blacklist quadruple entry.
                                        // Create an entry string array.
                                        val quadrupleEntry = arrayOf(firstEntry, realSecondEntry, realThirdEntry, fourthEntry, originalBlocklistEntry)

                                        // Add the entry to the blacklist.
                                        mainBlacklist.add(quadrupleEntry)

                                        //Log.i("Blocklists", headers.get(1)[0] + " main blacklist added: " + firstEntry + " , " + realSecondEntry + " , " + realThirdEntry + " , " + fourthEntry + "  -  " +
                                        //      originalBlocklistEntry);
                                    }
                                } else {  // Process a main blacklist triple entry.
                                    // Create an entry string array.
                                    val tripleEntry = arrayOf(firstEntry, realSecondEntry, thirdEntry, originalBlocklistEntry)

                                    // Add the entry to the blacklist.
                                    mainBlacklist.add(tripleEntry)

                                    //Log.i("Blocklists", headers.get(1)[0] + " main blacklist added: " + firstEntry + " , " + realSecondEntry + " , " + thirdEntry + "  -  " + originalBlocklistEntry);
                                }
                            } else {  // Process a main blacklist double entry.
                                // Create an entry string array.
                                val doubleEntry = arrayOf(firstEntry, secondEntry, originalBlocklistEntry)

                                // Add the entry to the blacklist.
                                mainBlacklist.add(doubleEntry)

                                //Log.i("Blocklists", headers.get(1)[0] + " main blacklist added: " + firstEntry + " , " + secondEntry + "  -  " + originalBlocklistEntry);
                            }
                        } else {  // Process a main blacklist single entry.
                            // Create an entry string array.
                            val singleEntry = arrayOf(blocklistEntry, originalBlocklistEntry)

                            // Add the entry to the blacklist.
                            mainBlacklist.add(singleEntry)

                            //Log.i("Blocklists", headers.get(1)[0] + " main blacklist added: " + blocklistEntry + "  -  " + originalBlocklistEntry);
                        }
                    }
                }
            }
            // Close `bufferedReader`.
            bufferedReader.close()
        } catch (e: IOException) {
            // The asset exists, so the `IOException` will never be thrown.
        }

        // Initialize the combined list.
        val combinedLists = ArrayList<List<Array<String>>>()

        // Add the headers (0).
        combinedLists.add(headers) // 0.

        // Add the whitelists (1-8).
        combinedLists.add(mainWhitelist) // 1.
        combinedLists.add(finalWhitelist) // 2.
        combinedLists.add(domainWhitelist) // 3.
        combinedLists.add(domainInitialWhitelist) // 4.
        combinedLists.add(domainFinalWhitelist) // 5.
        combinedLists.add(thirdPartyWhitelist) // 6.
        combinedLists.add(thirdPartyDomainWhitelist) // 7.
        combinedLists.add(thirdPartyDomainInitialWhitelist) // 8.

        // Add the blacklists (9-22).
        combinedLists.add(mainBlacklist) // 9.
        combinedLists.add(initialBlacklist) // 10.
        combinedLists.add(finalBlacklist) // 11.
        combinedLists.add(domainBlacklist) //  12.
        combinedLists.add(domainInitialBlacklist) // 13.
        combinedLists.add(domainFinalBlacklist) // 14.
        combinedLists.add(domainRegularExpressionBlacklist) // 15.
        combinedLists.add(thirdPartyBlacklist) // 16.
        combinedLists.add(thirdPartyInitialBlacklist) // 17.
        combinedLists.add(thirdPartyDomainBlacklist) // 18.
        combinedLists.add(thirdPartyDomainInitialBlacklist) // 19.
        combinedLists.add(thirdPartyRegularExpressionBlacklist) // 20.
        combinedLists.add(thirdPartyDomainRegularExpressionBlacklist) // 21.
        combinedLists.add(regularExpressionBlacklist) // 22.

        // Return the combined lists.
        return combinedLists
    }

    fun checkBlocklist(currentDomain: String?, resourceUrl: String, isThirdPartyRequest: Boolean, blocklist: ArrayList<List<Array<String>>>): Array<String> {
        // Get the blocklist name.
        val blocklistName = blocklist[0][1][0]

        // Process the whitelists.
        // Main whitelist.
        for (whitelistEntry in blocklist[MAIN_WHITELIST.toInt()]) {
            when (whitelistEntry.size) {
                // There is one entry.
                2 -> if (resourceUrl.contains(whitelistEntry[0])) {
                    // Return a whitelist match request allowed.
                    return arrayOf(REQUEST_ALLOWED, resourceUrl, blocklistName, MAIN_WHITELIST, whitelistEntry[0], whitelistEntry[1])
                }

                // There are two entries.
                3 -> if (resourceUrl.contains(whitelistEntry[0]) && resourceUrl.contains(whitelistEntry[1])) {
                    // Return a whitelist match request allowed.
                    return arrayOf(REQUEST_ALLOWED, resourceUrl, blocklistName, MAIN_WHITELIST, "${whitelistEntry[0]}\n${whitelistEntry[1]}", whitelistEntry[2]
                    )
                }

                // There are three entries.
                4 -> if (resourceUrl.contains(whitelistEntry[0]) && resourceUrl.contains(whitelistEntry[1]) && resourceUrl.contains(whitelistEntry[2])) {
                    // Return a whitelist match request allowed.
                    return arrayOf(REQUEST_ALLOWED, resourceUrl, blocklistName, MAIN_WHITELIST, "${whitelistEntry[0]}\n${whitelistEntry[1]}\n${whitelistEntry[2]}", whitelistEntry[3])
                }
            }
        }

        // Final whitelist.
        for (whitelistEntry in blocklist[FINAL_WHITELIST.toInt()]) {
            when (whitelistEntry.size) {
                // There is one entry.
                2 -> if (resourceUrl.contains(whitelistEntry[0])) {
                    // Return a whitelist match request allowed.
                    return arrayOf(REQUEST_ALLOWED, resourceUrl, blocklistName, FINAL_WHITELIST, whitelistEntry[0], whitelistEntry[1])
                }

                // There are two entries.
                3 -> if (resourceUrl.contains(whitelistEntry[0]) && resourceUrl.contains(whitelistEntry[1])) {
                    // Return a whitelist match request allowed.
                    return arrayOf(REQUEST_ALLOWED, resourceUrl, blocklistName, FINAL_WHITELIST, "${whitelistEntry[0]}\n${whitelistEntry[1]}", whitelistEntry[2])
                }
            }
        }

        // Only check the domain lists if the current domain is not null (like `about:blank`).
        if (currentDomain != null) {
            // Domain whitelist.
            for (whitelistEntry in blocklist[DOMAIN_WHITELIST.toInt()]) {
                when (whitelistEntry.size) {
                    // There is one entry.
                    3 -> if (currentDomain.endsWith(whitelistEntry[0]) && resourceUrl.contains(whitelistEntry[1])) {
                        // Return a whitelist match request allowed.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, blocklistName, DOMAIN_WHITELIST, "${whitelistEntry[0]}\n${whitelistEntry[1]}", whitelistEntry[2])
                    }

                    // There are two entries.
                    4 -> if (currentDomain.endsWith(whitelistEntry[0]) && resourceUrl.contains(whitelistEntry[1]) && resourceUrl.contains(whitelistEntry[2])) {
                        // Return a whitelist match request allowed.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, blocklistName, DOMAIN_WHITELIST, "${whitelistEntry[0]}\n${whitelistEntry[1]}\n${whitelistEntry[2]}", whitelistEntry[3])
                    }

                    // There are three entries.
                    5 -> if (currentDomain.endsWith(whitelistEntry[0]) && resourceUrl.contains(whitelistEntry[1]) && resourceUrl.contains(whitelistEntry[2]) && resourceUrl.contains(whitelistEntry[3])) {
                        // Return a whitelist match request allowed.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, blocklistName, DOMAIN_WHITELIST, "${whitelistEntry[0]}\n${whitelistEntry[1]}\n${whitelistEntry[2]}\n${whitelistEntry[3]}", whitelistEntry[4])
                    }

                    // There are four entries.
                    6 -> if (currentDomain.endsWith(whitelistEntry[0]) && resourceUrl.contains(whitelistEntry[1]) && resourceUrl.contains(whitelistEntry[2]) && resourceUrl.contains(whitelistEntry[3]) &&
                        resourceUrl.contains(whitelistEntry[4])) {
                        // Return a whitelist match request allowed.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, blocklistName, DOMAIN_WHITELIST,
                            "${whitelistEntry[0]}\n${whitelistEntry[1]}\n${whitelistEntry[2]}\n${whitelistEntry[3]}\n${whitelistEntry[4]}", whitelistEntry[5])
                    }
                }
            }

            // Domain initial whitelist.
            for (whitelistEntry in blocklist[DOMAIN_INITIAL_WHITELIST.toInt()]) {
                when (whitelistEntry.size) {
                    // There is one entry.
                    3 -> if (currentDomain.endsWith(whitelistEntry[0]) && resourceUrl.startsWith(whitelistEntry[1])) {
                        // Return a whitelist match request allowed.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, blocklistName, DOMAIN_INITIAL_WHITELIST, "${whitelistEntry[0]}\n${whitelistEntry[1]}".trimIndent(), whitelistEntry[2])
                    }

                    // There are two entries.
                    4 -> if (currentDomain.endsWith(whitelistEntry[0]) && resourceUrl.startsWith(whitelistEntry[1]) && resourceUrl.contains(whitelistEntry[2])) {
                        // Return a whitelist match request allowed.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, blocklistName, DOMAIN_INITIAL_WHITELIST, "${whitelistEntry[0]}\n${whitelistEntry[1]}\n${whitelistEntry[2]}", whitelistEntry[3])
                    }

                    // There are three entries.
                    5 -> if (currentDomain.endsWith(whitelistEntry[0]) && resourceUrl.startsWith(whitelistEntry[1]) && resourceUrl.contains(whitelistEntry[2]) && resourceUrl.startsWith(whitelistEntry[3])) {
                        // Return a whitelist match request allowed.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, blocklistName, DOMAIN_INITIAL_WHITELIST, "${whitelistEntry[0]}\n${whitelistEntry[1]}\n${whitelistEntry[2]}\n${whitelistEntry[3]}",
                            whitelistEntry[4])
                    }
                }
            }

            // Domain final whitelist.
            for (whitelistEntry in blocklist[DOMAIN_FINAL_WHITELIST.toInt()]) {
                when (whitelistEntry.size) {
                    // There is one entry.
                    3 -> if (currentDomain.endsWith(whitelistEntry[0]) && resourceUrl.endsWith(whitelistEntry[1])) {
                        // Return a whitelist match request allowed.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, blocklistName, DOMAIN_FINAL_WHITELIST, "${whitelistEntry[0]}\n${whitelistEntry[1]}", whitelistEntry[2])
                    }

                    // There are two entries.
                    4 -> if (currentDomain.endsWith(whitelistEntry[0]) && resourceUrl.contains(whitelistEntry[1]) && resourceUrl.endsWith(whitelistEntry[2])) {
                        // Return a whitelist match request allowed.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, blocklistName, DOMAIN_FINAL_WHITELIST, "${whitelistEntry[0]}\n${whitelistEntry[1]}\n${whitelistEntry[2]}", whitelistEntry[3])
                    }
                }
            }
        }

        // Only check the third-party whitelists if this is a third-party request.
        if (isThirdPartyRequest) {
            // Third-party whitelist.
            for (whitelistEntry in blocklist[THIRD_PARTY_WHITELIST.toInt()]) {
                when (whitelistEntry.size) {
                    // There is one entry.
                    2 -> if (resourceUrl.contains(whitelistEntry[0])) {
                        // Return a whitelist match request allowed.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, blocklistName, THIRD_PARTY_WHITELIST, whitelistEntry[0], whitelistEntry[1])
                    }

                    // There are two entries.
                    3 -> if (resourceUrl.contains(whitelistEntry[0]) && resourceUrl.contains(whitelistEntry[1])) {
                        // Return a whitelist match request allowed.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, blocklistName, THIRD_PARTY_WHITELIST, "${whitelistEntry[0]}\n${whitelistEntry[1]}", whitelistEntry[2])
                    }

                    // There are three entries.
                    4 -> if (resourceUrl.contains(whitelistEntry[0]) && resourceUrl.contains(whitelistEntry[1]) && resourceUrl.contains(whitelistEntry[2])) {
                        // Return a whitelist match request allowed.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, blocklistName, THIRD_PARTY_WHITELIST, "${whitelistEntry[0]}\n${whitelistEntry[1]}\n${whitelistEntry[2]}", whitelistEntry[3])
                    }

                    // There are four entries.
                    5 -> if (resourceUrl.contains(whitelistEntry[0]) && resourceUrl.contains(whitelistEntry[1]) && resourceUrl.contains(whitelistEntry[2]) && resourceUrl.contains(whitelistEntry[3])) {
                        // Return a whitelist match request allowed.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, blocklistName, THIRD_PARTY_WHITELIST, "${whitelistEntry[0]}\n${whitelistEntry[1]}\n${whitelistEntry[2]}\n${whitelistEntry[3]}",
                            whitelistEntry[4])
                    }

                    // There are five entries.
                    6 -> if (resourceUrl.contains(whitelistEntry[0]) && resourceUrl.contains(whitelistEntry[1]) && resourceUrl.contains(whitelistEntry[2]) && resourceUrl.contains(whitelistEntry[3]) &&
                        resourceUrl.contains(whitelistEntry[4])) {
                        // Return a whitelist match request allowed.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, blocklistName, THIRD_PARTY_WHITELIST,
                            "${whitelistEntry[0]}\n${whitelistEntry[1]}\n${whitelistEntry[2]}\n${whitelistEntry[3]}\n${whitelistEntry[4]}", whitelistEntry[5])
                    }
                }
            }

            // Third-party domain whitelist.
            for (whitelistEntry in blocklist[THIRD_PARTY_DOMAIN_WHITELIST.toInt()]) {
                when (whitelistEntry.size) {
                    // There is one entry.
                    3 -> if (currentDomain!!.endsWith(whitelistEntry[0]) && resourceUrl.contains(whitelistEntry[1])) {
                        // Return a whitelist match request allowed.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, blocklistName, THIRD_PARTY_DOMAIN_WHITELIST, "${whitelistEntry[0]}\n${whitelistEntry[1]}\n", whitelistEntry[2])
                    }

                    // There are two entries.
                    4 -> if (currentDomain!!.endsWith(whitelistEntry[0]) && resourceUrl.contains(whitelistEntry[1]) && resourceUrl.contains(whitelistEntry[2])) {
                        // Return a whitelist match request allowed.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, blocklistName, THIRD_PARTY_DOMAIN_WHITELIST, "${whitelistEntry[0]}\n${whitelistEntry[1]}\n${whitelistEntry[2]}", whitelistEntry[3])
                    }
                }
            }

            // Third-party domain initial whitelist.
            for (whitelistEntry in blocklist[THIRD_PARTY_DOMAIN_INITIAL_WHITELIST.toInt()]) {
                when (whitelistEntry.size) {
                    // There is one entry.
                    3 -> if (currentDomain!!.endsWith(whitelistEntry[0]) && resourceUrl.startsWith(whitelistEntry[1])) {
                        // Return a whitelist match request allowed.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, blocklistName, THIRD_PARTY_DOMAIN_INITIAL_WHITELIST, "${whitelistEntry[0]}\n${whitelistEntry[1]}\n", whitelistEntry[2])
                    }

                    // There are two entries.
                    4 -> if (currentDomain!!.endsWith(whitelistEntry[0]) && resourceUrl.startsWith(whitelistEntry[1]) && resourceUrl.contains(whitelistEntry[2])) {
                        // Return a whitelist match request allowed.
                        return arrayOf(REQUEST_ALLOWED, resourceUrl, blocklistName, THIRD_PARTY_DOMAIN_WHITELIST, "${whitelistEntry[0]}\n${whitelistEntry[1]}\n${whitelistEntry[2]}", whitelistEntry[3])
                    }
                }
            }
        }

        // Process the blacklists.
        // Main blacklist.
        for (blacklistEntry in blocklist[MAIN_BLACKLIST.toInt()]) {
            when (blacklistEntry.size) {
                // There is one entry.
                2 -> if (resourceUrl.contains(blacklistEntry[0])) {
                    // Return a blacklist match request blocked.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, MAIN_BLACKLIST, blacklistEntry[0], blacklistEntry[1])
                }

                // There are two entries.
                3 -> if (resourceUrl.contains(blacklistEntry[0]) && resourceUrl.contains(blacklistEntry[1])) {
                    // Return a blacklist match request blocked.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, MAIN_BLACKLIST, "${blacklistEntry[0]}\n${blacklistEntry[1]}", blacklistEntry[2])
                }

                // There are three entries.
                4 -> if (resourceUrl.contains(blacklistEntry[0]) && resourceUrl.contains(blacklistEntry[1]) && resourceUrl.contains(blacklistEntry[2])) {
                    // Return a blacklist match request blocked.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, MAIN_BLACKLIST, "${blacklistEntry[0]}\n${blacklistEntry[1]}\n${blacklistEntry[2]}", blacklistEntry[3])
                }

                // There are four entries.
                5 -> if (resourceUrl.contains(blacklistEntry[0]) && resourceUrl.contains(blacklistEntry[1]) && resourceUrl.contains(blacklistEntry[2]) && resourceUrl.contains(blacklistEntry[3])) {
                    // Return a blacklist match request blocked.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, MAIN_BLACKLIST, "${blacklistEntry[0]}\n${blacklistEntry[1]}\n${blacklistEntry[2]}\n${blacklistEntry[3]}", blacklistEntry[4])
                }

                // There are five entries.
                6 -> if (resourceUrl.contains(blacklistEntry[0]) && resourceUrl.contains(blacklistEntry[1]) && resourceUrl.contains(blacklistEntry[2]) && resourceUrl.contains(blacklistEntry[3]) &&
                    resourceUrl.contains(blacklistEntry[4])) {
                    // Return a blacklist match request blocked.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, MAIN_BLACKLIST, "${blacklistEntry[0]}\n${blacklistEntry[1]}\n${blacklistEntry[2]}\n${blacklistEntry[3]}\n${blacklistEntry[4]}",
                        blacklistEntry[5])
                }
            }
        }

        // Initial blacklist.
        for (blacklistEntry in blocklist[INITIAL_BLACKLIST.toInt()]) {
            when (blacklistEntry.size) {
                // There is one entry.
                2 -> if (resourceUrl.startsWith(blacklistEntry[0])) {
                    // Return a blacklist match request blocked.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, INITIAL_BLACKLIST, blacklistEntry[0], blacklistEntry[1])
                }

                // There are two entries
                3 -> if (resourceUrl.startsWith(blacklistEntry[0]) && resourceUrl.contains(blacklistEntry[1])) {
                    // Return a blacklist match request blocked.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, INITIAL_BLACKLIST, "${blacklistEntry[0]}\n${blacklistEntry[1]}", blacklistEntry[2])
                }
            }
        }

        // Final blacklist.
        for (blacklistEntry in blocklist[FINAL_BLACKLIST.toInt()]) {
            when (blacklistEntry.size) {
                // There is one entry.
                2 -> if (resourceUrl.endsWith(blacklistEntry[0])) {
                    // Return a blacklist match request blocked.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, FINAL_BLACKLIST, blacklistEntry[0], blacklistEntry[1])
                }

                // There are two entries.
                3 -> if (resourceUrl.contains(blacklistEntry[0]) && resourceUrl.endsWith(blacklistEntry[1])) {
                    // Return a blacklist match request blocked.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, FINAL_BLACKLIST, "${blacklistEntry[0]}\n${blacklistEntry[1]}", blacklistEntry[2])
                }

                // There are three entries.
                4 -> if (resourceUrl.contains(blacklistEntry[0]) && resourceUrl.contains(blacklistEntry[1]) && resourceUrl.endsWith(blacklistEntry[2])) {
                    // Return a blacklist match request blocked.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, FINAL_BLACKLIST, "${blacklistEntry[0]}\n${blacklistEntry[1]}\n${blacklistEntry[2]}", blacklistEntry[3])
                }
            }
        }

        // Only check the domain lists if the current domain is not null (like `about:blank`).
        if (currentDomain != null) {
            // Domain blacklist.
            for (blacklistEntry in blocklist[DOMAIN_BLACKLIST.toInt()]) {
                when (blacklistEntry.size) {
                    // There is one entry.
                    3 -> if (currentDomain.endsWith(blacklistEntry[0]) && resourceUrl.contains(blacklistEntry[1])) {
                        // Return a blacklist match request blocked.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, DOMAIN_BLACKLIST, "${blacklistEntry[0]}\n${blacklistEntry[1]}", blacklistEntry[2])
                    }

                    // There are two entries.
                    4 -> if (currentDomain.endsWith(blacklistEntry[0]) && resourceUrl.contains(blacklistEntry[1]) && resourceUrl.contains(blacklistEntry[2])) {
                        // Return a blacklist match request blocked.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, DOMAIN_BLACKLIST, "${blacklistEntry[0]}\n${blacklistEntry[1]}\n${blacklistEntry[2]}", blacklistEntry[3])
                    }

                    // There are three entries.
                    5 -> if (currentDomain.endsWith(blacklistEntry[0]) && resourceUrl.contains(blacklistEntry[1]) && resourceUrl.contains(blacklistEntry[2]) && resourceUrl.contains(blacklistEntry[3])) {
                        // Return a blacklist match request blocked.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, DOMAIN_BLACKLIST, "${blacklistEntry[0]}\n${blacklistEntry[1]}\n${blacklistEntry[2]}\n${blacklistEntry[3]}", blacklistEntry[4])
                    }
                }
            }

            // Domain initial blacklist.
            for (blacklistEntry in blocklist[DOMAIN_INITIAL_BLACKLIST.toInt()]) {
                // Store the entry in the resource request log.
                if (currentDomain.endsWith(blacklistEntry[0]) && resourceUrl.startsWith(blacklistEntry[1])) {
                    // Return a blacklist match request blocked.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, DOMAIN_INITIAL_BLACKLIST, "${blacklistEntry[0]}\n${blacklistEntry[1]}", blacklistEntry[2])
                }
            }

            // Domain final blacklist.
            for (blacklistEntry in blocklist[DOMAIN_FINAL_BLACKLIST.toInt()]) {
                when (blacklistEntry.size) {
                    // There is one entry.
                    3 -> if (currentDomain.endsWith(blacklistEntry[0]) && resourceUrl.endsWith(blacklistEntry[1])) {
                        // Return a blacklist match request blocked.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, DOMAIN_FINAL_BLACKLIST, "${blacklistEntry[0]}\n${blacklistEntry[1]}", blacklistEntry[2])
                    }

                    // There are two entries.
                    4 -> if (currentDomain.endsWith(blacklistEntry[0]) && resourceUrl.contains(blacklistEntry[1]) && resourceUrl.endsWith(blacklistEntry[2])) {
                        // Return a blacklist match request blocked.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, DOMAIN_FINAL_BLACKLIST, "${blacklistEntry[0]}\n${blacklistEntry[1]}\n${blacklistEntry[2]}", blacklistEntry[3])
                    }
                }
            }

            // Domain regular expression blacklist.
            for (blacklistEntry in blocklist[DOMAIN_REGULAR_EXPRESSION_BLACKLIST.toInt()]) {
                if (currentDomain.endsWith(blacklistEntry[0]) && Pattern.matches(blacklistEntry[1], resourceUrl)) {
                    // Return a blacklist match request blocked.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, DOMAIN_REGULAR_EXPRESSION_BLACKLIST, "${blacklistEntry[0]}\n${blacklistEntry[1]}", blacklistEntry[2])
                }
            }
        }

        // Only check the third-party blacklists if this is a third-party request.
        if (isThirdPartyRequest) {
            // Third-party blacklist.
            for (blacklistEntry in blocklist[THIRD_PARTY_BLACKLIST.toInt()]) {
                when (blacklistEntry.size) {
                    // There is one entry.
                    2 -> if (resourceUrl.contains(blacklistEntry[0])) {
                        // Return a blacklist match request blocked.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, THIRD_PARTY_BLACKLIST, blacklistEntry[0], blacklistEntry[1])
                    }

                    // There are two entries.
                    3 -> if (resourceUrl.contains(blacklistEntry[0]) && resourceUrl.contains(blacklistEntry[1])) {
                        // Return a blacklist match request blocked.
                        return arrayOf(
                            REQUEST_BLOCKED, resourceUrl, blocklistName, THIRD_PARTY_BLACKLIST, "${blacklistEntry[0]}\n${blacklistEntry[1]}\n", blacklistEntry[2])
                    }

                    // There are three entries.
                    4 -> if (resourceUrl.contains(blacklistEntry[0]) && resourceUrl.contains(blacklistEntry[1]) && resourceUrl.contains(blacklistEntry[2])) {
                        // Return a blacklist match request blocked.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, THIRD_PARTY_BLACKLIST, "${blacklistEntry[0]}\n${blacklistEntry[1]}\n${blacklistEntry[2]}", blacklistEntry[3])
                    }

                    // There are four entries.
                    5 -> if (resourceUrl.contains(blacklistEntry[0]) && resourceUrl.contains(blacklistEntry[1]) && resourceUrl.contains(blacklistEntry[2]) && resourceUrl.contains(blacklistEntry[3])) {
                        // Return a blacklist match request blocked.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, THIRD_PARTY_BLACKLIST, "${blacklistEntry[0]}\n${blacklistEntry[1]}\n${blacklistEntry[2]}\n${blacklistEntry[3]}",
                            blacklistEntry[4])
                    }
                }
            }

            // Third-party initial blacklist.
            for (blacklistEntry in blocklist[THIRD_PARTY_INITIAL_BLACKLIST.toInt()]) {
                when (blacklistEntry.size) {
                    // There is one entry.
                    2 -> if (resourceUrl.startsWith(blacklistEntry[0])) {
                        // Return a blacklist match request blocked.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, THIRD_PARTY_INITIAL_BLACKLIST, blacklistEntry[0], blacklistEntry[1])
                    }

                    // There are two entries.
                    3 -> if (resourceUrl.startsWith(blacklistEntry[0]) && resourceUrl.contains(blacklistEntry[1])) {
                        // Return a blacklist match request blocked.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, THIRD_PARTY_INITIAL_BLACKLIST, "${blacklistEntry[0]}\n${blacklistEntry[1]}", blacklistEntry[2])
                    }
                }
            }

            // Third-party domain blacklist.
            for (blacklistEntry in blocklist[THIRD_PARTY_DOMAIN_BLACKLIST.toInt()]) {
                when (blacklistEntry.size) {
                    // There is one entry.
                    3 -> if (currentDomain!!.endsWith(blacklistEntry[0]) && resourceUrl.contains(blacklistEntry[1])) {
                        // Return a blacklist match request blocked.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, THIRD_PARTY_DOMAIN_BLACKLIST, "${blacklistEntry[0]}\n${blacklistEntry[1]}", blacklistEntry[2])
                    }

                    // There are two entries.
                    4 -> if (currentDomain!!.endsWith(blacklistEntry[0]) && resourceUrl.contains(blacklistEntry[1]) && resourceUrl.contains(blacklistEntry[2])) {
                        // Return a blacklist match request blocked.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, THIRD_PARTY_DOMAIN_BLACKLIST, "${blacklistEntry[0]}\n${blacklistEntry[1]}\n${blacklistEntry[2]}", blacklistEntry[3])
                    }
                }
            }

            // Third-party domain initial blacklist.
            for (blacklistEntry in blocklist[THIRD_PARTY_DOMAIN_INITIAL_BLACKLIST.toInt()]) {
                when (blacklistEntry.size) {
                    // There is one entry.
                    3 -> if (currentDomain!!.endsWith(blacklistEntry[0]) && resourceUrl.startsWith(blacklistEntry[1])) {
                        // Return a blacklist match request blocked.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, THIRD_PARTY_DOMAIN_INITIAL_BLACKLIST, "${blacklistEntry[0]}\n${blacklistEntry[1]}\n", blacklistEntry[2])
                    }

                    // There are two entries.
                    4 -> if (currentDomain!!.endsWith(blacklistEntry[0]) && resourceUrl.startsWith(blacklistEntry[1]) && resourceUrl.contains(blacklistEntry[2])) {
                        // Return a blacklist match request blocked.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, THIRD_PARTY_DOMAIN_INITIAL_BLACKLIST, "${blacklistEntry[0]}\n${blacklistEntry[1]}\n${blacklistEntry[2]}", blacklistEntry[3])
                    }

                    // There are three entries.
                    5 -> if (currentDomain!!.endsWith(blacklistEntry[0]) && resourceUrl.startsWith(blacklistEntry[1]) && resourceUrl.contains(blacklistEntry[2]) && resourceUrl.contains(blacklistEntry[3])) {
                        // Return a blacklist match request blocked.
                        return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, THIRD_PARTY_DOMAIN_INITIAL_BLACKLIST,
                            "${blacklistEntry[0]}\n${blacklistEntry[1]}\n${blacklistEntry[2]}\n${blacklistEntry[3]}", blacklistEntry[4])
                    }
                }
            }

            // Third-party regular expression blacklist.
            for (blacklistEntry in blocklist[THIRD_PARTY_REGULAR_EXPRESSION_BLACKLIST.toInt()]) {
                if (Pattern.matches(blacklistEntry[0], resourceUrl)) {
                    // Return a blacklist match request blocked.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, THIRD_PARTY_REGULAR_EXPRESSION_BLACKLIST, blacklistEntry[0], blacklistEntry[1])
                }
            }

            // Third-party domain regular expression blacklist.
            for (blacklistEntry in blocklist[THIRD_PARTY_DOMAIN_REGULAR_EXPRESSION_BLACKLIST.toInt()]) {
                if (currentDomain!!.endsWith(blacklistEntry[0]) && Pattern.matches(blacklistEntry[1], resourceUrl)) {
                    // Return a blacklist match request blocked.
                    return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, THIRD_PARTY_DOMAIN_REGULAR_EXPRESSION_BLACKLIST, "${blacklistEntry[0]}\n${blacklistEntry[1]}", blacklistEntry[2])
                }
            }
        }

        // Regular expression blacklist.
        for (blacklistEntry in blocklist[REGULAR_EXPRESSION_BLACKLIST.toInt()]) {
            if (Pattern.matches(blacklistEntry[0], resourceUrl)) {
                // Return a blacklist match request blocked.
                return arrayOf(REQUEST_BLOCKED, resourceUrl, blocklistName, REGULAR_EXPRESSION_BLACKLIST, blacklistEntry[0], blacklistEntry[1])
            }
        }

        // Return a no match request default.
        return arrayOf(REQUEST_DEFAULT)
    }
}
