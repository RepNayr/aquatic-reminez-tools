package com.repp.aquaticremineztools.data.manga.source

import android.content.Context
import android.webkit.CookieManager
import com.repp.aquaticremineztools.core.security.CrotPediaSessionStore
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class CrotPediaCookieJar(context: Context) : CookieJar {

    private val appContext = context.applicationContext

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (!CrotPediaSessionStore.isAllowedCookieHost(url.host)) {
            return
        }

        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)

            val urlString = url.toString()
            for (cookie in cookies) {
                val cookieString = "${cookie.name}=${cookie.value}; Domain=${cookie.domain}; Path=${cookie.path}"
                cookieManager.setCookie(urlString, cookieString)
            }
            cookieManager.flush()

            // Also capture all cookies for crotpedia.net from CookieManager and save to session store
            val allCookies = cookieManager.getCookie(urlString)
            if (!allCookies.isNullOrBlank()) {
                CrotPediaSessionStore.saveSession(appContext, allCookies)
            }
        } catch (_: Exception) {}
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        if (!CrotPediaSessionStore.isAllowedCookieHost(url.host)) {
            return Collections.emptyList()
        }

        val cookieList = mutableListOf<Cookie>()
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)

            val urlString = url.toString()
            val cookiesHeader = cookieManager.getCookie(urlString)
                ?: CrotPediaSessionStore.getSavedCookies(appContext)

            if (!cookiesHeader.isNullOrBlank()) {
                val cookiePairs = cookiesHeader.split(";")
                for (pair in cookiePairs) {
                    val parts = pair.trim().split("=", limit = 2)
                    if (parts.size == 2) {
                        val name = parts[0].trim()
                        val value = parts[1].trim()
                        if (name.isNotEmpty()) {
                            val builder = Cookie.Builder()
                                .name(name)
                                .value(value)
                                .domain(url.host)
                                .path("/")

                            if (url.isHttps) {
                                builder.secure()
                            }
                            cookieList.add(builder.build())
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return cookieList
    }
}
