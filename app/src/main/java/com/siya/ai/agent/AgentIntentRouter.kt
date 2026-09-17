package com.siya.ai.agent

/**
 * Deterministic, offline-first command router. It deliberately avoids guessing
 * destructive or privacy-sensitive actions; those become explicit confirmations.
 */
object AgentIntentRouter {
    private val appAliases = mapOf(
        "youtube" to "com.google.android.youtube",
        "व्हाट्सएप" to "com.whatsapp",
        "whatsapp" to "com.whatsapp",
        "chrome" to "com.android.chrome",
        "instagram" to "com.instagram.android",
        "settings" to "com.android.settings",
    )

    fun route(raw: String): RoutedIntent? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        val lower = text.lowercase()

        when {
            lower == "play" || lower.contains("play music") || lower.contains("गाना चलाओ") ->
                return RoutedIntent(AgentIntent.MediaPlayPause, false)
            lower.contains("next song") || lower.contains("अगला गाना") ->
                return RoutedIntent(AgentIntent.MediaNext, false)
            lower.contains("previous song") || lower.contains("पिछला गाना") ->
                return RoutedIntent(AgentIntent.MediaPrevious, false)
            lower.startsWith("search ") || lower.startsWith("google search ") || lower.startsWith("सर्च ") -> {
                val query = text.substringAfter(' ', "").removePrefix("google ").removePrefix("search ").removePrefix("सर्च ").trim()
                if (query.isNotEmpty()) return RoutedIntent(AgentIntent.WebSearch(query), false)
            }
            lower.startsWith("open http://") || lower.startsWith("open https://") -> {
                val url = text.substringAfter(' ').trim()
                return RoutedIntent(AgentIntent.OpenUrl(url), false)
            }
            lower.startsWith("open ") || lower.startsWith("खोलो ") -> {
                val target = text.substringAfter(' ').trim().lowercase()
                appAliases[target]?.let { return RoutedIntent(AgentIntent.LaunchApp(it), false) }
            }
            lower.startsWith("call ") || lower.startsWith("कॉल ") -> {
                val target = text.substringAfter(' ').trim()
                if (target.isNotEmpty()) return RoutedIntent(AgentIntent.Call(target), true)
            }
            lower.startsWith("message ") || lower.startsWith("मैसेज ") -> {
                val body = text.substringAfter(' ').trim()
                val separator = body.indexOf(':')
                if (separator > 0 && separator < body.lastIndex) {
                    val recipient = body.substring(0, separator).trim()
                    val message = body.substring(separator + 1).trim()
                    if (recipient.isNotEmpty() && message.isNotEmpty()) {
                        return RoutedIntent(AgentIntent.SendMessage(recipient, message), true)
                    }
                }
            }
        }
        return null
    }
}
