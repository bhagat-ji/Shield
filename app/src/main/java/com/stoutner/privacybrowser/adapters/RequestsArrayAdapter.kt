/* SPDX-License-Identifier: GPL-3.0-or-later
 * SPDX-FileCopyrightText: 2018-2023, 2025-2026 Soren Stoutner <soren@stoutner.com>
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

package com.stoutner.privacybrowser.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.dataclasses.RequestDataClass
import com.stoutner.privacybrowser.dataclasses.RequestDisposition

// `0` is the `textViewResourceId`, which is unused in this implementation.
class RequestsArrayAdapter(context: Context, resourceRequestsDataClassList: MutableList<RequestDataClass>) : ArrayAdapter<RequestDataClass>(context, 0, resourceRequestsDataClassList) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Copy the input view to a new view.
        var newView = convertView

        // Inflate the new view if it is null.
        if (newView == null) {
            newView = LayoutInflater.from(context).inflate(R.layout.requests_item_linearlayout, parent, false)
        }

        // Get handles for the views.
        val linearLayout = newView!!.findViewById<LinearLayout>(R.id.request_item_linearlayout)
        val dispositionTextView = newView.findViewById<TextView>(R.id.request_item_disposition)
        val urlTextView = newView.findViewById<TextView>(R.id.request_item_url)

        // Get the request data class.
        val requestDataClass = getItem(position)!!

        // The ID is one greater than the position because it is 0 based.
        val id = position + 1

        // Set the action text and the background color.
        when (requestDataClass.disposition) {
            RequestDisposition.Default -> {
                // Set the disposition text.
                dispositionTextView.text = context.resources.getString(R.string.request_allowed, id)

                // Set the background color.
                linearLayout.setBackgroundColor(context.getColor(R.color.transparent))
            }

            RequestDisposition.Allowed -> {
                // Set the disposition text.
                dispositionTextView.text = context.resources.getString(R.string.request_allowed, id)

                // Set the background color.
                linearLayout.setBackgroundColor(context.getColor(R.color.requests_blue_background))
            }

            RequestDisposition.Blocked -> {
                // Set the disposition text.
                dispositionTextView.text = context.resources.getString(R.string.request_blocked, id)

                // Set the background color.
                linearLayout.setBackgroundColor(context.getColor(R.color.red_background))
            }

            RequestDisposition.ThirdParty -> {
                // Set the disposition text.
                dispositionTextView.text = context.resources.getString(R.string.request_blocked, id)

                // Set the background color.
                linearLayout.setBackgroundColor(context.getColor(R.color.yellow_background))
            }
        }

        // Set the request URL text.
        urlTextView.text = requestDataClass.requestUrlString

        // Return the modified view.
        return newView
    }
}
