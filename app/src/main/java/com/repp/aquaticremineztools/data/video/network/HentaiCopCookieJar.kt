package com.repp.aquaticremineztools.data.video.network

import android.content.Context
import android.webkit.CookieManager
import com.repp.aquaticremineztools.core.security.HentaiCopSessionStore
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class HentaiCopCookieJar(context: Context? = null) : CookieJar {

    private val appContext: Context? = context?.applicationContext
    private val inMemoryCookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (!HentaiCopSessionStore.isAllowedCookieHost(url.host)) {
            return
        }

        // Store in memory
        val existing = inMemoryCookieStore.getOrPut(url.host) { Collections.synchronizedList(mutableListOf()) }
        for (cookie in cookies) {
            existing.removeAll { it.name == cookie.name }
            existing.add(cookie)
        }

        // Persist to CookieManager and HentaiCopSessionStore if context is available
        val ctx = appContext ?: return
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)

            val urlString = url.toString()
            for (cookie in cookies) {
                val cookieString = "${cookie.name}=${cookie.value}; Domain=${cookie.domain}; Path=${cookie.path}"
                cookieManager.setCookie(urlString, cookieString)
            }
            cookieManager.flush()

            val allCookies = cookieManager.getCookie(urlString)
            if (!allCookies.isNullOrBlank()) {
                HentaiCopSessionStore.saveSession(ctx, allCookies)
            }
        } catch (_: Exception) {}
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        if (!HentaiCopSessionStore.isAllowedCookieHost(url.host)) {
            return Collections.emptyList()
        }

        val resultCookies = mutableListOf<Cookie>()

        // 1. Load from in-memory store first
        val memoryCookies = inMemoryCookieStore[url.host]
        if (!memoryCookies.isNullOrEmpty()) {
            val now = System.currentTimeMillis()
            val validMemoryCookies = memoryCookies.filter { it.expiresAt > now }
            resultCookies.addAll(validMemoryCookies)
        }

        // 2. Load from CookieManager / HentaiCopSessionStore if memory cookies empty and context exists
        if (resultCookies.isEmpty() && appContext != null) {
            try {
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)

                val urlString = url.toString()
                val managerCookies = cookieManager.getCookie(urlString)
                val cookiesHeader = if (!managerCookies.isNullOrBlank()) {
                    managerCookies
                } else {
                    HentaiCopSessionStore.getSavedCookies(appContext)
                }

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
                                resultCookies.add(builder.build())
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        return resultCookies
    }
}
