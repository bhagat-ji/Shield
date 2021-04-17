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

package com.stoutner.privacybrowser.viewmodelfactories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import java.net.Proxy
import java.util.concurrent.ExecutorService

class WebViewSourceFactory (private val urlString: String, private val userAgent: String, private val localeString: String, private val proxy: Proxy,
                            private val executorService: ExecutorService): ViewModelProvider.Factory {
    // Override the create function in order to add the provided arguments.
    override fun <T: ViewModel?> create(modelClass: Class<T>): T {
        // Return a new instance of the model class with the provided arguments.
        return modelClass.getConstructor(String::class.java, String::class.java, Boolean::class.java, String::class.java, Proxy::class.java, ExecutorService::class.java)
                .newInstance(urlString, userAgent, localeString, proxy, executorService)
    }
}