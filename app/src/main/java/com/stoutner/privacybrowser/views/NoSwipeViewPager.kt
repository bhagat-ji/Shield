/*
 * Copyright © 2019,2021-2022 Soren Stoutner <soren@stoutner.com>.
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

package com.stoutner.privacybrowser.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent

import androidx.viewpager.widget.ViewPager

import kotlin.jvm.JvmOverloads

class NoSwipeViewPager
@JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null) : ViewPager(context, attributeSet) {
    // It is necessary to override `performClick()` when overriding `onTouchEvent()`
    override fun performClick(): Boolean {
        // Run the default commands.
        super.performClick()

        // Do not consume the events.
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // `onTouchEvent()` requires calling `performClick()`.
        performClick()

        // Do not allow swiping.
        return false
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        // Do not allow swiping.
        return false
    }
}