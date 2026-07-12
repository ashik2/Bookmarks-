package com.example.ui

import android.net.Uri
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color

object BookmarkUtils {
    private val PaletteColors = listOf(
        Color(0xFFEF5350), // Red
        Color(0xFFEC407A), // Pink
        Color(0xFFAB47BC), // Purple
        Color(0xFF7E57C2), // Deep Purple
        Color(0xFF5C6BC0), // Indigo
        Color(0xFF42A5F5), // Blue
        Color(0xFF26C6DA), // Cyan
        Color(0xFF26A69A), // Teal
        Color(0xFF66BB6A), // Green
        Color(0xFF9CCC65), // Light Green
        Color(0xFFFFB74D), // Orange
        Color(0xFFFF7043), // Deep Orange
        Color(0xFF8D6E63), // Brown
    )

    fun getColorForTitle(title: String): Color {
        if (title.isEmpty()) return PaletteColors[0]
        val hash = title.hashCode()
        val index = kotlin.math.abs(hash) % PaletteColors.size
        return PaletteColors[index]
    }

    fun getCleanName(url: String): String {
        try {
            val uri = Uri.parse(url)
            var host = uri.host ?: ""
            if (host.startsWith("www.")) {
                host = host.substring(4)
            }
            // Remove common TLDs
            val tlds = listOf(".com", ".org", ".net", ".co.uk", ".io", ".edu", ".gov", ".app", ".dev", ".me")
            for (tld in tlds) {
                if (host.endsWith(tld)) {
                    host = host.substring(0, host.length - tld.length)
                    break
                }
            }
            if (host.isNotEmpty()) {
                return host.split('-', '.').joinToString(" ") { segment ->
                    segment.replaceFirstChar { char -> char.uppercase() }
                }
            }
        } catch (e: Exception) {
            // Fallback
        }
        // Fallback: extract domain-like string using simple regex
        val cleaned = url.replace(Regex("https?://(www\\.)?"), "")
        val slashIndex = cleaned.indexOf('/')
        val domain = if (slashIndex != -1) cleaned.substring(0, slashIndex) else cleaned
        return domain.ifEmpty { url }
    }

    fun extractUrlAndTitle(sharedText: String): Pair<String, String>? {
        val trimmedText = sharedText.trim()
        val lowerText = trimmedText.lowercase()
        if (lowerText.startsWith("geo:") || lowerText.startsWith("waze://")) {
            val title = if (lowerText.startsWith("geo:")) "Google Maps Location" else "Waze Location"
            return Pair(trimmedText, title)
        }

        // Regular expression to find URLs
        val urlRegex = Regex("(https?://[\\w-]+(\\.[\\w-]+)+(/\\S*)?)")
        val match = urlRegex.find(sharedText)
        if (match != null) {
            val url = match.value
            val beforeUrl = sharedText.substring(0, match.range.first).trim()
            var title = beforeUrl.trim()
            // Remove typical trailing connectors or separators
            if (title.endsWith(":") || title.endsWith("-") || title.endsWith("|")) {
                title = title.substring(0, title.length - 1).trim()
            }
            return Pair(url, title)
        }
        return null
    }

    fun formatUrl(url: String): String {
        var trimmed = url.trim()
        if (trimmed.isEmpty()) return ""
        val lower = trimmed.lowercase()
        if (lower.startsWith("geo:") || lower.startsWith("waze://")) {
            return trimmed
        }
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://$trimmed"
        }
        return trimmed
    }

    fun getFaviconUrl(url: String): String {
        val formatted = formatUrl(url)
        if (isGoogleMapsUrl(formatted)) {
            return "https://www.google.com/s2/favicons?sz=128&domain=maps.google.com"
        }
        if (isWazeUrl(formatted)) {
            return "https://www.google.com/s2/favicons?sz=128&domain=waze.com"
        }
        try {
            val uri = Uri.parse(formatted)
            val host = uri.host
            if (!host.isNullOrEmpty()) {
                return "https://www.google.com/s2/favicons?sz=128&domain=$host"
            }
        } catch (e: Exception) {
            // fallback
        }
        return "https://www.google.com/s2/favicons?sz=128&domain=$formatted"
    }

    fun isGoogleMapsUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("maps.google") || 
               lower.contains("google.com/maps") || 
               lower.contains("maps.app.goo.gl") ||
               lower.startsWith("geo:")
    }

    fun isWazeUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("waze.com") || lower.startsWith("waze://")
    }

    fun openSpecificMapApp(context: Context, url: String): Boolean {
        val formattedUrl = formatUrl(url)
        val uri = Uri.parse(formattedUrl)
        
        if (isGoogleMapsUrl(formattedUrl)) {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                // Google Maps app not installed
            }
        } else if (isWazeUrl(formattedUrl)) {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.waze")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                // Waze app not installed
            }
        }
        return false
    }
}
