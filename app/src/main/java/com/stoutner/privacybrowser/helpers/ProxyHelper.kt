/*
 * Copyright © 2016-2021 Soren Stoutner <soren@stoutner.com>.
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

package com.stoutner.privacybrowser.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.util.ArrayMap
import android.view.View

import androidx.preference.PreferenceManager
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature

import com.stoutner.privacybrowser.R
import com.stoutner.privacybrowser.activities.MainWebViewActivity

import com.google.android.material.snackbar.Snackbar

import java.lang.Exception
import java.lang.IllegalArgumentException
import java.lang.reflect.InvocationTargetException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.SocketAddress

class ProxyHelper {
    companion object {
        // Define the public companion object constants.  These can be moved to public class constants once the entire project has migrated to Kotlin.
        const val NONE = "None"
        const val TOR = "Tor"
        const val I2P = "I2P"
        const val CUSTOM = "Custom"
        const val ORBOT_STATUS_ON = "ON"
    }

    fun setProxy(context: Context, activityView: View, proxyMode: String) {
        // Initialize the proxy host and port strings.
        var proxyHost = "0"
        var proxyPort = "0"

        // Create a proxy config builder.
        val proxyConfigBuilder = ProxyConfig.Builder()

        // Run the commands that correlate to the proxy mode.
        when (proxyMode) {
            NONE -> {
                // Clear the proxy values.
                System.clearProperty("proxyHost")
                System.clearProperty("proxyPort")
            }

            TOR -> {
                // Update the proxy host and port strings.  These can be removed once the minimum API >= 21.
                proxyHost = "localhost"
                proxyPort = "8118"

                // Set the proxy values.  These can be removed once the minimum API >= 21.
                System.setProperty("proxyHost", proxyHost)
                System.setProperty("proxyPort", proxyPort)

                // Add the proxy to the builder.  The proxy config builder can use a SOCKS proxy.
                proxyConfigBuilder.addProxyRule("socks://localhost:9050")

                // Ask Orbot to connect if its current status is not `"ON"`.
                if (MainWebViewActivity.orbotStatus != ORBOT_STATUS_ON) {
                    // Create an intent to request Orbot to start.
                    val orbotIntent = Intent("org.torproject.android.intent.action.START")

                    // Send the intent to the Orbot package.
                    orbotIntent.setPackage("org.torproject.android")

                    // Request a status response be sent back to this package.
                    orbotIntent.putExtra("org.torproject.android.intent.extra.PACKAGE_NAME", context.packageName)

                    // Make it so.
                    context.sendBroadcast(orbotIntent)
                }
            }

            I2P -> {
                // Update the proxy host and port strings.  These can be removed once the minimum API >= 21.
                proxyHost = "localhost"
                proxyPort = "4444"

                // Set the proxy values.  These can be removed once the minimum API >= 21.
                System.setProperty("proxyHost", proxyHost)
                System.setProperty("proxyPort", proxyPort)

                // Add the proxy to the builder.
                proxyConfigBuilder.addProxyRule("http://localhost:4444")
            }

            CUSTOM -> {
                // Get a handle for the shared preferences.
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

                // Get the custom proxy URL string.
                val customProxyUrlString = sharedPreferences.getString(context.getString(R.string.proxy_custom_url_key), context.getString(R.string.proxy_custom_url_default_value))

                // Parse the custom proxy URL.
                try {
                    // Convert the custom proxy URL string to a URI.
                    val customProxyUri = Uri.parse(customProxyUrlString)

                    // Get the proxy host and port strings from the shared preferences.  These can be removed once the minimum API >= 21.
                    proxyHost = customProxyUri.host!!
                    proxyPort = customProxyUri.port.toString()

                    // Set the proxy values.  These can be removed once the minimum API >= 21.
                    System.setProperty("proxyHost", proxyHost)
                    System.setProperty("proxyPort", proxyPort)

                    // Add the proxy to the builder.
                    proxyConfigBuilder.addProxyRule(customProxyUrlString!!)
                } catch (exception: Exception) {  // The custom proxy URL is invalid.
                    // Display a Snackbar.
                    Snackbar.make(activityView, R.string.custom_proxy_invalid, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        // Apply the proxy settings
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {  // The fancy new proxy config can be used because the API >= 21.
            // Convert the proxy config builder into a proxy config.
            val proxyConfig = proxyConfigBuilder.build()

            // Get the proxy controller.
            val proxyController = ProxyController.getInstance()

            // Apply the proxy settings.
            if (proxyMode == NONE) {  // Remove the proxy.  A default executor and runnable are used.
                proxyController.clearProxyOverride({}, {})
            } else {  // Apply the proxy.
                try {
                    // Apply the proxy.  A default executor and runnable are used.
                    proxyController.setProxyOverride(proxyConfig, {}, {})
                } catch (exception: IllegalArgumentException) {  // The proxy config is invalid.
                    // Display a Snackbar.
                    Snackbar.make(activityView, R.string.custom_proxy_invalid, Snackbar.LENGTH_LONG).show()
                }
            }
        } else {  // The old proxy method must be used, either because an old WebView is installed or because the API == 19;
            // Get a handle for the shared preferences.
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

            // Check to make sure a SOCKS proxy is not selected.
            if ((proxyMode == CUSTOM) &&
                sharedPreferences.getString(context.getString(R.string.proxy_custom_url_key), context.getString(R.string.proxy_custom_url_default_value))!!.startsWith("socks://")) {
                // Display a Snackbar.
                Snackbar.make(activityView, R.string.socks_proxies_do_not_work_on_kitkat, Snackbar.LENGTH_LONG).show()
            } else {  // Use reflection to apply the new proxy values.
                try {
                    // Get the application and APK classes.
                    val applicationClass = Class.forName("android.app.Application")

                    // Suppress the lint warning that reflection may not always work in the future and on all devices.
                    @SuppressLint("PrivateApi") val loadedApkClass = Class.forName("android.app.LoadedApk")

                    // Get the declared fields.  Suppress the lint that it is discouraged to access private APIs.
                    @SuppressLint("DiscouragedPrivateApi") val methodLoadedApkField = applicationClass.getDeclaredField("mLoadedApk")
                    @SuppressLint("DiscouragedPrivateApi") val methodReceiversField = loadedApkClass.getDeclaredField("mReceivers")

                    // Allow the values to be changed.
                    methodLoadedApkField.isAccessible = true
                    methodReceiversField.isAccessible = true

                    // Get the APK object.
                    val methodLoadedApkObject = methodLoadedApkField[context]

                    // Get an array map of the receivers.
                    val receivers = methodReceiversField[methodLoadedApkObject] as ArrayMap<*, *>

                    // Set the proxy.
                    for (receiverMap in receivers.values) {
                        for (receiver in (receiverMap as ArrayMap<*, *>).keys) {
                            // Get the receiver class.
                            // `Class<*>`, which is an `unbounded wildcard parameterized type`, must be used instead of `Class`, which is a `raw type`.  Otherwise, `receiveClass.getDeclaredMethod()` is unhappy.
                            val receiverClass: Class<*> = receiver.javaClass

                            // Apply the new proxy settings to any classes whose names contain `ProxyChangeListener`.
                            if (receiverClass.name.contains("ProxyChangeListener")) {
                                // Get the `onReceive` method from the class.
                                val onReceiveMethod = receiverClass.getDeclaredMethod("onReceive", Context::class.java, Intent::class.java)

                                // Create a proxy change intent.
                                val proxyChangeIntent = Intent(android.net.Proxy.PROXY_CHANGE_ACTION)

                                // Set the proxy for API >= 21.
                                if (Build.VERSION.SDK_INT >= 21) {
                                    // Get the proxy info class.
                                    val proxyInfoClass = Class.forName("android.net.ProxyInfo")

                                    // Get the build direct proxy method from the proxy info class.
                                    val buildDirectProxyMethod = proxyInfoClass.getMethod("buildDirectProxy", String::class.java, Integer.TYPE)

                                    // Populate a proxy info object with the new proxy information.
                                    val proxyInfoObject = buildDirectProxyMethod.invoke(proxyInfoClass, proxyHost, Integer.valueOf(proxyPort))

                                    // Add the proxy info object into the proxy change intent.
                                    proxyChangeIntent.putExtra("proxy", proxyInfoObject as Parcelable)
                                }

                                // Pass the proxy change intent to the `onReceive` method of the receiver class.
                                onReceiveMethod.invoke(receiver, context, proxyChangeIntent)
                            }
                        }
                    }
                } catch (exception: ClassNotFoundException) {
                    // Do nothing.
                } catch (exception: NoSuchFieldException) {
                    // Do nothing.
                } catch (exception: IllegalAccessException) {
                    // Do nothing.
                } catch (exception: NoSuchMethodException) {
                    // Do nothing.
                } catch (exception: InvocationTargetException) {
                    // Do nothing.
                }
            }
        }
    }

    fun getCurrentProxy(context: Context): Proxy {
        // Get the proxy according to the current proxy mode.
        val proxy = when (MainWebViewActivity.proxyMode) {
            TOR -> if (Build.VERSION.SDK_INT >= 21) {
                // Use localhost port 9050 as the socket address.
                val torSocketAddress: SocketAddress = InetSocketAddress.createUnresolved("localhost", 9050)

                // Create a SOCKS proxy.
                Proxy(Proxy.Type.SOCKS, torSocketAddress)
            } else {
                // Use localhost port 8118 as the socket address.
                val oldTorSocketAddress: SocketAddress = InetSocketAddress.createUnresolved("localhost", 8118)

                // Create an HTTP proxy.
                Proxy(Proxy.Type.HTTP, oldTorSocketAddress)
            }

            I2P -> {
                // Use localhost port 4444 as the socket address.
                val i2pSocketAddress: SocketAddress = InetSocketAddress.createUnresolved("localhost", 4444)

                // Create an HTTP proxy.
                Proxy(Proxy.Type.HTTP, i2pSocketAddress)
            }

            CUSTOM -> {
                // Get the shared preferences.
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

                // Get the custom proxy URL string.
                val customProxyUrlString = sharedPreferences.getString(context.getString(R.string.proxy_custom_url_key), context.getString(R.string.proxy_custom_url_default_value))

                // Parse the custom proxy URL.
                try {
                    // Convert the custom proxy URL string to a URI.
                    val customProxyUri = Uri.parse(customProxyUrlString)

                    // Get the custom socket address.
                    val customSocketAddress: SocketAddress = InetSocketAddress.createUnresolved(customProxyUri.host, customProxyUri.port)

                    // Get the custom proxy scheme.
                    val customProxyScheme = customProxyUri.scheme

                    // Create a proxy according to the scheme.
                    if (customProxyScheme != null && customProxyScheme.startsWith("socks")) {  // A SOCKS proxy is specified.
                        // Create a SOCKS proxy.
                        Proxy(Proxy.Type.SOCKS, customSocketAddress)
                    } else {  // A SOCKS proxy is not specified.
                        // Create an HTTP proxy.
                        Proxy(Proxy.Type.HTTP, customSocketAddress)
                    }
                } catch (exception: Exception) {  // The custom proxy cannot be parsed.
                    // Disable the proxy.
                    Proxy.NO_PROXY
                }
            }

            else -> {
                // Create a direct proxy.
                Proxy.NO_PROXY
            }
        }

        // Return the proxy.
        return proxy
    }
}