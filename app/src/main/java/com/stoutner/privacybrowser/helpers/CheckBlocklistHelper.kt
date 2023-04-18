/*
 * Copyright 2018-2019,2021-2023 Soren Stoutner <soren@stoutner.com>.
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
import java.util.ArrayList
import java.util.regex.Pattern

// Define the request disposition options.
const val REQUEST_DEFAULT = "0"
const val REQUEST_ALLOWED = "1"
const val REQUEST_THIRD_PARTY = "2"
const val REQUEST_BLOCKED = "3"

class CheckBlocklistHelper {
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
