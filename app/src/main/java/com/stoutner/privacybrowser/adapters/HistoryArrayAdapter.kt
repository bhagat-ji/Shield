/*
 * Copyright © 2016-2019,2021-2022 Soren Stoutner <soren@stoutner.com>.
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

package com.stoutner.privacybrowser.adapters

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.dataclasses.History

import java.util.ArrayList

class HistoryArrayAdapter(context: Context, historyArrayList: ArrayList<History>, private val currentPageId: Int) : ArrayAdapter<History>(context, 0, historyArrayList) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Initialize a populated view from the convert view.
        var populatedView = convertView

        // Inflate the view if it is null.
        if (populatedView == null) {
            populatedView = LayoutInflater.from(context).inflate(R.layout.url_history_item_linearlayout, parent, false)
        }

        // Get handles for the views.
        val favoriteIconImageView = populatedView!!.findViewById<ImageView>(R.id.history_favorite_icon_imageview)
        val urlTextView = populatedView.findViewById<TextView>(R.id.history_url_textview)

        // Get the URL history for this position.
        val history = getItem(position)!!

        // Populate the views.
        favoriteIconImageView.setImageBitmap(history.favoriteIcon)
        urlTextView.text = history.url

        // Set the URL text for the current page to be bold.
        if (position == currentPageId) {
            urlTextView.typeface = Typeface.DEFAULT_BOLD
        } else {  // Set the default typeface for all the other entries.
            urlTextView.typeface = Typeface.DEFAULT
        }

        // Return the populated view.
        return populatedView
    }
}