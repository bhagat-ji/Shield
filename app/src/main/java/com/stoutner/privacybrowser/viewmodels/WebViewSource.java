/*
 * Copyright © 2020 Soren Stoutner <soren@stoutner.com>.
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

package com.stoutner.privacybrowser.viewmodels;

import android.text.SpannableStringBuilder;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.stoutner.privacybrowser.backgroundtasks.GetSourceBackgroundTask;

import java.net.Proxy;
import java.util.concurrent.ExecutorService;

public class WebViewSource extends ViewModel {
    // Initialize the mutable live data variables.
    private final MutableLiveData<SpannableStringBuilder[]> mutableLiveDataSourceStringArray = new MutableLiveData<>();
    private final MutableLiveData<String> mutableLiveDataErrorString = new MutableLiveData<>();

    // Define the class variables.
    private final String userAgent;
    private final boolean doNotTrack;
    private final String localeString;
    private final Proxy proxy;
    private final ExecutorService executorService;

    // The public constructor.
    public WebViewSource(@Nullable String urlString, String userAgent, boolean doNotTrack, String localeString, Proxy proxy, ExecutorService executorService) {
        // Store the class variables.
        this.userAgent = userAgent;
        this.doNotTrack = doNotTrack;
        this.localeString = localeString;
        this.proxy = proxy;
        this.executorService = executorService;

        // Get the source.
        updateSource(urlString);
    }

    // The source observer.
    public LiveData<SpannableStringBuilder[]> observeSource() {
        // Return the source to the activity.
        return mutableLiveDataSourceStringArray;
    }

    // The error observer.
    public LiveData<String> observeErrors() {
        // Return any errors to the activity.
        return mutableLiveDataErrorString;
    }

    // The interface for returning the error from the background task
    public void returnError(String errorString) {
        // Update the mutable live data error string.
        mutableLiveDataErrorString.postValue(errorString);
    }

    // The workhorse that gets the source.
    public void updateSource(String urlString) {
        // Reset the mutable live data error string.  This prevents the snackbar it from displaying a later if the activity restarts.
        mutableLiveDataErrorString.postValue("");

        // Instantiate the get source background task class.
        GetSourceBackgroundTask getSourceBackgroundTask = new GetSourceBackgroundTask();

        // Get the source.
        executorService.execute(() -> mutableLiveDataSourceStringArray.postValue(getSourceBackgroundTask.acquire(urlString, userAgent, doNotTrack, localeString, proxy, this)));
    }
}