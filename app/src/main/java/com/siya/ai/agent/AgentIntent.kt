package com.siya.ai.agent

/** Structured intents understood by Siya's offline action router. */
sealed interface AgentIntent {
    data class LaunchApp(val packageName: String) : AgentIntent
    data class WebSearch(val query: String) : AgentIntent
    data class OpenUrl(val url: String) : AgentIntent
    data class ShareText(val text: String) : AgentIntent
    data object MediaPlayPause : AgentIntent
    data object MediaNext : AgentIntent
    data object MediaPrevious : AgentIntent
    data class Call(val numberOrName: String) : AgentIntent
    data class SendMessage(val recipient: String, val text: String) : AgentIntent
}

data class RoutedIntent(
    val intent: AgentIntent,
    val confirmationRequired: Boolean,
)
