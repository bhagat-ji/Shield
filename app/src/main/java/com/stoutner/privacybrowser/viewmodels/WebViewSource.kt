/*
 * Copyright © 2020-2021 Soren Stoutner <soren@stoutner.com>.
 *
 * This file is part of Privacy Browser <https://www.stoutner.com/privacy-browser>.
 *
 * Privacy Browser is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Privacy Browser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Privacy Browser.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.stoutner.privacybrowser.viewmodels

import android.content.ContentResolver
import android.text.SpannableStringBuilder

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import com.stoutner.privacybrowser.backgroundtasks.GetSourceBackgroundTask

import java.net.Proxy
import java.util.concurrent.ExecutorService

class WebViewSource(private val urlString: String, private val userAgent: String, private val localeString: String, private val proxy: Proxy, private val contentResolver: ContentResolver,
                    private val executorService: ExecutorService): ViewModel() {
    // Initialize the mutable live data variables.
    private val mutableLiveDataSourceStringArray = MutableLiveData<Array<SpannableStringBuilder>>()
    private val mutableLiveDataErrorString = MutableLiveData<String>()

    // Initialize the view model.
    init {
        // Instantiate the get source background task class.
        val getSourceBackgroundTask = GetSourceBackgroundTask()

        // Get the source.
        executorService.execute { mutableLiveDataSourceStringArray.postValue(getSourceBackgroundTask.acquire(urlString, userAgent, localeString, proxy, contentResolver, this,
            false)) }
    }

    // The source observer.
    fun observeSource(): LiveData<Array<SpannableStringBuilder>> {
        // Return the source to the activity.
        return mutableLiveDataSourceStringArray
    }

    // The error observer.
    fun observeErrors(): LiveData<String> {
        // Return any errors to the activity.
        return mutableLiveDataErrorString
    }

    // The interface for returning the error from the background task
    fun returnError(errorString: String) {
        // Update the mutable live data error string.
        mutableLiveDataErrorString.postValue(errorString)
    }

    // The workhorse that gets the source.
    fun updateSource(urlString: String, ignoreSslErrors: Boolean) {
        // Reset the mutable live data error string.  This prevents the snackbar from displaying later if the activity restarts.
        mutableLiveDataErrorString.postValue("")

        // Instantiate the get source background task class.
        val getSourceBackgroundTask = GetSourceBackgroundTask()

        // Get the source.
        executorService.execute { mutableLiveDataSourceStringArray.postValue(getSourceBackgroundTask.acquire(urlString, userAgent, localeString, proxy, contentResolver, this,
            ignoreSslErrors)) }
    }
}