package com.altusix.slate.widgets.productivity

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object FaviconHelper {

    private val memCache = ConcurrentHashMap<String, Bitmap>()
    private val activeFetches = ConcurrentHashMap.newKeySet<String>()

    fun getCachedFavicon(context: Context, domain: String): Bitmap? {
        if (domain.isBlank()) return null
        val cleanDomain = sanitizeDomain(domain)

        // 1. Fast Memory Cache
        memCache[cleanDomain]?.let { return it }

        // 2. Persistent Disk Cache
        val file = File(context.cacheDir, "favicons/$cleanDomain.png")
        if (file.exists() && file.length() > 0) {
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            if (bmp != null) {
                memCache[cleanDomain] = bmp
                return bmp
            }
        }
        return null
    }

    fun fetchFaviconAsync(context: Context, domain: String) {
        if (domain.isBlank()) return
        val cleanDomain = sanitizeDomain(domain)

        // Prevent redundant simultaneous requests for the same domain
        if (activeFetches.contains(cleanDomain)) return

        val file = File(context.cacheDir, "favicons/$cleanDomain.png")
        if (file.exists() && file.length() > 0) return

        activeFetches.add(cleanDomain)

        Thread {
            try {
                val cacheDir = File(context.cacheDir, "favicons")
                cacheDir.mkdirs()

                // Endpoints prioritized by resolution (Google Favicon V2 -> DuckDuckGo -> S2 Legacy)
                val candidateUrls = listOf(
                    "https://t1.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://$cleanDomain&size=128",
                    "https://icons.duckduckgo.com/ip3/$cleanDomain.ico",
                    "https://www.google.com/s2/favicons?domain=$cleanDomain&sz=128"
                )

                var downloadedBmp: Bitmap? = null

                for (urlStr in candidateUrls) {
                    try {
                        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 3000
                            readTimeout = 3000
                            instanceFollowRedirects = true
                            setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
                        }

                        if (conn.responseCode == 200) {
                            val bytes = conn.inputStream.use { it.readBytes() }
                            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            // Discard transparent/empty 1x1 error tracking pixels
                            if (decoded != null && decoded.width > 1 && decoded.height > 1) {
                                FileOutputStream(file).use { out ->
                                    out.write(bytes)
                                }
                                downloadedBmp = decoded
                                break
                            }
                        }
                    } catch (_: Exception) {
                        continue
                    }
                }

                if (downloadedBmp != null) {
                    memCache[cleanDomain] = downloadedBmp

                    // Crucial: Invalidate and refresh widgets so the downloaded logo displays
                    Handler(Looper.getMainLooper()).post {
                        updateAllProductivityWidgets(context.applicationContext)
                    }
                }
            } catch (_: Exception) {
            } finally {
                activeFetches.remove(cleanDomain)
            }
        }.start()
    }

    private fun sanitizeDomain(domain: String): String {
        return domain.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("www.")
            .split("/")[0]
            .replace(":", "_")
    }
}